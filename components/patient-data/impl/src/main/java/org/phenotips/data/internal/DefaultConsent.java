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

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Default (currently only) implementation of {@link Consent}.
 *
 * @version $Id$
 * @since 1.2RC2
 */
public class DefaultConsent implements Consent
{
    private String id;
    private String description;
    private boolean required;

    private ConsentStatus status = ConsentStatus.NOT_SET;

    DefaultConsent(String id, String description, boolean required)
    {
        this.id = id;
        this.description = description;
        this.required = required;
    }

    @Override public String getId()
    {
        return this.id;
    }

    @Override public String getDescription()
    {
        return this.description;
    }

    @Override public ConsentStatus getStatus()
    {
        return this.status;
    }

    @Override public void setStatus(ConsentStatus status)
    {
        this.status = status;
    }

    @Override public boolean isRequired()
    {
        return this.required;
    }

    @Override public JSON toJson()
    {
        JSONObject json = new JSONObject();
        json.put("id", this.getId());
        json.put("description", this.getDescription());
        json.put("isRequired", this.isRequired());
        json.put("status", this.getStatus().toString());
        return json;
    }

    @Override public Consent fromJson(JSON json)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Copies an instance of {@link Consent} to a new instance.
     * @param from the consent whose state is to be copied
     * @return an instance which is identical to the `from` instance
     */
    public static Consent copy(Consent from)
    {
        Consent copy = new DefaultConsent(from.getId(), from.getDescription(), from.isRequired());
        copy.setStatus(from.getStatus());
        return copy;
    }
}
