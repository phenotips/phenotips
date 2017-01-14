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

import org.phenotips.data.ContactInfo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * A default representation of {@link ContactInfo}.
 *
 * @version $Id$
 * @since 1.3M5
 */
public class DefaultContactInfo implements ContactInfo
{
    /**
     * The id of a local PhenoTips user.
     */
    private String userId;

    /**
     * The full name of the contact.
     */
    private String name;

    /**
     * The institution of the contact.
     */
    private String institution;

    /**
     * The list of contact's email addresses.
     */
    private List<String> emails;

    /**
     * A URL for the contact.
     */
    private String url;

    private List<String> getFields()
    {
        List<String> fields = new ArrayList<>();
        fields.add(getName());
        fields.add(getInstitution());
        fields.addAll(getEmails());
        fields.add(getUrl());
        fields.add(getUserId());
        return fields;
    }

    @Override
    public boolean isEmpty()
    {
        for (String field : getFields()) {
            if (StringUtils.isNotBlank(field)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getUserId()
    {
        return this.userId;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getInstitution()
    {
        return this.institution;
    }

    @Override
    public List<String> getEmails()
    {
        return this.emails;
    }

    @Override
    public String getUrl()
    {
        return this.url;
    }

    /**
     * Set the user id.
     *
     * @param userId the local user's id
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * Set the contact's full name.
     *
     * @param name the contact's full name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Set the contact institution.
     *
     * @param institution the institution
     */
    public void setInstitution(String institution)
    {
        this.institution = institution;
    }

    /**
     * Set the email address for the contact.
     *
     * @param emails list of the email addresses
     */
    public void setEmails(List<String> emails)
    {
        this.emails = emails;
    }

    /**
     * Set the URL for the contact.
     *
     * @param url the url
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject info = new JSONObject();
        info.put("id", this.getUserId());
        info.put("name", this.getName());
        info.put("institution", this.getInstitution());
        info.put("email", StringUtils.join(this.getEmails(), ", "));
        info.put("url", this.getUrl());
        return info;
    }
}
