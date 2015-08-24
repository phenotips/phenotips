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
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

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
        for (AuthorizationModule service : this.modules.get()) {
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
}
