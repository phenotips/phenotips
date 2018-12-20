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
import org.phenotips.data.permissions.EntityPermissionsPreferencesManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.events.EntitiesLinkedEvent;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This listener creates a {@code PhenoTips.VisibilityClass} object when a new entity is created or when a patient is
 * assigned to a new study. Retrieves the configured defaultVisibility from a new owner or a new study profile.
 *
 * @version $Id$
 * @since 1.5M1
 */
@Component
@Named("phenotips-entity-visibility-updater")
@Singleton
public class SetDefaultVisibilityEventListener extends AbstractDefaultPermissionsEventListener
{
    private static final String VISIBILITY = "visibility";

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private EntityVisibilityManager visibilityManager;

    @Inject
    private EntityPermissionsPreferencesManager preferencesManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public SetDefaultVisibilityEventListener()
    {
        super("phenotips-entity-visibility-updater", new PatientCreatedEvent(), new EntitiesLinkedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // if the entity is linked to another entity, we are interested only in patient linked to a study
        if (event instanceof EntitiesLinkedEvent) {
            String entitySpace = ((EntitiesLinkedEvent) event).getSubjectEntitySpace();
            String linkedToSpace = ((EntitiesLinkedEvent) event).getLinkedToEntitySpace();
            if (!"data".equals(entitySpace) || !"Studies".equals(linkedToSpace)) {
                return;
            }
        }

        PrimaryEntity primaryEntity = getPrimaryEntity(event, source);
        if (primaryEntity == null) {
            return;
        }

        DocumentReference entityRef = getEntityRef(event);
        Visibility defaultVisibility = this.preferencesManager.getDefaultVisibility(entityRef);
        Visibility currentVisibility = this.visibilityManager.getVisibility(primaryEntity);
        if (defaultVisibility != null && !defaultVisibility.equals(currentVisibility)) {
            XWikiContext xcontext = this.provider.get();
            BaseObject obj = primaryEntity.getXDocument().getXObject(Visibility.CLASS_REFERENCE, true, xcontext);
            if (obj != null) {
                obj.setStringValue(VISIBILITY, defaultVisibility.getName());
            }
        }
    }
}
