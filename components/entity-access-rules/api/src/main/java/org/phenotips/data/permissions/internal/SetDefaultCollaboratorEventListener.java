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
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.EntityPermissionsPreferencesManager;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent.RightsUpdateEventType;
import org.phenotips.data.permissions.events.EntityStudyUpdatedEvent;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.event.Event;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;

/**
 * This listener sets default collaborators when a patient record is created or when a user or a workgroup becomes owner
 * of a patient record or when a patient is assigned to a new study. Retrieves the configured defaultCollaborator for
 * user (either from the user profile or, if missing, from its workgroup) or from the profile of the new record’s
 * owner.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("phenotips-entity-collaborator-updater")
@Singleton
public class SetDefaultCollaboratorEventListener extends AbstractDefaultPermissionsEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private EntityPermissionsManager permissions;

    @Inject
    private EntityPermissionsPreferencesManager preferencesManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public SetDefaultCollaboratorEventListener()
    {
        super("phenotips-entity-collaborator-updater", new PatientCreatedEvent(), new EntityRightsUpdatedEvent(),
            new EntityStudyUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // a change of ownership did not happen while updating patient access rights, do nothing
        if (event instanceof EntityRightsUpdatedEvent) {
            List<RightsUpdateEventType> eventTypes = ((EntityRightsUpdatedEvent) event).getEventTypes();
            if (!eventTypes.contains(RightsUpdateEventType.ENTITY_OWNER_UPDATED)) {
                return;
            }
        }

        PrimaryEntity primaryEntity = getPrimaryEntity(event, source);
        if (primaryEntity == null) {
            return;
        }

        try {
            DocumentReference entityRef = getEntityRef(event, primaryEntity);
            Map<EntityReference, Collaborator> defaultCollabs =
                this.preferencesManager.getDefaultCollaborators(entityRef);
            if (defaultCollabs.isEmpty()) {
                return;
            }

            EntityAccess access = this.permissions.getEntityAccess(primaryEntity);
            Map<EntityReference, Collaborator> patientCollabs = getPatientCollaboratorsMap(access, defaultCollabs);
            // yes, checking same thing again, because while we are collecting patient collabs, we remove duplicates
            // from defaultCollabs
            // if we removed all duplicated collabs and there are none left in defaultCollabs to save, exit
            if (defaultCollabs.isEmpty()) {
                return;
            }

            Map<EntityReference, Collaborator> joinCollaboratorsMap = new TreeMap<EntityReference, Collaborator>();
            joinCollaboratorsMap.putAll(defaultCollabs);
            joinCollaboratorsMap.putAll(patientCollabs);
            access.updateCollaborators(joinCollaboratorsMap.values());
        } catch (XWikiException ex) {
            this.logger.error("Failed to set default collaborators for entity [{}]: {}", primaryEntity.getName(),
                ex.getMessage(), ex);
        }
    }

    private Map<EntityReference, Collaborator> getPatientCollaboratorsMap(EntityAccess access,
        Map<EntityReference, Collaborator> defaultCollabsMap) throws XWikiException
    {
        Collection<Collaborator> collaborators = access.getCollaborators();

        Map<EntityReference, Collaborator> collaboratorsMap = collaborators == null
            ? Collections.emptyMap()
            : collaborators.stream()
                // Take only the non-null references.
                .filter(Objects::nonNull)
                // Collect into a TreeMap.
                .collect(TreeMap::new, (map, v) -> collectCollaborator(map, defaultCollabsMap, v), TreeMap::putAll);

        return collaboratorsMap;
    }

    private void collectCollaborator(
        @Nonnull final Map<EntityReference, Collaborator> map,
        Map<EntityReference, Collaborator> defaultCollaboratorsMap,
        @Nonnull final Collaborator c)
    {
        final EntityReference userOrGroup = c.getUser();
        final AccessLevel access = c.getAccessLevel();
        // if the collaborator in default settings has lower or equal access level than already existing, ignore it
        if (defaultCollaboratorsMap.containsKey(userOrGroup)) {
            if (access.compareTo(defaultCollaboratorsMap.get(userOrGroup).getAccessLevel()) >= 0) {
                map.put(userOrGroup, c);
                defaultCollaboratorsMap.remove(userOrGroup);
            }
        } else {
            map.put(userOrGroup, c);
        }
    }
}
