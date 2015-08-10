package org.phenotips.data;

import org.xwiki.component.annotation.Role;

import java.util.List;

import net.sf.json.JSON;

/**
 * Used for interacting with several consents (represented as {@link Consent}) at once; for example loading all consents
 * from a patient and converting them to JSON.
 */
@Role
public interface ConsentManager
{
    List<Consent> loadConsentsFromPatient(String patientId);

    List<Consent> loadConsentsFromPatient(Patient patient);

    /** bulk import/export. */
    JSON toJson(List<Consent> consents);

    List<Consent> fromJson(JSON json);

    boolean setPatientConsents(String patientId, List<Consent> consents);
}
