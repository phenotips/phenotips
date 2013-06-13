/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.phenotips.data.indexing.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.FilterableEvent;

import edu.toronto.cs.phenotips.data.Patient;
import edu.toronto.cs.phenotips.data.PatientData;
import edu.toronto.cs.phenotips.data.indexing.PatientIndexer;

/**
 * @version $Id$
 */
@Component
@Named("patient-indexer")
@Singleton
public class PatientEventListener implements EventListener
{
    @Inject
    private PatientIndexer indexer;

    @Inject
    private PatientData patients;

    @Override
    public String getName()
    {
        return "patient-indexer";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays
            .<Event> asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Patient patient = this.patients.getPatientById(((FilterableEvent) event).getEventFilter().getFilter());
        if (patient != null) {
            if (event instanceof DocumentDeletedEvent) {
                this.indexer.delete(patient);
            } else {
                this.indexer.index(patient);
            }
        }
    }
}
