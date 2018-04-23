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

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.component.phase.Initializable;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class for implementing specific connection managers. This base class deals with logical aggregate connection
 * operations and not with the actual implementation of handling individual connections.
 * <p>
 * Things that can be customized:
 * </p>
 * <ul>
 * <li>{@link #storeConnection} and {@link #deleteConnection} can store the connection in either the Subject or the
 * Object XDocument, depending on which one is passed as the {@code container} argument</li>
 * <li>{@link #getConnectionXClass()} and {@link #getReferenceProperty()} define how the connection is stored</li>
 * <li>the connection can be stored either in a separate XObject holding just the connection information, or, if
 * {@code true} is passed as the last argument for the {@link #storeConnection} and {@link #deleteConnection} methods,
 * as a separate XProperty in an XObject with other purposes, if the predicate is a *-to-one or one-to-* and only one
 * connection must be stored</li>
 * <li>additional data may be stored in the connection by overriding
 * {@link #setConnectionParameters(PrimaryEntity, PrimaryEntity, BaseObject)}</li>
 * </ul>
 *
 * @param <S> the type of entities being the subject of the connection
 * @param <O> the type of entities being the object of the connection
 * @version $Id$
 * @since 1.4
 */
@Unstable("New SPI introduced in 1.4")
public abstract class AbstractPrimaryEntityConnectionsManager<S extends PrimaryEntity, O extends PrimaryEntity>
    implements PrimaryEntityConnectionsManager<S, O>, Initializable
{
    /** Logging helper object. */
    @Inject
    protected Logger logger;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected EntityReferenceSerializer<String> fullSerializer;

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> localSerializer;

    /** This must be initialized in {@link Initializable#initialize()}. */
    protected PrimaryEntityManager<S> subjectsManager;

    /** This must be initialized in {@link Initializable#initialize()}. */
    protected PrimaryEntityManager<O> objectsManager;

    @Inject
    protected QueryManager queryManager;

    /**
     * Stores a connection as an XObject of the type returned by {@link #getConnectionXClass()}, with the XProperty
     * identified by {@link #getReferenceProperty()} storing a reference to either the subject or the object of the
     * connection.
     *
     * @param subject the subject of the connection
     * @param object the object of the connection
     * @param container the XDocument where the connection is to be stored, either the Subject or the Object
     * @param reference the reference to store, to either the Object or the Subject
     * @param useFirstObject if {@code true}, then instead of creating a new XObject for storing the connection, the
     *            first XObject of the {@link #getConnectionXClass() required XClass} is used; this allows storing the
     *            connection in an XObject with other data
     * @return {@code true} if the connection was successfully saved, {@code false} if the operation failed
     */
    protected boolean storeConnection(@Nonnull S subject, @Nonnull O object, @Nonnull XWikiDocument container,
        @Nonnull DocumentReference reference, boolean useFirstObject)
    {
        if (!ObjectUtils.allNotNull(subject, object, container, reference)) {
            throw new IllegalArgumentException();
        }

        try {
            BaseObject obj = container.getXObject(getConnectionXClass(), getReferenceProperty(),
                this.fullSerializer.serialize(reference), false);
            if (obj != null) {
                return true;
            }
            if (useFirstObject) {
                obj = container.getXObject(getConnectionXClass(), true, this.xcontextProvider.get());
            } else {
                obj = container.newXObject(getConnectionXClass(), this.xcontextProvider.get());
            }
            obj.setStringValue(getReferenceProperty(), this.fullSerializer.serialize(reference));
            setConnectionParameters(subject, object, obj);
            this.xcontextProvider.get().getWiki().saveDocument(container, "Added connection to " + reference, true,
                this.xcontextProvider.get());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to create connection between [{}] and [{}]: {}",
                subject.getDocumentReference(), object.getDocumentReference(), ex.getMessage());
        }
        return false;
    }

    /**
     * Stores additional parameters in the connection object. If a predicate wants to record parameters within a
     * connection, override this.
     *
     * @param subject the subject of the connection being saved
     * @param object the object of the connection being saved
     * @param connectionObject object for recording parameters
     */
    protected void setConnectionParameters(@Nonnull S subject, @Nonnull O object, @Nonnull BaseObject connectionObject)
    {
        // Nothing extra to store by default
    }

    /**
     * The reverse of {@link #storeConnection}, deletes the XObject storing a connection between the subject and the
     * object.
     *
     * @param subject the subject of the connection
     * @param object the object of the connection
     * @param container the XDocument where the connection is stored, either the subject or the object
     * @param reference the reference to delete, to either the Object or the Subject
     * @param onlyClear if {@code true}, instead of deleting the XObject completely, the relevant field is cleared
     *            instead; this is useful when the connection is stored not in a standalone XObject, but as a field in a
     *            larger XObject with other responsibilities
     * @return {@code true} if the connection was successfully deleted, {@code false} if the operation failed
     */
    protected boolean deleteConnection(@Nonnull S subject, @Nonnull O object, @Nonnull XWikiDocument container,
        @Nonnull DocumentReference reference, boolean onlyClear)
    {
        if (!ObjectUtils.allNotNull(subject, object, container, reference)) {
            throw new IllegalArgumentException();
        }

        try {
            BaseObject obj = container.getXObject(getConnectionXClass(), getReferenceProperty(),
                this.fullSerializer.serialize(reference), false);
            if (obj == null) {
                return true;
            }
            if (onlyClear) {
                obj.setStringValue(getReferenceProperty(), "");
            } else {
                container.removeXObject(obj);
            }
            this.xcontextProvider.get().getWiki().saveDocument(container, "Removed connection to " + reference, true,
                this.xcontextProvider.get());
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to delete connection between [{}] and [{}]: {}",
                subject.getDocumentReference(), object.getDocumentReference(), ex.getMessage());
        }
        return false;
    }

    /**
     * Override to change the XClass used for storing connections.
     *
     * @return a reference to an XClass document
     */
    @Nonnull
    protected abstract EntityReference getConnectionXClass();

    /**
     * Override to change the XProperty of the {@link #getConnectionXClass() XClass} that is used for storing the
     * reference to the connected entity.
     *
     * @return the name of an XProperty
     */
    @Nonnull
    protected abstract String getReferenceProperty();
}
