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
package org.phenotips.data.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.VocabularyProperty;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * Implementation of patient data based on the XWiki data model, where data is represented by properties in objects of
 * type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public abstract class AbstractPhenoTipsVocabularyProperty implements VocabularyProperty, Comparable<VocabularyProperty>
{
    /** Used for reading and writing properties to JSON. */
    protected static final String ID_JSON_KEY_NAME = "id";

    protected static final String NAME_JSON_KEY_NAME = "label";

    /** Pattern used for identifying vocabulary terms from free text terms. */
    private static final Pattern VOCABULARY_TERM_PATTERN = Pattern.compile("\\w++:\\w++");

    /** @see #getId() */
    protected final String id;

    /** @see #getName() */
    protected String name;

    /**
     * Simple constructor providing the {@link #id term identifier}.
     *
     * @param id the vocabulary term identifier
     * @throws IllegalArgumentException if the identifier is {@code null} or otherwise malformed for the vocabulary
     */
    protected AbstractPhenoTipsVocabularyProperty(String id)
    {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        if (VOCABULARY_TERM_PATTERN.matcher(id).matches()) {
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
    protected AbstractPhenoTipsVocabularyProperty(JSONObject json)
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
            VocabularyManager vm =
                ComponentManagerRegistry.getContextComponentManager().getInstance(VocabularyManager.class);
            VocabularyTerm term = vm.resolveTerm(this.id);
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
            result.put(ID_JSON_KEY_NAME, getId());
        }
        if (StringUtils.isNotEmpty(this.getName())) {
            result.put(NAME_JSON_KEY_NAME, getName());
        }
        return result;
    }

    @Override
    public int compareTo(VocabularyProperty o)
    {
        if (o == null) {
            // Nulls at the end
            return -1;
        }
        return getName().compareTo(o.getName());
    }
}
