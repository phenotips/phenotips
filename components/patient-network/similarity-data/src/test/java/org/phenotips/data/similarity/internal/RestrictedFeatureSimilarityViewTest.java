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
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityScorer;
import org.phenotips.data.similarity.FeatureSimilarityScorer;
import org.phenotips.data.similarity.FeatureSimilarityView;
import org.phenotips.data.similarity.internal.mocks.MockFeatureMetadatum;
import org.phenotips.data.similarity.internal.mocks.MockOntologyTerm;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the "restricted" {@link FeatureSimilarityView} implementation, {@link RestrictedFeatureSimilarityView}.
 * 
 * @version $Id$
 */
public class RestrictedFeatureSimilarityViewTest
{
    private static AccessType open;

    private static AccessType limited;

    private static AccessType priv;

    @BeforeClass
    public static void setupAccessTypes()
    {
        open = mock(AccessType.class);
        when(open.isOpenAccess()).thenReturn(true);
        when(open.isLimitedAccess()).thenReturn(false);
        when(open.isPrivateAccess()).thenReturn(false);
        when(open.toString()).thenReturn("owner");

        limited = mock(AccessType.class);
        when(limited.isOpenAccess()).thenReturn(false);
        when(limited.isLimitedAccess()).thenReturn(true);
        when(limited.isPrivateAccess()).thenReturn(false);
        when(limited.toString()).thenReturn("match");

        priv = mock(AccessType.class);
        when(priv.isOpenAccess()).thenReturn(false);
        when(priv.isLimitedAccess()).thenReturn(false);
        when(priv.isPrivateAccess()).thenReturn(true);
        when(priv.toString()).thenReturn("none");
    }

    /** Basic test for type retrieval. */
    @Test
    public void testGetType()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        when(mockMatch.getType()).thenReturn("prenatal_phenotype");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals("prenatal_phenotype", o.getType());
    }

    /** When the match is missing, the type is taken from the reference. */
    @Test
    public void testGetTypeWithNullMatch()
    {
        Feature mockReference = mock(Feature.class);
        when(mockReference.getType()).thenReturn("prenatal_phenotype");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);
        Assert.assertEquals("prenatal_phenotype", o.getType());
    }

    /** No NPE is thrown when both the match and reference are missing. */
    @Test
    public void testGetTypeWithNullMatchAndReference()
    {
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, null, open);
        Assert.assertNull(o.getType());
    }

    /** The access type shouldn't matter, the type is always available. */
    @Test
    public void testGetTypeWithPrivateAccess()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        when(mockMatch.getType()).thenReturn("prenatal_phenotype");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, priv);
        Assert.assertEquals("prenatal_phenotype", o.getType());
    }

    /** Basic test for ID retrieval. */
    @Test
    public void testGetId()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        when(mockMatch.getId()).thenReturn("ONTO:123");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals("ONTO:123", o.getId());
    }

    /** The ID is not disclosed for private patients. */
    @Test
    public void testGetIdWithPrivateAccess()
    {
        Feature mockMatch = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, priv);
        Assert.assertNull(o.getId());
        Mockito.verify(mockMatch, Mockito.never()).getId();
    }

    /** The ID is not disclosed for matchable patients. */
    @Test
    public void testGetIdWithMatchAccess()
    {
        Feature mockMatch = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, limited);
        Assert.assertNull(o.getId());
        Mockito.verify(mockMatch, Mockito.never()).getId();
    }

    /** Trying to retrieve the ID doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetIdWithNullMatch()
    {
        Feature mockReference = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);
        Assert.assertNull(o.getId());
        Mockito.verify(mockReference, Mockito.never()).getId();
    }

    /** Basic test for name retrieval. */
    @Test
    public void testGetName()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        when(mockMatch.getName()).thenReturn("Some phenotype");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals("Some phenotype", o.getName());
    }

    /** The name is not disclosed for private patients. */
    @Test
    public void testGetNameWithPrivateAccess()
    {
        Feature mockMatch = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, priv);
        Assert.assertNull(o.getName());
        Mockito.verify(mockMatch, Mockito.never()).getName();
    }

    /** The name is not disclosed for matchable patients. */
    @Test
    public void testGetNameWithMatchAccess()
    {
        Feature mockMatch = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, limited);
        Assert.assertNull(o.getName());
        Mockito.verify(mockMatch, Mockito.never()).getName();
    }

    /** Trying to retrieve the name doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetNameWithNullMatch()
    {
        Feature mockReference = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);
        Assert.assertNull(o.getName());
        Mockito.verify(mockReference, Mockito.never()).getName();
    }

    /** Basic test for status retrieval. */
    @Test
    public void testIsPresent()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        when(mockMatch.isPresent()).thenReturn(false);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Assert.assertFalse(o.isPresent());

        when(mockMatch.isPresent()).thenReturn(true);
        Assert.assertTrue(o.isPresent());
    }

    /** The status is not disclosed for private patients. */
    @Test
    public void testIsPresentWithPrivateAccess()
    {
        Feature mockMatch = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, priv);
        Assert.assertTrue(o.isPresent());
        Mockito.verify(mockMatch, Mockito.never()).isPresent();
    }

    /** The status is not disclosed for matchable patients. */
    @Test
    public void testIsPresentWithMatchAccess()
    {
        Feature mockMatch = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, limited);
        Assert.assertTrue(o.isPresent());
        Mockito.verify(mockMatch, Mockito.never()).isPresent();
    }

    /** Trying to retrieve the status doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testIsPresentWithNullMatch()
    {
        Feature mockReference = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);
        Assert.assertTrue(o.isPresent());
        Mockito.verify(mockReference, Mockito.never()).isPresent();
    }

    /** Basic test for reference retrieval. */
    @Test
    public void testGetReference()
    {
        Feature mockReference = mock(Feature.class);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Accessing the reference doesn't throw NPE. */
    @Test
    public void testGetReferenceWithNullReference()
    {
        Feature mockMatch = mock(Feature.class);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, open);
        Assert.assertNull(o.getReference());
    }

    /** Retrieving the reference feature is always allowed, no matter the access type to the matched patient. */
    @Test
    public void testGetReferenceWithPrivateAccess()
    {
        Feature mockReference = mock(Feature.class);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, priv);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Tests for metadata matching. */
    @Test
    public void testMetadataMatching()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        matchMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003677", "Slow", "pace"));
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));

        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Map<String, ? extends FeatureMetadatum> result = o.getMetadata();

        // Equal types should find a match
        RestrictedFeatureMetadatumSimilarityView entry =
            (RestrictedFeatureMetadatumSimilarityView) result.get("age_of_onset");
        Assert.assertNotNull(entry);
        Assert.assertEquals("HP:0003577", entry.getId());
        Assert.assertEquals("HP:0003577", entry.getReference().getId());

        // Equal types should find a match, even if the values are different
        entry = (RestrictedFeatureMetadatumSimilarityView) result.get("speed_of_onset");
        Assert.assertNotNull(entry);
        Assert.assertEquals("HP:0011010", entry.getId());
        Assert.assertEquals("HP:0011009", entry.getReference().getId());

        // There should be an entry even if there's no related metadata in the reference
        entry = (RestrictedFeatureMetadatumSimilarityView) result.get("pace");
        Assert.assertNotNull(entry);
        Assert.assertEquals("pace", entry.getType());
        Assert.assertEquals("HP:0003677", entry.getId());
        Assert.assertNull(entry.getReference());

        // There shouldn't be an entry if there's no related metadata in the match
        Assert.assertFalse(result.containsKey("death"));
    }

    /** Valid unmatched metadata should be returned when the reference is null. */
    @Test
    public void testMetadataMatchingWithNullReference()
    {
        Feature mockMatch = mock(Feature.class);
        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));

        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, open);
        Map<String, ? extends FeatureMetadatum> result = o.getMetadata();

        RestrictedFeatureMetadatumSimilarityView entry =
            (RestrictedFeatureMetadatumSimilarityView) result.get("age_of_onset");
        Assert.assertNotNull(entry);
        Assert.assertEquals("HP:0003577", entry.getId());
        Assert.assertNull("HP:0003577", entry.getReference());
    }

    /** Valid unmatched metadata should be returned when the reference metadata is null. */
    @Test
    public void testMetadataMatchingWithMissingReferenceMetadata()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003677", "Slow", "pace"));

        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        when(mockReference.getMetadata()).thenReturn(null);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Map<String, ? extends FeatureMetadatum> result = o.getMetadata();

        // There should be an entry even if there's no related metadata in the reference
        RestrictedFeatureMetadatumSimilarityView entry = (RestrictedFeatureMetadatumSimilarityView) result.get("pace");
        Assert.assertNotNull(entry);
        Assert.assertEquals("pace", entry.getType());
        Assert.assertEquals("HP:0003677", entry.getId());
        Assert.assertNull(entry.getReference());
    }

    /** No metadata should be returned when the match is null. */
    @Test
    public void testMetadataMatchingWithNullMatch()
    {
        Feature mockReference = mock(Feature.class);
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));

        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);
        Assert.assertTrue(o.getMetadata().isEmpty());
    }

    /** No metadata should be returned when the match metadata is null. */
    @Test
    public void testMetadataMatchingWithMissingMatchMetadata()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));

        when(mockMatch.getMetadata()).thenReturn(null);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Assert.assertTrue(o.getMetadata().isEmpty());
    }

    /** Metadata isn't disclosed for private patients. */
    @Test
    public void testGetMetadataWithPrivateAccess()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));

        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, priv);

        Assert.assertTrue(o.getMetadata().isEmpty());
    }

    /** Metadata isn't disclosed for matchable patients. */
    @Test
    public void testGetMetadataWithMatchAccess()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));

        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, limited);

        Assert.assertTrue(o.getMetadata().isEmpty());
    }

    /** Basic test for score computation, no metadata present. */
    @Test
    public void testGetScoreWithNoMetadata() throws ComponentLookupException
    {
        setupComponents();
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);

        // Maximum score for the same feature, present in both patients
        when(mockMatch.getId()).thenReturn("HP:0123456");
        when(mockReference.getId()).thenReturn("HP:0123456");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);

        // Minimum score for the same phenotype, present in one and absent in the other patient
        when(mockMatch.getId()).thenReturn("HP:0123456");
        when(mockReference.getId()).thenReturn("HP:0123456");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(false);
        Assert.assertEquals(-1.0, o.getScore(), 1.0E-5);
        when(mockMatch.getId()).thenReturn("HP:0123456");
        when(mockReference.getId()).thenReturn("HP:0123456");
        when(mockMatch.isPresent()).thenReturn(false);
        when(mockReference.isPresent()).thenReturn(true);
        Assert.assertEquals(-1.0, o.getScore(), 1.0E-5);

        // Zero score for different features
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockReference.getId()).thenReturn("HP:0001256");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Zero score for different features, even if one is missing
        when(mockReference.isPresent()).thenReturn(false);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);

        // Positive score for related features
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockReference.getId()).thenReturn("HP:0011729");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        Double closeRelatedScore = o.getScore();
        Assert.assertTrue(closeRelatedScore > 0.3);
        Assert.assertTrue(closeRelatedScore < 0.7);

        // Same score when the order is reversed
        when(mockMatch.getId()).thenReturn("HP:0011729");
        when(mockReference.getId()).thenReturn("HP:0001382");
        Assert.assertEquals(closeRelatedScore, o.getScore(), 1.0E-5);

        // Lower score for farther away features
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockReference.getId()).thenReturn("HP:0001367");
        Double fartherRelatedScore = o.getScore();
        Assert.assertTrue(fartherRelatedScore > 0.1);
        Assert.assertTrue(fartherRelatedScore < 0.9);
        Assert.assertTrue(fartherRelatedScore < closeRelatedScore);

        // Negative score for related features, different present status
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockReference.getId()).thenReturn("HP:0011729");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(false);
        Assert.assertTrue(o.getScore() < -0.1);
        Assert.assertTrue(o.getScore() > -0.9);

        // Zero score for too far related features
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockReference.getId()).thenReturn("HP:0000001");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);
    }

    /** Mismatching metadata should bring the score closer to 0. */
    @Test
    public void testGetScoreWithMismatchedMetadata() throws ComponentLookupException
    {
        setupComponents();
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        // Positive score is lowered when metadata don't match
        when(mockMatch.getId()).thenReturn("HP:0123456");
        when(mockReference.getId()).thenReturn("HP:0123456");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003581", "Adult onset", "age_of_onset"));
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double scoreWith1MetadataMismatch = o.getScore();
        Assert.assertTrue(scoreWith1MetadataMismatch > 0.5);
        Assert.assertTrue(scoreWith1MetadataMismatch < 0.7);

        // Lowered even more when there are more mismatched metadata pairs
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003680", "Slow", "pace"));
        referenceMeta.put("pace", new MockFeatureMetadatum("HP:0003678", "Rapid", "pace"));
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double scoreWith2MetadataMismatches = o.getScore();
        Assert.assertTrue(scoreWith2MetadataMismatches > 0.5);
        Assert.assertTrue(scoreWith2MetadataMismatches < 0.6);
        Assert.assertTrue(scoreWith2MetadataMismatches < scoreWith1MetadataMismatch);

        // But one-sided metadata doesn't affect the score
        matchMeta.put("death", new MockFeatureMetadatum("HP:0003680", "Slow", "death"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0003678", "Rapid", "speed_of_onset"));
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double scoreWith2MetadataMismatchesAndUnpairedMetadata = o.getScore();
        Assert.assertEquals(scoreWith2MetadataMismatches, scoreWith2MetadataMismatchesAndUnpairedMetadata, 1.0E-5);

        // Negative scores are symmetrically raised towards 0
        when(mockReference.isPresent()).thenReturn(false);
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double negativeScore = o.getScore();
        Assert.assertEquals(scoreWith2MetadataMismatches, -negativeScore, 1.0E-5);
    }

    /** Matching metadata should bring the score closer to +1 or -1. */
    @Test
    public void testGetScoreWithMatchingMetadata() throws ComponentLookupException
    {
        setupComponents();
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        when(mockMatch.getId()).thenReturn("HP:0001256");
        when(mockReference.getId()).thenReturn("HP:0001249");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);

        Double baseScore = o.getScore();
        Assert.assertTrue(baseScore < 1);
        Assert.assertTrue(baseScore > 0);

        // Positive score is raised when metadata match
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003581", "Adult onset", "age_of_onset"));
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003581", "Adult onset", "age_of_onset"));
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double scoreWith1MetadataMatch = o.getScore();
        Assert.assertTrue(scoreWith1MetadataMatch > baseScore);
        Assert.assertTrue(scoreWith1MetadataMatch < 1);

        // Raised even more when there are more matching metadata pairs
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003678", "Rapid", "pace"));
        referenceMeta.put("pace", new MockFeatureMetadatum("HP:0003678", "Rapid", "pace"));
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double scoreWith2MetadataMatches = o.getScore();
        Assert.assertTrue(scoreWith2MetadataMatches > scoreWith1MetadataMatch);
        Assert.assertTrue(scoreWith2MetadataMatches < 1);

        // But one-sided metadata doesn't affect the score
        matchMeta.put("death", new MockFeatureMetadatum("HP:0003680", "Slow", "death"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0003678", "Rapid", "speed_of_onset"));
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double scoreWith2MetadataMatchesAndUnpairedMetadata = o.getScore();
        Assert.assertEquals(scoreWith2MetadataMatches, scoreWith2MetadataMatchesAndUnpairedMetadata, 1.0E-5);

        // Negative scores are symmetrically raised towards 0
        when(mockReference.isPresent()).thenReturn(false);
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double negativeScore = o.getScore();
        Assert.assertEquals(scoreWith2MetadataMatches, -negativeScore, 1.0E-5);
    }

    /** Unpaired metadata should not alter the score. */
    @Test
    public void testGetScoreWithUnpairedMetadata() throws ComponentLookupException
    {
        setupComponents();
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        when(mockMatch.getId()).thenReturn("HP:0001256");
        when(mockReference.getId()).thenReturn("HP:0001249");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);

        Double baseScore = o.getScore();
        Assert.assertTrue(baseScore < 1);
        Assert.assertTrue(baseScore > 0);

        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003581", "Adult onset", "age_of_onset"));
        referenceMeta.put("pace", new MockFeatureMetadatum("HP:0003678", "Rapid", "pace"));
        o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);
        Double newScore = o.getScore();
        Assert.assertEquals(baseScore, newScore, 1.0E-5);
    }

    /** When the features can't be resolved, NaN is returned. */
    @Test
    public void testGetScoreWithUnknownFeatures() throws ComponentLookupException
    {
        setupComponents();
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);

        // NaN when the reference is unknown
        when(mockMatch.getId()).thenReturn("HP:0001367");
        when(mockReference.getId()).thenReturn("ONTO:654321");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        Assert.assertTrue(Double.isNaN(o.getScore()));

        // NaN when the match is unknown
        when(mockMatch.getId()).thenReturn("ONTO:654321");
        when(mockReference.getId()).thenReturn("HP:0001367");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(false);
        Assert.assertTrue(Double.isNaN(o.getScore()));

        when(ComponentManagerRegistry.getContextComponentManager().getInstance(FeatureSimilarityScorer.class))
            .thenThrow(new ComponentLookupException("No implementation"));
        when(mockMatch.getId()).thenReturn("HP:0011729");
        when(mockReference.getId()).thenReturn("HP:0001367");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** Score cannot be computed when the match is missing. */
    @Test
    public void testGetScoreWithNullMatch()
    {
        Feature mockReference = mock(Feature.class);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** Score cannot be computed when the reference is missing. */
    @Test
    public void testGetScoreWithNullReference()
    {
        Feature mockMatch = mock(Feature.class);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** The access type shouldn't matter, the score is always available. */
    @Test
    public void testGetScoreWithPrivateAccess()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, priv);

        when(mockMatch.getId()).thenReturn("HP:0123456");
        when(mockReference.getId()).thenReturn("HP:0123456");
        when(mockMatch.isPresent()).thenReturn(true);
        when(mockReference.isPresent()).thenReturn(true);
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);
    }

    /** Basic JSON tests. */
    @Test
    public void testToJSON() throws ComponentLookupException
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        when(mockMatch.getType()).thenReturn("phenotype");
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockMatch.getName()).thenReturn("Joint hypermobility");
        when(mockMatch.isPresent()).thenReturn(true);

        when(mockReference.getId()).thenReturn("HP:0011729");
        when(mockReference.getName()).thenReturn("Abnormality of joint mobility");
        when(mockReference.getType()).thenReturn("prenatal_phenotype");
        when(mockReference.isPresent()).thenReturn(true);

        Map<String, FeatureMetadatum> matchMeta = new HashMap<String, FeatureMetadatum>();
        matchMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        matchMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011010", "Chronic", "speed_of_onset"));
        matchMeta.put("pace", new MockFeatureMetadatum("HP:0003677", "Slow", "pace"));
        Map<String, FeatureMetadatum> referenceMeta = new HashMap<String, FeatureMetadatum>();
        referenceMeta.put("age_of_onset", new MockFeatureMetadatum("HP:0003577", "Congenital onset", "age_of_onset"));
        referenceMeta.put("speed_of_onset", new MockFeatureMetadatum("HP:0011009", "Acute", "speed_of_onset"));
        referenceMeta.put("death", new MockFeatureMetadatum("HP:0003826", "Stillbirth", "death"));
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockMatch.getMetadata()).thenReturn(matchMeta);
        Mockito.<Map<String, ? extends FeatureMetadatum>> when(mockReference.getMetadata()).thenReturn(referenceMeta);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("phenotype", result.getString("type"));
        Assert.assertEquals("prenatal_phenotype", result.getString("queryType"));
        Assert.assertEquals("HP:0001382", result.getString("id"));
        Assert.assertEquals("Joint hypermobility", result.getString("name"));
        Assert.assertEquals("HP:0011729", result.getString("queryId"));
        Assert.assertTrue(result.getDouble("score") > 0);

        JSONArray meta = result.getJSONArray("metadata");
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
    }

    /** Negative features should be marked as such. */
    @Test
    public void testToJSONWithNotPresent() throws ComponentLookupException
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        when(mockMatch.getType()).thenReturn("phenotype");
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockMatch.getName()).thenReturn("Joint hypermobility");
        when(mockMatch.isPresent()).thenReturn(false);

        when(mockReference.getId()).thenReturn("HP:0001382");
        when(mockReference.getName()).thenReturn("Joint hypermobility");
        when(mockReference.getType()).thenReturn("phenotype");
        when(mockReference.isPresent()).thenReturn(true);

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("phenotype", result.getString("type"));
        Assert.assertEquals("phenotype", result.getString("queryType"));
        Assert.assertEquals("HP:0001382", result.getString("id"));
        Assert.assertEquals("Joint hypermobility", result.getString("name"));
        Assert.assertEquals("HP:0001382", result.getString("queryId"));
        Assert.assertFalse(result.getBoolean("isPresent"));
        Assert.assertTrue(result.getDouble("score") < 0);
    }

    /** When the reference is missing, there's no queryId and no score. */
    @Test
    public void testToJSONWithMissingReference() throws ComponentLookupException
    {
        Feature mockMatch = mock(Feature.class);

        when(mockMatch.getType()).thenReturn("phenotype");
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockMatch.getName()).thenReturn("Joint hypermobility");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, null, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("phenotype", result.getString("type"));
        Assert.assertEquals("HP:0001382", result.getString("id"));
        Assert.assertEquals("Joint hypermobility", result.getString("name"));
        Assert.assertFalse(result.has("queryType"));
        Assert.assertFalse(result.has("queryId"));
        Assert.assertFalse(result.has("score"));
    }

    /** When the match is missing, a null JSON is returned. */
    @Test
    public void testToJSONWithMissingMatch() throws ComponentLookupException
    {
        Feature mockReference = mock(Feature.class);

        when(mockReference.getType()).thenReturn("phenotype");
        when(mockReference.getId()).thenReturn("HP:0011729");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, mockReference, open);

        Assert.assertTrue(o.toJSON().isNullObject());
    }

    /** When both the match and reference are missing, a null JSON is returned. */
    @Test
    public void testToJSONWithMissingMatchAndReference() throws ComponentLookupException
    {
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(null, null, open);
        JSONObject result = o.toJSON();
        Assert.assertTrue(result.isNullObject());
    }

    /** A null JSON is returned for private patients. */
    @Test
    public void testToJSONWithPrivateAccess()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);
        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, priv);
        Assert.assertTrue(o.toJSON().isNullObject());
    }

    /** Only query information is returned for matchable patients. */
    @Test
    public void testToJSONWithMatchAccess()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        when(mockMatch.getType()).thenReturn("phenotype");
        when(mockMatch.getId()).thenReturn("HP:0001382");
        when(mockMatch.getName()).thenReturn("Joint hypermobility");

        when(mockReference.getId()).thenReturn("HP:0011729");
        when(mockReference.getName()).thenReturn("Abnormality of joint mobility");
        when(mockReference.getType()).thenReturn("prenatal_phenotype");

        FeatureSimilarityView o = new RestrictedFeatureSimilarityView(mockMatch, mockReference, limited);

        JSONObject result = o.toJSON();
        Assert.assertFalse(result.has("type"));
        Assert.assertFalse(result.has("id"));
        Assert.assertFalse(result.has("name"));
        Assert.assertEquals("prenatal_phenotype", result.getString("queryType"));
        Assert.assertEquals("HP:0011729", result.getString("queryId"));
        Assert.assertTrue(result.getDouble("score") > 0);
    }

    /** Tests for isMatchingPair. */
    @Test
    public void testIsMatchingPair()
    {
        Feature mockMatch = mock(Feature.class);
        Feature mockReference = mock(Feature.class);

        Assert.assertTrue(new RestrictedFeatureSimilarityView(mockMatch, mockReference, open).isMatchingPair());
        Assert.assertFalse(new RestrictedFeatureSimilarityView(mockMatch, null, open).isMatchingPair());
        Assert.assertFalse(new RestrictedFeatureSimilarityView(null, mockReference, open).isMatchingPair());
    }

    private void setupComponents() throws ComponentLookupException
    {
        ComponentManager cm = mock(ComponentManager.class);

        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(cm);

        // Setup the ontology manager
        OntologyManager om = mock(OntologyManager.class);
        when(cm.getInstance(OntologyManager.class)).thenReturn(om);
        Set<OntologyTerm> ancestors = new HashSet<OntologyTerm>();

        // Setup the feature scorer
        FeatureSimilarityScorer featureScorer = new DefaultFeatureSimilarityScorer();
        ReflectionUtils.setFieldValue(featureScorer, "ontologyManager", om);
        when(cm.getInstance(FeatureSimilarityScorer.class)).thenReturn(featureScorer);

        // Setup the metadata scorers
        when(cm.getInstance(FeatureMetadatumSimilarityScorer.class, "pace")).thenReturn(
            new PaceOfProgressionFeatureMetadatumSimilarityScorer());
        when(cm.getInstance(FeatureMetadatumSimilarityScorer.class, "age_of_onset")).thenReturn(
            new AgeOfOnsetFeatureMetadatumSimilarityScorer());
        when(cm.getInstance(FeatureMetadatumSimilarityScorer.class, "speed_of_onset")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        when(cm.getInstance(FeatureMetadatumSimilarityScorer.class, "death")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        when(cm.getInstance(FeatureMetadatumSimilarityScorer.class)).thenReturn(
            new DefaultFeatureMetadatumSimilarityScorer());

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
