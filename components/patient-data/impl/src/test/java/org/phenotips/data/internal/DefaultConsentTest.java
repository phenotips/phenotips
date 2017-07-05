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

import org.phenotips.data.Consent;
import org.phenotips.data.ConsentStatus;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class DefaultConsentTest
{
    private static final String ID_STRING = "ID_STRING";

    private static final String LABEL_STRING = "LABEL_STRING";

    private static final String DESCRIPTION_STRING =
        "Sample consent explanation <a href=\"http://abc.def\">sample link</a>";

    private static final List<String> FORM_FIELDS = Arrays.asList("field1", "field2", "field3");

    private static final List<String> FORM_FIELDS_ALL = new LinkedList<>();

    private static final String EXCLUDED_FIELD = "field4";

    @Test
    public void testNormalInitialization()
    {
        boolean required = true;
        Consent consent = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, required, null);
        Assert.assertSame(consent.getId(), ID_STRING);
        Assert.assertSame(consent.getLabel(), LABEL_STRING);
        Assert.assertSame(consent.getDescription(), DESCRIPTION_STRING);
        Assert.assertSame(consent.isRequired(), required);
        Assert.assertSame(consent.getStatus(), ConsentStatus.NOT_SET);
        Assert.assertSame(consent.getFields(), null);

        // test normal initialization with no long description and with some fields
        Consent consent2 = new DefaultConsent(ID_STRING, LABEL_STRING, null, required, FORM_FIELDS);
        Assert.assertSame(consent2.getLabel(), LABEL_STRING);
        for (int i = 0; i < FORM_FIELDS.size(); ++i) {
            Assert.assertTrue(consent2.getFields().contains(FORM_FIELDS.get(i)));
        }
        Assert.assertFalse(consent2.getFields().contains(EXCLUDED_FIELD));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitializationWhenIdIsNull()
    {
        new DefaultConsent(null, LABEL_STRING, DESCRIPTION_STRING, true, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitializationWhenIdIsEmpty()
    {
        new DefaultConsent("", LABEL_STRING, DESCRIPTION_STRING, true, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitializationWhenLabelIsNull()
    {
        new DefaultConsent(ID_STRING, null, DESCRIPTION_STRING, true, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitializationWhenLabelIsEmpty()
    {
        new DefaultConsent(ID_STRING, "", DESCRIPTION_STRING, true, null);
    }

    @Test
    public void fieldFunctionsWork()
    {
        boolean required = false;
        Consent consent1 = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, required, FORM_FIELDS);
        Assert.assertFalse(consent1.affectsAllFields());
        Assert.assertTrue(consent1.affectsSomeFields());

        Consent consent2 = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, required, FORM_FIELDS_ALL);
        Assert.assertTrue(consent2.affectsAllFields());
        Assert.assertTrue(consent2.affectsSomeFields());

        Consent consent3 = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, required, null);
        Assert.assertFalse(consent3.affectsAllFields());
        Assert.assertFalse(consent3.affectsSomeFields());
    }

    @Test
    public void setStatusChangesStatus()
    {
        Consent consent = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, false, null);
        consent.setStatus(ConsentStatus.NO);
        Assert.assertSame(consent.getStatus(), ConsentStatus.NO);
        consent.setStatus(ConsentStatus.YES);
        Assert.assertSame(consent.getStatus(), ConsentStatus.YES);
    }

    @Test
    public void copiesCorrectly()
    {
        ConsentStatus status = ConsentStatus.NO;
        boolean required = false;
        Consent original = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, required, FORM_FIELDS);
        original.setStatus(status);
        Consent copy = original.copy(original.getStatus());
        Assert.assertSame(copy.getId(), ID_STRING);
        Assert.assertSame(copy.getLabel(), LABEL_STRING);
        Assert.assertSame(copy.getDescription(), DESCRIPTION_STRING);
        Assert.assertSame(copy.getStatus(), status);
        Assert.assertSame(copy.isRequired(), required);
        Consent copyYes = original.copy(ConsentStatus.YES);
        Assert.assertSame(copyYes.getStatus(), ConsentStatus.YES);
        Assert.assertSame(copyYes.getFields().size(), FORM_FIELDS.size());
    }

    @Test
    public void producesCorrectJson()
    {
        ConsentStatus status = ConsentStatus.NO;
        boolean required = false;
        Consent consent = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, required, FORM_FIELDS);
        consent.setStatus(status);

        JSONObject json = consent.toJSON();
        Assert.assertNotNull(json);

        Assert.assertSame(json.getString("id"), ID_STRING);
        Assert.assertSame(json.getString("label"), LABEL_STRING);
        Assert.assertSame(json.getString("description"), DESCRIPTION_STRING);
        Assert.assertSame(json.getString("status"), status.toString());
        Assert.assertSame(json.getBoolean("isRequired"), required);
        Assert.assertSame(json.getJSONArray("formFields").length(), FORM_FIELDS.size());
    }

    @Test
    public void initializesFromJsonCorrectly()
    {
        boolean required = true;
        Consent consent = new DefaultConsent(ID_STRING, LABEL_STRING, DESCRIPTION_STRING, required, FORM_FIELDS);
        JSONObject json = consent.toJSON();

        Consent newConsent = new DefaultConsent(json);
        Assert.assertSame(newConsent.getId(), ID_STRING);
        Assert.assertSame(newConsent.getLabel(), LABEL_STRING);
        Assert.assertSame(newConsent.getDescription(), DESCRIPTION_STRING);
        Assert.assertSame(newConsent.getStatus().toString(), ConsentStatus.NOT_SET.toString());
        Assert.assertSame(newConsent.isRequired(), required);
        Assert.assertSame(newConsent.getFields().size(), FORM_FIELDS.size());
        for (int i = 0; i < FORM_FIELDS.size(); ++i) {
            Assert.assertTrue(newConsent.getFields().contains(FORM_FIELDS.get(i)));
        }
        Assert.assertFalse(newConsent.getFields().contains(EXCLUDED_FIELD));
    }
}
