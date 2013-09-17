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

import org.phenotips.data.Disorder;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.DisorderSimilarityView;

import org.xwiki.component.manager.ComponentLookupException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import net.sf.json.JSONObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the "restricted" {@link DisorderSimilarityView} implementation, {@link RestrictedDisorderSimilarityView}.
 * 
 * @version $Id$
 */
public class RestrictedDisorderSimilarityViewTest
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

    /** Basic test for ID retrieval. */
    @Test
    public void testGetId()
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);
        when(mockMatch.getId()).thenReturn("MIM:136140");

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals("MIM:136140", o.getId());
    }

    /** The ID is not disclosed for private patients. */
    @Test
    public void testGetIdWithPrivateAccess()
    {
        Disorder mockMatch = mock(Disorder.class);

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, null, priv);
        Assert.assertNull(o.getId());
        Mockito.verify(mockMatch, Mockito.never()).getId();
    }

    /** The ID is not disclosed for matchable patients. */
    @Test
    public void testGetIdWithMatchAccess()
    {
        Disorder mockMatch = mock(Disorder.class);

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, null, limited);
        Assert.assertNull(o.getId());
        Mockito.verify(mockMatch, Mockito.never()).getId();
    }

    /** Trying to retrieve the ID doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetIdWithNullMatch()
    {
        Disorder mockReference = mock(Disorder.class);

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(null, mockReference, open);
        Assert.assertNull(o.getId());
        Mockito.verify(mockReference, Mockito.never()).getId();
    }

    /** Basic test for name retrieval. */
    @Test
    public void testGetName()
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);
        when(mockMatch.getName()).thenReturn("Some disease");

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, mockReference, open);
        Assert.assertEquals("Some disease", o.getName());
    }

    /** The name is not disclosed for private patients. */
    @Test
    public void testGetNameWithPrivateAccess()
    {
        Disorder mockMatch = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, null, priv);
        Assert.assertNull(o.getName());
        Mockito.verify(mockMatch, Mockito.never()).getName();
    }

    /** The name is not disclosed for matchable patients. */
    @Test
    public void testGetNameWithMatchAccess()
    {
        Disorder mockMatch = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, null, limited);
        Assert.assertNull(o.getName());
        Mockito.verify(mockMatch, Mockito.never()).getName();
    }

    /** Trying to retrieve the name doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetNameWithNullMatch()
    {
        Disorder mockReference = mock(Disorder.class);

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(null, mockReference, open);
        Assert.assertNull(o.getName());
        Mockito.verify(mockReference, Mockito.never()).getName();
    }

    /** Basic test for reference retrieval. */
    @Test
    public void testGetReference()
    {
        Disorder mockReference = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(null, mockReference, open);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Accessing the reference doesn't throw NPE. */
    @Test
    public void testGetReferenceWithNullReference()
    {
        Disorder mockMatch = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, null, open);
        Assert.assertNull(o.getReference());
    }

    /** Retrieving the reference disorder is always allowed, no matter the access type to the matched patient. */
    @Test
    public void testGetReferenceWithPrivateAccess()
    {
        Disorder mockReference = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(null, mockReference, priv);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Basic test for score computation. */
    @Test
    public void testGetScore() throws ComponentLookupException
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, mockReference, open);

        // Maximum score for the same disorder
        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockReference.getId()).thenReturn("MIM:136140");
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);

        // Minimum score for different disorders
        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockReference.getId()).thenReturn("MIM:136141");
        Assert.assertEquals(-1.0, o.getScore(), 1.0E-5);
    }

    /** Score cannot be computed when the match is missing. */
    @Test
    public void testGetScoreWithNullMatch()
    {
        Disorder mockReference = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(null, mockReference, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** Score cannot be computed when the reference is missing. */
    @Test
    public void testGetScoreWithNullReference()
    {
        Disorder mockMatch = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, null, open);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** The access type shouldn't matter, the score is always available. */
    @Test
    public void testGetScoreWithPrivateAccess()
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, mockReference, priv);

        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockReference.getId()).thenReturn("MIM:136140");
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);
    }

    /** Basic JSON tests. */
    @Test
    public void testToJSON() throws ComponentLookupException
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);

        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockMatch.getName()).thenReturn("#136140 FLOATING-HARBOR SYNDROME; FLHS");

        when(mockReference.getId()).thenReturn("MIM:136140");

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, mockReference, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("MIM:136140", result.getString("id"));
        Assert.assertEquals("#136140 FLOATING-HARBOR SYNDROME; FLHS", result.getString("name"));
        Assert.assertEquals("MIM:136140", result.getString("queryId"));
        Assert.assertEquals(1.0, result.getDouble("score"), 1.0E-5);
    }

    /** When the reference is missing, there's no queryId and no score. */
    @Test
    public void testToJSONWithMissingReference() throws ComponentLookupException
    {
        Disorder mockMatch = mock(Disorder.class);

        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockMatch.getName()).thenReturn("#136140 FLOATING-HARBOR SYNDROME; FLHS");

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, null, open);

        JSONObject result = o.toJSON();
        Assert.assertEquals("MIM:136140", result.getString("id"));
        Assert.assertEquals("#136140 FLOATING-HARBOR SYNDROME; FLHS", result.getString("name"));
        Assert.assertFalse(result.has("queryId"));
        Assert.assertFalse(result.has("score"));
    }

    /** When the match is missing, there's no id, name or score. */
    @Test
    public void testToJSONWithMissingMatch() throws ComponentLookupException
    {
        Disorder mockReference = mock(Disorder.class);

        when(mockReference.getId()).thenReturn("MIM:136140");

        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(null, mockReference, open);

        JSONObject result = o.toJSON();
        Assert.assertFalse(result.has("id"));
        Assert.assertFalse(result.has("name"));
        Assert.assertEquals("MIM:136140", result.getString("queryId"));
        Assert.assertFalse(result.has("score"));
    }

    /** When both the match and reference are missing, a null JSON is returned. */
    @Test
    public void testToJSONWithMissingMatchAndReference() throws ComponentLookupException
    {
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(null, null, open);
        JSONObject result = o.toJSON();
        Assert.assertTrue(result.isNullObject());
    }

    /** A null JSON is returned for private patients. */
    @Test
    public void testToJSONWithPrivateAccess()
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, mockReference, priv);
        Assert.assertTrue(o.toJSON().isNullObject());
    }

    /** A null JSON is returned for matchable patients. */
    @Test
    public void testToJSONWithMatchAccess()
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);
        DisorderSimilarityView o = new RestrictedDisorderSimilarityView(mockMatch, mockReference, limited);
        Assert.assertTrue(o.toJSON().isNullObject());
    }

    /** Tests for isMatchingPair. */
    @Test
    public void testIsMatchingPair()
    {
        Disorder mockMatch = mock(Disorder.class);
        Disorder mockReference = mock(Disorder.class);
        Assert.assertTrue(new RestrictedDisorderSimilarityView(mockMatch, mockReference, open).isMatchingPair());
        Assert.assertFalse(new RestrictedDisorderSimilarityView(mockMatch, null, open).isMatchingPair());
        Assert.assertFalse(new RestrictedDisorderSimilarityView(null, mockReference, open).isMatchingPair());
    }
}
