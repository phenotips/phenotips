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
package org.phenotips.data;

import org.xwiki.model.EntityType;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link CancerQualifier}.
 *
 * @version $Id$
 */
public class CancerQualifierTest
{
    private static final String CANCER_LABEL = "cancer";

    private static final String AGE_AT_DIAGNOSIS_LABEL = "ageAtDiagnosis";

    private static final String NUM_AGE_AT_DIAGNOSIS_LABEL = "numericAgeAtDiagnosis";

    private static final String PRIMARY_LABEL = "primary";

    private static final String LATERALITY_LABEL = "laterality";

    private static final String NOTES_LABEL = "notes";

    private static final String CANCER_1_ID = "HP:001";

    private static final String CANCER_1_AGE_STR = "22";

    private static final String CANCER_1_NEG_AGE_STR = "-22";

    private static final String CANCER_1_DOUBLE_AGE_STR = "22.2";

    private static final int CANCER_1_AGE_NUM = 22;

    private static final int CANCER_1_NEG_AGE_NUM = -22;

    private static final double CANCER_1_DOUBLE_AGE_NUM = 22.2;

    private static final String BI_STR = "bi";

    private static final CancerQualifier.CancerQualifierProperty CANCER =
            CancerQualifier.CancerQualifierProperty.CANCER;

    private static final CancerQualifier.CancerQualifierProperty AGE_AT_DIAGNOSIS =
            CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS;

    private static final CancerQualifier.CancerQualifierProperty NUMERIC_AGE_AT_DIAGNOSIS =
            CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS;

    private static final CancerQualifier.CancerQualifierProperty PRIMARY =
            CancerQualifier.CancerQualifierProperty.PRIMARY;

    private static final CancerQualifier.CancerQualifierProperty LATERALITY =
            CancerQualifier.CancerQualifierProperty.LATERALITY;

    private static final CancerQualifier.CancerQualifierProperty NOTES = CancerQualifier.CancerQualifierProperty.NOTES;

    @Mock
    private BaseObject qualifierObject;

    @Mock
    private BaseStringProperty field;

    private XWikiContext context;

    private JSONObject qualifierJson;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.context = new XWikiContext();

        this.qualifierJson = new JSONObject()
            .put(CANCER_LABEL, CANCER_1_ID)
            .put(AGE_AT_DIAGNOSIS_LABEL, CANCER_1_AGE_STR)
            .put(NUM_AGE_AT_DIAGNOSIS_LABEL, CANCER_1_AGE_NUM)
            .put(PRIMARY_LABEL, true)
            .put(LATERALITY_LABEL, BI_STR)
            .put(NOTES_LABEL, NOTES_LABEL);

        when(this.qualifierObject.getField(anyString())).thenReturn(this.field);
        when(this.field.getValue()).thenReturn(CANCER_1_ID);
    }

    @Test
    public void classReferenceIsCorrect()
    {
        Assert.assertEquals("CancerQualifierClass", CancerQualifier.CLASS_REFERENCE.getName());
        Assert.assertEquals("PhenoTips", CancerQualifier.CLASS_REFERENCE.extractReference(EntityType.SPACE).getName());
        Assert.assertNull(CancerQualifier.CLASS_REFERENCE.extractReference(EntityType.WIKI));
    }

    @Test
    public void valueIsValidCancerIdIsNull()
    {
        Assert.assertFalse(CANCER.valueIsValid(null));
    }

    @Test
    public void valueIsValidCancerIdIsEmpty()
    {
        Assert.assertFalse(CANCER.valueIsValid(StringUtils.EMPTY));
    }

    @Test
    public void valueIsValidCancerIdIsBlank()
    {
        Assert.assertFalse(CANCER.valueIsValid(StringUtils.SPACE));
    }

    @Test
    public void valueIsValidCancerIdIsNotBlank()
    {
        Assert.assertTrue(CANCER.valueIsValid(CANCER_1_ID));
    }

    @Test
    public void valueIsValidCancerIdIsNotAString()
    {
        Assert.assertFalse(CANCER.valueIsValid(CANCER_1_AGE_NUM));
        Assert.assertFalse(CANCER.valueIsValid(CANCER));
    }

    @Test
    public void valueIsValidAgeAtDiagnosisValueIsNull()
    {
        Assert.assertTrue(AGE_AT_DIAGNOSIS.valueIsValid(null));
    }

    @Test
    public void valueIsValidAgeAtDiagnosisValueIsEmpty()
    {
        Assert.assertTrue(AGE_AT_DIAGNOSIS.valueIsValid(StringUtils.EMPTY));
    }

    @Test
    public void valueIsValidAgeAtDiagnosisValueIsBlank()
    {
        Assert.assertTrue(AGE_AT_DIAGNOSIS.valueIsValid(StringUtils.SPACE));
    }

    @Test
    public void valueIsValidAgeAtDiagnosisValueIsString()
    {
        Assert.assertTrue(AGE_AT_DIAGNOSIS.valueIsValid(CANCER_1_AGE_STR));
    }

    @Test
    public void valueIsValidAgeAtDiagnosisValueIsNotAString()
    {
        Assert.assertFalse(AGE_AT_DIAGNOSIS.valueIsValid(CANCER_1_AGE_NUM));
        Assert.assertFalse(AGE_AT_DIAGNOSIS.valueIsValid(CANCER));
    }

    @Test
    public void valueIsValidNumericAgeAtDiagnosisValueIsNull()
    {
        Assert.assertTrue(NUMERIC_AGE_AT_DIAGNOSIS.valueIsValid(null));
    }


    @Test
    public void valueIsValidNumericAgeAtDiagnosisValueIsNegative()
    {
        Assert.assertFalse(NUMERIC_AGE_AT_DIAGNOSIS.valueIsValid(CANCER_1_NEG_AGE_NUM));
    }

    @Test
    public void valueIsValidNumericAgeAtDiagnosisValueIsNotAnInteger()
    {
        Assert.assertFalse(NUMERIC_AGE_AT_DIAGNOSIS.valueIsValid(CANCER_1_DOUBLE_AGE_NUM));
        Assert.assertFalse(NUMERIC_AGE_AT_DIAGNOSIS.valueIsValid(CANCER));
        Assert.assertFalse(NUMERIC_AGE_AT_DIAGNOSIS.valueIsValid(CANCER_1_AGE_STR));
    }

    @Test
    public void valueIsValidNumericAgeAtDiagnosisValueIsValid()
    {
        Assert.assertTrue(NUMERIC_AGE_AT_DIAGNOSIS.valueIsValid(CANCER_1_AGE_NUM));
    }

    @Test
    public void valueIsValidPrimaryValueIsNull()
    {
        Assert.assertFalse(PRIMARY.valueIsValid(null));
    }

    @Test
    public void valueIsValidPrimaryValueIsNotABoolean()
    {
        Assert.assertFalse(PRIMARY.valueIsValid(CANCER_1_DOUBLE_AGE_STR));
        Assert.assertFalse(PRIMARY.valueIsValid(CANCER_1_AGE_NUM));
    }

    @Test
    public void valueIsValidPrimaryValueIsValid()
    {
        Assert.assertTrue(PRIMARY.valueIsValid(true));
        Assert.assertTrue(PRIMARY.valueIsValid(false));
    }

    @Test
    public void valueIsValidLateralityValueIsNull()
    {
        Assert.assertTrue(LATERALITY.valueIsValid(null));
    }

    @Test
    public void valueIsValidLateralityValueIsEmpty()
    {
        Assert.assertTrue(LATERALITY.valueIsValid(StringUtils.EMPTY));
    }

    @Test
    public void valueIsValidLateralityValueIsBlank()
    {
        Assert.assertFalse(LATERALITY.valueIsValid(StringUtils.SPACE));
    }

    @Test
    public void valueIsValidLateralityValueIsNotAllowedString()
    {
        Assert.assertFalse(LATERALITY.valueIsValid("bilateral"));
    }

    @Test
    public void valueIsValidLateralityValueIsNotAString()
    {
        Assert.assertFalse(LATERALITY.valueIsValid(CANCER_1_AGE_NUM));
        Assert.assertFalse(LATERALITY.valueIsValid(CANCER));
    }

    @Test
    public void valueIsValidLateralityValueIsValid()
    {
        Assert.assertTrue(LATERALITY.valueIsValid(BI_STR));
    }

    @Test
    public void valueIsValidNotesValueIsNull()
    {
        Assert.assertTrue(NOTES.valueIsValid(null));
    }

    @Test
    public void valueIsValidNotesValueIsEmpty()
    {
        Assert.assertTrue(NOTES.valueIsValid(StringUtils.EMPTY));
    }

    @Test
    public void valueIsValidNotesValueIsBlank()
    {
        Assert.assertTrue(NOTES.valueIsValid(StringUtils.SPACE));
    }

    @Test
    public void valueIsValidNotesValueIsNotAString()
    {
        Assert.assertFalse(NOTES.valueIsValid(CANCER_1_AGE_NUM));
        Assert.assertFalse(NOTES.valueIsValid(CANCER));
    }

    @Test
    public void valueIsValidNotesValueIsValid()
    {
        Assert.assertTrue(NOTES.valueIsValid(CANCER_LABEL));
    }

    @Test
    public void extractValueCancerIdFieldIsNull()
    {
        when(this.qualifierObject.getField(CANCER_LABEL)).thenReturn(null);
        Assert.assertNull(CANCER.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueCancerIdJsonFieldIsNull()
    {
        this.qualifierJson.put(CANCER_LABEL, (String) null);
        Assert.assertNull(CANCER.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueCancerIdFieldValueIsNull()
    {
        when(this.field.getValue()).thenReturn(null);
        Assert.assertNull(CANCER.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueCancerIdFieldValueIsEmpty()
    {
        when(this.field.getValue()).thenReturn(StringUtils.EMPTY);
        Assert.assertNull(CANCER.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueCancerIdJsonFieldIsEmpty()
    {
        this.qualifierJson.put(CANCER_LABEL, StringUtils.EMPTY);
        Assert.assertNull(CANCER.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueCancerIdFieldValueIsBlank()
    {
        when(this.field.getValue()).thenReturn(StringUtils.SPACE);
        Assert.assertNull(CANCER.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueCancerIdJsonFieldIsBlank()
    {
        this.qualifierJson.put(CANCER_LABEL, StringUtils.SPACE);
        Assert.assertNull(CANCER.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueCancerIdFieldValueIsNotBlank()
    {
        Assert.assertEquals(CANCER_1_ID, CANCER.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueCancerIdJsonFieldIsNotBlank()
    {
        Assert.assertEquals(CANCER_1_ID, CANCER.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueAgeAtDiagnosisFieldIsNull()
    {
        when(this.qualifierObject.getField(AGE_AT_DIAGNOSIS_LABEL)).thenReturn(null);
        Assert.assertNull(AGE_AT_DIAGNOSIS.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueAgeAtDiagnosisJsonFieldIsNull()
    {
        this.qualifierJson.put(AGE_AT_DIAGNOSIS_LABEL, (String) null);
        Assert.assertNull(AGE_AT_DIAGNOSIS.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueAgeAtDiagnosisFieldValueIsEmpty()
    {
        when(this.field.getValue()).thenReturn(StringUtils.EMPTY);
        Assert.assertNull(AGE_AT_DIAGNOSIS.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueAgeAtDiagnosisJsonFieldIsEmpty()
    {
        this.qualifierJson.put(AGE_AT_DIAGNOSIS_LABEL, StringUtils.EMPTY);
        Assert.assertNull(AGE_AT_DIAGNOSIS.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueAgeAtDiagnosisFieldValueIsBlank()
    {
        when(this.field.getValue()).thenReturn(StringUtils.SPACE);
        Assert.assertNull(AGE_AT_DIAGNOSIS.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueAgeAtDiagnosisJsonFieldIsBlank()
    {
        this.qualifierJson.put(AGE_AT_DIAGNOSIS_LABEL, StringUtils.SPACE);
        Assert.assertNull(AGE_AT_DIAGNOSIS.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueAgeAtDiagnosisFieldValueIsNotBlank()
    {
        when(this.field.getValue()).thenReturn(CANCER_1_AGE_STR);
        Assert.assertEquals(CANCER_1_AGE_STR, AGE_AT_DIAGNOSIS.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueAgeAtDiagnosisJsonFieldValueIsNotBlank()
    {
        Assert.assertEquals(CANCER_1_AGE_STR, AGE_AT_DIAGNOSIS.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueNumericAgeAtDiagnosisNoSuchNumericValue()
    {
        when(this.qualifierObject.getIntValue(NUM_AGE_AT_DIAGNOSIS_LABEL, -1)).thenReturn(-1);
        Assert.assertNull(NUMERIC_AGE_AT_DIAGNOSIS.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueNumericAgeAtDiagnosisJsonNegativeNumericValue()
    {
        this.qualifierJson.put(NUM_AGE_AT_DIAGNOSIS_LABEL, -1);
        Assert.assertNull(NUMERIC_AGE_AT_DIAGNOSIS.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueNumericAgeAtDiagnosisIsNegative()
    {
        when(this.qualifierObject.getIntValue(NUM_AGE_AT_DIAGNOSIS_LABEL, -1)).thenReturn(CANCER_1_NEG_AGE_NUM);
        Assert.assertNull(NUMERIC_AGE_AT_DIAGNOSIS.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueValidNumericAgeAtDiagnosis()
    {
        when(this.qualifierObject.getIntValue(NUM_AGE_AT_DIAGNOSIS_LABEL, -1)).thenReturn(CANCER_1_AGE_NUM);
        Assert.assertEquals(CANCER_1_AGE_NUM, NUMERIC_AGE_AT_DIAGNOSIS.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueValidNumericAgeAtDiagnosisJson()
    {
        Assert.assertEquals(CANCER_1_AGE_NUM, NUMERIC_AGE_AT_DIAGNOSIS.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValuePrimaryIsNegative()
    {
        when(this.qualifierObject.getIntValue(PRIMARY_LABEL, 1)).thenReturn(CANCER_1_NEG_AGE_NUM);
        Assert.assertFalse((Boolean) PRIMARY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValuePrimaryJsonNull()
    {
        this.qualifierJson.put(PRIMARY_LABEL, (Boolean) null);
        Assert.assertTrue((Boolean) PRIMARY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValuePrimaryIsPositive()
    {
        when(this.qualifierObject.getIntValue(PRIMARY_LABEL, 1)).thenReturn(1);
        Assert.assertTrue((Boolean) PRIMARY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValuePrimaryJsonTrue()
    {
        Assert.assertTrue((Boolean) PRIMARY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValuePrimaryJsonFalse()
    {
        this.qualifierJson.put(PRIMARY_LABEL, false);
        Assert.assertFalse((Boolean) PRIMARY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValuePrimaryZeroIsFalse()
    {
        when(this.qualifierObject.getIntValue(PRIMARY_LABEL, -1)).thenReturn(0);
        Assert.assertFalse((Boolean) PRIMARY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueLateralityFieldIsNull()
    {
        when(this.qualifierObject.getField(LATERALITY_LABEL)).thenReturn(null);
        Assert.assertNull(LATERALITY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueLateralityJsonNull()
    {
        this.qualifierJson.put(LATERALITY_LABEL, (Boolean) null);
        Assert.assertNull(LATERALITY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueLateralityFieldValueIsEmpty()
    {
        when(this.field.getValue()).thenReturn(StringUtils.EMPTY);
        Assert.assertEquals(StringUtils.EMPTY, LATERALITY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueLateralityJsonEmpty()
    {
        this.qualifierJson.put(LATERALITY_LABEL, StringUtils.EMPTY);
        Assert.assertEquals(StringUtils.EMPTY, LATERALITY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueLateralityFieldValueIsBlank()
    {
        when(this.field.getValue()).thenReturn(StringUtils.SPACE);
        Assert.assertNull(LATERALITY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueLateralityJsonBlank()
    {
        this.qualifierJson.put(LATERALITY_LABEL, StringUtils.SPACE);
        Assert.assertNull(LATERALITY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueLateralityFieldValueIsNotAnAllowedString()
    {
        when(this.field.getValue()).thenReturn(CANCER_1_AGE_STR);
        Assert.assertNull(LATERALITY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueLateralityJsonNotAllowedStr()
    {
        this.qualifierJson.put(LATERALITY_LABEL, CANCER_1_AGE_STR);
        Assert.assertNull(LATERALITY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueLateralityFieldValueIsValid()
    {
        when(this.field.getValue()).thenReturn(BI_STR);
        Assert.assertEquals(BI_STR, LATERALITY.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueLateralityJsonValid()
    {
        Assert.assertEquals(BI_STR, LATERALITY.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueNotesFieldIsNull()
    {
        when(this.qualifierObject.getField(NOTES_LABEL)).thenReturn(null);
        Assert.assertNull(NOTES.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueNotesJsonNull()
    {
        this.qualifierJson.put(NOTES_LABEL, (Boolean) null);
        Assert.assertNull(NOTES.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueNotesFieldValueIsEmpty()
    {
        when(this.field.getValue()).thenReturn(StringUtils.EMPTY);
        Assert.assertNull(NOTES.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueNotesJsonEmpty()
    {
        this.qualifierJson.put(NOTES_LABEL, StringUtils.EMPTY);
        Assert.assertNull(NOTES.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueNotesFieldValueIsBlank()
    {
        when(this.field.getValue()).thenReturn(StringUtils.SPACE);
        Assert.assertNull(NOTES.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueNotesJsonBlank()
    {
        this.qualifierJson.put(NOTES_LABEL, StringUtils.SPACE);
        Assert.assertNull(NOTES.extractValue(this.qualifierJson));
    }

    @Test
    public void extractValueNotesFieldValueIsNotBlank()
    {
        when(this.field.getValue()).thenReturn(CANCER_LABEL);
        Assert.assertEquals(CANCER_LABEL, NOTES.extractValue(this.qualifierObject));
    }

    @Test
    public void extractValueNotesJsonIsNotBlank()
    {
        Assert.assertEquals(NOTES_LABEL, NOTES.extractValue(this.qualifierJson));
    }

    @Test
    public void writeValueCancerIdIsNull()
    {
        CANCER.writeValue(this.qualifierObject, null, this.context);
        verify(this.qualifierObject, times(1)).set(CANCER_LABEL, null, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsNull()
    {
        CANCER.writeValue(this.qualifierObject, null, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsEmpty()
    {
        CANCER.writeValue(this.qualifierObject, StringUtils.EMPTY, this.context);
        verify(this.qualifierObject, times(1)).set(CANCER_LABEL, StringUtils.EMPTY, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsEmpty()
    {
        CANCER.writeValue(this.qualifierObject, StringUtils.EMPTY, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsBlank()
    {
        CANCER.writeValue(this.qualifierObject, StringUtils.SPACE, this.context);
        verify(this.qualifierObject, times(1)).set(CANCER_LABEL, StringUtils.SPACE, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsBlank()
    {
        CANCER.writeValue(this.qualifierObject, StringUtils.SPACE, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsNotAString()
    {
        CANCER.writeValue(this.qualifierObject, CANCER, this.context);
        verify(this.qualifierObject, times(1)).set(CANCER_LABEL, CANCER, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsNotAString()
    {
        CANCER.writeValue(this.qualifierObject, CANCER, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsValid()
    {
        CANCER.writeValue(this.qualifierObject, CANCER_1_ID, this.context);
        verify(this.qualifierObject, times(1)).set(CANCER_LABEL, CANCER_1_ID, this.context);
    }

    @Test
    public void writeValueAgeAtDiagnosisValueIsNull()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, null, this.context);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, null, this.context);
    }

    @Test
    public void writeValueVerifiedAgeAtDiagnosisValueIsNull()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, null, this.context, true);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, null, this.context);
    }

    @Test
    public void writeValueAgeAtDiagnosisValueIsEmpty()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, StringUtils.EMPTY, this.context);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, StringUtils.EMPTY, this.context);
    }

    @Test
    public void writeValueVerifiedAgeAtDiagnosisValueIsEmpty()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, StringUtils.EMPTY, this.context, true);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, StringUtils.EMPTY, this.context);
    }

    @Test
    public void writeValueAgeAtDiagnosisValueIsBlank()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, StringUtils.SPACE, this.context);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, StringUtils.SPACE, this.context);
    }

    @Test
    public void writeValueVerifiedAgeAtDiagnosisValueIsBlank()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, StringUtils.SPACE, this.context, true);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, StringUtils.SPACE, this.context);
    }

    @Test
    public void writeValueAgeAtDiagnosisValueIsNotAString()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER, this.context);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, CANCER, this.context);
    }


    @Test
    public void writeValueVerifiedAgeAtDiagnosisValueIsNotAString()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueAgeAtDiagnosisValueIsNotAnIntStr()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_DOUBLE_AGE_STR, this.context);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, CANCER_1_DOUBLE_AGE_STR, this.context);
    }

    @Test
    public void writeValueVerifiedAgeAtDiagnosisValueIsNotAnIntStr()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_DOUBLE_AGE_STR, this.context, true);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, CANCER_1_DOUBLE_AGE_STR, this.context);
    }

    @Test
    public void writeValueAgeAtDiagnosisValueIsNegativeIntStr()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_NEG_AGE_STR, this.context);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, CANCER_1_NEG_AGE_STR, this.context);
    }

    @Test
    public void writeValueVerifiedAgeAtDiagnosisValueIsNegativeIntStr()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_NEG_AGE_STR, this.context, true);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, CANCER_1_NEG_AGE_STR, this.context);
    }

    @Test
    public void writeValueAgeAtDiagnosisValueIsValid()
    {
        AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_AGE_STR, this.context);
        verify(this.qualifierObject, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, CANCER_1_AGE_STR, this.context);
    }

    @Test
    public void writeValueNumericAgeAtDiagnosisValueIsNull()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, null, this.context);
        verify(this.qualifierObject, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, null, this.context);
    }

    @Test
    public void writeValueVerifiedNumericAgeAtDiagnosisValueIsNull()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, null, this.context, true);
        verify(this.qualifierObject, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, null, this.context);
    }

    @Test
    public void writeValueNumericAgeAtDiagnosisValueIsNotANumber()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER, this.context);
        verify(this.qualifierObject, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, CANCER, this.context);
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_AGE_STR, this.context);
        verify(this.qualifierObject, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, CANCER_1_AGE_STR, this.context);

    }

    @Test
    public void writeValueVerifiedNumericAgeAtDiagnosisValueIsNotANumber()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER, this.context, true);
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_AGE_STR, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueNumericAgeAtDiagnosisValueIsNotAnInt()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_DOUBLE_AGE_NUM, this.context);
        verify(this.qualifierObject, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, CANCER_1_DOUBLE_AGE_NUM, this.context);
    }

    @Test
    public void writeValueVerifiedNumericAgeAtDiagnosisValueIsNotAnInt()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_DOUBLE_AGE_NUM, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueNumericAgeAtDiagnosisValueIsNegativeInt()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_NEG_AGE_NUM, this.context);
        verify(this.qualifierObject, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, CANCER_1_NEG_AGE_NUM, this.context);
    }

    @Test
    public void writeValueVerifiedNumericAgeAtDiagnosisValueIsNegativeInt()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_NEG_AGE_NUM, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueNumericAgeAtDiagnosisValueIsValid()
    {
        NUMERIC_AGE_AT_DIAGNOSIS.writeValue(this.qualifierObject, CANCER_1_AGE_NUM, this.context);
        verify(this.qualifierObject, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, CANCER_1_AGE_NUM, this.context);
    }

    @Test
    public void writeValuePrimaryValueIsNull()
    {
        PRIMARY.writeValue(this.qualifierObject, null, this.context);
        verify(this.qualifierObject, times(1)).set(PRIMARY_LABEL, 1, this.context);
    }

    @Test
    public void writeValueVerifiedPrimaryValueIsNull()
    {
        PRIMARY.writeValue(this.qualifierObject, null, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValuePrimaryValueIsNotABoolean()
    {
        PRIMARY.writeValue(this.qualifierObject, "true", this.context);
        PRIMARY.writeValue(this.qualifierObject, 1, this.context);
        verify(this.qualifierObject, times(2)).set(PRIMARY_LABEL, 1, this.context);
    }

    @Test
    public void writeValueVerifiedPrimaryValueIsNotABoolean()
    {
        PRIMARY.writeValue(this.qualifierObject, "true", this.context, true);
        PRIMARY.writeValue(this.qualifierObject, 1, this.context, true);

        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValuePrimaryValueIsValid()
    {
        PRIMARY.writeValue(this.qualifierObject, true, this.context);
        PRIMARY.writeValue(this.qualifierObject, false, this.context);

        verify(this.qualifierObject, times(1)).set(PRIMARY_LABEL, 1, this.context);
        verify(this.qualifierObject, times(1)).set(PRIMARY_LABEL, 0, this.context);
    }

    @Test
    public void writeValueVerifiedPrimaryValueIsValid()
    {
        PRIMARY.writeValue(this.qualifierObject, true, this.context, true);
        PRIMARY.writeValue(this.qualifierObject, false, this.context, true);

        verify(this.qualifierObject, times(1)).set(PRIMARY_LABEL, 1, this.context);
        verify(this.qualifierObject, times(1)).set(PRIMARY_LABEL, 0, this.context);
    }

    @Test
    public void writeValueLateralityValueIsNull()
    {
        LATERALITY.writeValue(this.qualifierObject, null, this.context);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, null, this.context);
    }

    @Test
    public void writeValueVerifiedLateralityValueIsNull()
    {
        LATERALITY.writeValue(this.qualifierObject, null, this.context);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, null, this.context);
    }

    @Test
    public void writeValueLateralityValueIsEmpty()
    {
        LATERALITY.writeValue(this.qualifierObject, StringUtils.EMPTY, this.context, true);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, StringUtils.EMPTY, this.context);
    }

    @Test
    public void writeValueLateralityValueIsBlank()
    {
        LATERALITY.writeValue(this.qualifierObject, StringUtils.SPACE, this.context);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, StringUtils.SPACE, this.context);
    }

    @Test
    public void writeValueVerifiedLateralityValueIsBlank()
    {
        LATERALITY.writeValue(this.qualifierObject, StringUtils.SPACE, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueLateralityValueIsNotAString()
    {
        LATERALITY.writeValue(this.qualifierObject, CANCER, this.context);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, CANCER, this.context);
    }

    @Test
    public void writeValueVerifiedLateralityValueIsNotAString()
    {
        LATERALITY.writeValue(this.qualifierObject, CANCER, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueLateralityValueIsNotAllowed()
    {
        LATERALITY.writeValue(this.qualifierObject, CANCER_LABEL, this.context);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, CANCER_LABEL, this.context);
    }

    @Test
    public void writeValueVerifiedLateralityValueIsNotAllowed()
    {
        LATERALITY.writeValue(this.qualifierObject, CANCER_LABEL, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueLateralityValueIsValid()
    {
        LATERALITY.writeValue(this.qualifierObject, BI_STR, this.context);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, BI_STR, this.context);
    }

    @Test
    public void writeValueVerifiedLateralityValueIsValid()
    {
        LATERALITY.writeValue(this.qualifierObject, BI_STR, this.context, true);
        verify(this.qualifierObject, times(1)).set(LATERALITY_LABEL, BI_STR, this.context);
    }

    @Test
    public void writeValueNotesValueIsNull()
    {
        NOTES.writeValue(this.qualifierObject, null, this.context);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, null, this.context);
    }

    @Test
    public void writeValueVerifiedNotesValueIsNull()
    {
        NOTES.writeValue(this.qualifierObject, null, this.context, true);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, null, this.context);
    }

    @Test
    public void writeValueNotesValueIsEmpty()
    {
        NOTES.writeValue(this.qualifierObject, StringUtils.EMPTY, this.context);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, StringUtils.EMPTY, this.context);
    }

    @Test
    public void writeValueVerifiedNotesValueIsEmpty()
    {
        NOTES.writeValue(this.qualifierObject, StringUtils.EMPTY, this.context, true);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, StringUtils.EMPTY, this.context);
    }

    @Test
    public void writeValueNotesValueIsBlank()
    {
        NOTES.writeValue(this.qualifierObject, StringUtils.SPACE, this.context);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, StringUtils.SPACE, this.context);
    }

    @Test
    public void writeValueVerifiedNotesValueIsBlank()
    {
        NOTES.writeValue(this.qualifierObject, StringUtils.SPACE, this.context, true);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, StringUtils.SPACE, this.context);
    }

    @Test
    public void writeValueNotesValueIsNotAString()
    {
        NOTES.writeValue(this.qualifierObject, CANCER, this.context);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, CANCER, this.context);
    }

    @Test
    public void writeValueVerifiedNotesValueIsNotAString()
    {
        NOTES.writeValue(this.qualifierObject, CANCER, this.context, true);
        verify(this.qualifierObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueNotesValueIsValid()
    {
        NOTES.writeValue(this.qualifierObject, BI_STR, this.context);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, BI_STR, this.context);
    }

    @Test
    public void writeValuVerifiedeNotesValueIsValid()
    {
        NOTES.writeValue(this.qualifierObject, BI_STR, this.context, true);
        verify(this.qualifierObject, times(1)).set(NOTES_LABEL, BI_STR, this.context);
    }

    @Test
    public void toStringTest()
    {
        Assert.assertEquals(CANCER_LABEL, CANCER.toString());
        Assert.assertEquals(AGE_AT_DIAGNOSIS_LABEL, AGE_AT_DIAGNOSIS.toString());
        Assert.assertEquals(NUM_AGE_AT_DIAGNOSIS_LABEL, NUMERIC_AGE_AT_DIAGNOSIS.toString());
        Assert.assertEquals(PRIMARY_LABEL, PRIMARY.toString());
        Assert.assertEquals(LATERALITY_LABEL, LATERALITY.toString());
        Assert.assertEquals(NOTES_LABEL, NOTES.toString());
    }

    @Test
    public void getProperty()
    {
        Assert.assertEquals(CANCER_LABEL, CANCER.getProperty());
        Assert.assertEquals(AGE_AT_DIAGNOSIS_LABEL, AGE_AT_DIAGNOSIS.getProperty());
        Assert.assertEquals(NUM_AGE_AT_DIAGNOSIS_LABEL, NUMERIC_AGE_AT_DIAGNOSIS.getProperty());
        Assert.assertEquals(PRIMARY_LABEL, PRIMARY.getProperty());
        Assert.assertEquals(LATERALITY_LABEL, LATERALITY.getProperty());
        Assert.assertEquals(NOTES_LABEL, NOTES.getProperty());
    }

    @Test
    public void getJsonProperty()
    {
        Assert.assertEquals(CANCER_LABEL, CANCER.getJsonProperty());
        Assert.assertEquals(AGE_AT_DIAGNOSIS_LABEL, AGE_AT_DIAGNOSIS.getJsonProperty());
        Assert.assertEquals(NUM_AGE_AT_DIAGNOSIS_LABEL, NUMERIC_AGE_AT_DIAGNOSIS.getJsonProperty());
        Assert.assertEquals(PRIMARY_LABEL, PRIMARY.getJsonProperty());
        Assert.assertEquals(LATERALITY_LABEL, LATERALITY.getJsonProperty());
        Assert.assertEquals(NOTES_LABEL, NOTES.getJsonProperty());
    }
}
