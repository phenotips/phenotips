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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Authorization module using XWiki's classic ACLs.
 *
 * @version $Id$
 * @since 1.0M13
 */
@Component
@Named("xwiki-acl")
@Singleton
public class XWikiACLAuthorizationModule implements AuthorizationModule
{
    /** The global configuration. */
    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public int getPriority()
    {
        return 100;
    }

    @Override
    public Boolean hasAccess(User user, Right access, DocumentReference document)
    {
        return this.authorizationManager.hasAccess(access, user.getProfileDocument(), document);
    }
}
