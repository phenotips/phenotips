package org.phenotips.configuration.internal.consent;

import org.phenotips.configuration.RecordElement;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;

import java.util.List;

/**
 * Grants authorization to keep a {@link RecordElement} enabled.
 */
@Role
public interface ConsentAuthorizer
{
    List<RecordElement> filterForm(List<RecordElement> elements, Patient patient);

    boolean authorizeInteraction(Patient patient);

    boolean isElementEnabled(RecordElement element, Patient patient);
}
