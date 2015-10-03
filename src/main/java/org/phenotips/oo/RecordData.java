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
package org.phenotips.oo;

import org.phenotips.obo2solr.maps.SetMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RecordData extends SetMap<String, String>
{
    public final static String ID_FIELD_NAME = "NO";

    public final static String NAME_FIELD_NAME = "TI";

    public final static Set<String> ENABLED_FIELDS = new HashSet<String>()
    {
        private static final long serialVersionUID = 1L;

        {
            add("TX");
            add("RF");
            add("GENE");
        }
    };

    private String id;

    private String name;

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

    String getName()
    {
        return this.name;
    }

    @Override
    public boolean addTo(String key, String value)
    {
        if (ID_FIELD_NAME.equals(key)) {
            this.id = value;
            return true;
        } else if (NAME_FIELD_NAME.equals(key)) {
            this.name = value;
            return true;
        } else if (ENABLED_FIELDS.contains(key)) {
            return super.addTo(key, value);
        } else {
            return false;
        }
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
}
