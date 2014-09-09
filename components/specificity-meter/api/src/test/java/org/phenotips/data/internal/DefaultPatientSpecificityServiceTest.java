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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.cache.CacheException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class DefaultPatientSpecificityServiceTest
{
    @Mock
    private Patient patient;

    @Mock
    private PatientSpecificity spec;

    private PatientScorer monarchScorer;

    private PatientScorer omimScorer;

    @Rule
    public final MockitoComponentMockingRule<PatientSpecificityService> mocker =
        new MockitoComponentMockingRule<PatientSpecificityService>(DefaultPatientSpecificityService.class);

    @Before
    public void setup() throws CacheException, ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.monarchScorer = this.mocker.getInstance(PatientScorer.class, "monarch");
        this.omimScorer = this.mocker.getInstance(PatientScorer.class, "omimInformationContent");
    }

    @Test
    public void getScoreForwardsToMonarch() throws ComponentLookupException
    {
        when(this.monarchScorer.getScore(this.patient)).thenReturn(0.5);
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(0.5, score, 0.0);
        Mockito.verifyZeroInteractions(this.omimScorer);
    }

    @Test
    public void getScoreForwardsToOmimWhenMonarchScorerUnavailable() throws ComponentLookupException
    {
        when(this.monarchScorer.getScore(this.patient)).thenReturn(-1.0);
        when(this.omimScorer.getScore(this.patient)).thenReturn(0.25);
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(0.25, score, 0.0);
    }

    @Test
    public void getSpecificityForwardsToMonarch() throws ComponentLookupException
    {
        when(this.monarchScorer.getSpecificity(this.patient)).thenReturn(this.spec);
        Assert.assertSame(this.spec, this.mocker.getComponentUnderTest().getSpecificity(this.patient));
        Mockito.verifyZeroInteractions(this.omimScorer);
    }

    @Test
    public void getSpecificityForwardsToOmimWhenMonarchScorerUnavailable() throws ComponentLookupException
    {
        when(this.monarchScorer.getSpecificity(this.patient)).thenReturn(null);
        when(this.omimScorer.getSpecificity(this.patient)).thenReturn(this.spec);
        Assert.assertSame(this.spec, this.mocker.getComponentUnderTest().getSpecificity(this.patient));
    }
}
