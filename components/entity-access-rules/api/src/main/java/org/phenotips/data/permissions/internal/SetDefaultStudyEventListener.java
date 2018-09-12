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
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent.RightsUpdateEventType;
import org.phenotips.data.permissions.events.EntityStudyUpdatedEvent;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.studies.internal.StudyRecordConfigurationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

import java.util.List;

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
 * This listener creates a {@code PhenoTips.StudyBindingClass} object when a new entity is created or when a user or a
 * workgroup becomes owner of a patient record or when a patient is assigned to a new study. Retrieves the configured
 * defaultStudy for the user (either from the user profile or, if missing, from its workgroup).
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("phenotips-entity-study-updater")
@Singleton
public class SetDefaultStudyEventListener extends AbstractDefaultPermissionsEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private EntityPermissionsPreferencesManager preferencesManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public SetDefaultStudyEventListener()
    {
        super("phenotips-entity-study-updater", new PatientCreatedEvent(), new EntityRightsUpdatedEvent());
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
            XWikiDocument doc = primaryEntity.getXDocument();
            DocumentReference entityRef = getEntityRef(event, primaryEntity);

            BaseObject studyObject = doc.getXObject(StudyRecordConfigurationModule.STUDY_BINDING_CLASS_REFERENCE);
            if (studyObject == null) {
                XWikiContext context = this.provider.get();
                studyObject = doc.newXObject(StudyRecordConfigurationModule.STUDY_BINDING_CLASS_REFERENCE,
                    context);
            }
            String studyID =
                studyObject.getStringValue(StudyRecordConfigurationModule.STUDY_REFERENCE_PROPERTY_LABEL);

            DocumentReference defaultStudyDocRef = this.preferencesManager.getDefaultStudy(entityRef);
            if (defaultStudyDocRef != null && !studyID.equals(defaultStudyDocRef.toString())) {
                String newStudy = defaultStudyDocRef.toString();
                studyObject.setStringValue(StudyRecordConfigurationModule.STUDY_REFERENCE_PROPERTY_LABEL, newStudy);
                if (event instanceof EntityRightsUpdatedEvent || event instanceof EntityStudyUpdatedEvent) {
                    XWikiContext xcontext = this.provider.get();
                    xcontext.getWiki().saveDocument(doc, "Updated study to " + newStudy, true, xcontext);
                }
            }
        } catch (XWikiException ex) {
            this.logger.error("Failed to set the initial study for entity [{}]: {}", primaryEntity.getName(),
                ex.getMessage(), ex);
        }
    }
}
