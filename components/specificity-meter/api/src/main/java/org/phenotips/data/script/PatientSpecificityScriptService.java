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
package org.phenotips.data.script;

import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

/**
 * A service that computes the patient specificity, a score estimating how "good" a patient record is.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Unstable
@Component
@Named("specificity")
@Singleton
public class PatientSpecificityScriptService implements ScriptService
{
    /** The wrapped service that does the actual work. */
    @Inject
    private PatientSpecificityService service;

    /**
     * Compute the raw specificity score for a patient.
     *
     * @param patient the patient to score
     * @return a score between {@code 0} and {@code 1}, or {@code -1} if the score cannot be computed by this scorer
     */
    public double getScore(Patient patient)
    {
        return this.service.getScore(patient);
    }

    /**
     * Compute the raw specificity score for a patient snapshot, literally a collection of positive and negative
     * features.
     *
     * @param features the relevant features observed in the patient
     * @param negativeFeatures the relevant features that were NOT observed in the patient
     * @return a score between {@code 0} and {@code 1}, or {@code -1} if the score cannot be computed by this scorer
     */
    public double getScore(String[] features, String[] negativeFeatures)
    {
        Patient patient = new FakePatient(features, negativeFeatures);
        return this.service.getScore(patient);
    }

    private static final class FakeFeature implements Feature
    {
        private final String id;

        private final boolean present;

        private FakeFeature(final String id, final boolean present)
        {
            this.id = id;
            this.present = present;
        }

        @Override
        public String getId()
        {
            return this.id;
        }

        @Override
        public String getName()
        {
            return null;
        }

        @Override
        public String getType()
        {
            return null;
        }

        @Override
        public boolean isPresent()
        {
            return this.present;
        }

        @Override
        public Map<String, ? extends FeatureMetadatum> getMetadata()
        {
            return null;
        }

        @Override
        public String getNotes()
        {
            return null;
        }

        @Override
        public JSONObject toJSON()
        {
            return null;
        }

        @Override
        public String getValue()
        {
            return null;
        }
    }

    private static final class FakePatient implements Patient
    {
        private final Set<Feature> features;

        private FakePatient(final String[] symptoms, final String[] negativeSymptoms)
        {
            Set<Feature> result = new HashSet<Feature>();
            if (symptoms != null && symptoms.length > 0) {
                for (String symptom : symptoms) {
                    result.add(new FakeFeature(symptom, true));
                }
            }
            if (negativeSymptoms != null && negativeSymptoms.length > 0) {
                for (String symptom : negativeSymptoms) {
                    result.add(new FakeFeature(symptom, false));
                }
            }
            this.features = Collections.unmodifiableSet(result);
        }

        @Override
        public String getId()
        {
            return null;
        }

        @Override
        public String getExternalId()
        {
            return null;
        }

        @Override
        public DocumentReference getDocument()
        {
            return null;
        }

        @Override
        public DocumentReference getReporter()
        {
            return null;
        }

        @Override
        public Set<? extends Feature> getFeatures()
        {
            return this.features;
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
            // Nothing to do
        }
    }
}
