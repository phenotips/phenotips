/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.security.authorization.internal;

import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * The default authorization service implementation, which queries all the individual {@link AuthorizationModule}s, in
 * descending order of priority, until one responds with a non-null decision.
 *
 * @version $Id$
 * @since 1.0M13
 */
@Component
@Singleton
public class DefaultAuthorizationService implements AuthorizationService
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides the list of all the available authorization modules, which perform the actual rights checking. */
    @Inject
    private Provider<List<AuthorizationModule>> modules;

    @Override
    public boolean hasAccess(User user, Right access, DocumentReference document)
    {
        List<AuthorizationModule> services = new LinkedList<>();
        services.addAll(this.modules.get());
        Collections.sort(services, AuthorizationModuleComparator.INSTANCE);
        for (AuthorizationModule service : services) {
            try {
                Boolean decision = service.hasAccess(user, access, document);
                if (decision != null) {
                    return decision;
                }
            } catch (Exception ex) {
                // Don't fail because of bad authorization modules
                this.logger.warn("Failed to invoke authorization service [{}]: {}",
                    service.getClass().getCanonicalName(), ex.getMessage());
            }
        }

        return false;
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
