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
package org.phenotips.entities.configuration.internal;

import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationModule;

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
 * Returns the list of active {@link PrimaryEntityConfigurationModule}s, sorted in ascending order of their priority.
 *
 * @version $Id$
 * @since 1.4
 */
@Singleton
@Component
public class PrimaryEntityConfigurationModuleProvider implements Provider<List<PrimaryEntityConfigurationModule>>
{
    @Inject
    @Named("wiki")
    private ComponentManager cm;

    @Override
    public List<PrimaryEntityConfigurationModule> get()
    {
        try {
            List<PrimaryEntityConfigurationModule> modules = new LinkedList<>();
            modules.addAll(
                this.cm.<PrimaryEntityConfigurationModule>getInstanceList(PrimaryEntityConfigurationModule.class));
            Collections.sort(modules, ModulePriorityComparator.INSTANCE);
            return modules;
        } catch (ComponentLookupException ex) {
            throw new RuntimeException("Failed to look up record configuration modules: " + ex.getMessage(), ex);
        }
    }

    private static final class ModulePriorityComparator implements Comparator<PrimaryEntityConfigurationModule>
    {
        private static final ModulePriorityComparator INSTANCE = new ModulePriorityComparator();

        @Override
        public int compare(PrimaryEntityConfigurationModule o1, PrimaryEntityConfigurationModule o2)
        {
            final int result = o1.getPriority() - o2.getPriority();
            if (result == 0) {
                return o1.getClass().getSimpleName().compareToIgnoreCase(o2.getClass().getSimpleName());
            }
            return result;
        }
    }
}
