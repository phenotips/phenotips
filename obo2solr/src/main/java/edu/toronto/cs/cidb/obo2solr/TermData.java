package edu.toronto.cs.cidb.obo2solr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.toronto.cs.cidb.obo2solr.maps.SetMap;

public class TermData extends SetMap<String, String>
{
    public final static String ID_FIELD_NAME = "id";

    public final static String PARENT_FIELD_NAME = "is_a";

    public final static String TERM_CATEGORY_FIELD_NAME = "term_category";

    public final static String PARENT_ID_REGEX = "^(HP\\:[0-9]{7})\\s*!\\s*.*";

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
