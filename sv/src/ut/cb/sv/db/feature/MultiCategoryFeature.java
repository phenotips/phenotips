package ut.cb.sv.db.feature;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import ut.cb.sv.db.DatabaseEntry;

public class MultiCategoryFeature extends CategoryFeature
{

    public MultiCategoryFeature(String name)
    {
        super(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean registerValue(Object value)
    {
        if (value == null) {
            return false;
        }
        if (value instanceof Collection< ? >) {
            boolean result = true;
            for (Object v : (Collection<Object>) value) {
                result &= this.registerValue(v);
            }
            return result;
        }
        int size = 0;
        Set<String> values = FeatureValueMapping.getValuesFromList(value.toString());
        for (String valObj : values) {
            if (!super.registerValue(valObj)) {
                return false;
            }
            size += valObj.toString().length() + 2;
        }
        this.maxValueSize = Math.max(this.maxValueSize, size);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object prepareValueForEntry(DatabaseEntry entry, Object value)
    {
        Set<Object> values = new LinkedHashSet<Object>();
        Set<Object> prev = (Set<Object>) entry.get(this.name);
        if (prev != null) {
            values.addAll(prev);
        }
        if (value instanceof Collection< ? >) {
            values.addAll((Collection<String>) value);
        } else {
            values.add(value);
        }
        return values;
    }

    @Override
    public Object preProcessValue(String valueString)
    {
        String pieces[] = valueString.trim().toLowerCase().split("\\s*(,|\r?\n|;)\\s*");
        Set<String> result = new LinkedHashSet<String>();
        for (String piece : pieces) {
            result.add(piece);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Object> postProcessValue(String valueString)
    {
        String pieces[] = valueString.split("\\s*[,]\\s*");
        Set<Object> values = new LinkedHashSet<Object>();
        for (String value : pieces) {
            if (isValueValid(valueString)) {
                values.add(value);
            }
        }
        return values;
    }
}
