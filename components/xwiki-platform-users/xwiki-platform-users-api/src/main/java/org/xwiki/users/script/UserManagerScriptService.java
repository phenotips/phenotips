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
package org.xwiki.users.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * User management services.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Component
@Named("users")
@Singleton
public class UserManagerScriptService implements ScriptService
{
    @Inject
    private UserManager manager;

    /**
     * Get the currently logged in user.
     *
     * @return the currently logged in user, or {@code null} if no user is logged in
     */
    public User getCurrentUser()
    {
        return this.manager.getCurrentUser();
    }
}
