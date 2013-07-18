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
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.PhenotypeMetadatumSimilarityScorer;
import org.phenotips.data.similarity.PhenotypeSimilarityScorer;
import org.phenotips.data.similarity.SimilarPatient;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the "restricted" {@link SimilarPatient} implementation, {@link RestrictedSimilarPatient}.
 * 
 * @version $Id$
 */
public class RestrictedSimilarPatientTest
{
    /** The matched patient document. */
    private static final DocumentReference PATIENT_1 = new DocumentReference("xwiki", "data", "P0000001");

    /** The default user used as the referrer of the matched patient, and of the reference patient for public access. */
    private static final DocumentReference USER_1 = new DocumentReference("xwiki", "XWiki", "padams");

    /** The alternative user used as the referrer of the reference patient for matchable or private access. */
    private static final DocumentReference USER_2 = new DocumentReference("xwiki", "XWiki", "hmccoy");

    /** Mocked component manager. */
    private ComponentManager cm;

    /** Missing match throws exception. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithMissingMatch()
    {
        Patient mockReference = mock(Patient.class);
        new RestrictedSimilarPatient(null, mockReference);
    }

    /** Missing reference throws exception. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithMissingReference()
    {
        Patient mockMatch = mock(Patient.class);
        new RestrictedSimilarPatient(mockMatch, null);
    }

    /** Same author results in owner access. */
    @Test
    public void testSameReferrerAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertSame(AccessType.OWNED, o.getAccess());
    }

    /** Different authors results in matchable access. */
    @Test
    public void testDifferentReferrerAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_2);
        // FIXME This isn't correct, revisit after proper access types are implemented

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertSame(AccessType.MATCH, o.getAccess());
    }

    /** Missing reference referrer results in private access. */
    @Test
    public void testMissingReferenceReferrerAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(null);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertSame(AccessType.PRIVATE, o.getAccess());
    }

    /** The document is disclosed for public patients. */
    @Test
    public void testGetDocumentWithPublicAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertSame(PATIENT_1, o.getDocument());
    }

    /** The document is not disclosed for matchable patients. */
    @Test
    public void testGetDocumentWithMatchAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_2);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertNull(o.getDocument());
    }

    /** The reporter is disclosed for public patients. */
    @Test
    public void testGetReporterWithPublicAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertSame(USER_1, o.getReporter());
    }

    /** The reporter is not disclosed for matchable patients. */
    @Test
    public void testGetReporterWithMatchAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_2);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertNull(o.getReporter());
    }

    /** The reference is always disclosed. */
    @Test
    public void testGetReference()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** All the patient's phenotypes are disclosed for public patients. */
    @Test
    public void testGetPhenotypesWithPublicAccess() throws ComponentLookupException
    {
        setupComponents();

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        Phenotype jhm = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", true);
        Phenotype od = new MockPhenotype("HP:0012165", "Oligodactyly", "phenotype", true);
        Phenotype cat = new MockPhenotype("HP:0000518", "Cataract", "phenotype", true);
        Phenotype id = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", false);
        Phenotype mid = new MockPhenotype("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Phenotype> matchPhenotypes = new HashSet<Phenotype>();
        Set<Phenotype> referencePhenotypes = new HashSet<Phenotype>();
        Mockito.<Set<? extends Phenotype>> when(mockMatch.getPhenotypes()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Phenotype>> when(mockReference.getPhenotypes()).thenReturn(referencePhenotypes);

        matchPhenotypes.add(jhm);
        matchPhenotypes.add(id);
        matchPhenotypes.add(od);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Set<? extends Phenotype> phenotypes = o.getPhenotypes();
        Assert.assertEquals(3, phenotypes.size());
        for (Phenotype p : phenotypes) {
            Assert.assertNotNull(p.getId());
        }
    }

    /** Only matching phenotypes are disclosed for matchable patients. */
    @Test
    public void testGetPhenotypesWithMatchAccess() throws ComponentLookupException
    {
        setupComponents();

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_2);

        Phenotype jhm = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", true);
        Phenotype od = new MockPhenotype("HP:0012165", "Oligodactyly", "phenotype", true);
        Phenotype cat = new MockPhenotype("HP:0000518", "Cataract", "phenotype", true);
        Phenotype id = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", false);
        Phenotype mid = new MockPhenotype("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Phenotype> matchPhenotypes = new HashSet<Phenotype>();
        Set<Phenotype> referencePhenotypes = new HashSet<Phenotype>();
        Mockito.<Set<? extends Phenotype>> when(mockMatch.getPhenotypes()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Phenotype>> when(mockReference.getPhenotypes()).thenReturn(referencePhenotypes);

        matchPhenotypes.add(jhm);
        matchPhenotypes.add(id);
        matchPhenotypes.add(od);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Set<? extends Phenotype> phenotypes = o.getPhenotypes();
        Assert.assertEquals(2, phenotypes.size());
        for (Phenotype p : phenotypes) {
            Assert.assertNull(p.getId());
        }
    }

    /** No phenotypes are disclosed for private patients. */
    @Test
    public void testGetPhenotypesWithPrivateAccess() throws ComponentLookupException
    {
        setupComponents();

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(null);

        // Define phenotypes for later use
        Phenotype jhm = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", true);
        Phenotype od = new MockPhenotype("HP:0012165", "Oligodactyly", "phenotype", true);
        Phenotype cat = new MockPhenotype("HP:0000518", "Cataract", "phenotype", true);
        Phenotype id = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", false);
        Phenotype mid = new MockPhenotype("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Phenotype> matchPhenotypes = new HashSet<Phenotype>();
        Set<Phenotype> referencePhenotypes = new HashSet<Phenotype>();
        Mockito.<Set<? extends Phenotype>> when(mockMatch.getPhenotypes()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Phenotype>> when(mockReference.getPhenotypes()).thenReturn(referencePhenotypes);

        matchPhenotypes.add(jhm);
        matchPhenotypes.add(id);
        matchPhenotypes.add(od);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Set<? extends Phenotype> phenotypes = o.getPhenotypes();
        Assert.assertTrue(phenotypes.isEmpty());
    }

    /** All the patient's diseases are disclosed for public patients. */
    @Test
    public void testGetDiseasesWithPublicAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        Set<Disease> matchDiseases = new HashSet<Disease>();
        matchDiseases.add(new MockDisease("MIM:123", "Some disease"));
        matchDiseases.add(new MockDisease("MIM:234", "Some other disease"));
        Mockito.<Set<? extends Disease>> when(mockMatch.getDiseases()).thenReturn(matchDiseases);
        Set<Disease> referenceDiseases = new HashSet<Disease>();
        referenceDiseases.add(new MockDisease("MIM:123", "Some disease"));
        referenceDiseases.add(new MockDisease("MIM:345", "Some new disease"));
        Mockito.<Set<? extends Disease>> when(mockReference.getDiseases()).thenReturn(referenceDiseases);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Set<? extends Disease> matchedDiseases = o.getDiseases();
        Assert.assertEquals(2, matchedDiseases.size());
    }

    /** Diseases aren't disclosed for matchable patients. */
    @Test
    public void testGetDiseasesWithMatchAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_2);

        Set<Disease> matchDiseases = new HashSet<Disease>();
        matchDiseases.add(new MockDisease("MIM:123", "Some disease"));
        matchDiseases.add(new MockDisease("MIM:234", "Some other disease"));
        Mockito.<Set<? extends Disease>> when(mockMatch.getDiseases()).thenReturn(matchDiseases);
        Set<Disease> referenceDiseases = new HashSet<Disease>();
        referenceDiseases.add(new MockDisease("MIM:123", "Some disease"));
        referenceDiseases.add(new MockDisease("MIM:345", "Some new disease"));
        Mockito.<Set<? extends Disease>> when(mockReference.getDiseases()).thenReturn(referenceDiseases);

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Set<? extends Disease> matchedDiseases = o.getDiseases();
        Assert.assertTrue(matchedDiseases.isEmpty());
    }

    /** Basic tests for score computation. */
    @Test
    public void testGetScore() throws ComponentLookupException
    {
        setupComponents();

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        Map<String, PhenotypeMetadatum> matchMeta = new HashMap<String, PhenotypeMetadatum>();
        Map<String, PhenotypeMetadatum> referenceMeta = new HashMap<String, PhenotypeMetadatum>();

        // Define phenotypes for later use
        Phenotype jhm = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", true);
        Phenotype od = new MockPhenotype("HP:0012165", "Oligodactyly", "phenotype", true);
        Phenotype cat = new MockPhenotype("HP:0000518", "Cataract", "phenotype", true);
        Phenotype id = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", matchMeta, false);
        Phenotype mid =
            new MockPhenotype("HP:0001256", "Mild intellectual disability", "phenotype", referenceMeta, true);
        Set<Phenotype> matchPhenotypes = new HashSet<Phenotype>();
        Set<Phenotype> referencePhenotypes = new HashSet<Phenotype>();
        Mockito.<Set<? extends Phenotype>> when(mockMatch.getPhenotypes()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Phenotype>> when(mockReference.getPhenotypes()).thenReturn(referencePhenotypes);
        // Define diseases for later use
        Disease d1 = new MockDisease("MIM:123", "Some disease");
        Disease d2 = new MockDisease("MIM:234", "Some other disease");
        Disease d3 = new MockDisease("MIM:345", "Yet another disease");
        Set<Disease> matchDiseases = new HashSet<Disease>();
        Set<Disease> referenceDiseases = new HashSet<Disease>();
        Mockito.<Set<? extends Disease>> when(mockMatch.getDiseases()).thenReturn(matchDiseases);
        Mockito.<Set<? extends Disease>> when(mockReference.getDiseases()).thenReturn(referenceDiseases);

        // No phenotypes => 0 score
        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Only unpaired phenotypes => 0 score
        matchPhenotypes.add(id);
        referencePhenotypes.add(cat);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Opposite phenotypes => negative score
        referencePhenotypes.add(mid);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertEquals(-0.5, o.getScore(), 0.2);

        // Matching phenotypes => positive score
        matchPhenotypes.add(jhm);
        referencePhenotypes.add(jhm);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        double score = o.getScore();
        Assert.assertEquals(0.4, score, 0.2);

        // More unpaired phenotypes lower the score
        matchPhenotypes.add(od);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        double prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score < prevScore);
        Assert.assertTrue(score > 0);

        // Opposite metadata lowers the score (but since phenotype is mismatched, this actually lowers a penalty)
        matchMeta.put("speed_of_onset", new MockPhenotypeMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        referenceMeta.put("speed_of_onset", new MockPhenotypeMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score > prevScore);
        Assert.assertTrue(score > 0);
        Assert.assertTrue(score < 1);

        // Positive metadata matches increase the score (again, increases the penalty)
        matchMeta.put("age_of_onset", new MockPhenotypeMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("age_of_onset", new MockPhenotypeMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score < prevScore);
        Assert.assertTrue(score > 0);
        Assert.assertTrue(score < 1);

        // Unmatched metadata don't affect the score
        matchMeta.put("pace", new MockPhenotypeMetadatum("HP:0003677", "Slow", "pace"));
        referenceMeta.put("death", new MockPhenotypeMetadatum("HP:0003826", "Stillbirth", "death"));
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        prevScore = score;
        score = o.getScore();
        Assert.assertEquals(prevScore, score, 1.0E-5);

        // Matching diseases increase the score
        matchDiseases.add(d1);
        referenceDiseases.add(d1);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score > prevScore);
        Assert.assertTrue(score < 1);

        // Unmatched diseases don't affect the score
        matchDiseases.add(d2);
        referenceDiseases.add(d3);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        prevScore = score;
        score = o.getScore();
        Assert.assertEquals(prevScore, score, 1.0E-5);
    }

    /** When no specific scorer is available, only exact phenotype matches are taken into account. */
    @Test
    public void testGetScoreWithNoPhenotypeScorer() throws ComponentLookupException
    {
        setupComponents();

        Mockito.doThrow(new ComponentLookupException("No implementation for this role")).when(this.cm)
            .getInstance(PhenotypeSimilarityScorer.class);

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        // Define phenotypes for later use
        Phenotype jhm = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", true);
        Phenotype jhmN = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", false);
        Phenotype od = new MockPhenotype("HP:0012165", "Oligodactyly", "phenotype", true);
        Phenotype cat = new MockPhenotype("HP:0000518", "Cataract", "phenotype", true);
        Phenotype id = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Phenotype mid = new MockPhenotype("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Phenotype> matchPhenotypes = new HashSet<Phenotype>();
        Set<Phenotype> referencePhenotypes = new HashSet<Phenotype>();
        Mockito.<Set<? extends Phenotype>> when(mockMatch.getPhenotypes()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Phenotype>> when(mockReference.getPhenotypes()).thenReturn(referencePhenotypes);

        // No phenotypes => 0 score
        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Opposite phenotypes => negative score
        matchPhenotypes.add(jhm);
        referencePhenotypes.add(jhmN);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        double score = o.getScore();
        Assert.assertEquals(-1.0, score, 1.0E-5);

        // Unpaired phenotypes don't affect a negative score
        matchPhenotypes.add(id);
        referencePhenotypes.add(cat);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        double prevScore = score;
        score = o.getScore();
        Assert.assertEquals(prevScore, score, 1.0E-5);

        // Related phenotypes don't affect the score, since they can't be used in the absence of a real scorer
        referencePhenotypes.add(mid);
        o = new RestrictedSimilarPatient(mockMatch, mockReference);
        prevScore = score;
        score = o.getScore();
        Assert.assertEquals(prevScore, score, 1.0E-5);
    }

    /** Basic JSON tests. */
    @Test
    public void testToJSON() throws ComponentLookupException
    {
        setupComponents();

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

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

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);

        JSONObject result = o.toJSON();
        Assert.assertEquals("P0000001", result.getString("id"));
        Assert.assertEquals("owned", result.getString("access"));
        Assert.assertEquals("padams", result.getString("owner"));
        Assert.assertTrue(result.getBoolean("myCase"));
        Assert.assertEquals(0.5, result.getDouble("score"), 0.1);
        Assert.assertEquals(3, result.getInt("featuresCount"));

        JSONArray features = result.getJSONArray("features");
        Assert.assertEquals(3, features.size());

        JSONObject jhmFeature = findFeature("HP:0001382", "id", features);
        Assert.assertNotNull(jhmFeature);
        Assert.assertEquals(1.0, jhmFeature.getDouble("score"), 1.0E-5);
        Assert.assertEquals("HP:0001382", jhmFeature.getString("id"));
        Assert.assertEquals("HP:0001382", jhmFeature.getString("queryId"));
        Assert.assertEquals("Joint hypermobility", jhmFeature.getString("name"));
        Assert.assertEquals("phenotype", jhmFeature.getString("type"));
        Assert.assertEquals("phenotype", jhmFeature.getString("queryType"));

        JSONObject odFeature = findFeature("HP:0012165", "id", features);
        Assert.assertNotNull(odFeature);
        Assert.assertFalse(odFeature.has("score"));
        Assert.assertEquals("HP:0012165", odFeature.getString("id"));
        Assert.assertFalse(odFeature.has("queryId"));
        Assert.assertEquals("Oligodactyly", odFeature.getString("name"));
        Assert.assertEquals("phenotype", odFeature.getString("type"));
        Assert.assertFalse(odFeature.has("queryType"));

        JSONObject idFeature = findFeature("HP:0001249", "id", features);
        Assert.assertNotNull(idFeature);
        Assert.assertEquals(-0.5, idFeature.getDouble("score"), 0.2);
        Assert.assertEquals("HP:0001249", idFeature.getString("id"));
        Assert.assertEquals("HP:0001256", idFeature.getString("queryId"));
        Assert.assertEquals("Intellectual disability", idFeature.getString("name"));
        Assert.assertEquals("phenotype", idFeature.getString("type"));
        Assert.assertEquals("phenotype", idFeature.getString("queryType"));
        Assert.assertFalse(idFeature.getBoolean("isPresent"));

        JSONArray meta = idFeature.getJSONArray("metadata");
        Assert.assertEquals(3, meta.size());

        JSONObject ageMeta = findFeature("age_of_onset", "type", meta);
        Assert.assertNotNull(ageMeta);
        Assert.assertEquals("HP:0003577", ageMeta.getString("id"));
        Assert.assertEquals("HP:0003577", ageMeta.getString("queryId"));
        Assert.assertEquals("age_of_onset", ageMeta.getString("type"));
        Assert.assertEquals("Congenital onset", ageMeta.getString("name"));

        JSONObject speedMeta = findFeature("speed_of_onset", "type", meta);
        Assert.assertNotNull(speedMeta);
        Assert.assertEquals("HP:0011010", speedMeta.getString("id"));
        Assert.assertEquals("HP:0011009", speedMeta.getString("queryId"));
        Assert.assertEquals("speed_of_onset", speedMeta.getString("type"));
        Assert.assertEquals("Chronic", speedMeta.getString("name"));

        JSONObject paceMeta = findFeature("pace", "type", meta);
        Assert.assertNotNull(paceMeta);
        Assert.assertEquals("HP:0003677", paceMeta.getString("id"));
        Assert.assertFalse(paceMeta.has("queryId"));
        Assert.assertEquals("pace", paceMeta.getString("type"));
        Assert.assertEquals("Slow", paceMeta.getString("name"));

        JSONArray disorders = result.getJSONArray("disorders");
        Assert.assertEquals(2, disorders.size());

        JSONObject d1Disorder = findFeature("MIM:123", "id", disorders);
        Assert.assertNotNull(d1Disorder);
        Assert.assertEquals(1.0, d1Disorder.getDouble("score"), 0.1);
        Assert.assertEquals("MIM:123", d1Disorder.getString("id"));
        Assert.assertEquals("MIM:123", d1Disorder.getString("queryId"));
        Assert.assertEquals("Some disease", d1Disorder.getString("name"));

        JSONObject d2Disorder = findFeature("MIM:234", "id", disorders);
        Assert.assertNotNull(d2Disorder);
        Assert.assertFalse(d2Disorder.has("score"));
        Assert.assertEquals("MIM:234", d2Disorder.getString("id"));
        Assert.assertFalse(d2Disorder.has("queryId"));
        Assert.assertEquals("Some other disease", d2Disorder.getString("name"));
    }

    /** No concrete information about the patient should be displayed for matchable patients. */
    @Test
    public void testToJSONWithMatchAccess() throws ComponentLookupException
    {
        setupComponents();

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

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);

        JSONObject result = o.toJSON();
        Assert.assertFalse(result.has("id"));
        Assert.assertEquals("match", result.getString("access"));
        Assert.assertFalse(result.has("owner"));
        Assert.assertFalse(result.getBoolean("myCase"));
        Assert.assertEquals(0.5, result.getDouble("score"), 0.1);
        Assert.assertEquals(3, result.getInt("featuresCount"));

        JSONArray features = result.getJSONArray("features");
        // Only matching features are returned
        Assert.assertEquals(2, features.size());

        JSONObject jhmFeature = findFeature("HP:0001382", "queryId", features);
        Assert.assertNotNull(jhmFeature);
        Assert.assertEquals(1.0, jhmFeature.getDouble("score"), 1.0E-5);
        Assert.assertFalse(jhmFeature.has("id"));
        Assert.assertEquals("HP:0001382", jhmFeature.getString("queryId"));
        Assert.assertFalse(jhmFeature.has("name"));
        Assert.assertFalse(jhmFeature.has("type"));
        Assert.assertEquals("phenotype", jhmFeature.getString("queryType"));

        JSONObject idFeature = findFeature("HP:0001256", "queryId", features);
        Assert.assertNotNull(idFeature);
        Assert.assertEquals(-0.5, idFeature.getDouble("score"), 0.2);
        Assert.assertFalse(idFeature.has("id"));
        Assert.assertEquals("HP:0001256", idFeature.getString("queryId"));
        Assert.assertFalse(idFeature.has("name"));
        Assert.assertFalse(idFeature.has("type"));
        Assert.assertEquals("phenotype", idFeature.getString("queryType"));
        Assert.assertFalse(idFeature.has("isPresent"));

        // Metadata aren't returned
        Assert.assertFalse(idFeature.has("metadata"));

        // Diseases aren't returned
        Assert.assertFalse(result.has("disorders"));
    }

    /** No information is disclosed for private access. */
    @Test
    public void testToJSONWithPrivateAccess() throws ComponentLookupException
    {
        setupComponents();

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(null);

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

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);

        // Nothing at all
        Assert.assertTrue(o.toJSON().isNullObject());
    }

    /** No "features" or "disorders" empty arrays are included when none are available. */
    @Test
    public void testToJSONWithNoPhenotypesOrDiseases() throws ComponentLookupException
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        Phenotype jhm = new MockPhenotype("HP:0001382", "Joint hypermobility", "phenotype", true);
        Disease d = new MockDisease("MIM:234", "Some other disease");
        Mockito.<Set<? extends Phenotype>> when(mockReference.getPhenotypes()).thenReturn(Collections.singleton(jhm));
        Mockito.<Set<? extends Disease>> when(mockReference.getDiseases()).thenReturn(Collections.singleton(d));

        SimilarPatient o = new RestrictedSimilarPatient(mockMatch, mockReference);

        JSONObject result = o.toJSON();
        Assert.assertFalse(result.has("features"));
        Assert.assertFalse(result.has("disorders"));
    }

    private void setupComponents() throws ComponentLookupException
    {
        this.cm = mock(ComponentManager.class);

        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(Mockito.any(Class.class))).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        OntologyManager om = mock(OntologyManager.class);

        // Setup the phenotype scorer
        PhenotypeSimilarityScorer scorer = new DefaultPhenotypeSimilarityScorer();
        ReflectionUtils.setFieldValue(scorer, "ontologyManager", om);
        doReturn(scorer).when(this.cm).getInstance(PhenotypeSimilarityScorer.class);

        // Setup the metadata scorers
        when(this.cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "pace")).thenReturn(
            new PaceOfProgressionPhenotypeMetadatumSimilarityScorer());
        when(this.cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "age_of_onset")).thenReturn(
            new AgeOfOnsetPhenotypeMetadatumSimilarityScorer());
        when(this.cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "speed_of_onset")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        when(this.cm.getInstance(PhenotypeMetadatumSimilarityScorer.class, "death")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        doReturn(new DefaultPhenotypeMetadatumSimilarityScorer()).when(this.cm).getInstance(
            PhenotypeMetadatumSimilarityScorer.class);

        // Setup the ontology manager
        doReturn(om).when(this.cm).getInstance(OntologyManager.class);
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

    private JSONObject findFeature(String value, String key, JSONArray features)
    {
        for (int i = 0; i < features.size(); ++i) {
            JSONObject feature = features.getJSONObject(i);
            if (StringUtils.equals(value, feature.getString(key))) {
                return feature;
            }
        }
        return null;
    }
}
