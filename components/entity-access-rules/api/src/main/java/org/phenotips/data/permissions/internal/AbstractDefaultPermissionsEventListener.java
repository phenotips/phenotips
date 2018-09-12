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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.events.PatientCreatedEvent;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent;
import org.phenotips.data.permissions.events.EntityStudyUpdatedEvent;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base abstract class for handling a collection of permissions and study update events. Main purpose is avoiding code
 * duplication.
 *
 * @version $Id$
 * @since 1.4
 */
public abstract class AbstractDefaultPermissionsEventListener extends AbstractEventListener
{
    private static final String OWNER = "owner";

    @Inject
    protected PrimaryEntityResolver resolver;

    @Inject
    @Named("userOrGroup")
    protected DocumentReferenceResolver<String> userOrGroupResolver;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    protected AbstractDefaultPermissionsEventListener(String name, Event... events)
    {
        super(name, events);
    }

    /** Get the entity the {@link PrimaryEntity} that is being affected. */
    protected PrimaryEntity getPrimaryEntity(Event event, Object source)
    {
        String primaryEntityId = null;
        if (event instanceof PatientCreatedEvent) {
            XWikiDocument doc = (XWikiDocument) source;
            primaryEntityId = doc.getDocumentReference().toString();
        } else if (event instanceof EntityRightsUpdatedEvent) {
            primaryEntityId = ((EntityRightsUpdatedEvent) event).getEntityId();
        } else if (event instanceof EntityStudyUpdatedEvent) {
            primaryEntityId = ((EntityStudyUpdatedEvent) event).getEntityId();
        }
        return this.resolver.resolveEntity(primaryEntityId);
    }

    /** Get the entity {@link DocumentReference} source of getting default settings (user, group or study document). */
    protected DocumentReference getEntityRef(Event event, PrimaryEntity primaryEntity)
    {
        // if the ownership has been transferred, get the new owner profile document
        if (event instanceof EntityRightsUpdatedEvent) {
            BaseObject ownerObj = primaryEntity.getXDocument().getXObject(Owner.CLASS_REFERENCE);
            if (ownerObj != null) {
                String ownerID = ownerObj.getStringValue(OWNER);
                return this.userOrGroupResolver.resolve(ownerID);
            }
        }

        // if the entity was assigned to a new study, get the the study document
        if (event instanceof EntityStudyUpdatedEvent) {
            String studyId = ((EntityStudyUpdatedEvent) event).getStudyId();
            return this.userOrGroupResolver.resolve(studyId);
        }

        return null;
    }
}
