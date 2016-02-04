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
package org.phenotips.groups.script;

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Service for manipulating groups.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Component
@Named("groups")
@Singleton
public class GroupManagerScriptService implements ScriptService
{
    /** The actual group manager. */
    @Inject
    private GroupManager manager;

    @Inject
    private UserManager userManager;

    /**
     * List the groups that this user is a member of.
     *
     * @param username name of the user whose groups will be retrieved
     * @return an unmodifiable set of groups, empty if the user isn't part of any groups.
     */
    public Set<Group> getGroupsForUser(String username)
    {
        User user = this.userManager.getUser(username);
        if (user == null) {
            return Collections.EMPTY_SET;
        }

        try {
            return this.manager.getGroupsForUser(user);
        } catch (Exception ex) {
            return Collections.EMPTY_SET;
        }
    }
}
