package ut.cb.sv.cdata;

import java.util.LinkedHashMap;
import java.util.Map;

public enum Gender
{
    M(true, "Male"), F(false, "Female");

    private boolean isMale;

    private String longValue;

    Gender(boolean isMale, String longValue)
    {
        this.isMale = isMale;
        this.longValue = longValue;
    }

    private static Map<String, Gender> valuesMap = new LinkedHashMap<String, Gender>();
    static {
        for (Gender instance : Gender.values()) {
            valuesMap.put(instance.name().toUpperCase(), instance);
            valuesMap.put((instance.isMale + "").toUpperCase(), instance);
            valuesMap.put(instance.longValue.toUpperCase(), instance);
        }
    }

    public static Gender getValue(String label)
    {
        String _label = label.trim().toUpperCase();
        Gender value = valuesMap.get(_label);
        if (value == null) {
            _label = _label.replaceAll("[^MF]", "");
            if (_label.length() > 0) {
                value = valuesMap.get(_label.substring(0, 1));
            }
        }
        // System.out.println("Gender <" + label + "> ---> " + value);
        return value;
    }

    public boolean getBooleanValue()
    {
        return this.isMale;
    }

    public String getLongValue()
    {
        return this.longValue;
    }
}
