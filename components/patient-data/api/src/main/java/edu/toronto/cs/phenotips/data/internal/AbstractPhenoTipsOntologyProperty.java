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
import edu.toronto.cs.phenotips.data.OntologyProperty;

/**
 * Implementation of patient data based on the XWiki data model, where disease data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 * 
 * @version $Id$
 */
public abstract class AbstractPhenoTipsOntologyProperty implements OntologyProperty
{
    /** @see #getId() */
    protected final String id;

    /**
     * Simple constructor providing the {@link #id term identifier}.
     * 
     * @param id the ontology term identifier
     */
    protected AbstractPhenoTipsOntologyProperty(String id)
    {
        this.id = id;
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
        result.element("name", getName());
        return result;
    }
}
