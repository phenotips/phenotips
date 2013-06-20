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
package edu.toronto.cs.phenotips.data.internal;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.DBStringListProperty;

import edu.toronto.cs.phenotips.data.Disease;

/**
 * Implementation of patient data based on the XWiki data model, where disease data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 * 
 * @version $Id$
 */
public class PhenoTipsDisease implements Disease
{
    /** @see #getId() */
    private final String id;

    /**
     * Constructor that copies the data from an XProperty value.
     * 
     * @param doc the XDocument representing the described patient in XWiki
     * @param property the disease XProperty
     * @param value the specific value from the property represented by this object
     */
    PhenoTipsDisease(XWikiDocument doc, DBStringListProperty property, String value)
    {
        if (StringUtils.equals(property.getName(), "omim_id")) {
            this.id = "MIM:" + value;
        } else {
            this.id = value;
        }
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        // FIXME implementation missing
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        return toJSON().toString(2);
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        result.element("id", getId());
        return result;
    }
}
