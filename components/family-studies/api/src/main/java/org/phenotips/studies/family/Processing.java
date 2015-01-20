package org.phenotips.studies.family;

import org.phenotips.studies.family.internal.StatusResponse;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

import net.sf.json.JSONObject;

@Role
public interface Processing
{
    public StatusResponse processPatientPedigree(String patientId, JSONObject json, String image) throws XWikiException;
}
