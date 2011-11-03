package ut.cb.sv.cdata;

import java.util.LinkedHashMap;
import java.util.Map;

public enum VariantType
{
    DUP("Gain", true), DEL("Loss", false);

    private String alternativeLabel;

    private boolean isDuplication;

    VariantType(String alternativeLabel, boolean isDuplication)
    {
        this.alternativeLabel = alternativeLabel;
        this.isDuplication = isDuplication;
    }

    private static Map<String, VariantType> valuesMap = new LinkedHashMap<String, VariantType>();
    static {
        for (VariantType instance : VariantType.values()) {
            valuesMap.put(instance.name().toUpperCase(), instance);
            valuesMap.put(instance.alternativeLabel.toUpperCase(), instance);
            String isDup = instance.isDuplication + "";
            valuesMap.put(isDup.toUpperCase(), instance);
        }
    }

    public static VariantType getValue(String label)
    {
        return valuesMap.get(label.trim().toUpperCase());
    }

    public boolean getBooleanValue()
    {
        return this.isDuplication;
    }

    public String getLongValue()
    {
        return this.alternativeLabel;
    }
}
