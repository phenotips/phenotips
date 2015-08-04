package org.phenotips.data;

/**
 * Used to indicate the status of a consent, which is not limited to 'given'/'not given'.
 */
public enum ConsentStatus
{
    NOT_SET("not_set"), YES("yes"), NO("no");

    String stringRepresentation;

    ConsentStatus(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    public String toString()
    {
        return this.stringRepresentation;
    }

    public static ConsentStatus fromString(String string) {
        for (ConsentStatus status : ConsentStatus.values()) {
            if (status.toString().contentEquals(string))
            {
                return status;
            }
        }
        return null;
    }
}
