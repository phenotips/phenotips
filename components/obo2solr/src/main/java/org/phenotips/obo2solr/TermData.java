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
package org.phenotips.obo2solr;

import org.phenotips.obo2solr.maps.SetMap;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TermData extends SetMap<String, String>
{
    private static final long serialVersionUID = 1L;

    public static final String ID_FIELD_NAME = "id";

    public static final String PARENT_FIELD_NAME = "is_a";

    public static final String TERM_CATEGORY_FIELD_NAME = "term_category";

    public static final String PARENT_ID_REGEX = "^(HP\\:[0-9]{7})\\s*!\\s*.*";

    private String id;

    @Override
    public void clear()
    {
        this.id = null;
        super.clear();
    };

    String getId()
    {
        return this.id;
    }

    @Override
    public boolean addTo(String key, String value)
    {
        if (ID_FIELD_NAME.equals(key)) {
            this.id = value;
        } else if (PARENT_FIELD_NAME.equals(key)) {
            this.addTo(TERM_CATEGORY_FIELD_NAME, value.replaceAll(PARENT_ID_REGEX, "$1"));
        }
        return super.addTo(key, value);
    }

    @Override
    public boolean addTo(String key, Collection<String> values)
    {
        boolean result = true;
        for (String value : values) {
            result &= this.addTo(key, value);
        }
        return result;
    }

    public void expandTermCategories(Map<String, TermData> data)
    {
        Set<String> result = new LinkedHashSet<>();
        Queue<String> front = new LinkedList<>();

        if (this.get(TERM_CATEGORY_FIELD_NAME) == null) {
            this.put(TERM_CATEGORY_FIELD_NAME, super.getEmptyCollection());
        }

        result.add(this.id);
        front.addAll(this.get(TERM_CATEGORY_FIELD_NAME));
        String nextTermId;
        while ((nextTermId = front.poll()) != null) {
            result.add(nextTermId);
            if (data.get(nextTermId).get(PARENT_FIELD_NAME) == null) {
                continue;
            }
            for (String parentTermId : data.get(nextTermId).get(PARENT_FIELD_NAME)) {
                parentTermId = parentTermId.replaceAll(PARENT_ID_REGEX, "$1");
                if (!result.contains(parentTermId) && !front.contains(parentTermId)) {
                    front.add(parentTermId);
                }
            }
        }
        this.put(TERM_CATEGORY_FIELD_NAME, result);
    }
}
