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

import org.phenotips.data.Disorder;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.objects.ListProperty;

/**
 * Implementation of patient data based on the XWiki data model, where disorder data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsDisorder extends AbstractPhenoTipsVocabularyProperty implements Disorder
{
    protected static final Pattern OMIM_TERM_PATTERN = Pattern.compile("\\d++");

    protected static final String MIM_PREFIX = "MIM:";

    /**
     * Constructor that copies the data from an XProperty value.
     *
     * @param property the disorder XProperty
     * @param value the specific value from the property represented by this object
     * @throws IllegalArgumentException if one of the arguments is {@code null} or otherwise malformed for the ontology
     */
    public PhenoTipsDisorder(ListProperty property, String value)
    {
        super((value != null && StringUtils.equals(property.getName(), "omim_id") && OMIM_TERM_PATTERN.matcher(
            value).matches()) ? MIM_PREFIX + value : value);
    }

    /**
     * Constructor for initializing from a JSON Object.
     *
     * @param json JSON object describing this property
     */
    public PhenoTipsDisorder(JSONObject json)
    {
        super(json);
    }

    @Override
    public String getValue()
    {
        if (StringUtils.isEmpty(getId())) {
            return getName();
        }
        String id = StringUtils.removeStart(getId(), MIM_PREFIX);
        return id;
    }
}
