/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.entities.internal;

import org.phenotips.Constants;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Base class for implementing specific entity managers. This can be used as an almost complete base implementation,
 * since the only unimplemented method is {@link #getDataSpace()}, however, several requirements are imposed on the
 * subclasses to fully work:
 * <ul>
 * <li>the concrete implementation must have its {@code @Named} annotation set to reference the XClass used for the
 * primary entity, the one that's also returned by the {@link PrimaryEntity#getType()}</li>
 * <li>the {@code <E>} parameter must be set to the concrete class managed</li>
 * <li>the class used for {@code <E>} must have a constructor that takes as an argument a {@link DocumentModelBridge} or
 * a {@link XWikiDocument} argument</li>
 * <li>all documents {@link #create() created} by this manager will have the name in the format
 * {@code <PREFIX><7 digit sequential number>}, unless {@link #getNextDocument()} is overridden, where:
 * <ul>
 * <li>the prefix is computed from the uppercase letters of the XClass name, excluding {@code Class}, e.g. for
 * {@code PhenoTips.DiseaseStudyClass} the prefix will be {@code DS}; override {@link #getIdPrefix()} to change this
 * behavior</li>
 * <li>the number is a 0-padded 7 digit number, starting at {@code 0000001} and automatically incremented for each new
 * entity created</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @param <E> the type of entities handled by this manager
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New class and interface added in 1.3")
public abstract class AbstractPrimaryEntityManager<E extends PrimaryEntity> implements PrimaryEntityManager<E>
{
    /** Logging helper object. */
    @Inject
    protected Logger logger;

    /** Runs queries for finding entities. */
    @Inject
    protected QueryManager qm;

    /** Provides access to the current execution context. */
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /** Provides access to the XWiki data. */
    @Inject
    protected DocumentAccessBridge bridge;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> stringResolver;

    /** Fills in missing reference fields with those from the current context to create a full reference. */
    @Inject
    @Named("current")
    protected DocumentReferenceResolver<EntityReference> referenceResolver;

    /** Serializes references without the wiki prefix. */
    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> localSerializer;

    /** The concrete {@link PrimaryEntity} instance class being managed. */
    private Class<? extends E> eclass;

    /** The constructor for concrete {@link PrimaryEntity} instance class being managed. */
    private Constructor<? extends E> econstructor;

    @Override
    public E create()
    {
        return create(this.bridge.getCurrentUserReference());
    }

    @Override
    public synchronized E create(DocumentReference creator)
    {
        try {
            XWikiContext context = this.xcontextProvider.get();
            DocumentReference newDoc = getNextDocument();
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(newDoc);

            DocumentReference template = getEntityXClassReference();
            template = new DocumentReference(template.getName().concat("Template"), template.getLastSpaceReference());
            if (!this.bridge.exists(template)) {
                template = new DocumentReference(template.getName().replaceAll("Class(Template)$", "$1"),
                    template.getLastSpaceReference());
            }
            if (this.bridge.exists(template)) {
                doc.readFromTemplate(template, context);
            }
            if (creator != null) {
                doc.setCreatorReference(creator);
                doc.setAuthorReference(creator);
                doc.setContentAuthorReference(creator);
            }
            context.getWiki().saveDocument(doc, context);

            return load(doc);
        } catch (Exception ex) {
            this.logger.warn("Failed to create entity: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public E get(String id)
    {
        return get(this.stringResolver.resolve(id, getDataSpace()));
    }

    @Override
    public E get(DocumentReference reference)
    {
        try {
            DocumentModelBridge document = this.bridge.getDocument(reference);
            if (document == null || ((XWikiDocument) document).isNew()) {
                return null;
            }
            return load(document);
        } catch (Exception ex) {
            this.logger.error("Failed to read document [{}]: {}", reference, ex.getMessage());
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This assumes that the name is stored in the document title. Override for a different behavior.
     * </p>
     */
    @Override
    public E getByName(String name)
    {
        try {
            Query q = this.qm.createQuery(
                "select doc.fullName from Document doc, doc.object("
                    + this.localSerializer.serialize(getEntityXClassReference())
                    + ") as entity where doc.space = :space and doc.title = :name",
                Query.XWQL).bindValue("space", this.getDataSpace().getName()).bindValue("name", name);
            List<String> docNames = q.execute();
            for (String docName : docNames) {
                return get(docName);
            }
        } catch (QueryException ex) {
            this.logger.warn("Failed to retrieve the entity named [{}]: {}", name, ex.getMessage());
        }
        return null;
    }

    @Override
    public Iterator<E> getAll()
    {
        try {
            Query q = this.qm.createQuery(
                "select doc.fullName from Document as doc, doc.object("
                    + this.localSerializer.serialize(getEntityXClassReference())
                    + ") as entity where doc.name not in (:template1, :template2) order by doc.name asc",
                Query.XWQL).bindValue("template1", this.getEntityXClassReference().getName() + "Template")
                .bindValue("template2",
                    StringUtils.removeEnd(this.getEntityXClassReference().getName(), "Class") + "Template");
            List<String> docNames = q.execute();
            return new LazyPrimaryEntityIterator<>(docNames, this);
        } catch (QueryException ex) {
            this.logger.warn("Failed to query all entities of type [{}]: {}", getEntityXClassReference(),
                ex.getMessage());
        }
        return Collections.emptyIterator();
    }

    @Override
    public boolean delete(E entity)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            // use getDocument()
            XWikiDocument doc = xcontext.getWiki().getDocument(entity.getDocumentReference(), xcontext);
            xcontext.getWiki().deleteDocument(doc, xcontext);
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to delete entity [{}]: {}", entity.getDocumentReference(), ex.getMessage());
        }
        return false;
    }

    @Override
    public E load(DocumentModelBridge document) throws IllegalArgumentException
    {
        try {
            return getEntityConstructor().newInstance(document);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            this.logger.error("Failed to instantiate primary entity of type [{}] from document [{}]: {}",
                getEntityXClassReference(), document, ex.getMessage());
        }
        return null;
    }

    /**
     * Gets a reference to the next document that can be used for a newly created entity. It uses {@link #getIdPrefix()
     * a short prefix} and {@link #getLastUsedId() a sequence number} for the document name, and {@link #getDataSpace()
     * a space that can be configured by subclases}.
     *
     * @return a reference for a new document
     */
    protected DocumentReference getNextDocument()
    {
        String prefix = getIdPrefix();
        long id = getLastUsedId();
        DocumentReference newDoc;
        do {
            newDoc = this.referenceResolver.resolve(new EntityReference(
                prefix + String.format("%07d", ++id), EntityType.DOCUMENT, getDataSpace()));
        } while (this.bridge.exists(newDoc));
        return newDoc;
    }

    /**
     * Gets a prefix for all {@code #create() generated} documents. This implementation computes it from the uppercase
     * letters of the XClass name, excluding {@code Class}, e.g. for {@code PhenoTips.DiseaseStudyClass} the prefix will
     * be {@code DS}.
     *
     * @return a short string
     */
    protected String getIdPrefix()
    {
        String name = getEntityXClassReference().getName();
        name = StringUtils.removeEnd(name, "Class");
        return name.replaceAll("\\p{Lower}++", "");
    }

    protected long getLastUsedId()
    {
        long crtMaxID = 0;
        try {
            Query q =
                this.qm.createQuery(
                    "select doc.name from Document doc, doc.object("
                        + this.localSerializer.serialize(getEntityXClassReference())
                        + ") as entity where doc.space = :space order by doc.name desc",
                    Query.XWQL).bindValue("space", this.getDataSpace().getName()).setLimit(1);
            List<String> crtMaxIDList = q.execute();
            if (!crtMaxIDList.isEmpty() && crtMaxIDList.get(0) != null) {
                crtMaxID = Integer.parseInt(crtMaxIDList.get(0).replaceAll("\\D++", ""));
            }
            crtMaxID = Math.max(crtMaxID, 0);
        } catch (QueryException ex) {
            this.logger.warn("Failed to get the last used identifier: {}", ex.getMessage());
        }
        return crtMaxID;
    }

    /**
     * Gets a reference to {@link PrimaryEntity#getType() the XClass used} for the primary entities being managed. The
     * base implementation assumes that this class is annotated with a {@code Named} with its value set to a partial
     * reference to the target XClass. This name shouldn't have a wiki reference, since this depends on the current
     * instance. If the name doesn't have a space, it will be assumed to be {@code PhenoTips}. If the name doesn't end
     * with {@code Class}, it is added automatically. For example, assuming the targeted XClass is
     * {@code PhenoTips.PatientClass}, the following names are supported, in order of preference:
     * <ul>
     * <li>Patient</li>
     * <li>PhenoTips.PatientClass</li>
     * <li>PhenoTips.Patient</li>
     * <li>PatientClass</li>
     * </ul>
     * If, instead, the targeted XClass is {@code OncoTips.Family}, then only that exact name is supported.
     *
     * @return a full reference resolved in the current virtual instance
     */
    protected DocumentReference getEntityXClassReference()
    {
        if (this.getClass().getAnnotation(Named.class) == null) {
            this.logger.error("Invalid component configuration: {} does not have a @Named annotation",
                this.getClass().getName());
            throw new AbstractMethodError(
                "Missing @Named annotation on PrimaryEntityManager class " + this.getClass().getCanonicalName());
        }
        String name = this.getClass().getAnnotation(Named.class).value();
        DocumentReference result = this.stringResolver.resolve(name, Constants.CODE_SPACE_REFERENCE);
        if (!this.bridge.exists(result) && !name.endsWith("Class")) {
            result = this.stringResolver.resolve(name + "Class", Constants.CODE_SPACE_REFERENCE);
        }
        if (!this.bridge.exists(result)) {
            this.logger.error("Invalid component configuration: {} does not have a valid @Named annotation",
                this.getClass().getName());
            throw new AbstractMethodError(
                "The @Named annotation on PrimaryEntityManager class " + this.getClass().getCanonicalName()
                    + " must be a reference to the XClass used by its managed entity");
        }
        return result;
    }

    /**
     * Gets the concrete class of the managed PrimaryEntity. The base implementation assumes that the class is passed
     * for the first generic parameter.
     *
     * @return a class
     * @throws AbstractMethodError if the manager class doesn't properly define the generic parameter
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends E> getEntityClass()
    {
        if (this.eclass == null) {
            Type cls = this.getClass().getGenericSuperclass();
            while (cls instanceof Class) {
                cls = ((Class<?>) cls).getGenericSuperclass();
            }
            ParameterizedType pcls = (ParameterizedType) cls;
            if (pcls.getActualTypeArguments().length == 0 || !(pcls.getActualTypeArguments()[0] instanceof Class)) {
                this.logger.error(
                    "Invalid component configuration: {} does not define a real class for the <E> parameter",
                    this.getClass().getName());
                throw new AbstractMethodError("The PrimaryEntityManager class " + this.getClass().getCanonicalName()
                    + " must define a real class for the <E> parameter");
            }
            this.eclass = (Class<E>) pcls.getActualTypeArguments()[0];
        }
        return this.eclass;
    }

    /**
     * Gets the constructor of the managed PrimaryEntity concrete class that accepts an XDocument as the only parameter.
     * Override if another constructor is needed, but keep in mind that {@link #load(DocumentModelBridge)} must also be
     * overridden to pass in the required arguments.
     *
     * @return a constructor that requires an XWikiDocument as its only parameter
     * @throws AbstractMethodError if no such constructor can be found
     */
    protected Constructor<? extends E> getEntityConstructor() throws AbstractMethodError
    {
        if (this.econstructor == null) {
            try {
                this.econstructor = getEntityClass().getConstructor(XWikiDocument.class);
            } catch (NoSuchMethodException | SecurityException e) {
                try {
                    this.econstructor = getEntityClass().getConstructor(DocumentModelBridge.class);
                } catch (NoSuchMethodException | SecurityException ex) {
                    this.logger.error("Cannot instantiate primary entity [{}]: {}", getEntityClass().getName(),
                        ex.getMessage());
                    throw new AbstractMethodError("The PrimaryEntity class " + getEntityClass().getCanonicalName()
                        + " must have a public constructor that accepts an XWikiDocument parameter");
                }
            }
        }
        return this.econstructor;
    }
}
