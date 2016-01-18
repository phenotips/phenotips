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

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class DefaultConsentTest
{
    private String ID_STRING = "ID_STRING";
    private String DESCRIPTION_STRING = "DESCRIPTION_STRING";

    @Test
    public void testNormalInitialization()
    {
        boolean required = true;
        Consent consent = new DefaultConsent(ID_STRING, DESCRIPTION_STRING, required);
        Assert.assertSame(consent.getId(), ID_STRING);
        Assert.assertSame(consent.getDescription(), DESCRIPTION_STRING);
        Assert.assertSame(consent.isRequired(), required);
        Assert.assertSame(consent.getStatus(), ConsentStatus.NOT_SET);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitialization_IdNull()
    {
        new DefaultConsent(null, DESCRIPTION_STRING, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitialization_IdEmpty()
    {
        new DefaultConsent("", DESCRIPTION_STRING, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitialization_DescrNull()
    {
        new DefaultConsent(ID_STRING, null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImproperInitialization_DescrEmpty()
    {
        new DefaultConsent(ID_STRING, "", true);
    }

    @Test
    public void setStatusChangesStatus()
    {
        Consent consent = new DefaultConsent(ID_STRING, DESCRIPTION_STRING, false);
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
        Consent original = new DefaultConsent(ID_STRING, DESCRIPTION_STRING, required);
        original.setStatus(status);
        Consent copy = DefaultConsent.copy(original);
        Assert.assertSame(copy.getId(), ID_STRING);
        Assert.assertSame(copy.getDescription(), DESCRIPTION_STRING);
        Assert.assertSame(copy.getStatus(), status);
        Assert.assertSame(copy.isRequired(), required);
    }

    @Test
    public void producesCorrectJson()
    {
        ConsentStatus status = ConsentStatus.NO;
        boolean required = false;
        Consent consent = new DefaultConsent(ID_STRING, DESCRIPTION_STRING, required);
        consent.setStatus(status);

        JSONObject json = consent.toJson();
        Assert.assertNotNull(json);

        Assert.assertSame(json.getString("id"), ID_STRING);
        Assert.assertSame(json.getString("description"), DESCRIPTION_STRING);
        Assert.assertSame(json.getString("status"), status.toString());
        Assert.assertSame(json.getBoolean("isRequired"), required);
    }
}
