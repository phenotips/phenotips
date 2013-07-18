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

import org.phenotips.data.Disease;
import org.phenotips.data.similarity.AccessType;
import org.phenotips.data.similarity.SimilarDisease;
import org.phenotips.data.similarity.internal.RestrictedSimilarDisease;

import org.xwiki.component.manager.ComponentLookupException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import net.sf.json.JSONObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the "restricted" {@link SimilarDisease} implementation, {@link RestrictedSimilarDisease}.
 * 
 * @version $Id$
 */
public class RestrictedSimilarDiseaseTest
{
    /** Basic test for ID retrieval. */
    @Test
    public void testGetId()
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);
        when(mockMatch.getId()).thenReturn("MIM:136140");

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.PUBLIC);
        Assert.assertEquals("MIM:136140", o.getId());
    }

    /** The ID is not disclosed for private patients. */
    @Test
    public void testGetIdWithPrivateAccess()
    {
        Disease mockMatch = mock(Disease.class);

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, null, AccessType.PRIVATE);
        Assert.assertNull(o.getId());
        Mockito.verify(mockMatch, Mockito.never()).getId();
    }

    /** The ID is not disclosed for matchable patients. */
    @Test
    public void testGetIdWithMatchAccess()
    {
        Disease mockMatch = mock(Disease.class);

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, null, AccessType.MATCH);
        Assert.assertNull(o.getId());
        Mockito.verify(mockMatch, Mockito.never()).getId();
    }

    /** Trying to retrieve the ID doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetIdWithNullMatch()
    {
        Disease mockReference = mock(Disease.class);

        SimilarDisease o = new RestrictedSimilarDisease(null, mockReference, AccessType.PUBLIC);
        Assert.assertNull(o.getId());
        Mockito.verify(mockReference, Mockito.never()).getId();
    }

    /** Basic test for name retrieval. */
    @Test
    public void testGetName()
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);
        when(mockMatch.getName()).thenReturn("Some disease");

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.PUBLIC);
        Assert.assertEquals("Some disease", o.getName());
    }

    /** The name is not disclosed for private patients. */
    @Test
    public void testGetNameWithPrivateAccess()
    {
        Disease mockMatch = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, null, AccessType.PRIVATE);
        Assert.assertNull(o.getName());
        Mockito.verify(mockMatch, Mockito.never()).getName();
    }

    /** The name is not disclosed for matchable patients. */
    @Test
    public void testGetNameWithMatchAccess()
    {
        Disease mockMatch = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, null, AccessType.MATCH);
        Assert.assertNull(o.getName());
        Mockito.verify(mockMatch, Mockito.never()).getName();
    }

    /** Trying to retrieve the name doesn't throw NPE when the match is null, and doesn't access the reference. */
    @Test
    public void testGetNameWithNullMatch()
    {
        Disease mockReference = mock(Disease.class);

        SimilarDisease o = new RestrictedSimilarDisease(null, mockReference, AccessType.PUBLIC);
        Assert.assertNull(o.getName());
        Mockito.verify(mockReference, Mockito.never()).getName();
    }

    /** Basic test for reference retrieval. */
    @Test
    public void testGetReference()
    {
        Disease mockReference = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(null, mockReference, AccessType.PUBLIC);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Accessing the reference doesn't throw NPE. */
    @Test
    public void testGetReferenceWithNullReference()
    {
        Disease mockMatch = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, null, AccessType.PUBLIC);
        Assert.assertNull(o.getReference());
    }

    /** Retrieving the reference disease is always allowed, no matter the access type to the matched patient. */
    @Test
    public void testGetReferenceWithPrivateAccess()
    {
        Disease mockReference = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(null, mockReference, AccessType.PRIVATE);
        Assert.assertSame(mockReference, o.getReference());
    }

    /** Basic test for score computation. */
    @Test
    public void testGetScore() throws ComponentLookupException
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.PUBLIC);

        // Maximum score for the same disease
        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockReference.getId()).thenReturn("MIM:136140");
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);

        // Minimum score for different diseases
        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockReference.getId()).thenReturn("MIM:136141");
        Assert.assertEquals(-1.0, o.getScore(), 1.0E-5);
    }

    /** Score cannot be computed when the match is missing. */
    @Test
    public void testGetScoreWithNullMatch()
    {
        Disease mockReference = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(null, mockReference, AccessType.PUBLIC);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** Score cannot be computed when the reference is missing. */
    @Test
    public void testGetScoreWithNullReference()
    {
        Disease mockMatch = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, null, AccessType.PUBLIC);
        Assert.assertTrue(Double.isNaN(o.getScore()));
    }

    /** The access type shouldn't matter, the score is always available. */
    @Test
    public void testGetScoreWithPrivateAccess()
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.PRIVATE);

        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockReference.getId()).thenReturn("MIM:136140");
        Assert.assertEquals(1.0, o.getScore(), 1.0E-5);
    }

    /** Basic JSON tests. */
    @Test
    public void testToJSON() throws ComponentLookupException
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);

        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockMatch.getName()).thenReturn("#136140 FLOATING-HARBOR SYNDROME; FLHS");

        when(mockReference.getId()).thenReturn("MIM:136140");

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.PUBLIC);

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
        Disease mockMatch = mock(Disease.class);

        when(mockMatch.getId()).thenReturn("MIM:136140");
        when(mockMatch.getName()).thenReturn("#136140 FLOATING-HARBOR SYNDROME; FLHS");

        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, null, AccessType.PUBLIC);

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
        Disease mockReference = mock(Disease.class);

        when(mockReference.getId()).thenReturn("MIM:136140");

        SimilarDisease o = new RestrictedSimilarDisease(null, mockReference, AccessType.PUBLIC);

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
        SimilarDisease o = new RestrictedSimilarDisease(null, null, AccessType.PUBLIC);
        JSONObject result = o.toJSON();
        Assert.assertTrue(result.isNullObject());
    }

    /** A null JSON is returned for private patients. */
    @Test
    public void testToJSONWithPrivateAccess()
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.PRIVATE);
        Assert.assertTrue(o.toJSON().isNullObject());
    }

    /** A null JSON is returned for matchable patients. */
    @Test
    public void testToJSONWithMatchAccess()
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);
        SimilarDisease o = new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.MATCH);
        Assert.assertTrue(o.toJSON().isNullObject());
    }

    /** Tests for isMatchingPair. */
    @Test
    public void testIsMatchingPair()
    {
        Disease mockMatch = mock(Disease.class);
        Disease mockReference = mock(Disease.class);
        Assert.assertTrue(new RestrictedSimilarDisease(mockMatch, mockReference, AccessType.OWNED).isMatchingPair());
        Assert.assertFalse(new RestrictedSimilarDisease(mockMatch, null, AccessType.OWNED).isMatchingPair());
        Assert.assertFalse(new RestrictedSimilarDisease(null, mockReference, AccessType.OWNED).isMatchingPair());
    }
}
