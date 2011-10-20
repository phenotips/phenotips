package ut.cb.sv.db.feature;

import java.util.LinkedHashSet;
import java.util.Set;

public class MultiCategoryFeature extends CategoryFeature
{

    public MultiCategoryFeature(String name)
    {
        super(name);
    }

    @Override
    public boolean registerValue(Object value)
    {
        if (value == null) {
            return false;
        }
        int size = 0;
        Set<Object> values = postProcessValue(value.toString());
        for (Object valObj : values) {
            if (!super.registerValue(valObj)) {
                return false;
            }
            size += valObj.toString().length() + 2;
        }
        this.maxValueSize = Math.max(this.maxValueSize, size);
        return true;
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
