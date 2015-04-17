package org.phenotips.studies.family;

import org.phenotips.studies.family.internal.StatusResponse;

import org.xwiki.component.annotation.Role;
import org.xwiki.query.QueryException;

import javax.naming.NamingException;

import com.xpn.xwiki.XWikiException;

import net.sf.json.JSONObject;

@Role
public interface Processing
{
    StatusResponse processPatientPedigree(String patientId, JSONObject json, String image)
        throws XWikiException, NamingException, QueryException;

    String PATIENT_LINK_JSON_KEY = "phenotipsId";
}
