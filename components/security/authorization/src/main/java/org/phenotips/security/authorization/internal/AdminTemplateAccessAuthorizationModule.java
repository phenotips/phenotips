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

import org.phenotips.Constants;
import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation that restricts access to templates to administrators.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("template-access")
@Singleton
public class AdminTemplateAccessAuthorizationModule implements AuthorizationModule
{
    private static final String TEMPLATE = "Template";

    @Inject
    private AuthorizationService auth;

    @Inject
    private DocumentReferenceResolver<EntityReference> resolver;

    @Override
    public int getPriority()
    {
        return 500;
    }

    @Override
    public Boolean hasAccess(final User user, final Right access, final EntityReference entity)
    {
        // If this is not a template, or the right is read-only, then do not try to authorize.
        if (!entity.toString().contains(TEMPLATE) || access.isReadOnly()) {
            return null;
        }

        // For templates, only grant access to administrators.
        return user != null && user.getProfileDocument() != null
            && this.auth.hasAccess(user, Right.ADMIN, this.resolver.resolve(Constants.XWIKI_SPACE_REFERENCE));
    }
}
