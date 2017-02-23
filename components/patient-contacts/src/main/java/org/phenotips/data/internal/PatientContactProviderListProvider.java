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

import org.phenotips.data.PatientContactProvider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides an ordered list of authorization modules.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class PatientContactProviderListProvider implements Provider<List<PatientContactProvider>>
{
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Override
    public List<PatientContactProvider> get()
    {
        try {
            List<PatientContactProvider> services = new LinkedList<>();
            services
                .addAll(this.componentManager.<PatientContactProvider>getInstanceList(PatientContactProvider.class));
            Collections.sort(services, PatientContactProviderComparator.INSTANCE);
            return services;
        } catch (ComponentLookupException ex) {
            return Collections.emptyList();
        }
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
            int result = o1.getPriority() - o2.getPriority();
            // If they have the same priority, to avoid randomness order them alphabetically by their class name
            if (result == 0) {
                result = o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
            }
            return result;
        }
    }
}
