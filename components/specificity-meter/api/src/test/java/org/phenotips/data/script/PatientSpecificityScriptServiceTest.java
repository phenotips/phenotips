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
package org.phenotips.data.script;

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.internal.matchers.CapturingMatcher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatientSpecificityScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientSpecificityScriptService> mocker =
        new MockitoComponentMockingRule<>(PatientSpecificityScriptService.class);

    @Test
    public void getScoreWithPatientForwardsToService() throws ComponentLookupException
    {
        Patient patient = mock(Patient.class);
        PatientSpecificityService service = this.mocker.getInstance(PatientSpecificityService.class);
        when(service.getScore(patient)).thenReturn(2.5);
        Assert.assertEquals(2.5, this.mocker.getComponentUnderTest().getScore(patient), 0.0);
    }

    @Test
    public void getScoreWithFeaturesForwardsToService() throws ComponentLookupException
    {
        String[] features = new String[] { "HP:1" };
        String[] negativeFeatures = new String[] { "HP:2" };
        PatientSpecificityService service = this.mocker.getInstance(PatientSpecificityService.class);
        CapturingMatcher<Patient> patientCapture = new CapturingMatcher<>();
        when(service.getScore(Matchers.argThat(patientCapture))).thenReturn(2.5);
        Assert.assertEquals(2.5, this.mocker.getComponentUnderTest().getScore(features, negativeFeatures), 0.0);
        Patient patient = patientCapture.getLastValue();
        Set<? extends Feature> setFeatures = patient.getFeatures();
        boolean hasHP1 = false;
        boolean hasHP2 = false;
        for (Feature feature : setFeatures) {
            switch (feature.getId()) {
                case "HP:1":
                    hasHP1 = true;
                    Assert.assertTrue(feature.isPresent());
                    break;
                case "HP:2":
                    hasHP2 = true;
                    Assert.assertFalse(feature.isPresent());
                    break;
                default:
                    Assert.fail();
            }
        }
        Assert.assertTrue(hasHP1);
        Assert.assertTrue(hasHP2);
    }

    @Test
    public void getScoreWithNoFeaturesDoesNothing() throws ComponentLookupException
    {
        String[] features = new String[] {};
        String[] negativeFeatures = new String[] {};
        PatientSpecificityService service = this.mocker.getInstance(PatientSpecificityService.class);
        CapturingMatcher<Patient> patientCapture = new CapturingMatcher<>();
        when(service.getScore(Matchers.argThat(patientCapture))).thenReturn(0.0);
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(features, negativeFeatures), 0.0);
        Patient patient = patientCapture.getLastValue();
        // This is a stub patient, no real values are used
        Assert.assertTrue(patient.getFeatures().isEmpty());
        Assert.assertNull(patient.getId());
        Assert.assertNull(patient.getExternalId());
        Assert.assertNull(patient.getDocument());
        Assert.assertNull(patient.getReporter());
        Assert.assertNull(patient.getDisorders());
        Assert.assertNull(patient.getData(""));
        Assert.assertNull(patient.getData(null));
        Assert.assertNull(patient.toJSON());
        Assert.assertNull(patient.toJSON(Collections.<String>emptySet()));
        Assert.assertNull(patient.toJSON(null));
        patient.updateFromJSON(null);
    }

    @Test
    public void getScoreWithFeaturesCreatesStubPatient() throws ComponentLookupException
    {
        String[] features = new String[] { "HP:1", "custom" };
        String[] negativeFeatures = new String[] { "HP:2", "custom negative" };
        PatientSpecificityService service = this.mocker.getInstance(PatientSpecificityService.class);
        CapturingMatcher<Patient> patientCapture = new CapturingMatcher<>();
        when(service.getScore(Matchers.argThat(patientCapture))).thenReturn(2.5);
        Assert.assertEquals(2.5, this.mocker.getComponentUnderTest().getScore(features, negativeFeatures), 0.0);
        Patient patient = patientCapture.getLastValue();
        Set<? extends Feature> setFeatures = patient.getFeatures();
        boolean hasHP1 = false;
        boolean hasHP2 = false;
        for (Feature feature : setFeatures) {
            switch (feature.getId()) {
                case "HP:1":
                    hasHP1 = true;
                    Assert.assertTrue(feature.isPresent());
                    // This is a stub feature, no real values are used
                    Assert.assertNull(feature.getName());
                    Assert.assertNull(feature.getNotes());
                    Assert.assertNull(feature.getType());
                    Assert.assertNull(feature.getValue());
                    Assert.assertNull(feature.getMetadata());
                    Assert.assertNull(feature.toJSON());
                    break;
                case "HP:2":
                    hasHP2 = true;
                    Assert.assertFalse(feature.isPresent());
                    break;
                default:
                    Assert.fail();
            }
        }
        Assert.assertTrue(hasHP1);
        Assert.assertTrue(hasHP2);
        // This is a stub patient, no real values are used
        Assert.assertNull(patient.getId());
        Assert.assertNull(patient.getExternalId());
        Assert.assertNull(patient.getDocument());
        Assert.assertNull(patient.getReporter());
        Assert.assertNull(patient.getDisorders());
        Assert.assertNull(patient.getData(""));
        Assert.assertNull(patient.getData(null));
        Assert.assertNull(patient.toJSON());
        Assert.assertNull(patient.toJSON(Collections.<String>emptySet()));
        Assert.assertNull(patient.toJSON(null));
        patient.updateFromJSON(null);
    }

    @Test
    public void getScoreWithNullFeaturesDoesntFail() throws ComponentLookupException
    {
        String[] features = new String[] { "HP:1" };
        String[] negativeFeatures = new String[] { "HP:2" };
        PatientSpecificityService service = this.mocker.getInstance(PatientSpecificityService.class);
        CapturingMatcher<Patient> patientCapture = new CapturingMatcher<>();
        when(service.getScore(Matchers.argThat(patientCapture))).thenReturn(2.5);

        this.mocker.getComponentUnderTest().getScore(features, null);
        Patient patient = patientCapture.getLastValue();
        Assert.assertEquals(1, patient.getFeatures().size());
        Assert.assertEquals("HP:1", patient.getFeatures().iterator().next().getId());
        Assert.assertTrue(patient.getFeatures().iterator().next().isPresent());

        this.mocker.getComponentUnderTest().getScore(null, negativeFeatures);
        patient = patientCapture.getLastValue();
        Assert.assertEquals(1, patient.getFeatures().size());
        Assert.assertEquals("HP:2", patient.getFeatures().iterator().next().getId());
        Assert.assertFalse(patient.getFeatures().iterator().next().isPresent());

        this.mocker.getComponentUnderTest().getScore(null, null);
        patient = patientCapture.getLastValue();
        Assert.assertTrue(patient.getFeatures().isEmpty());
    }
}
