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
package org.phenotips.data.similarity.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disease;
import org.phenotips.data.Patient;
import org.phenotips.data.Phenotype;
import org.phenotips.data.PhenotypeMetadatum;
import org.phenotips.data.similarity.PhenotypeMetadatumSimilarityScorer;
import org.phenotips.data.similarity.PhenotypeSimilarityScorer;
import org.phenotips.data.similarity.SimilarPatient;
import org.phenotips.data.similarity.SimilarPatientFactory;
import org.phenotips.data.similarity.internal.mocks.MockDisease;
import org.phenotips.data.similarity.internal.mocks.MockOntologyTerm;
import org.phenotips.data.similarity.internal.mocks.MockPhenotype;
import org.phenotips.data.similarity.internal.mocks.MockPhenotypeMetadatum;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the "restricted" {@link SimilarPatientFactory} implementation, {@link RestrictedSimilarPatientFactory}.
 * 
 * @version $Id$
 */
public class RestrictedSimilarPatientFactoryTest
{
    /** The matched patient document. */
    private static final DocumentReference PATIENT_1 = new DocumentReference("xwiki", "data", "P0000001");

    /** The default user used as the referrer of the matched patient, and of the reference patient for public access. */
    private static final DocumentReference USER_1 = new DocumentReference("xwiki", "XWiki", "padams");

    /** The alternative user used as the referrer of the reference patient for matchable or private access. */
    private static final DocumentReference USER_2 = new DocumentReference("xwiki", "XWiki", "hmccoy");

    @Rule
    public final MockitoComponentMockingRule<SimilarPatientFactory> mocker =
        new MockitoComponentMockingRule<SimilarPatientFactory>(RestrictedSimilarPatientFactory.class);

    /** Basic tests for makeSimilarPatient. */
    @Test
    public void testMakeSimilarPatient() throws ComponentLookupException
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        SimilarPatient result = this.mocker.getComponentUnderTest().makeSimilarPatient(mockMatch, mockReference);
        Assert.assertNotNull(result);
        Assert.assertSame(PATIENT_1, result.getDocument());
        Assert.assertSame(mockReference, result.getReference());
    }

    /** Pairing with a matchable patient does indeed restrict access to private information. */
    @Test
    public void testMakeSimilarPatientIsRestricted() throws ComponentLookupException
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_2);

        Map<String, PhenotypeMetadatum> matchMeta = new HashMap<String, PhenotypeMetadatum>();
        matchMeta.put("age_of_onset", new MockPhenotypeMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        matchMeta.put("speed_of_onset", new MockPhenotypeMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        matchMeta.put("pace", new MockPhenotypeMetadatum("HP:0003677", "Slow", "pace"));
        Map<String, PhenotypeMetadatum> referenceMeta = new HashMap<String, PhenotypeMetadatum>();
        referenceMeta.put("age_of_onset", new MockPhenotypeMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("speed_of_onset", new MockPhenotypeMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        referenceMeta.put("death", new MockPhenotypeMetadatum("HP:0003826", "Stillbirth", "death"));

        Phenotype jhm = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", true);
        Phenotype od = new MockPhenotype("HP:0012165", "Oligodactyly", "phenotype", true);
        Phenotype cat = new MockPhenotype("HP:0000518", "Cataract", "phenotype", true);
        Phenotype id = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", matchMeta, false);
        Phenotype mid =
            new MockPhenotype("HP:0001256", "Mild intellectual disability", "phenotype", referenceMeta, true);
        Set<Phenotype> matchPhenotypes = new HashSet<Phenotype>();
        Set<Phenotype> referencePhenotypes = new HashSet<Phenotype>();
        matchPhenotypes.add(jhm);
        matchPhenotypes.add(od);
        matchPhenotypes.add(id);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);
        Mockito.<Set<? extends Phenotype>> when(mockMatch.getPhenotypes()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Phenotype>> when(mockReference.getPhenotypes()).thenReturn(referencePhenotypes);

        Disease d1 = new MockDisease("MIM:123", "Some disease");
        Disease d2 = new MockDisease("MIM:234", "Some other disease");
        Disease d3 = new MockDisease("MIM:345", "Yet another disease");
        Set<Disease> matchDiseases = new HashSet<Disease>();
        matchDiseases.add(d1);
        matchDiseases.add(d2);
        Set<Disease> referenceDiseases = new HashSet<Disease>();
        referenceDiseases.add(d1);
        referenceDiseases.add(d3);
        Mockito.<Set<? extends Disease>> when(mockMatch.getDiseases()).thenReturn(matchDiseases);
        Mockito.<Set<? extends Disease>> when(mockReference.getDiseases()).thenReturn(referenceDiseases);

        SimilarPatient result = this.mocker.getComponentUnderTest().makeSimilarPatient(mockMatch, mockReference);
        Assert.assertNotNull(result);
        Assert.assertSame(mockReference, result.getReference());
        Assert.assertNull(result.getDocument());
        Assert.assertEquals(2, result.getPhenotypes().size());
        Assert.assertEquals(0, result.getDiseases().size());
    }

    /** Missing reference throws exception. */
    @Test(expected = IllegalArgumentException.class)
    public void testMakeSimilarPatientWithNullReference() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().makeSimilarPatient(mock(Patient.class), null);
    }

    /** Missing match throws exception. */
    @Test(expected = IllegalArgumentException.class)
    public void testMakeSimilarPatientWithNullMatch() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().makeSimilarPatient(null, mock(Patient.class));
    }

    @Before
    public void setupComponents() throws ComponentLookupException
    {
        ComponentManager cm = mock(ComponentManager.class);

        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(cm);
        // when(cm.getInstance(Mockito.any(Class.class))).thenThrow(
        // new ComponentLookupException("No implementation for this role"));
        OntologyManager om = mock(OntologyManager.class);

        // Setup the phenotype scorer
        PhenotypeSimilarityScorer scorer = new DefaultPhenotypeSimilarityScorer();
        ReflectionUtils.setFieldValue(scorer, "ontologyManager", om);
        doReturn(scorer).when(cm).getInstance(PhenotypeSimilarityScorer.class);

        // Setup the metadata scorers
        when(cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "pace")).thenReturn(
            new PaceOfProgressionPhenotypeMetadatumSimilarityScorer());
        when(cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "age_of_onset")).thenReturn(
            new AgeOfOnsetPhenotypeMetadatumSimilarityScorer());
        when(cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "speed_of_onset")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        when(cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "death")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        doReturn(new DefaultPhenotypeMetadatumSimilarityScorer()).when(cm).getInstance(
            PhenotypeMetadatumSimilarityScorer.class);

        // Setup the ontology manager
        doReturn(om).when(cm).getInstance(OntologyManager.class);
        Set<OntologyTerm> ancestors = new HashSet<OntologyTerm>();

        OntologyTerm all =
            new MockOntologyTerm("HP:0000001", Collections.<OntologyTerm> emptySet(),
                Collections.<OntologyTerm> emptySet());
        ancestors.add(all);
        OntologyTerm phenotypes =
            new MockOntologyTerm("HP:0000118", Collections.singleton(all), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(phenotypes);
        OntologyTerm abnormalNS =
            new MockOntologyTerm("HP:0000707", Collections.singleton(phenotypes), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalNS);
        OntologyTerm abnormalCNS =
            new MockOntologyTerm("HP:0002011", Collections.singleton(abnormalNS), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalCNS);
        OntologyTerm abnormalHMF =
            new MockOntologyTerm("HP:0011446", Collections.singleton(abnormalCNS), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalHMF);
        OntologyTerm cognImp =
            new MockOntologyTerm("HP:0100543", Collections.singleton(abnormalHMF), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(cognImp);
        OntologyTerm intDis =
            new MockOntologyTerm("HP:0001249", Collections.singleton(cognImp), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(intDis);
        OntologyTerm mildIntDis =
            new MockOntologyTerm("HP:0001256", Collections.singleton(intDis), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(mildIntDis);
        for (OntologyTerm term : ancestors) {
            when(om.resolveTerm(term.getId())).thenReturn(term);
        }

        ancestors.clear();
        ancestors.add(all);
        ancestors.add(phenotypes);
        OntologyTerm abnormalSkelS =
            new MockOntologyTerm("HP:0000924", Collections.singleton(phenotypes), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalSkelS);
        OntologyTerm abnormalSkelM =
            new MockOntologyTerm("HP:0011842", Collections.singleton(abnormalSkelS), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(abnormalSkelM);
        OntologyTerm abnormalJointMorph =
            new MockOntologyTerm("HP:0001367", Collections.singleton(abnormalSkelM), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(abnormalJointMorph);
        OntologyTerm abnormalJointMob =
            new MockOntologyTerm("HP:0011729", Collections.singleton(abnormalJointMorph), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(abnormalJointMob);
        OntologyTerm jointHyperm =
            new MockOntologyTerm("HP:0001382", Collections.singleton(abnormalJointMob), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(jointHyperm);
        for (OntologyTerm term : ancestors) {
            when(om.resolveTerm(term.getId())).thenReturn(term);
        }
    }
}
