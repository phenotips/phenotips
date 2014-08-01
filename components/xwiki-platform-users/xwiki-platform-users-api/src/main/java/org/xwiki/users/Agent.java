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
package org.xwiki.users;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import java.net.URI;

/**
 * Represents a person or group of persons participating in the wiki.
 * 
 * @version $Id$
 * @since 1.0M10
 */
@Unstable
public interface Agent
{
    /**
     * Check if the returned agent actually exists or not.
     * 
     * @return {@code true} if the agent profile is valid (e.g. the profile wiki document exists for a wiki user or
     *         group)
     */
    boolean exists();

    /**
     * An identifier which can be used internally for identifying the agent. This is the value that should be stored to
     * remember the user or group.
     * 
     * @return serialized identifier which can be used to store and retrieve back this agent object
     */
    String getId();

    /**
     * A short name to display to users for identifying this agent. For users, this is their real name (in a Givenname
     * Familyname format), for groups this is the group's name.
     * 
     * @return the agent's (real) name, displayed in the UI to other wiki users
     */
    String getName();

    /**
     * If the agent has an associated wiki document, return a reference to it. This happens for users or groups defined
     * in the wiki, or for SSO users mirrored/cloned in the wiki. For external agents without a profile clone in the
     * wiki, {@code null} is returned.
     * 
     * @return a reference to the agent's profile document, if one exists, or {@code null} otherwise
     */
    DocumentReference getReference();

    /**
     * If the agent has an associated URI where their profile can be seen, return it. For users or groups defined (or
     * mirrored) in the wiki, a link to their profile document is returned. For external SSO users with a publicly
     * accessible profile, a link to their external profile is returned. For SSO services not accessible on the web,
     * {@code null} is returned.
     * 
     * @return a link to the agent's profile page, if one exists, or {@code null} otherwise
     */
    URI getProfileURI();

    /**
     * Get the value of an attribute defined for the agent. Some example attributes are the user's given and family
     * names, email address, company, birth date. Actual attributes depend on the actual agent type and the possible
     * attributes enabled.
     * 
     * @param attributeName the name of the attribute to retrieve
     * @return the attribute value, if defined, or {@code null} otherwise
     */
    Object getAttribute(String attributeName);
}
