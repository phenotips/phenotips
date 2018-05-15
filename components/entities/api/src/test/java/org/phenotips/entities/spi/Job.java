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
package org.phenotips.entities.spi;

import org.phenotips.Constants;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.internal.AbstractPrimaryEntity;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A sample primary entity implementation used for tests.
 *
 * @version $Id$
 */
public class Job extends AbstractPrimaryEntity implements PrimaryEntity
{
    public static final EntityReference CLASS_REFERENCE = new EntityReference("Job", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    public Job(XWikiDocument doc)
    {
        super(doc);
    }

    @Override
    public EntityReference getType()
    {
        return CLASS_REFERENCE;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        // Nothing to do here
    }
}
