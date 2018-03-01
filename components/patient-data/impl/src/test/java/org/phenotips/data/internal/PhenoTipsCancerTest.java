/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Cancer;
import org.phenotips.data.CancerQualifier;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.diff.DiffManager;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import net.jcip.annotations.NotThreadSafe;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class PhenoTipsCancerTest
{
    private static final String HP001 = "HP:001";

    private static final String HP001_LABEL = "Cancer one";

    private static final String HP002 = "HP:002";

    private static final String HP002_LABEL = "Cancer two";

    private static final String HP003 = "HP:003";

    private static final String HP003_LABEL = "Cancer three";

    private static final String QUALIFIERS = "qualifiers";

    private static final String AGE_STR = "22";

    private static final int AGE_NUM = 22;

    private static final String LATERALITY_BI = "bi";

    private JSONObject hp001Json;

    private JSONObject hp002Json;

    private JSONObject hp003Json;

    private Cancer hp001obj;

    private Cancer hp002obj;

    private Cancer hp003obj;

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private VocabularyManager vm;

    @Mock
    private VocabularyTerm hp001;

    @Mock
    private VocabularyTerm hp002;

    @Mock
    private VocabularyTerm hp003;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject cancerBaseObject;

    @Mock
    private BaseObject qualifierBaseObject;

    @Mock
    private XWikiContext context;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(DiffManager.class)).thenReturn(null);
        when(this.cm.getInstance(VocabularyManager.class)).thenReturn(this.vm);

        when(this.hp001.getId()).thenReturn(HP001);
        when(this.hp001.getName()).thenReturn(HP001_LABEL);
        when(this.vm.resolveTerm(HP001)).thenReturn(this.hp001);

        when(this.hp002.getId()).thenReturn(HP002);
        when(this.hp002.getName()).thenReturn(HP002_LABEL);
        when(this.vm.resolveTerm(HP002)).thenReturn(this.hp002);

        when(this.hp003.getId()).thenReturn(HP003);
        when(this.hp003.getName()).thenReturn(HP003_LABEL);
        when(this.vm.resolveTerm(HP003)).thenReturn(this.hp003);

        this.hp001Json = new JSONObject()
            .put(Cancer.CancerProperty.CANCER.getJsonProperty(), HP001)
            .put(Cancer.CancerProperty.AFFECTED.getJsonProperty(), true)
            .put(QUALIFIERS, new JSONArray()
                .put(new JSONObject()
                    .put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001)
                    .put(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_STR)
                    .put(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_NUM)
                    .put(CancerQualifier.CancerQualifierProperty.PRIMARY.getJsonProperty(), true)
                    .put(CancerQualifier.CancerQualifierProperty.LATERALITY.getJsonProperty(), LATERALITY_BI)));

        this.hp002Json = new JSONObject()
            .put(Cancer.CancerProperty.CANCER.getJsonProperty(), HP002)
            .put(QUALIFIERS, new JSONArray()
                .put(new JSONObject()
                    .put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP002)
                    .put(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_STR)
                    .put(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_NUM)
                    .put(CancerQualifier.CancerQualifierProperty.PRIMARY.getJsonProperty(), true)
                    .put(CancerQualifier.CancerQualifierProperty.LATERALITY.getJsonProperty(), LATERALITY_BI)));

        this.hp003Json = new JSONObject()
            .put(Cancer.CancerProperty.CANCER.getJsonProperty(), HP003)
            .put(Cancer.CancerProperty.AFFECTED.getJsonProperty(), false)
            .put(QUALIFIERS, new JSONArray());

        this.hp001obj = new PhenoTipsCancer(this.hp001Json);
        this.hp002obj = new PhenoTipsCancer(this.hp002Json);
        this.hp003obj = new PhenoTipsCancer(this.hp003Json);
    }

    @Test
    public void isAffectedShouldBeFalseIfNotProvided()
    {
        Assert.assertFalse(this.hp002obj.isAffected());
    }

    @Test
    public void isAffectedReturnsCorrectResponse()
    {
        Assert.assertTrue(this.hp001obj.isAffected());
        Assert.assertFalse(this.hp003obj.isAffected());
    }

    @Test
    public void getQualifiersNoQualifiers()
    {
        Assert.assertTrue(this.hp003obj.getQualifiers().isEmpty());
    }

    @Test
    public void getQualifiersReturnsQualifiers()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
    }

    @Test
    public void getProperty()
    {
        Assert.assertEquals(HP001, this.hp001obj.getProperty(Cancer.CancerProperty.CANCER));
        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
    }

    @Test
    public void getName()
    {
        Assert.assertEquals(HP001_LABEL, this.hp001obj.getName());
    }

    @Test
    public void getId()
    {
        Assert.assertEquals(HP001, this.hp001obj.getId());
    }

    @Test
    public void toJSON()
    {
        final JSONObject copyJson = new JSONObject()
            .put(Cancer.CancerProperty.CANCER.getJsonProperty(), HP001)
            .put("label", HP001_LABEL)
            .put(Cancer.CancerProperty.AFFECTED.getJsonProperty(), true)
            .put(QUALIFIERS, new JSONArray()
                .put(new JSONObject()
                    .put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001)
                    .put(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_STR)
                    .put(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_NUM)
                    .put(CancerQualifier.CancerQualifierProperty.PRIMARY.getJsonProperty(), true)
                    .put(CancerQualifier.CancerQualifierProperty.LATERALITY.getJsonProperty(), LATERALITY_BI)));
        Assert.assertTrue(copyJson.similar(this.hp001obj.toJSON()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mergeDataDifferentCancers()
    {
        this.hp001obj.mergeData(this.hp003obj);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mergeDataSameCancers()
    {
        this.hp003Json.put(Cancer.CancerProperty.CANCER.getJsonProperty(), HP001);

        final Cancer cancer = new PhenoTipsCancer(this.hp003Json);
        Assert.assertTrue(this.hp001obj.isAffected());
        Assert.assertEquals(HP001, this.hp001obj.getProperty(Cancer.CancerProperty.CANCER));
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());

        Assert.assertFalse(cancer.isAffected());
        Assert.assertEquals(HP001, cancer.getProperty(Cancer.CancerProperty.CANCER));
        Assert.assertTrue(this.hp003obj.getQualifiers().isEmpty());

        this.hp001obj.mergeData(this.hp003obj);

        Assert.assertFalse(this.hp001obj.isAffected());
        Assert.assertEquals(HP001, this.hp001obj.getProperty(Cancer.CancerProperty.CANCER));
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());

        Assert.assertFalse(cancer.isAffected());
        Assert.assertEquals(HP001, cancer.getProperty(Cancer.CancerProperty.CANCER));
        Assert.assertTrue(this.hp003obj.getQualifiers().isEmpty());
    }

    @Test
    public void writeCatchesXWikiException() throws XWikiException
    {
        when(this.doc.newXObject(Cancer.CLASS_REFERENCE, this.context)).thenThrow(new XWikiException());

        this.hp001obj.write(this.doc, this.context);

        verify(this.doc, times(1)).newXObject(Cancer.CLASS_REFERENCE, this.context);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void writeRecordsData() throws XWikiException
    {
        when(this.doc.newXObject(Cancer.CLASS_REFERENCE, this.context)).thenReturn(this.cancerBaseObject);
        when(this.doc.newXObject(CancerQualifier.CLASS_REFERENCE, this.context)).thenReturn(this.qualifierBaseObject);

        this.hp001obj.write(this.doc, this.context);

        verify(this.doc, times(1)).newXObject(Cancer.CLASS_REFERENCE, this.context);
        verify(this.doc, times(1)).newXObject(CancerQualifier.CLASS_REFERENCE, this.context);

        verify(this.cancerBaseObject).set(Cancer.CancerProperty.CANCER.getProperty(), HP001, this.context);
        verify(this.cancerBaseObject).set(Cancer.CancerProperty.AFFECTED.getProperty(), 1, this.context);

        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.CANCER.getProperty(), HP001, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getProperty(), AGE_STR, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getProperty(), AGE_NUM, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.PRIMARY.getProperty(), 1, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.LATERALITY.getProperty(), LATERALITY_BI, this.context);
        verifyNoMoreInteractions(this.doc, this.cancerBaseObject, this.qualifierBaseObject);
    }

    @Test
    public void addQualifierNull()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
        ((PhenoTipsCancer) this.hp001obj).addQualifier(null);
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
    }

    @Test
    public void addQualifierNotNull()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
        final CancerQualifier qualifier = new PhenoTipsCancerQualifier(
            new JSONObject().put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001));
        ((PhenoTipsCancer) this.hp001obj).addQualifier(qualifier);
        Assert.assertEquals(2, this.hp001obj.getQualifiers().size());
        Assert.assertTrue(this.hp001obj.getQualifiers().contains(qualifier));
    }

    @Test
    public void addQualifiersNull()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
        ((PhenoTipsCancer) this.hp001obj).addQualifiers(null);
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
    }

    @Test
    public void addQualifiersEmpty()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
        ((PhenoTipsCancer) this.hp001obj).addQualifiers(Collections.emptySet());
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
    }

    @Test
    public void addQualifiersCollection()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());

        final CancerQualifier qualifier = new PhenoTipsCancerQualifier(
            new JSONObject().put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001));

        ((PhenoTipsCancer) this.hp001obj).addQualifiers(Arrays.asList(null, qualifier, null));
        Assert.assertEquals(2, this.hp001obj.getQualifiers().size());
        Assert.assertTrue(this.hp001obj.getQualifiers().contains(qualifier));
    }

    @Test
    public void setQualifiersNull()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
        ((PhenoTipsCancer) this.hp001obj).setQualifiers(null);
        Assert.assertTrue(this.hp001obj.getQualifiers().isEmpty());
    }

    @Test
    public void setQualifiersUnmodifiable()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
        ((PhenoTipsCancer) this.hp001obj).setQualifiers(Collections.emptyList());
        Assert.assertTrue(this.hp001obj.getQualifiers().isEmpty());

        final CancerQualifier qualifier = new PhenoTipsCancerQualifier(
            new JSONObject().put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001));
        ((PhenoTipsCancer) this.hp001obj).addQualifier(qualifier);
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
    }

    @Test
    public void setQualifiersCollection()
    {
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());

        final CancerQualifier qualifier = new PhenoTipsCancerQualifier(
            new JSONObject().put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001));
        ((PhenoTipsCancer) this.hp001obj).setQualifiers(Arrays.asList(null, qualifier, null));
        Assert.assertEquals(1, this.hp001obj.getQualifiers().size());
        Assert.assertTrue(this.hp001obj.getQualifiers().contains(qualifier));
    }

    @Test
    public void setPropertyTryToResetId()
    {
        Assert.assertEquals(HP001, this.hp001obj.getProperty(Cancer.CancerProperty.CANCER));
        Assert.assertEquals(HP001, this.hp001obj.getId());

        ((PhenoTipsCancer) this.hp001obj).setProperty(Cancer.CancerProperty.CANCER, HP002);

        Assert.assertEquals(HP001, this.hp001obj.getProperty(Cancer.CancerProperty.CANCER));
        Assert.assertEquals(HP001, this.hp001obj.getId());
    }

    @Test
    public void setPropertyTryToResetAffectedWithNonBoolean()
    {
        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(true, this.hp001obj.isAffected());

        ((PhenoTipsCancer) this.hp001obj).setProperty(Cancer.CancerProperty.AFFECTED, HP002);

        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(true, this.hp001obj.isAffected());
    }

    @Test
    public void setPropertyTryToResetAffectedWithNull()
    {
        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(true, this.hp001obj.isAffected());

        ((PhenoTipsCancer) this.hp001obj).setProperty(Cancer.CancerProperty.AFFECTED, null);

        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(true, this.hp001obj.isAffected());
    }

    @Test
    public void setPropertyTryToResetAffectedWithTrue()
    {
        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(true, this.hp001obj.isAffected());

        ((PhenoTipsCancer) this.hp001obj).setProperty(Cancer.CancerProperty.AFFECTED, true);

        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(true, this.hp001obj.isAffected());
    }

    @Test
    public void setPropertyTryToResetAffectedWithFalse()
    {
        Assert.assertEquals(true, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(true, this.hp001obj.isAffected());

        ((PhenoTipsCancer) this.hp001obj).setProperty(Cancer.CancerProperty.AFFECTED, false);

        Assert.assertEquals(false, this.hp001obj.getProperty(Cancer.CancerProperty.AFFECTED));
        Assert.assertEquals(false, this.hp001obj.isAffected());
    }
}
