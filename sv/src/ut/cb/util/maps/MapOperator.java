package ut.cb.util.maps;

interface MapOperator<K, N extends Number>
{
    public NumericValueMap<K, N> apply(NumericValueMap<K, N> a, NumericValueMap<K, N> b);
}
