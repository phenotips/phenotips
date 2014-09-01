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
package org.phenotips.groups.script;

import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

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

    /**
     * List the groups that this user is a member of.
     *
     * @param user the user whose groups will be retrieved
     * @return an unmodifiable set of groups, empty if the user isn't part of any groups.
     */
    public Set<Group> getGroupsForUser(User user)
    {
        try {
            return this.manager.getGroupsForUser(user);
        } catch (Exception ex) {
            return Collections.emptySet();
        }
    }
}
