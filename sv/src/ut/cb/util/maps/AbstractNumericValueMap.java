package ut.cb.util.maps;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public abstract class AbstractNumericValueMap<K, N extends Number> extends TreeMap<K, N> implements
    NumericValueMap<K, N>
{
    public AbstractNumericValueMap()
    {
        super();
    }

    public AbstractNumericValueMap(int initialCapacity)
    {
        super();// initialCapacity);
    }

    public N addTo(K key, N value)
    {
        N crtValue = this.get(key);
        if (crtValue == null) {
            return this.put(key, value);
        } else {
            return this.put(key, value);
        }
    }

    protected abstract N getZero();

    public N reset(K key)
    {
        return this.put(key, getZero());
    }

    public N safeGet(K key)
    {
        N value = this.get(key);
        return value == null ? getZero() : value;
    }

    public List<K> sort()
    {
        return this.sort(false);
    }

    @SuppressWarnings("unchecked")
    public List<K> sort(final boolean descending)
    {
        K[] sortedKeys = (K[]) this.keySet().toArray();
        Arrays.sort(sortedKeys, new Comparator<K>()
                    {
            public int compare(K a, K b)
                        {
                if (safeGet(a).equals(safeGet(b))) {
                    return 0;
                }
                try {
                    return ((((Comparable<N>) safeGet(a)).compareTo(safeGet(b)) > 0) && descending) ? -1 : 1;
                } catch (ClassCastException ex) {
                    return 0;
                }
            }
        });
        LinkedList<K> result = new LinkedList<K>();
        for (K key : sortedKeys) {
            result.add(key);
        }
        return result;
    }

    public K getMax()
    {
        if (this.size() == 0) {
            return null;
        }
        return this.sort(true).get(0);
    }

    public K getMin()
    {
        if (this.size() == 0) {
            return null;
        }
        return this.sort().get(0);
    }

    public N getMaxValue()
    {
        return this.safeGet(getMax());
    }

    public N getMinValue()
    {
        return this.safeGet(getMin());
    }

    public void writeTo(PrintStream out)
    {
        for (K key : this.keySet()) {
            out.println(key + " : " + this.get(key));
        }
    }
}
