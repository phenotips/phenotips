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
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The base authorization module, always returning a globally configured default access decision, for all users,
 * documents, and requested rights. This is controlled by the
 * {@code phenotips.security.authorization.allowAllAccessByDefault} property in {@code xwiki.properties}, with the
 * default value {@code false}.
 *
 * @version $Id$
 * @since 1.0M13
 */
@Component
@Named("base")
@Singleton
public class BaseAuthorizationModule implements AuthorizationModule
{
    /** The global configuration. */
    @Inject
    @Named("restricted")
    private ConfigurationSource configuration;

    @Override
    public int getPriority()
    {
        return 0;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference entity)
    {
        Boolean result = this.configuration.getProperty("phenotips.security.authorization.allowAllAccessByDefault");
        if (result == null) {
            result = Boolean.FALSE;
        }
        return result;
    }
}
