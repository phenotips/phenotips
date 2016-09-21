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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.entities.internal.AbstractPrimaryEntity;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;

public abstract class AbstractCollaboratorPrimaryEntity extends AbstractPrimaryEntity implements Collaborator
{
    /** The key used in the JSON serialization for the {@link #getAccessLevel()}. */
    public static final String JSON_KEY_ACCESS_LEVEL = "accessLevel";

    protected final EntityReference user;

    protected final AccessLevel access;

    public AbstractCollaboratorPrimaryEntity(EntityReference user, AccessLevel access)
    {
        super((DocumentReference)user);

        this.user = user;
        this.access = access;
    }

    @Override
    public EntityReference getUser()
    {
        return this.user;
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return this.access;
    }

    @Override
    public EntityReference getType()
    {
        return Collaborator.CLASS_REFERENCE;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject json = super.toJSON();
        json.put(JSON_KEY_ACCESS_LEVEL, this.getAccessLevel());
        return json;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        // TODO
    }
}
