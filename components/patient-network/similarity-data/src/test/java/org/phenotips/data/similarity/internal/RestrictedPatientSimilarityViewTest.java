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
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Patient;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityScorer;
import org.phenotips.data.similarity.FeatureSimilarityScorer;
import org.phenotips.data.similarity.PatientSimilarityView;
import org.phenotips.data.similarity.internal.mocks.MockDisorder;
import org.phenotips.data.similarity.internal.mocks.MockFeature;
import org.phenotips.data.similarity.internal.mocks.MockFeatureMetadatum;
import org.phenotips.data.similarity.internal.mocks.MockOntologyTerm;
import org.phenotips.data.similarity.permissions.internal.MatchAccessLevel;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the "restricted" {@link PatientSimilarityView} implementation, {@link RestrictedPatientSimilarityView}.
 * 
 * @version $Id$
 */
public class RestrictedPatientSimilarityViewTest
{
    /** The matched patient document. */
    private static final DocumentReference PATIENT_1 = new DocumentReference("xwiki", "data", "P0000001");

    /** The default user used as the referrer of the matched patient, and of the reference patient for public access. */
    private static final DocumentReference USER_1 = new DocumentReference("xwiki", "XWiki", "padams");

    /** The alternative user used as the referrer of the reference patient for matchable or private access. */
    private static final DocumentReference USER_2 = new DocumentReference("xwiki", "XWiki", "hmccoy");

    private static AccessType open;

    private static AccessType limited;

    private static AccessType priv;

    /** Mocked component manager. */
    private ComponentManager cm;

    @BeforeClass
    public static void setupAccessTypes()
    {
        open = mock(AccessType.class);
        when(open.isOpenAccess()).thenReturn(true);
        when(open.isLimitedAccess()).thenReturn(false);
        when(open.isPrivateAccess()).thenReturn(false);
        when(open.toString()).thenReturn("owner");
        when(open.getAccessLevel()).thenReturn(new OwnerAccessLevel());

        limited = mock(AccessType.class);
        when(limited.isOpenAccess()).thenReturn(false);
        when(limited.isLimitedAccess()).thenReturn(true);
        when(limited.isPrivateAccess()).thenReturn(false);
        when(limited.toString()).thenReturn("match");
        when(limited.getAccessLevel()).thenReturn(new MatchAccessLevel());

        priv = mock(AccessType.class);
        when(priv.isOpenAccess()).thenReturn(false);
        when(priv.isLimitedAccess()).thenReturn(false);
        when(priv.isPrivateAccess()).thenReturn(true);
        when(priv.toString()).thenReturn("none");
        when(priv.getAccessLevel()).thenReturn(new NoAccessLevel());
    }

    /** Missing match throws exception. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithMissingMatch()
    {
        Patient mockReference = mock(Patient.class);
        new RestrictedPatientSimilarityView(null, mockReference, null);
    }

    /** Missing reference throws exception. */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithMissingReference()
    {
        Patient mockMatch = mock(Patient.class);
        new RestrictedPatientSimilarityView(mockMatch, null, null);
    }

    /** The document is disclosed for public patients. */
    @Test
    public void testGetAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        Assert.assertEquals("match", o.getAccess().getName());
    }

    /** The document is disclosed for public patients. */
    @Test
    public void testGetDocumentWithPublicAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        Assert.assertSame(PATIENT_1, o.getDocument());
    }

    /** The document is not disclosed for matchable patients. */
    @Test
    public void testGetDocumentWithMatchAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        Assert.assertNull(o.getDocument());
    }

    /** The document is not disclosed for private patients. */
    @Test
    public void testGetDocumentWithNoAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);
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

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
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

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        Assert.assertNull(o.getReporter());
    }

    /** The reporter is not disclosed for private patients. */
    @Test
    public void testGetReporterWithNoAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);
        Assert.assertNull(o.getReporter());
    }

    /** The reference is always disclosed. */
    @Test
    public void testGetReference()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);
        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);
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

        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature od = new MockFeature("HP:0012165", "Oligodactyly", "phenotype", true);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", false);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);

        matchPhenotypes.add(jhm);
        matchPhenotypes.add(id);
        matchPhenotypes.add(od);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        Set<? extends Feature> phenotypes = o.getFeatures();
        Assert.assertEquals(3, phenotypes.size());
        for (Feature p : phenotypes) {
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

        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature od = new MockFeature("HP:0012165", "Oligodactyly", "phenotype", true);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", false);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);

        matchPhenotypes.add(jhm);
        matchPhenotypes.add(id);
        matchPhenotypes.add(od);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        Set<? extends Feature> phenotypes = o.getFeatures();
        Assert.assertEquals(2, phenotypes.size());
        for (Feature p : phenotypes) {
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

        // Define phenotypes for later use
        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature od = new MockFeature("HP:0012165", "Oligodactyly", "phenotype", true);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", false);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);

        matchPhenotypes.add(jhm);
        matchPhenotypes.add(id);
        matchPhenotypes.add(od);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);
        Set<? extends Feature> phenotypes = o.getFeatures();
        Assert.assertTrue(phenotypes.isEmpty());
    }

    /** All the patient's diseases are disclosed for public patients. */
    @Test
    public void testGetDiseasesWithPublicAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);

        Set<Disorder> matchDiseases = new HashSet<Disorder>();
        matchDiseases.add(new MockDisorder("MIM:123", "Some disease"));
        matchDiseases.add(new MockDisorder("MIM:234", "Some other disease"));
        Mockito.<Set<? extends Disorder>> when(mockMatch.getDisorders()).thenReturn(matchDiseases);
        Set<Disorder> referenceDiseases = new HashSet<Disorder>();
        referenceDiseases.add(new MockDisorder("MIM:123", "Some disease"));
        referenceDiseases.add(new MockDisorder("MIM:345", "Some new disease"));
        Mockito.<Set<? extends Disorder>> when(mockReference.getDisorders()).thenReturn(referenceDiseases);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        Set<? extends Disorder> matchedDiseases = o.getDisorders();
        Assert.assertEquals(2, matchedDiseases.size());
    }

    /** Diseases aren't disclosed for matchable patients. */
    @Test
    public void testGetDiseasesWithMatchAccess()
    {
        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);

        Set<Disorder> matchDiseases = new HashSet<Disorder>();
        matchDiseases.add(new MockDisorder("MIM:123", "Some disease"));
        matchDiseases.add(new MockDisorder("MIM:234", "Some other disease"));
        Mockito.<Set<? extends Disorder>> when(mockMatch.getDisorders()).thenReturn(matchDiseases);
        Set<Disorder> referenceDiseases = new HashSet<Disorder>();
        referenceDiseases.add(new MockDisorder("MIM:123", "Some disease"));
        referenceDiseases.add(new MockDisorder("MIM:345", "Some new disease"));
        Mockito.<Set<? extends Disorder>> when(mockReference.getDisorders()).thenReturn(referenceDiseases);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        Set<? extends Disorder> matchedDiseases = o.getDisorders();
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

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();

        // Define phenotypes for later use
        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature od = new MockFeature("HP:0012165", "Oligodactyly", "phenotype", true);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", matchMeta, false);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", referenceMeta, true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);
        // Define diseases for later use
        Disorder d1 = new MockDisorder("MIM:123", "Some disease");
        Disorder d2 = new MockDisorder("MIM:234", "Some other disease");
        Disorder d3 = new MockDisorder("MIM:345", "Yet another disease");
        Set<Disorder> matchDiseases = new HashSet<Disorder>();
        Set<Disorder> referenceDiseases = new HashSet<Disorder>();
        Mockito.<Set<? extends Disorder>> when(mockMatch.getDisorders()).thenReturn(matchDiseases);
        Mockito.<Set<? extends Disorder>> when(mockReference.getDisorders()).thenReturn(referenceDiseases);

        // No phenotypes => 0 score
        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Only unpaired phenotypes => 0 score
        matchPhenotypes.add(id);
        referencePhenotypes.add(cat);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Opposite phenotypes => negative score
        referencePhenotypes.add(mid);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        Assert.assertEquals(-0.5, o.getScore(), 0.2);

        // Matching phenotypes => positive score
        matchPhenotypes.add(jhm);
        referencePhenotypes.add(jhm);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);
        double score = o.getScore();
        Assert.assertEquals(0.4, score, 0.2);

        // More unpaired phenotypes lower the score
        matchPhenotypes.add(od);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);
        double prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score < prevScore);
        Assert.assertTrue(score > 0);

        // Opposite metadata lowers the score (but since phenotype is mismatched, this actually lowers a penalty)
        matchMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score > prevScore);
        Assert.assertTrue(score > 0);
        Assert.assertTrue(score < 1);

        // Positive metadata matches increase the score (again, increases the penalty)
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score < prevScore);
        Assert.assertTrue(score > 0);
        Assert.assertTrue(score < 1);

        // Unmatched metadata don't affect the score
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003677", "Slow", "pace"));
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        prevScore = score;
        score = o.getScore();
        Assert.assertEquals(prevScore, score, 1.0E-5);

        // Matching diseases increase the score
        matchDiseases.add(d1);
        referenceDiseases.add(d1);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        prevScore = score;
        score = o.getScore();
        Assert.assertTrue(score > prevScore);
        Assert.assertTrue(score < 1);

        // Unmatched diseases don't affect the score
        matchDiseases.add(d2);
        referenceDiseases.add(d3);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
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
            .getInstance(FeatureSimilarityScorer.class);

        Patient mockMatch = mock(Patient.class);
        Patient mockReference = mock(Patient.class);

        when(mockMatch.getDocument()).thenReturn(PATIENT_1);
        when(mockMatch.getReporter()).thenReturn(USER_1);
        when(mockReference.getReporter()).thenReturn(USER_1);

        // Define phenotypes for later use
        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature jhmN = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", false);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", true);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);

        // No phenotypes => 0 score
        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Opposite phenotypes => negative score
        matchPhenotypes.add(jhm);
        referencePhenotypes.add(jhmN);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);
        double score = o.getScore();
        Assert.assertEquals(-1.0, score, 1.0E-5);

        // Unpaired phenotypes don't affect a negative score
        matchPhenotypes.add(id);
        referencePhenotypes.add(cat);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);
        double prevScore = score;
        score = o.getScore();
        Assert.assertEquals(prevScore, score, 1.0E-5);

        // Related phenotypes don't affect the score, since they can't be used in the absence of a real scorer
        referencePhenotypes.add(mid);
        o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);
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

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        matchMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003677", "Slow", "pace"));
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));

        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature od = new MockFeature("HP:0012165", "Oligodactyly", "phenotype", true);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", matchMeta, false);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", referenceMeta, true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        matchPhenotypes.add(jhm);
        matchPhenotypes.add(od);
        matchPhenotypes.add(id);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);

        Disorder d1 = new MockDisorder("MIM:123", "Some disease");
        Disorder d2 = new MockDisorder("MIM:234", "Some other disease");
        Disorder d3 = new MockDisorder("MIM:345", "Yet another disease");
        Set<Disorder> matchDiseases = new HashSet<Disorder>();
        matchDiseases.add(d1);
        matchDiseases.add(d2);
        Set<Disorder> referenceDiseases = new HashSet<Disorder>();
        referenceDiseases.add(d1);
        referenceDiseases.add(d3);
        Mockito.<Set<? extends Disorder>> when(mockMatch.getDisorders()).thenReturn(matchDiseases);
        Mockito.<Set<? extends Disorder>> when(mockReference.getDisorders()).thenReturn(referenceDiseases);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("P0000001", result.getString("id"));
        Assert.assertEquals("owner", result.getString("access"));
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

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        matchMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003677", "Slow", "pace"));
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));

        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature od = new MockFeature("HP:0012165", "Oligodactyly", "phenotype", true);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", matchMeta, false);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", referenceMeta, true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        matchPhenotypes.add(jhm);
        matchPhenotypes.add(od);
        matchPhenotypes.add(id);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);

        Disorder d1 = new MockDisorder("MIM:123", "Some disease");
        Disorder d2 = new MockDisorder("MIM:234", "Some other disease");
        Disorder d3 = new MockDisorder("MIM:345", "Yet another disease");
        Set<Disorder> matchDiseases = new HashSet<Disorder>();
        matchDiseases.add(d1);
        matchDiseases.add(d2);
        Set<Disorder> referenceDiseases = new HashSet<Disorder>();
        referenceDiseases.add(d1);
        referenceDiseases.add(d3);
        Mockito.<Set<? extends Disorder>> when(mockMatch.getDisorders()).thenReturn(matchDiseases);
        Mockito.<Set<? extends Disorder>> when(mockReference.getDisorders()).thenReturn(referenceDiseases);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, limited);

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

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        matchMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003677", "Slow", "pace"));
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));

        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Feature od = new MockFeature("HP:0012165", "Oligodactyly", "phenotype", true);
        Feature cat = new MockFeature("HP:0000518", "Cataract", "phenotype", true);
        Feature id = new MockFeature("HP:0001249", "Intellectual disability", "phenotype", matchMeta, false);
        Feature mid = new MockFeature("HP:0001256", "Mild intellectual disability", "phenotype", referenceMeta, true);
        Set<Feature> matchPhenotypes = new HashSet<Feature>();
        Set<Feature> referencePhenotypes = new HashSet<Feature>();
        matchPhenotypes.add(jhm);
        matchPhenotypes.add(od);
        matchPhenotypes.add(id);
        referencePhenotypes.add(jhm);
        referencePhenotypes.add(mid);
        referencePhenotypes.add(cat);
        Mockito.<Set<? extends Feature>> when(mockMatch.getFeatures()).thenReturn(matchPhenotypes);
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(referencePhenotypes);

        Disorder d1 = new MockDisorder("MIM:123", "Some disease");
        Disorder d2 = new MockDisorder("MIM:234", "Some other disease");
        Disorder d3 = new MockDisorder("MIM:345", "Yet another disease");
        Set<Disorder> matchDiseases = new HashSet<Disorder>();
        matchDiseases.add(d1);
        matchDiseases.add(d2);
        Set<Disorder> referenceDiseases = new HashSet<Disorder>();
        referenceDiseases.add(d1);
        referenceDiseases.add(d3);
        Mockito.<Set<? extends Disorder>> when(mockMatch.getDisorders()).thenReturn(matchDiseases);
        Mockito.<Set<? extends Disorder>> when(mockReference.getDisorders()).thenReturn(referenceDiseases);

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, priv);

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

        Feature jhm = new MockFeature("HP:0001382", "Joint hypermobility", "phenotype", true);
        Disorder d = new MockDisorder("MIM:234", "Some other disease");
        Mockito.<Set<? extends Feature>> when(mockReference.getFeatures()).thenReturn(Collections.singleton(jhm));
        Mockito.<Set<? extends Disorder>> when(mockReference.getDisorders()).thenReturn(Collections.singleton(d));

        PatientSimilarityView o = new RestrictedPatientSimilarityView(mockMatch, mockReference, open);

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
        FeatureSimilarityScorer scorer = new DefaultFeatureSimilarityScorer();
        ReflectionUtils.setFieldValue(scorer, "ontologyManager", om);
        doReturn(scorer).when(this.cm).getInstance(FeatureSimilarityScorer.class);

        // Setup the metadata scorers
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class, "pace")).thenReturn(
            new PaceOfProgressionFeatureMetadatumSimilarityScorer());
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class, "age_of_onset")).thenReturn(
            new AgeOfOnsetFeatureMetadatumSimilarityScorer());
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class, "speed_of_onset")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class, "death")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        doReturn(new DefaultFeatureMetadatumSimilarityScorer()).when(this.cm).getInstance(
            FeatureMetadatumSimilarityScorer.class);

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
