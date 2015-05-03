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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
