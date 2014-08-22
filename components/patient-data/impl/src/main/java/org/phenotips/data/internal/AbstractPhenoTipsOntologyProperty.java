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
package org.phenotips.data.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.OntologyProperty;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONObject;

/**
 * Implementation of patient data based on the XWiki data model, where disease data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public abstract class AbstractPhenoTipsOntologyProperty implements OntologyProperty, Comparable<OntologyProperty>
{
    /** Used for reading and writing properties to JSON. */
    protected static final String ID_JSON_KEY_NAME = "id";

    protected static final String NAME_JSON_KEY_NAME = "label";

    /** Pattern used for identifying ontology terms from free text terms. */
    private static final Pattern ONTOLOGY_TERM_PATTERN = Pattern.compile("\\w++:\\w++");

    /** @see #getId() */
    protected final String id;

    /** @see #getName() */
    protected String name;

    /**
     * Simple constructor providing the {@link #id term identifier}.
     *
     * @param id the ontology term identifier
     * @throws IllegalArgumentException if the identifier is {@code null} or otherwise malformed for the ontology
     */
    protected AbstractPhenoTipsOntologyProperty(String id)
    {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        if (ONTOLOGY_TERM_PATTERN.matcher(id).matches()) {
            this.id = id;
            this.name = null;
        } else {
            this.id = "";
            this.name = id;
        }
    }

    /**
     * Constructor for initializing from a JSON Object.
     *
     * @param json JSON object describing this property
     */
    protected AbstractPhenoTipsOntologyProperty(JSONObject json)
    {
        this.id = json.has(ID_JSON_KEY_NAME) ? json.getString(ID_JSON_KEY_NAME) : "";
        this.name = json.has(NAME_JSON_KEY_NAME) ? json.getString(NAME_JSON_KEY_NAME) : null;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        if (this.name != null) {
            return this.name;
        }
        try {
            OntologyManager om =
                ComponentManagerRegistry.getContextComponentManager().getInstance(OntologyManager.class);
            OntologyTerm term = om.resolveTerm(this.id);
            if (term != null && StringUtils.isNotEmpty(term.getName())) {
                this.name = term.getName();
                return this.name;
            }
        } catch (ComponentLookupException ex) {
            // Shouldn't happen
        }
        return this.id;
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
        if (StringUtils.isNotEmpty(this.getId())) {
            result.element(ID_JSON_KEY_NAME, getId());
        }
        if (StringUtils.isNotEmpty(this.getName())) {
            result.element(NAME_JSON_KEY_NAME, getName());
        }
        return result;
    }

    @Override
    public int compareTo(OntologyProperty o)
    {
        if (o == null) {
            // Nulls at the end
            return -1;
        }
        return getName().compareTo(o.getName());
    }
}
