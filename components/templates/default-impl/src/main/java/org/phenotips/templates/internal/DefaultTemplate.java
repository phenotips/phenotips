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
package org.phenotips.templates.internal;

import org.phenotips.entities.internal.AbstractPrimaryEntity;
import org.phenotips.templates.data.Template;

import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class DefaultTemplate extends AbstractPrimaryEntity implements Template
{
    /**
     * Create a template.
     *
     * @param document of template
     */
    public DefaultTemplate(XWikiDocument document)
    {
        super(document);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DefaultTemplate)) {
            return false;
        }
        DefaultTemplate other = (DefaultTemplate) obj;
        return this.getId().equals(other.getId());
    }

    @Override
    public int hashCode()
    {
        return this.getId().hashCode();
    }

    @Override
    public EntityReference getType()
    {
        return Template.CLASS_REFERENCE;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public int compareTo(Template other)
    {
        return this.getId().compareTo(other.getId());
    }
}
