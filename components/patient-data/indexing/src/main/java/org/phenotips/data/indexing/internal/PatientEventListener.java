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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.data.indexing.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.data.events.PatientDeletedEvent;
import org.phenotips.data.events.PatientEvent;
import org.phenotips.data.indexing.PatientIndexer;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Monitors document changes and submits modified patients to the {@link PatientIndexer indexer}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("phenotips-patient-indexer")
@Singleton
public class PatientEventListener extends AbstractEventListener
{
    /** Does the actual indexing. */
    @Inject
    private PatientIndexer indexer;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientEventListener()
    {
        super("phenotips-patient-indexer", new PatientChangedEvent(), new PatientDeletedEvent());
    }

    @Override
    public void onEvent(final Event event, final Object source, final Object data)
    {
        Patient patient = ((PatientEvent) event).getPatient();
        if (event instanceof PatientDeletedEvent) {
            this.indexer.delete(patient);
        } else if (patient != null) {
            this.indexer.index(patient);
        }
    }
}
