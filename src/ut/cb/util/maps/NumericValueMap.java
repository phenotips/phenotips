package ut.cb.util.maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

interface NumericValueMap<K, N extends Number>
{

    public N addTo(K key, N value);

    public N reset(K key);

    public N get(K key);

    public N safeGet(K key);

    public List<K> sort();

    public List<K> sort(final boolean descending);

    public K getMax();

    public K getMin();

    public N getMaxValue();

    public N getMinValue();

    public void clear();

    public boolean containsKey(K key);

    public boolean isEmpty();

    public Set<K> keySet();

    public N put(K key, N value);

    public void putAll(Map< ? extends K, ? extends N> m);

    public N remove(K key);

    public int size();
}
