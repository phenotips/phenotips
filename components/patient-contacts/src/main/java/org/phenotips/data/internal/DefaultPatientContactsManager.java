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
import org.phenotips.data.PatientContactProvider;
import org.phenotips.data.PatientContactsManager;

import org.xwiki.component.annotation.Component;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Straightforward implementation of {@link PatientContactsManager}.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class DefaultPatientContactsManager implements PatientContactsManager
{
    /** The set of PatientContactProvider implementations, ordered by priority. */
    @Inject
    private Provider<List<PatientContactProvider>> providers;

    @Override
    public List<ContactInfo> getAll(Patient patient)
    {
        List<ContactInfo> contactInfos = new LinkedList<>();
        for (PatientContactProvider provider : this.providers.get()) {
            List<ContactInfo> info = provider.getContacts(patient);
            if (info != null && !info.isEmpty()) {
                contactInfos.addAll(info);
            }
        }
        return contactInfos;
    }

    @Override
    public ContactInfo getFirst(Patient patient)
    {
        List<ContactInfo> all = getAll(patient);
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }
}
