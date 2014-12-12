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

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;
import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.cache.CacheException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OmimInformationContentPatientScorerTest
{
    @Mock
    private Patient patient;

    private Set<Feature> features = new LinkedHashSet<>();

    private OntologyService hpo;

    private OntologyService omim;

    @Rule
    public final MockitoComponentMockingRule<PatientScorer> mocker =
        new MockitoComponentMockingRule<PatientScorer>(OmimInformationContentPatientScorer.class);

    @Before
    public void setup() throws CacheException, ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Feature feature = mock(Feature.class);
        when(feature.getId()).thenReturn("HP:1");
        when(feature.isPresent()).thenReturn(true);
        this.features.add(feature);
        feature = mock(Feature.class);
        when(feature.getId()).thenReturn("HP:2");
        when(feature.isPresent()).thenReturn(false);
        this.features.add(feature);
        feature = mock(Feature.class);
        when(feature.getId()).thenReturn("HP:3");
        when(feature.isPresent()).thenReturn(true);
        this.features.add(feature);
        feature = mock(Feature.class);
        when(feature.getName()).thenReturn("custom");
        this.features.add(feature);

        this.hpo = this.mocker.getInstance(OntologyService.class, "hpo");
        OntologyTerm hp3 = mock(OntologyTerm.class);
        OntologyTerm hp4 = mock(OntologyTerm.class);
        when(this.hpo.getTerm("HP:3")).thenReturn(hp3);
        when(hp3.getParents()).thenReturn(Collections.singleton(hp4));
        when(hp4.getId()).thenReturn("HP:4");

        this.omim = this.mocker.getInstance(OntologyService.class, "omim");
        when(this.omim.count(Collections.singletonMap("symptom", "HP:0000001"))).thenReturn(60L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:1"))).thenReturn(3L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:2"))).thenReturn(1L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:3"))).thenReturn(0L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:4"))).thenReturn(2L);
    }

    @Test
    public void getScoreWithNoFeaturesReturns0() throws ComponentLookupException
    {
        Mockito.doReturn(Collections.emptySet()).when(this.patient).getFeatures();
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(0.0, score, 0.0);
    }

    @Test
    public void getScoreUsesInformationContent() throws Exception
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        double score = this.mocker.getComponentUnderTest().getScore(this.patient);
        Assert.assertEquals(0.56, score, 0.01);
    }

    @Test
    public void getSpecificityWithNoFeaturesReturns0() throws ComponentLookupException
    {
        Mockito.doReturn(Collections.emptySet()).when(this.patient).getFeatures();
        PatientSpecificity spec = this.mocker.getComponentUnderTest().getSpecificity(this.patient);
        Assert.assertEquals(0.0, spec.getScore(), 0.0);
        Assert.assertEquals("local-omim", spec.getComputingMethod());
    }

    @Test
    public void getSpecificityUsesInformationContent() throws Exception
    {
        Mockito.doReturn(this.features).when(this.patient).getFeatures();
        PatientSpecificity spec = this.mocker.getComponentUnderTest().getSpecificity(this.patient);
        Assert.assertEquals(0.56, spec.getScore(), 0.01);
        Assert.assertEquals("local-omim", spec.getComputingMethod());
    }

    @Test
    public void getScoreWithNonInformativeFeaturesReturns0() throws ComponentLookupException
    {
        OntologyTerm hp10 = mock(OntologyTerm.class);
        OntologyTerm hp11 = mock(OntologyTerm.class);
        OntologyTerm hp12 = mock(OntologyTerm.class);
        OntologyTerm hp13 = mock(OntologyTerm.class);
        OntologyTerm hp14 = mock(OntologyTerm.class);
        OntologyTerm hp15 = mock(OntologyTerm.class);
        when(this.hpo.getTerm("HP:10")).thenReturn(hp10);
        when(this.hpo.getTerm("HP:11")).thenReturn(hp11);
        when(this.hpo.getTerm("HP:12")).thenReturn(hp12);
        when(this.hpo.getTerm("HP:13")).thenReturn(hp13);
        when(this.hpo.getTerm("HP:14")).thenReturn(hp14);
        when(this.hpo.getTerm("HP:15")).thenReturn(hp15);
        when(hp10.getId()).thenReturn("HP:10");
        when(hp11.getId()).thenReturn("HP:11");
        when(hp12.getId()).thenReturn("HP:12");
        when(hp13.getId()).thenReturn("HP:13");
        when(hp14.getId()).thenReturn("HP:14");
        when(hp15.getId()).thenReturn("HP:15");
        when(hp10.getParents()).thenReturn(Collections.singleton(hp11));
        when(hp11.getParents()).thenReturn(Collections.singleton(hp12));
        when(hp12.getParents()).thenReturn(Collections.singleton(hp13));
        when(hp13.getParents()).thenReturn(Collections.singleton(hp14));
        when(hp14.getParents()).thenReturn(Collections.singleton(hp15));

        when(this.omim.count(Collections.singletonMap("symptom", "HP:10"))).thenReturn(0L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:11"))).thenReturn(0L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:12"))).thenReturn(0L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:13"))).thenReturn(0L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:14"))).thenReturn(0L);
        when(this.omim.count(Collections.singletonMap("symptom", "HP:15"))).thenReturn(0L);

        Feature feature = mock(Feature.class);
        when(feature.getId()).thenReturn("HP:10");
        when(feature.isPresent()).thenReturn(true);
        Mockito.doReturn(Collections.singleton(feature)).when(this.patient).getFeatures();

        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(this.patient), 0.0);
    }

    @Test
    public void getScoreWithParentlessTermDoesntThrowException() throws ComponentLookupException
    {
        OntologyTerm hp10 = mock(OntologyTerm.class);
        when(this.hpo.getTerm("HP:10")).thenReturn(hp10);
        when(hp10.getId()).thenReturn("HP:10");
        when(hp10.getParents()).thenReturn(Collections.<OntologyTerm>emptySet());
        when(this.omim.count(Collections.singletonMap("symptom", "HP:10"))).thenReturn(0L);

        Feature feature = mock(Feature.class);
        when(feature.getId()).thenReturn("HP:10");
        when(feature.isPresent()).thenReturn(true);
        Mockito.doReturn(Collections.singleton(feature)).when(this.patient).getFeatures();

        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(this.patient), 0.0);
    }
}
