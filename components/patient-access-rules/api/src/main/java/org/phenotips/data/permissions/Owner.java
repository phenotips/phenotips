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
package org.phenotips.data.permissions;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * The owner of a patient record, either a user or a group that has full control on the patient record. Only one owner
 * can be set for a patient record at a time, but ownership can be transfered.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Unstable
public interface Owner
{
    /** The XClass used to store the owner in the patient record. */
    EntityReference CLASS_REFERENCE = new EntityReference("OwnerClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The name of the property used to store the owner in the XClass. */
    String PROPERTY_NAME = "owner";

    String getType();

    boolean isUser();

    boolean isGroup();

    /**
     * The user or group that has been set as collaborator.
     *
     * @return a reference to the user's or group's profile
     */
    EntityReference getUser();

    /**
     * The username or group name.
     *
     * @return the name of the document holding the user or group (just the name without the space or instance name)
     */
    String getUsername();
}
