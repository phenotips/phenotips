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
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityScorer;
import org.phenotips.data.similarity.FeatureMetadatumSimilarityView;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import net.sf.json.JSONObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the "restricted" {@link FeatureMetadatumSimilarityView} implementation,
 * {@link RestrictedFeatureMetadatumSimilarityView}.
 * 
 * @version $Id$
 */
public class RestrictedFeatureMetadatumSimilarityViewTest
{
    private static AccessType open;

    private static AccessType limited;

    private static AccessType priv;

    private ComponentManager cm = mock(ComponentManager.class);

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
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        when(mockMatch.getType()).thenReturn("pace_of_progression");
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, open);
        Assert.assertEquals("pace_of_progression", o.getType());
    }

    /** The type is not disclosed for private patients. */
    @Test
    public void testGetTypeWithPrivateAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, priv);
        Assert.assertNull(o.getType());
        Mockito.verifyZeroInteractions(mockMatch);
    }

    /** The type is not disclosed for matchable patients. */
    @Test
    public void testGetTypeWithMatchAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, limited);
        Assert.assertNull(o.getType());
        Mockito.verifyZeroInteractions(mockMatch);
    }

    /** The type is retrieved from the reference when the match is null. */
    @Test
    public void testGetTypeWithNullMatch()
    {
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        when(mockReference.getType()).thenReturn("age_of_onset");
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(null, mockReference, open);
        Assert.assertEquals("age_of_onset", o.getType());
    }

    /** Trying to retrieve the type doesn't throw NPE when both the match and reference are null. */
    @Test
    public void testGetTypeWithNullMatchAndReference()
    {
        FeatureMetadatumSimilarityView o = new RestrictedFeatureMetadatumSimilarityView(null, null, open);
        Assert.assertNull(o.getType());
    }

    /** Basic test for ID retrieval. */
    @Test
    public void testGetId()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        when(mockMatch.getId()).thenReturn("HP:0003677");
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, open);
        Assert.assertEquals("HP:0003677", o.getId());
    }

    /** The ID is not disclosed for private patients. */
    @Test
    public void testGetIdWithPrivateAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, priv);
        Assert.assertNull(o.getId());
        Mockito.verifyZeroInteractions(mockMatch);
    }

    /** The ID is not disclosed for matchable patients. */
    @Test
    public void testGetIdWithMatchAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, limited);
        Assert.assertNull(o.getId());
        Mockito.verifyZeroInteractions(mockMatch);
    }

    /** Trying to retrieve the ID doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetIdWithNullMatch()
    {
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(null, mockReference, open);
        Assert.assertNull(o.getId());
        Mockito.verifyZeroInteractions(mockReference);
    }

    /** Basic test for name retrieval. */
    @Test
    public void testGetName()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        when(mockMatch.getName()).thenReturn("A name");
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, open);
        Assert.assertEquals("A name", o.getName());
    }

    /** The name is not disclosed for private patients. */
    @Test
    public void testGetNameWithPrivateAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, priv);
        Assert.assertNull(o.getName());
        Mockito.verifyZeroInteractions(mockMatch);
    }

    /** The name is not disclosed for matchable patients. */
    @Test
    public void testGetNameWithMatchAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, limited);
        Assert.assertNull(o.getName());
        Mockito.verifyZeroInteractions(mockMatch);
    }

    /** Trying to retrieve the name doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetNameWithNullMatch()
    {
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(null, mockReference, open);
        Assert.assertNull(o.getName());
        Mockito.verifyZeroInteractions(mockReference);
    }

    /** Basic test for reference retrieval. */
    @Test
    public void testGetReference()
    {
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(null, mockReference, open);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Accessing the reference doesn't throw NPE. */
    @Test
    public void testGetReferenceWithNullReference()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, open);
        Assert.assertNull(o.getReference());
    }

    /** Retrieving the reference patient is always allowed, no matter the access type to the matched patient. */
    @Test
    public void testGetReferenceWithPrivateAccess()
    {
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(null, mockReference, priv);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Basic test for score computation, using the fallback scorer. */
    @Test
    public void testGetScoreWithDefaultImplementation() throws ComponentLookupException
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        when(mockMatch.getType()).thenReturn("meta_type");
        when(mockReference.getType()).thenReturn("meta_type");
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, open);

        // Missing IDs should give NaN
        Assert.assertTrue(Double.isNaN(o.getScore()));

        // Equal IDs should give +1.0
        when(mockMatch.getId()).thenReturn("ONTO:001");
        when(mockReference.getId()).thenReturn("ONTO:001");
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);

        // Different IDs should give -1.0
        when(mockMatch.getId()).thenReturn("ONTO:002");
        when(mockReference.getId()).thenReturn("ONTO:003");
        Assert.assertEquals(-1.0, o.getScore(), 1.0E-5);
    }

    /** Basic test for score computation, using a specific scorer for this metadata type. */
    @Test
    public void testGetScoreWithCustomImplementation() throws ComponentLookupException
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        when(mockMatch.getType()).thenReturn("pace_of_progression");

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, open);

        // Equal IDs should give +1.0
        when(mockMatch.getId()).thenReturn("HP:0003680");
        when(mockReference.getId()).thenReturn("HP:0003680");
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);

        // Unkown IDs should give 0
        when(mockMatch.getId()).thenReturn("ONTO:002");
        when(mockReference.getId()).thenReturn("ONTO:003");
        Assert.assertEquals(0.0, o.getScore(), 1.0E-5);
    }

    /** Requesting a score when no scorer is available should return NaN. */
    @Test
    public void testGetScoreWithMissingImplementations() throws ComponentLookupException
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        when(mockMatch.getId()).thenReturn("HP:0003680");
        when(mockReference.getId()).thenReturn("HP:0003680");
        when(mockMatch.getType()).thenReturn("age_of_onset");
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class, "age_of_onset")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class)).thenThrow(
            new ComponentLookupException("No implementation for this role"));

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));

    }

    /** The access type shouldn't matter, the score is always available. */
    @Test
    public void testGetScoreWithPrivateAccess() throws ComponentLookupException
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        when(mockMatch.getType()).thenReturn("meta_type");
        when(mockReference.getType()).thenReturn("meta_type");

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, priv);

        when(mockMatch.getId()).thenReturn("ONTO:001");
        when(mockReference.getId()).thenReturn("ONTO:001");
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);
    }

    /** When information is missing, we can't compute a score, so NaN should be returned. */
    @Test
    public void testGetScoreWithMissingMatch()
    {
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(null, mockReference, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** When information is missing, we can't compute a score, so NaN should be returned. */
    @Test
    public void testGetScoreWithMissingReference()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** Score computation uses IDs, so when the match or reference don't have an ID, NaN must be returned. */
    @Test
    public void testGetScoreWithMissingIDs()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));

        when(mockMatch.getId()).thenReturn("M");
        Assert.assertTrue(Double.isNaN(o.getScore()));

        when(mockMatch.getId()).thenReturn(null);
        when(mockReference.getId()).thenReturn("R");
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** Basic JSON tests. */
    @Test
    public void testToJSON() throws ComponentLookupException
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        when(mockMatch.getType()).thenReturn("meta_type");
        when(mockReference.getType()).thenReturn("meta_type");

        when(mockMatch.getId()).thenReturn("ONTO:001");
        when(mockReference.getId()).thenReturn("ONTO:001");

        when(mockMatch.getName()).thenReturn("Some term");

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("meta_type", result.getString("type"));
        Assert.assertEquals("ONTO:001", result.getString("id"));
        Assert.assertEquals("Some term", result.getString("name"));
        Assert.assertEquals("ONTO:001", result.getString("queryId"));
        Assert.assertEquals(1.0, result.getDouble("score"), 1.0E-5);
    }

    /** When the reference is missing, there's no queryId and no score. */
    @Test
    public void testToJSONWithMissingReference() throws ComponentLookupException
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        when(mockMatch.getType()).thenReturn("meta_type");

        when(mockMatch.getId()).thenReturn("ONTO:001");
        when(mockMatch.getName()).thenReturn("Some term");

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("meta_type", result.getString("type"));
        Assert.assertEquals("ONTO:001", result.getString("id"));
        Assert.assertEquals("Some term", result.getString("name"));
        Assert.assertNull(result.get("queryId"));
        Assert.assertNull(result.get("score"));
    }

    /** When the match is missing, there's no id, name or score. */
    @Test
    public void testToJSONWithMissingMatch() throws ComponentLookupException
    {
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        when(mockReference.getType()).thenReturn("meta_type");

        when(mockReference.getId()).thenReturn("ONTO:001");
        when(mockReference.getType()).thenReturn("meta_type");

        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(null, mockReference, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("meta_type", result.getString("type"));
        Assert.assertNull(result.get("id"));
        Assert.assertNull(result.get("name"));
        Assert.assertEquals("ONTO:001", result.getString("queryId"));
        Assert.assertNull(result.get("score"));
    }

    /** When both the match and reference are missing, a null JSON is returned. */
    @Test
    public void testToJSONWithMissingMatchAndReference() throws ComponentLookupException
    {
        FeatureMetadatumSimilarityView o = new RestrictedFeatureMetadatumSimilarityView(null, null, open);
        JSONObject result = o.toJSON();

        Assert.assertTrue(result.isNullObject());
    }

    /** A null JSON is returned for private patients. */
    @Test
    public void testToJSONWithPrivateAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, priv);

        Assert.assertTrue(o.toJSON().isNullObject());
        Mockito.verifyZeroInteractions(mockMatch, mockReference);
    }

    /** A null JSON is returned for matchable patients. */
    @Test
    public void testToJSONWithMatchAccess()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);
        FeatureMetadatumSimilarityView o =
            new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, limited);

        Assert.assertTrue(o.toJSON().isNullObject());
        Mockito.verifyZeroInteractions(mockMatch, mockReference);
    }

    /** Tests for isMatchingPair. */
    @Test
    public void testIsMatchingPair()
    {
        FeatureMetadatum mockMatch = mock(FeatureMetadatum.class);
        FeatureMetadatum mockReference = mock(FeatureMetadatum.class);

        Assert.assertTrue(new RestrictedFeatureMetadatumSimilarityView(mockMatch, mockReference, open)
            .isMatchingPair());
        Assert.assertFalse(new RestrictedFeatureMetadatumSimilarityView(mockMatch, null, open).isMatchingPair());
        Assert.assertFalse(new RestrictedFeatureMetadatumSimilarityView(null, mockReference, open).isMatchingPair());
    }

    /** Set up the component manager registry to return the {@link #cm prepared mock}. */
    @Before
    public void setupComponentManagerRegistry() throws ComponentLookupException
    {
        @SuppressWarnings("unchecked")
        Provider<ComponentManager> mockProvider = mock(Provider.class);
        // This is a bit fragile, let's hope the field name doesn't change
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", mockProvider);
        when(mockProvider.get()).thenReturn(this.cm);

        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class, "pace_of_progression")).thenReturn(
            new PaceOfProgressionFeatureMetadatumSimilarityScorer());
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class, "meta_type")).thenThrow(
            new ComponentLookupException("No implementation for this role"));
        when(this.cm.getInstance(FeatureMetadatumSimilarityScorer.class)).thenReturn(
            new DefaultFeatureMetadatumSimilarityScorer());
    }
}
