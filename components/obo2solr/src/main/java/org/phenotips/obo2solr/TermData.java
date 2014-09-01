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
package org.phenotips.obo2solr;

import org.phenotips.obo2solr.maps.SetMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
        Set<String> result = new HashSet<String>();
        Set<String> front = new HashSet<String>();

        if (this.get(TERM_CATEGORY_FIELD_NAME) == null) {
            this.put(TERM_CATEGORY_FIELD_NAME, super.getEmptyCollection());
        }

        front.addAll(this.get(TERM_CATEGORY_FIELD_NAME));
        Set<String> newFront = new HashSet<String>();
        while (!front.isEmpty()) {
            for (String nextTermId : front) {
                if (data.get(nextTermId).get(TERM_CATEGORY_FIELD_NAME) == null) {
                    continue;
                }
                for (String parentTermId : data.get(nextTermId).get(TERM_CATEGORY_FIELD_NAME)) {
                    if (!result.contains(parentTermId)) {
                        newFront.add(parentTermId);
                        result.add(parentTermId);
                    }
                }
            }
            front.clear();
            front.addAll(newFront);
            newFront.clear();
        }
        result.add(this.id);
        this.addTo(TERM_CATEGORY_FIELD_NAME, result);
    }
}
