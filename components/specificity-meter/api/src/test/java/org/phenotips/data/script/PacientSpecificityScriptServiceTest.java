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

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.CapturingMatcher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PacientSpecificityScriptServiceTest
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
        String[] features = new String[] { "HP:1", "custom" };
        String[] negativeFeatures = new String[] { "HP:2", "custom negative" };
        PatientSpecificityService service = this.mocker.getInstance(PatientSpecificityService.class);
        CapturingMatcher<Patient> patientCapture = new CapturingMatcher<>();
        when(service.getScore(Mockito.argThat(patientCapture))).thenReturn(2.5);
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
}
