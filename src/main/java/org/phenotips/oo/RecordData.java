package org.phenotips.oo;

import org.phenotips.util.maps.SetMap;

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
