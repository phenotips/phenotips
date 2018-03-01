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
import org.phenotips.data.CancerQualifier;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.diff.DiffManager;

import javax.inject.Provider;

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
public class PhenoTipsCancerQualifierTest
{
    private static final String HP001 = "HP:001";

    private static final String HP001_LABEL = "Cancer one";

    private static final String HP002 = "HP:002";

    private static final String HP002_LABEL = "Cancer two";

    private static final String HP003 = "HP:003";

    private static final String HP003_LABEL = "Cancer three";

    private static final String AGE_STR = "22";

    private static final int AGE_NUM = 22;

    private static final String LATERALITY_BI = "bi";

    private static final String ALT_AGE_STR = "10";

    private static final int ALT_AGE_NUM = 10;

    private static final String LATERALITY_L = "l";

    private static final String INVALID = "invalid";

    private JSONObject hp001Json;

    private JSONObject hp002Json;

    private JSONObject hp003Json;

    private CancerQualifier hp001obj;

    private CancerQualifier hp002obj;

    private CancerQualifier hp003obj;

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
            .put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001)
            .put(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_STR)
            .put(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_NUM)
            .put(CancerQualifier.CancerQualifierProperty.PRIMARY.getJsonProperty(), false)
            .put(CancerQualifier.CancerQualifierProperty.LATERALITY.getJsonProperty(), LATERALITY_BI);

        this.hp002Json = new JSONObject()
            .put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP002)
            .put(CancerQualifier.CancerQualifierProperty.PRIMARY.getJsonProperty(), true)
            .put(CancerQualifier.CancerQualifierProperty.LATERALITY.getJsonProperty(), LATERALITY_BI);

        this.hp003Json = new JSONObject()
            .put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001)
            .put(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_STR)
            .put(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_NUM)
            .put(CancerQualifier.CancerQualifierProperty.LATERALITY.getJsonProperty(), LATERALITY_BI);

        this.hp001obj = new PhenoTipsCancerQualifier(this.hp001Json);
        this.hp002obj = new PhenoTipsCancerQualifier(this.hp002Json);
        this.hp003obj = new PhenoTipsCancerQualifier(this.hp003Json);
    }

    @Test
    public void writeCatchesXWikiException() throws XWikiException
    {
        when(this.doc.newXObject(CancerQualifier.CLASS_REFERENCE, this.context)).thenThrow(new XWikiException());

        this.hp001obj.write(this.doc, this.context);

        verify(this.doc, times(1)).newXObject(CancerQualifier.CLASS_REFERENCE, this.context);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void writeRecordsData() throws XWikiException
    {
        when(this.doc.newXObject(CancerQualifier.CLASS_REFERENCE, this.context)).thenReturn(this.qualifierBaseObject);

        this.hp001obj.write(this.doc, this.context);

        verify(this.doc, times(1)).newXObject(CancerQualifier.CLASS_REFERENCE, this.context);

        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.CANCER.getProperty(), HP001, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getProperty(), AGE_STR, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getProperty(), AGE_NUM, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.PRIMARY.getProperty(), 0, this.context);
        verify(this.qualifierBaseObject)
            .set(CancerQualifier.CancerQualifierProperty.LATERALITY.getProperty(), LATERALITY_BI, this.context);
        verifyNoMoreInteractions(this.doc, this.cancerBaseObject, this.qualifierBaseObject);
    }

    @Test
    public void writeDefaultsForMandatoryMissingData() throws XWikiException
    {
        when(this.doc.newXObject(CancerQualifier.CLASS_REFERENCE, this.context)).thenReturn(this.qualifierBaseObject);

        this.hp003obj.write(this.doc, this.context);

        verify(this.doc, times(1)).newXObject(CancerQualifier.CLASS_REFERENCE, this.context);

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
    public void getProperty()
    {
        Assert.assertEquals(HP001, this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.CANCER));
        Assert.assertEquals(AGE_STR, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS));
        Assert.assertEquals(AGE_NUM, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));
        Assert.assertEquals(false, this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));
        Assert.assertEquals(LATERALITY_BI, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));
        Assert.assertNull(this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.NOTES));
    }

    @Test
    public void toJSON()
    {
        final JSONObject expected = new JSONObject()
            .put(CancerQualifier.CancerQualifierProperty.CANCER.getJsonProperty(), HP001)
            .put(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_STR)
            .put(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS.getJsonProperty(), AGE_NUM)
            .put(CancerQualifier.CancerQualifierProperty.PRIMARY.getJsonProperty(), true)
            .put(CancerQualifier.CancerQualifierProperty.LATERALITY.getJsonProperty(), LATERALITY_BI);

        final JSONObject actual = this.hp003obj.toJSON();
        Assert.assertTrue(expected.similar(actual));
    }

    @Test
    public void setPropertyTryToResetId()
    {
        Assert.assertEquals(HP001, this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.CANCER));
        Assert.assertEquals(HP001, this.hp001obj.getId());

        ((PhenoTipsCancerQualifier) this.hp001obj).setProperty(CancerQualifier.CancerQualifierProperty.CANCER, HP002);

        Assert.assertEquals(HP001, this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.CANCER));
        Assert.assertEquals(HP001, this.hp001obj.getId());
    }

    @Test
    public void setPropertyTryToResetAgeAtDiagnosisWithNull()
    {
        Assert.assertEquals(AGE_STR, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS, null);

        Assert.assertNull(this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS));
    }

    @Test
    public void setPropertyTryToResetAgeAtDiagnosis()
    {
        Assert.assertEquals(AGE_STR, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS, ALT_AGE_STR);

        Assert.assertEquals(ALT_AGE_STR, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS));
    }

    @Test
    public void setPropertyTryToResetNumericAgeAtDiagnosisWithNull()
    {
        Assert.assertEquals(AGE_NUM, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS, null);

        Assert.assertNull(this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));
    }

    @Test
    public void setPropertyTryToResetNumericAgeAtDiagnosis()
    {
        Assert.assertEquals(AGE_NUM, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS, ALT_AGE_NUM);

        Assert.assertEquals(ALT_AGE_NUM, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));
    }

    @Test
    public void setPropertyTryToResetNumericAgeAtDiagnosisWithInvalid()
    {
        Assert.assertEquals(AGE_NUM, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS, -10);

        Assert.assertEquals(AGE_NUM, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));
    }

    @Test
    public void setPropertyTryToResetLateralityWithNull()
    {
        Assert.assertEquals(LATERALITY_BI, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.LATERALITY, null);

        Assert.assertNull(this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));
    }

    @Test
    public void setPropertyTryToResetLaterality()
    {
        Assert.assertEquals(LATERALITY_BI, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.LATERALITY, LATERALITY_L);

        Assert.assertEquals(LATERALITY_L, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));
    }

    @Test
    public void setPropertyTryToResetLateralityWithInvalid()
    {
        Assert.assertEquals(LATERALITY_BI, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));

        ((PhenoTipsCancerQualifier) this.hp001obj)
            .setProperty(CancerQualifier.CancerQualifierProperty.LATERALITY, INVALID);

        Assert.assertEquals(LATERALITY_BI, this.hp001obj
            .getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));
    }

    @Test
    public void setPropertyTryToResetPrimaryWithNull()
    {
        Assert.assertFalse((Boolean) this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));

        ((PhenoTipsCancerQualifier) this.hp001obj).setProperty(CancerQualifier.CancerQualifierProperty.PRIMARY, null);

        Assert.assertFalse((Boolean) this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));
    }

    @Test
    public void setPropertyTryToResetPrimaryTrue()
    {
        Assert.assertFalse((Boolean) this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));

        ((PhenoTipsCancerQualifier) this.hp001obj).setProperty(CancerQualifier.CancerQualifierProperty.PRIMARY, true);

        Assert.assertTrue((Boolean) this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));
    }

    @Test
    public void setPropertyTryToResetPrimaryFalse()
    {
        Assert.assertFalse((Boolean) this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));

        ((PhenoTipsCancerQualifier) this.hp001obj).setProperty(CancerQualifier.CancerQualifierProperty.PRIMARY, false);

        Assert.assertFalse((Boolean) this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));
    }

    @Test
    public void setPropertyTryToResetNotesWithNull()
    {
        Assert.assertNull(this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.NOTES));

        ((PhenoTipsCancerQualifier) this.hp001obj).setProperty(CancerQualifier.CancerQualifierProperty.NOTES, null);

        Assert.assertNull(this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.NOTES));
    }

    @Test
    public void setPropertyTryToResetNotes()
    {
        Assert.assertNull(this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.NOTES));

        ((PhenoTipsCancerQualifier) this.hp001obj).setProperty(CancerQualifier.CancerQualifierProperty.NOTES, AGE_STR);

        Assert.assertEquals(AGE_STR, this.hp001obj.getProperty(CancerQualifier.CancerQualifierProperty.NOTES));
    }

    @Test
    public void equalsWorks()
    {
        Assert.assertTrue(this.hp001obj.equals(this.hp001obj));
        Assert.assertFalse(this.hp001obj.equals(null));
        Assert.assertFalse(this.hp001obj.equals(AGE_NUM));
        Assert.assertFalse(this.hp001obj.equals(this.hp002obj));
        Assert.assertTrue(this.hp001obj.equals(new PhenoTipsCancerQualifier(this.hp001Json)));
    }

    @Test
    public void hashCodeTest()
    {
        Assert.assertEquals(this.hp001obj.hashCode(), new PhenoTipsCancerQualifier(this.hp001Json).hashCode());
        Assert.assertNotEquals(this.hp001obj.hashCode(), this.hp003obj.hashCode());
    }
}
