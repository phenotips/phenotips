package ut.cb.sv.gene;

import java.util.LinkedHashMap;
import java.util.Map;

public enum Chromosome
{
    _1("1"),
    _2("2"),
    _3("3"),
    _4("4"),
    _5("5"),
    _6("6"),
    _7("7"),
    _8("8"),
    _9("9"),
    _10("10"),
    _11("11"),
    _12("12"),
    _13("13"),
    _14("14"),
    _15("15"),
    _16("16"),
    _17("17"),
    _18("18"),
    _19("19"),
    _20("20"),
    _21("21"),
    _22("22"),
    _X("X"),
    _Y("Y"),
    UNKNOWN("U");
    private String value;

    private static Map<String, Chromosome> valuesMap = new LinkedHashMap<String, Chromosome>();

    Chromosome(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }

    static {
        for (Chromosome instance : Chromosome.values()) {
            valuesMap.put(instance.value, instance);
            valuesMap.put(instance.name(), instance);
        }
    }

    public static Chromosome getValue(String label)
    {
        String _label = label.trim().toUpperCase();
        Chromosome value = valuesMap.get(_label);
        if (value == null && _label.startsWith("CHR")) {
            value = valuesMap.get(_label.substring(3));
        }
        return value;
    }
}
