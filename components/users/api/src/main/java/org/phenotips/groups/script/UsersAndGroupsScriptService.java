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

import org.phenotips.groups.internal.UsersAndGroups;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import net.sf.json.JSON;

/**
 * @version $Id$
 */
@Component
@Named("usersAndGroups")
@Singleton
public class UsersAndGroupsScriptService implements ScriptService
{
    @Inject
    private UsersAndGroups usersAndGroups;

    /**
     * Searches for users and/or groups matching the input parameter. Result is returned as JSON
     *
     * @param input string to look for
     * @param searchUsers if true, includes users in result
     * @param searchGroups if true, includes groups in result
     * @return a json object containing all results found
     */
    public JSON searchUsersAndGroups(String input, boolean searchUsers, boolean searchGroups)
    {
        return usersAndGroups.search(input, searchUsers, searchGroups);
    }
}
