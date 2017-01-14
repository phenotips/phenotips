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

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactsManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Straightforward implementation of {@link PatientContactsManager}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Component
@Singleton
public class DefaultPatientContactsManager implements PatientContactsManager
{
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /** The set of PatientContactProvider implementations, ordered by priority. */
    private Set<PatientContactProvider> providers = new TreeSet<PatientContactProvider>();

    private Patient patient;

    /**
     * Simple constructor given a patient.
     *
     * @param patient the patient to get contacts for
     */
    public DefaultPatientContactsManager(Patient patient)
    {
        this.patient = patient;

        try {
            List<PatientContactProvider> availableProviders =
                this.componentManager.<PatientContactProvider>getInstanceList(PatientContactProvider.class);
            Collections.sort(availableProviders, PatientContactProviderComparator.INSTANCE);
            for (PatientContactProvider provider : availableProviders) {
                this.providers.add(provider);
            }
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to lookup serializers", ex);
        }
    }

    @Override
    public int size()
    {
        return this.providers.size();
    }

    @Override
    public ContactInfo getFirst()
    {
        try {
            // Get first non-empty, in order of decreasing priority
            return getAll().iterator().next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public Collection<ContactInfo> getAll()
    {
        List<ContactInfo> contactInfos = new ArrayList<ContactInfo>();
        for (PatientContactProvider provider : this.providers) {
            List<ContactInfo> info = provider.getContacts(patient);
            if (info != null && !info.isEmpty()) {
                contactInfos.addAll(info);
            }
        }
        if (contactInfos.isEmpty()) {
            return null;
        }
        return contactInfos;
    }

    @Override
    public Collection<String> getEmails()
    {
        Collection<ContactInfo> allInfo = getAll();
        List<String> allEmails = new ArrayList<String>();
        if (allInfo != null) {
            for (ContactInfo info : allInfo) {
                List<String> emails = info.getEmails();
                if (!emails.isEmpty()) {
                    allEmails.addAll(emails);
                }
            }
        }
        return allEmails;
    }

    /**
     * Sorts the available patient contact providers in descending order of their priority, then alphabetically if two
     * or more modules have the same priority.
     */
    private static final class PatientContactProviderComparator implements Comparator<PatientContactProvider>
    {
        /** Singleton instance. */
        private static final PatientContactProviderComparator INSTANCE = new PatientContactProviderComparator();

        @Override
        public int compare(PatientContactProvider o1, PatientContactProvider o2)
        {
            int result = o2.compareTo(o1);
            // If the happen to have the same priority, to avoid randomness, order them alphabetically by their name
            if (result == 0) {
                result = o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
            }
            return result;
        }
    }
}
