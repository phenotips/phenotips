package org.phenotips.data.push.internal;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.phenotips.data.shareprotocol.ShareProtocol;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;

public class DefaultPushServerConfigurationResponse extends DefaultPushServerResponse implements PushServerConfigurationResponse
{
    DefaultPushServerConfigurationResponse(JSONObject serverResponse)
    {
        super(serverResponse);
    }

    protected Set<String> getSetFromJSONList(String key)
    {
        JSONArray stringList = response.optJSONArray(key);
        if (stringList == null)
            return null;

        Set<String> result = new TreeSet<String>();  // to make sure order is unchanged
        for(Object field : stringList) {
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

            if (remoteAcceptedFields == null)
                return Collections.emptySet();

            RecordConfigurationManager configurationManager = ComponentManagerRegistry.getContextComponentManager().
                                                              getInstance(RecordConfigurationManager.class);

            RecordConfiguration patientConfig = configurationManager.getActiveConfiguration();

            Set<String> commonFields = new TreeSet<String>(patientConfig.getEnabledNonIdentifiableFieldNames());

            commonFields.retainAll(remoteAcceptedFields);  // of the non-personal fields available,
                                                           // only keep those fields enable doin the remote server
            return commonFields;
        } catch(Exception ex) {
            return Collections.emptySet();
        }
    }

    @Override
    public String getRemoteUserToken()
    {
        return valueOrNull(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_USERTOKEN);
    }
}
