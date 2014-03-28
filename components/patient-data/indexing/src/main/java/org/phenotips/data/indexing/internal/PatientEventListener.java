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
package org.phenotips.data.indexing.internal;

import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.indexing.PatientIndexer;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.FilterableEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

/**
 * Monitors document changes and submits modified patients to the {@link PatientIndexer indexer}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("phenotips-patient-indexer")
@Singleton
public class PatientEventListener implements EventListener
{
    /**
     * A deleted patient, we only care about its document.
     *
     * @version $Id$
     * @since 1.0M10
     */
    private static class DeletedPatient implements Patient
    {
        /** @see #getDocument() */
        private final DocumentReference document;

        /**
         * Simple constructor passing the document reference.
         *
         * @param document the document reference where this patient existed
         */
        public DeletedPatient(DocumentReference document)
        {
            this.document = document;
        }

        @Override
        public String getId()
        {
            return this.document.getName();
        }

        @Override
        public String getExternalId()
        {
            return null;
        }

        @Override
        public JSONObject toJSON()
        {
            return null;
        }

        @Override
        public JSONObject toJSON(Collection<String> onlyFieldNames)
        {
            return null;
        }

        @Override
        public void updateFromJSON(JSONObject json)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public DocumentReference getReporter()
        {
            return null;
        }

        @Override
        public Set<? extends Feature> getFeatures()
        {
            return null;
        }

        @Override
        public DocumentReference getDocument()
        {
            return this.document;
        }

        @Override
        public Set<? extends Disorder> getDisorders()
        {
            return null;
        }

        @Override
        public <T> PatientData<T> getData(String name)
        {
            return null;
        }
    }

    /** Does the actual indexing. */
    @Inject
    private PatientIndexer indexer;

    /** Transforms raw documents into patient data. */
    @Inject
    private PatientRepository patients;

    @Override
    public String getName()
    {
        return "phenotips-patient-indexer";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays
            .<Event> asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(final Event event, final Object source, final Object data)
    {
        Patient patient = this.patients.getPatientById(((FilterableEvent) event).getEventFilter().getFilter());
        if (event instanceof DocumentDeletedEvent) {
            this.indexer.delete(new DeletedPatient(((DocumentModelBridge) source).getDocumentReference()));
        } else if (patient != null) {
            this.indexer.index(patient);
        }
    }
}
