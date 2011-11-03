package ut.cb.util.maps;

public abstract class AbstractMapOperator<K, N extends Number> implements MapOperator<K, N>
{
    @SuppressWarnings("unchecked")
    public NumericValueMap<K, N> apply(NumericValueMap<K, N> a, NumericValueMap<K, N> b)
    {
        if (!a.keySet().equals(b.keySet())) {
            return null;
        }
        DoubleMap<K> result = new DoubleMap<K>();
        for (K key : a.keySet()) {
            result.put(key, this.applyToValues(a.get(key), b.get(key)));
        }
        return (NumericValueMap<K, N>) result;
    }

    protected abstract Double applyToValues(N a, N b);
}
