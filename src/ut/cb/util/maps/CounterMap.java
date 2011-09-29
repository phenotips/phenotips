package ut.cb.util.maps;

public class CounterMap<K> extends IntegerMap<K>
{
    public CounterMap()
    {
        super();
    }

    public CounterMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    public Integer addTo(K key)
    {
        Integer value = this.get(key);
        if (value == null) {
            return this.put(key, 1);
        } else {
            return this.put(key, value + 1);
        }
    }
}
