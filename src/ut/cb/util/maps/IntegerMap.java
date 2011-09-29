package ut.cb.util.maps;

public class IntegerMap<K> extends AbstractNumericValueMap<K, Integer>
{
    public IntegerMap()
    {
        super();
    }

    public IntegerMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    @Override
    protected final Integer getZero()
    {
        return 0;
    }

    @Override
    public Integer addTo(K key, Integer value)
    {
        Integer crtValue = this.get(key);
        if (value == null) {
            return this.put(key, value);
        } else {
            return this.put(key, crtValue + value);
        }
    }
}
