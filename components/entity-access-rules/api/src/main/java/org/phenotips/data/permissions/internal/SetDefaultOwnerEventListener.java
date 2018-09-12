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
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.EntityPermissionsPreferencesManager;
import org.phenotips.data.permissions.Owner;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This listener creates a {@code PhenoTips.OwnerClass} object when a new entity is created. Retrieves the configured
 * defaultOwner for the user (either from the user profile or, if missing, from its workgroup) If no such entry exists,
 * sets the creator as the owner by the default.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("phenotips-entity-owner-updater")
@Singleton
public class SetDefaultOwnerEventListener extends AbstractDefaultPermissionsEventListener
{
    private static final String OWNER = "owner";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private EntityPermissionsManager permissions;

    @Inject
    private EntityPermissionsPreferencesManager preferencesManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public SetDefaultOwnerEventListener()
    {
        super("phenotips-entity-owner-updater", new PatientCreatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        PrimaryEntity primaryEntity = getPrimaryEntity(event, source);
        if (primaryEntity == null) {
            return;
        }

        XWikiDocument doc = primaryEntity.getXDocument();
        DocumentReference creator = doc.getCreatorReference();

        try {
            BaseObject ownerObject = doc.getXObject(Owner.CLASS_REFERENCE);
            if (ownerObject == null) {
                XWikiContext context = this.provider.get();
                ownerObject = doc.newXObject(Owner.CLASS_REFERENCE, context);
            }

            // set the creator as the owner by default
            // this is needed for proper workflow of EntityAccess.setOwner()
            if (creator != null) {
                ownerObject.setStringValue(OWNER, creator.toString());
            }

            DocumentReference defaultOwnerDocRef = this.preferencesManager.getDefaultOwner(null);
            if (defaultOwnerDocRef != null) {
                EntityAccess access = this.permissions.getEntityAccess(primaryEntity);
                access.setOwner(defaultOwnerDocRef);
            } else {
                ownerObject.setStringValue(OWNER, "");
            }
        } catch (XWikiException ex) {
            this.logger.error("Failed to set the initial owner for entity [{}]: {}", doc.getDocumentReference(),
                ex.getMessage(), ex);
        }
    }
}
