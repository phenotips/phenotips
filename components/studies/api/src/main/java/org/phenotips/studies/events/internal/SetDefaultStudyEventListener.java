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
package org.phenotips.studies.events.internal;

import org.phenotips.data.events.PatientCreatedEvent;
import org.phenotips.data.permissions.EntityPermissionsPreferencesManager;
import org.phenotips.data.permissions.events.EntitiesLinkedEvent;
import org.phenotips.data.permissions.internal.AbstractDefaultPermissionsEventListener;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.studies.internal.StudyRecordConfigurationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
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
 * This listener creates a {@code PhenoTips.StudyBindingClass} object when a new entity is created. Retrieves the
 * configured defaultStudy for the new owner (either from the group profile or defaultWorkgroup profile specified in
 * user profile or, if no defaultWorkgroup specified, from the user profile).
 *
 * @version $Id$
 * @since 1.5M1
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

    @Inject
    private ObservationManager observationManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public SetDefaultStudyEventListener()
    {
        super("phenotips-entity-study-updater", new PatientCreatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        PrimaryEntity primaryEntity = getPrimaryEntity(event, source);
        if (primaryEntity == null) {
            return;
        }

        try {
            XWikiDocument doc = primaryEntity.getXDocument();
            DocumentReference entityRef = getEntityRef(event);

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

                // firing a cascading event of a patient record is placed into a study
                // to assign any study default permissions
                this.observationManager.notify(new EntitiesLinkedEvent(primaryEntity.getId(), primaryEntity
                    .getXDocument().getSpace(), newStudy, "Studies"), null);
            }
        } catch (XWikiException ex) {
            this.logger.error("Failed to set the initial study for entity [{}]: {}", primaryEntity.getName(),
                ex.getMessage(), ex);
        }
    }
}
