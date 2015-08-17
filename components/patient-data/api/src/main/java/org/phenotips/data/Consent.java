package org.phenotips.data;

import net.sf.json.JSON;

/**
 * Used for representing a single consent; for example in a patient record.
 */
public interface Consent
{
    String getID();

    String getDescription();

    /**
     * Could be notLoaded/Yes/No. The consent configurations (description, id) will be coming from the database, and
     * should not be reloaded often. There should then be a cache of all the consents in the system with a status of
     * notLoaded.
     */
    ConsentStatus getStatus();

    void setStatus(ConsentStatus status);

    boolean isRequired();

    JSON toJson();

    /** Returns a new {@link Consent} instance. Should be a static method, but should also be in the interface. */
    Consent fromJson();
}