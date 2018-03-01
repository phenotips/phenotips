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
 * Tests for the {@link org.phenotips.data.Cancer.CancerProperty}.
 *
 * @version $Id$
 */
public class CancerTest
{
    private static final String ID_LABEL = "id";

    private static final String CANCER_LABEL = "cancer";

    private static final String AFFECTED_LABEL = "affected";

    private static final String CANCER_1_ID = "HP:001";

    private static final Cancer.CancerProperty CANCER = Cancer.CancerProperty.CANCER;

    private static final Cancer.CancerProperty AFFECTED = Cancer.CancerProperty.AFFECTED;

    @Mock
    private BaseObject cancerObject;

    @Mock
    private BaseStringProperty idField;

    private XWikiContext context;

    private JSONObject cancerJson;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.context = new XWikiContext();
        this.cancerJson = new JSONObject().put(ID_LABEL, CANCER_1_ID).put(AFFECTED_LABEL, true);

        when(this.cancerObject.getField(CANCER_LABEL)).thenReturn(this.idField);
        when(this.idField.getValue()).thenReturn(CANCER_1_ID);

        when(this.cancerObject.getIntValue(AFFECTED_LABEL, -1)).thenReturn(1);
    }

    @Test
    public void classReferenceIsCorrect()
    {
        Assert.assertEquals("CancerClass", Cancer.CLASS_REFERENCE.getName());
        Assert.assertEquals("PhenoTips", Cancer.CLASS_REFERENCE.extractReference(EntityType.SPACE).getName());
        Assert.assertNull(Cancer.CLASS_REFERENCE.extractReference(EntityType.WIKI));
    }

    @Test
    public void extractValueCancerIdFieldIsNull()
    {
        when(this.cancerObject.getField(CANCER_LABEL)).thenReturn(null);
        Assert.assertNull(CANCER.extractValue(this.cancerObject));
    }

    @Test
    public void extractValueJsonCancerIdFieldIsNull()
    {
        this.cancerJson.put(ID_LABEL, (String) null);
        Assert.assertNull(CANCER.extractValue(this.cancerJson));
    }

    @Test
    public void extractValueCancerIdFieldValueIsNull()
    {
        when(this.idField.getValue()).thenReturn(null);
        Assert.assertNull(CANCER.extractValue(this.cancerObject));
    }

    @Test
    public void extractValueCancerIdFieldValueIsEmpty()
    {
        when(this.idField.getValue()).thenReturn(StringUtils.EMPTY);
        Assert.assertNull(CANCER.extractValue(this.cancerObject));
    }

    @Test
    public void extractValueJsonCancerIdFieldIsEmpty()
    {
        this.cancerJson.put(ID_LABEL, StringUtils.EMPTY);
        Assert.assertNull(CANCER.extractValue(this.cancerJson));
    }

    @Test
    public void extractValueCancerIdFieldValueIsBlank()
    {
        when(this.idField.getValue()).thenReturn(StringUtils.SPACE);
        Assert.assertNull(CANCER.extractValue(this.cancerObject));
    }

    @Test
    public void extractValueJsonCancerIdFieldIsBlank()
    {
        this.cancerJson.put(ID_LABEL, StringUtils.SPACE);
        Assert.assertNull(CANCER.extractValue(this.cancerJson));
    }

    @Test
    public void extractValueCancerIdFieldValueIsNotBlank()
    {
        Assert.assertEquals(CANCER_1_ID, CANCER.extractValue(this.cancerObject));
    }

    @Test
    public void extractValueJsonCancerIdFieldValueIsNotBlank()
    {
        Assert.assertEquals(CANCER_1_ID, CANCER.extractValue(this.cancerJson));
    }

    @Test
    public void extractValueAffectedIsZero()
    {
        when(this.cancerObject.getIntValue(AFFECTED_LABEL, 0)).thenReturn(0);
        Assert.assertFalse((Boolean) AFFECTED.extractValue(this.cancerObject));
    }

    @Test
    public void extractValueJsonAffectedIsFalse()
    {
        this.cancerJson.put(AFFECTED_LABEL, false);
        Assert.assertFalse((Boolean) AFFECTED.extractValue(this.cancerJson));
    }

    @Test
    public void extractValueAffectedIsPositive()
    {
        when(this.cancerObject.getIntValue(AFFECTED_LABEL, 0)).thenReturn(5);
        final Boolean affected = (Boolean) AFFECTED.extractValue(this.cancerObject);
        Assert.assertTrue(affected);
    }

    @Test
    public void extractValueJsonAffectedIsTrue()
    {
        this.cancerJson.put(AFFECTED_LABEL, true);
        Assert.assertTrue((Boolean) AFFECTED.extractValue(this.cancerJson));
    }

    @Test
    public void valueIsValidReturnsFalseWhenProvidedCancerIdValueIsNull()
    {
        Assert.assertFalse(CANCER.valueIsValid(null));
    }

    @Test
    public void valueIsValidReturnsFalseWhenProvidedCancerIdValueIsEmpty()
    {
        Assert.assertFalse(CANCER.valueIsValid(StringUtils.EMPTY));
    }

    @Test
    public void valueIsValidReturnsFalseWhenProvidedCancerIdValueIsBlank()
    {
        Assert.assertFalse(CANCER.valueIsValid(StringUtils.SPACE));
    }

    @Test
    public void valueIsValidReturnsFalseWhenProvidedCancerIdValueIsNotAString()
    {
        Assert.assertFalse(CANCER.valueIsValid(1));
        Assert.assertFalse(CANCER.valueIsValid(true));
        Assert.assertFalse(CANCER.valueIsValid(CANCER));
    }

    @Test
    public void valueIsValidReturnsTrueWhenTheProvidedCancerIdValueIsAString()
    {
        Assert.assertTrue(CANCER.valueIsValid(CANCER_LABEL));
    }


    @Test
    public void valueIsValidReturnsFalseWhenProvidedAffectedValueIsNull()
    {
        Assert.assertFalse(AFFECTED.valueIsValid(null));
    }

    @Test
    public void valueIsValidReturnsFalseWhenProvidedAffectedValueIsNotABoolean()
    {
        Assert.assertFalse(AFFECTED.valueIsValid(1));
        Assert.assertFalse(AFFECTED.valueIsValid(CANCER));
        Assert.assertFalse(AFFECTED.valueIsValid(CANCER_LABEL));
    }

    @Test
    public void valueIsValidReturnsTrueWhenTheProvidedAffectedValueIsABoolean()
    {
        Assert.assertTrue(AFFECTED.valueIsValid(true));
        Assert.assertTrue(AFFECTED.valueIsValid(false));
    }

    @Test
    public void writeValueCancerIdIsNull()
    {
        CANCER.writeValue(this.cancerObject, null, this.context);
        verify(this.cancerObject, times(1)).set(CANCER_LABEL, null, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsNull()
    {
        CANCER.writeValue(this.cancerObject, null, this.context, true);
        verify(this.cancerObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsEmpty()
    {
        CANCER.writeValue(this.cancerObject, StringUtils.EMPTY, this.context);
        verify(this.cancerObject, times(1)).set(CANCER_LABEL, StringUtils.EMPTY, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsEmpty()
    {
        CANCER.writeValue(this.cancerObject, StringUtils.EMPTY, this.context, true);
        verify(this.cancerObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsBlank()
    {
        CANCER.writeValue(this.cancerObject, StringUtils.SPACE, this.context);
        verify(this.cancerObject, times(1)).set(CANCER_LABEL, StringUtils.SPACE, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsBlank()
    {
        CANCER.writeValue(this.cancerObject, StringUtils.SPACE, this.context, true);
        verify(this.cancerObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsNotString()
    {
        CANCER.writeValue(this.cancerObject, 3, this.context);
        verify(this.cancerObject, times(1)).set(CANCER_LABEL, 3, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsNotString()
    {
        CANCER.writeValue(this.cancerObject, 3, this.context, true);
        verify(this.cancerObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueCancerIdIsSetIfValid()
    {
        CANCER.writeValue(this.cancerObject, CANCER_1_ID, this.context);
        verify(this.cancerObject, times(1)).set(CANCER_LABEL, CANCER_1_ID, this.context);
    }

    @Test
    public void writeValueVerifiedCancerIdIsSetIfValid()
    {
        CANCER.writeValue(this.cancerObject, CANCER_1_ID, this.context, true);
        verify(this.cancerObject, times(1)).set(CANCER_LABEL, CANCER_1_ID, this.context);
    }

    @Test
    public void writeValueAffectedStatusIsNull()
    {
        AFFECTED.writeValue(this.cancerObject, null, this.context);
        verify(this.cancerObject, times(1)).set(AFFECTED_LABEL, 0, this.context);
    }

    @Test
    public void writeValueVerifiedAffectedStatusIsNull()
    {
        AFFECTED.writeValue(this.cancerObject, null, this.context, true);
        verify(this.cancerObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueAffectedStatusIsNotBoolean()
    {
        AFFECTED.writeValue(this.cancerObject, CANCER_1_ID, this.context);
        verify(this.cancerObject, times(1)).set(AFFECTED_LABEL, 0, this.context);
    }

    @Test
    public void writeValueVerifiedAffectedStatusIsNotBoolean()
    {
        AFFECTED.writeValue(this.cancerObject, CANCER_1_ID, this.context, true);
        verify(this.cancerObject, never()).set(anyString(), any(), any());
    }

    @Test
    public void writeValueAffectedStatusIsSetIfValid()
    {
        AFFECTED.writeValue(this.cancerObject, true, this.context);
        verify(this.cancerObject, times(1)).set(AFFECTED_LABEL, 1, this.context);
    }

    @Test
    public void toStringReturnsCorrectPropertyName()
    {
        Assert.assertEquals(ID_LABEL, CANCER.toString());
        Assert.assertEquals(AFFECTED_LABEL, AFFECTED.toString());
    }

    @Test
    public void getPropertyReturnsInternalPropertyName()
    {
        Assert.assertEquals(CANCER_LABEL, CANCER.getProperty());
        Assert.assertEquals(AFFECTED_LABEL, AFFECTED.getProperty());
    }

    @Test
    public void getJsonPropertyReturnsJsonPropertyName()
    {
        Assert.assertEquals(ID_LABEL, CANCER.getJsonProperty());
        Assert.assertEquals(AFFECTED_LABEL, AFFECTED.getJsonProperty());
    }
}
