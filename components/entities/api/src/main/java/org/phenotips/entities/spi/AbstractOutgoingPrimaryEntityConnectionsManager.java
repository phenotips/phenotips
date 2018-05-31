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
package org.phenotips.entities.spi;

import org.phenotips.Constants;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityConnectionsManager;

import org.xwiki.component.phase.Initializable;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Base class for implementing predicates, where the connections are stored in the Subjects, referencing the Objects.
 * <p>
 * The abstract class works as-is. In order to create a proper connection manager component all that is needed is to
 * extend this abstract class, set the right values for {@link AbstractPrimaryEntityConnectionsManager#subjectsManager}
 * and {@link AbstractPrimaryEntityConnectionsManager#objectsManager} in {@link Initializable#initialize()}, add a
 * {@code @Component} annotation and a proper {@code @Named} name following the recommended convention of
 * {@code subjectType-predicate-objectType}.
 * <p>
 * The default behavior of this base class is to store connections in the Subject XDocument, as a new XObject of type
 * {@code PhenoTips.EntityConnectionClass}, with a full reference to the Subject stored in the {@code reference}
 * XProperty.
 * </p>
 *
 * @param <S> the type of entities being the subject of the connection
 * @param <O> the type of entities being the object of the connection
 * @version $Id$
 * @since 1.4
 */
@Unstable("New SPI introduced in 1.4")
@SuppressWarnings("checkstyle:MultipleStringLiterals")
public abstract class AbstractOutgoingPrimaryEntityConnectionsManager<S extends PrimaryEntity, O extends PrimaryEntity>
    extends AbstractPrimaryEntityConnectionsManager<S, O>
    implements PrimaryEntityConnectionsManager<S, O>
{
    /** The XClass used for storing connections by default. */
    public static final EntityReference OUTGOING_CONNECTION_XCLASS =
        new EntityReference("EntityConnectionClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** The XProperty used for referencing the Object document. */
    public static final String REFERENCE_XPROPERTY = "reference";

    @Override
    public boolean connect(S subject, O object)
    {
        if (!ObjectUtils.allNotNull(subject, object)) {
            throw new IllegalArgumentException();
        }

        return storeConnection(subject, object, subject.getXDocument(), object.getDocumentReference(), false);
    }

    @Override
    public boolean disconnect(S subject, O object)
    {
        if (!ObjectUtils.allNotNull(subject, object)) {
            throw new IllegalArgumentException();
        }
        return deleteConnection(subject, object, subject.getXDocument(), object.getDocumentReference(), false);
    }

    @Override
    public Collection<O> getAllConnections(S subject)
    {
        if (subject == null) {
            throw new IllegalArgumentException();
        }

        Collection<O> result = new LinkedList<>();
        try {
            String wikiId = this.xcontextProvider.get().getWikiId() + ":";
            Query q = this.queryManager.createQuery(
                // Select the referenced Objects
                "select distinct objectObj.name "
                    + "  from BaseObject connectionObj, StringProperty referenceProperty, BaseObject objectObj"
                    + "  where "
                    // The connection is stored in the Subject
                    + "    connectionObj.name = :subjectDocument and "
                    + "    connectionObj.className = :connectionClass and "
                    // The reference property belongs to the connection
                    + "    referenceProperty.id.id = connectionObj.id and "
                    + "    referenceProperty.id.name = :referenceProperty and "
                    // The connection points to the Object
                    + "    referenceProperty.value = concat(:wikiId, objectObj.name) and "
                    // The Object must have the right type
                    + "    objectObj.className = :objectClass",
                Query.HQL);

            q.bindValue("subjectDocument", this.localSerializer.serialize(subject.getDocumentReference()));
            q.bindValue("connectionClass", this.localSerializer.serialize(getConnectionXClass()));
            q.bindValue("referenceProperty", getReferenceProperty());
            q.bindValue("wikiId", wikiId);
            q.bindValue("objectClass", this.localSerializer.serialize(this.objectsManager.getEntityType()));

            List<String> docNames = q.execute();
            return docNames.stream().map(id -> this.objectsManager.get(id)).collect(Collectors.toList());
        } catch (QueryException ex) {
            this.logger.warn("Failed to query the Objects of type [{}] connected from [{}]: {}",
                this.objectsManager.getEntityType(), subject.getId(), ex.getMessage());
        }
        return result;
    }

    @Override
    public Collection<S> getAllReverseConnections(O object)
    {
        if (object == null) {
            throw new IllegalArgumentException();
        }

        try {
            Query q = this.queryManager.createQuery(
                // Look for Subject documents
                ", BaseObject subjectObj, BaseObject connectionObj, StringProperty property "
                    + "  where "
                    // The Subject must have the right type
                    + "    subjectObj.name = doc.fullName and "
                    + "    subjectObj.className = :subjectClass and "
                    // The connection is stored in the Subject
                    + "    connectionObj.name = doc.fullName and "
                    + "    connectionObj.className = :connectionClass and "
                    // The reference property belongs to the connection
                    + "    property.id.id = connectionObj.id and "
                    + "    property.id.name = :referenceProperty and "
                    // The reference property references the target Object
                    + "    property.value = :objectDocument",
                Query.HQL);

            q.bindValue("subjectClass", this.localSerializer.serialize(this.subjectsManager.getEntityType()));
            q.bindValue("connectionClass", this.localSerializer.serialize(getConnectionXClass()));
            q.bindValue("referenceProperty", this.getReferenceProperty());
            q.bindValue("objectDocument", this.fullSerializer.serialize(object.getDocumentReference()));

            List<String> docNames = q.execute();
            return docNames.stream().map(id -> this.subjectsManager.get(id)).collect(Collectors.toList());
        } catch (QueryException ex) {
            this.logger.warn("Failed to query the Subjects of type [{}] connected to [{}]: {}",
                this.subjectsManager.getEntityType(), object.getId(), ex.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    protected EntityReference getConnectionXClass()
    {
        return OUTGOING_CONNECTION_XCLASS;
    }

    @Override
    protected String getReferenceProperty()
    {
        return REFERENCE_XPROPERTY;
    }
}
