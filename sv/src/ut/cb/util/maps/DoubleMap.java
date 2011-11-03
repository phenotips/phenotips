package ut.cb.util.maps;

public class DoubleMap<K> extends AbstractNumericValueMap<K, Double>
{
    public DoubleMap()
    {
        super();
    }

    public DoubleMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    @Override
    protected final Double getZero()
    {
        return 0.0;
    }
}
