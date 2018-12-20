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

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.data.events.PatientEvent;
import org.phenotips.data.permissions.events.EntityStudyUpdatedEvent;
import org.phenotips.studies.internal.StudyRecordConfigurationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Monitors document changes and fires {@link EntityStudyUpdatedEvent event} if study was changed.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named("phenotips-patient-study-updated")
@Singleton
public class StudyChangedEventListener extends AbstractEventListener
{
    @Inject
    private ObservationManager observationManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public StudyChangedEventListener()
    {
        super("phenotips-patient-study-updated", new PatientChangedEvent());
    }

    @Override
    public void onEvent(final Event event, final Object source, final Object data)
    {
        Patient patient = ((PatientEvent) event).getPatient();

        BaseObject newStudyObject =
            patient.getXDocument().getXObject(StudyRecordConfigurationModule.STUDY_BINDING_CLASS_REFERENCE);
        if (newStudyObject == null) {
            return;
        }

        String newStudy = newStudyObject.getStringValue(StudyRecordConfigurationModule.STUDY_REFERENCE_PROPERTY_LABEL);
        if (StringUtils.isBlank(newStudy)) {
            return;
        }

        BaseObject oldStudyObject =
            patient.getXDocument().getOriginalDocument()
                .getXObject(StudyRecordConfigurationModule.STUDY_BINDING_CLASS_REFERENCE);
        if (oldStudyObject == null) {
            this.observationManager.notify(new EntityStudyUpdatedEvent(patient.getId(), newStudy), null);
            return;
        }

        String oldStudy = oldStudyObject.getStringValue(StudyRecordConfigurationModule.STUDY_REFERENCE_PROPERTY_LABEL);
        if (!oldStudy.equals(newStudy)) {
            this.observationManager.notify(new EntityStudyUpdatedEvent(patient.getId(), newStudy), null);
        }
    }
}
