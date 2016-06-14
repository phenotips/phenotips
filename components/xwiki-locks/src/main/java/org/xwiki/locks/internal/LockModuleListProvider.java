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
package org.xwiki.locks.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.locks.LockModule;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides an ordered list of lock modules.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class LockModuleListProvider implements Provider<List<LockModule>>
{
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Override
    public List<LockModule> get()
    {
        try {
            List<LockModule> services = new LinkedList<>();
            services.addAll(this.componentManager.<LockModule>getInstanceList(LockModule.class));
            Collections.sort(services, LockModuleComparator.INSTANCE);
            return services;
        } catch (ComponentLookupException ex) {
            throw new RuntimeException("Failed to look up lock modules", ex);
        }
    }

    /**
     * Sorts the available lock modules in descending order of their priority, then alphabetically if two or more
     * modules have the same priority.
     */
    private static final class LockModuleComparator implements Comparator<LockModule>
    {
        /** Singleton instance. */
        private static final LockModuleComparator INSTANCE = new LockModuleComparator();

        @Override
        public int compare(LockModule o1, LockModule o2)
        {
            int result = o2.getPriority() - o1.getPriority();
            // If the happen to have the same priority, to avoid randomness, order them alphabetically by their name
            if (result == 0) {
                result = o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
            }
            return result;
        }
    }
}
