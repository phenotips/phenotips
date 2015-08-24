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
package org.phenotips.security.authorization.internal;

import org.phenotips.security.authorization.AuthorizationModule;

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
 * @since 1.2RC1
 */
@Component
@Singleton
public class AuthorizationModuleListProvider implements Provider<List<AuthorizationModule>>
{
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Override
    public List<AuthorizationModule> get()
    {
        try {
            List<AuthorizationModule> services = new LinkedList<>();
            services.addAll(this.componentManager.<AuthorizationModule>getInstanceList(AuthorizationModule.class));
            Collections.sort(services, AuthorizationModuleComparator.INSTANCE);
            return services;
        } catch (ComponentLookupException ex) {
            throw new RuntimeException("Failed to look up authorization modules", ex);
        }
    }

    /**
     * Sorts the available authorization modules in descending order of their priority, then alphabetically if two or
     * more modules have the same priority.
     */
    private static final class AuthorizationModuleComparator implements Comparator<AuthorizationModule>
    {
        /** Singleton instance. */
        private static final AuthorizationModuleComparator INSTANCE = new AuthorizationModuleComparator();

        @Override
        public int compare(AuthorizationModule o1, AuthorizationModule o2)
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
