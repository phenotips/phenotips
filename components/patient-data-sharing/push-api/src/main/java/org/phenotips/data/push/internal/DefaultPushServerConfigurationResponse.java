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
    private static final String PATIENT_LABEL = "patient";

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

        Set<String> result = new TreeSet<>(); // to make sure order is unchanged
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
        Set<String> remoteAcceptedPatientFields =
            getSetFromJSONList(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_ACCEPTEDFIELDS);
        String serverProtocolVersion = getServerProtocolVersion();
        // if genes are enabled for push, manually add variants to the list of enabled fields to generate push dialog
        // when pushing from the new instance where pushing genes and variants are enabled to push separately
        // to the older version where they are still grouped together
        if (remoteAcceptedPatientFields != null && !remoteAcceptedPatientFields.contains("variants")
            && ShareProtocol.INCOMPATIBILITIES_IN_OLD_PROTOCOL_VERSIONS.containsKey(serverProtocolVersion)
            && ShareProtocol.INCOMPATIBILITIES_IN_OLD_PROTOCOL_VERSIONS.get(serverProtocolVersion).contains(
                ShareProtocol.GENE_STATUS_INCOMPAT)
            && remoteAcceptedPatientFields.contains("genes")) {
            Set<String> result = new TreeSet<>(remoteAcceptedPatientFields);
            result.add("variants");
            return Collections.unmodifiableSet(result);
        }
        return remoteAcceptedPatientFields;
    }

    @Override
    public Set<String> getPushableFields()
    {
        return getPushableFields(null);
    }

    @Override
    public Set<String> getPushableFields(String groupName)
    {
        try {
            Set<String> remoteAcceptedFields = getRemoteAcceptedPatientFields(groupName);

            if (remoteAcceptedFields == null) {
                return Collections.emptySet();
            }

            RecordConfigurationManager configurationManager =
                ComponentManagerRegistry.getContextComponentManager().getInstance(RecordConfigurationManager.class);

            RecordConfiguration patientConfig = configurationManager.getConfiguration(PATIENT_LABEL);

            Set<String> commonFields = new TreeSet<>(patientConfig.getEnabledFieldNames());

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

    @Override
    public JSONArray getConsents()
    {
        return this.response.optJSONArray(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_CONSENTS);
    }
}
