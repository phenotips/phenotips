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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default (currently only) implementation of {@link Consent}.
 *
 * @version $Id$
 * @since 1.3M1
 */
public class DefaultConsent implements Consent
{
    private static final String JSON_KEY_ID = "id";
    private static final String JSON_KEY_LABEL = "label";
    private static final String JSON_KEY_DESCRIPTION = "description";
    private static final String JSON_KEY_ISREQUIRED = "isRequired";
    private static final String JSON_KEY_STATUS = "status";
    private static final String JSON_KEY_FIELDS = "formFields";

    private final String id;
    private final String label;
    private final String description;
    private final List<String> formFields;
    private boolean required;

    private ConsentStatus status = ConsentStatus.NOT_SET;

    /**
     * @param id consent id
     * @param label consent label/title (required)
     * @param description consent detailed description (optional, may be null)
     * @param required when true, no interaction with a document is allowed until this consent is granted
     * @param formFields form fields which are only availablke when this consent is granted
     */
    public DefaultConsent(String id, String label, String description, boolean required, List<String> formFields)
    {
        this.id = id;
        this.label = label;
        this.description = processDescription(description);
        this.required = required;
        this.formFields = (formFields == null) ? null : Collections.unmodifiableList(formFields);
        validate();
    }

    /**
     * A constructor from JSON representation.
     * @param consentJSON JSON in the format produced by toJSON()
     */
    public DefaultConsent(JSONObject consentJSON)
    {
        this.id = consentJSON.optString(JSON_KEY_ID);
        this.label = consentJSON.optString(JSON_KEY_LABEL);
        this.description = processDescription(consentJSON.optString(JSON_KEY_DESCRIPTION));
        this.required = consentJSON.optBoolean(JSON_KEY_ISREQUIRED);
        setStatus(ConsentStatus.fromString(consentJSON.optString(JSON_KEY_ISREQUIRED)));
        JSONArray fields = consentJSON.optJSONArray(JSON_KEY_FIELDS);
        if (fields == null) {
            this.formFields = null;
        } else {
            this.formFields = new LinkedList<String>();
            for (Object field : fields) {
                this.formFields.add((String) field);
            }
        }
        validate();
    }

    /**
     * Converts empty descripton to null for consistency.
     */
    private static String processDescription(String rawDescription)
    {
        return StringUtils.isEmpty(rawDescription) ? null : rawDescription;
    }

    private void validate()
    {
        if (StringUtils.isEmpty(this.id) || StringUtils.isEmpty(this.label)) {
            throw new IllegalArgumentException("a consent cannot have empty id or description");
        }
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getLabel()
    {
        return this.label;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public ConsentStatus getStatus()
    {
        return this.status;
    }

    @Override
    public boolean isGranted()
    {
        return this.getStatus() == ConsentStatus.YES;
    }

    @Override
    public void setStatus(ConsentStatus status)
    {
        if (status != null) {
            this.status = status;
        } else {
            this.status = ConsentStatus.NOT_SET;
        }
    }

    @Override
    public boolean isRequired()
    {
        return this.required;
    }

    @Override
    public List<String> getFields()
    {
        return this.formFields;
    }

    @Override
    public boolean affectsAllFields()
    {
        return (this.formFields != null) && (this.formFields.size() == 0);
    }

    @Override
    public boolean affectsSomeFields()
    {
        return this.formFields != null;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put(JSON_KEY_ID, this.getId());
        json.put(JSON_KEY_LABEL, this.getLabel());
        json.put(JSON_KEY_DESCRIPTION, this.getDescription());
        json.put(JSON_KEY_ISREQUIRED, this.isRequired());
        json.put(JSON_KEY_STATUS, this.getStatus().toString());
        if (this.formFields != null) {
            JSONArray fields = new JSONArray(this.formFields);
            json.put(JSON_KEY_FIELDS, fields);
        }
        return json;
    }

    /**
     * Copies an instance of {@link Consent} to a new instance.
     * @param from the consent whose state is to be copied
     * @return an instance which is identical to the `from` instance
     */
    @Override
    public Consent copy(ConsentStatus status)
    {
        Consent copy = new DefaultConsent(
            this.getId(), this.getLabel(), this.getDescription(), this.isRequired(), this.getFields());
        copy.setStatus(status);
        return copy;
    }
}
