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
package org.phenotips.data.push.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

public class DefaultPushServerConfigurationResponse extends DefaultPushServerResponse implements
    PushServerConfigurationResponse
{
    DefaultPushServerConfigurationResponse(JSONObject serverResponse)
    {
        super(serverResponse);
    }

    protected Set<String> getSetFromJSONList(String key)
    {
        JSONArray stringList = this.response.optJSONArray(key);
        if (stringList == null) {
            return null;
        }

        Set<String> result = new TreeSet<String>(); // to make sure order is unchanged
        for (Object field : stringList) {
            result.add(field.toString());
        }

        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<String> getRemoteUserGroups()
    {
        return getSetFromJSONList(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_USERGROUPS);
    }

    @Override
    public Set<String> getRemoteAcceptedPatientFields()
    {
        return getRemoteAcceptedPatientFields(null);
    }

    @Override
    public Set<String> getRemoteAcceptedPatientFields(String groupName)
    {
        return getSetFromJSONList(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_ACCEPTEDFIELDS);
    }

    @Override
    public Set<String> getPushableFields()
    {
        return getPushableFields(null);
    }

    @Override
    public Set<String> getPushableFields(String groupName)
    {
        try
        {
            Set<String> remoteAcceptedFields = getRemoteAcceptedPatientFields(groupName);

            if (remoteAcceptedFields == null) {
                return Collections.emptySet();
            }

            RecordConfigurationManager configurationManager = ComponentManagerRegistry.getContextComponentManager().
                getInstance(RecordConfigurationManager.class);

            RecordConfiguration patientConfig = configurationManager.getActiveConfiguration();

            Set<String> commonFields = new TreeSet<String>(patientConfig.getEnabledNonIdentifiableFieldNames());

            // From the non-PII fields available, keep only those that are also enabled on the remote server
            commonFields.retainAll(remoteAcceptedFields);
            return commonFields;
        } catch (Exception ex) {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean remoteUpdatesEnabled()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_UPDATESENABLED);
    }

    @Override
    public String getRemoteUserToken()
    {
        return valueOrNull(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_USERTOKEN);
    }
}
