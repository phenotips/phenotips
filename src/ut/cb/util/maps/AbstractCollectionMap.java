package ut.cb.util.maps;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractCollectionMap<K, V> extends LinkedHashMap<K, Collection<V>>
{
    public AbstractCollectionMap()
    {
        super();
    }

    public AbstractCollectionMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    public boolean addTo(K key, V value)
    {
        Collection<V> crtValue = this.get(key);
        if (crtValue == null) {
            crtValue = getEmptyCollection();
            this.put(key, crtValue);
        }
        return crtValue.add(value);
    }

    public boolean addTo(K key, Collection<V> values)
    {
        Collection<V> crtValue = this.get(key);
        if (crtValue == null) {
            crtValue = getEmptyCollection();
            this.put(key, crtValue);
        }
        return crtValue.addAll(values);
    }

    protected abstract Collection<V> getEmptyCollection();

    public void reset(K key)
    {
        if (this.get(key) == null) {
            this.put(key, getEmptyCollection());
        }
        this.get(key).clear();
    }

    public Collection<V> safeGet(K key)
    {
        Collection<V> value = this.get(key);
        return value == null ? getEmptyCollection() : value;
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
                if (safeGet(a).size() == safeGet(b).size()) {
                    return 0;
                }
                try {
                    return ((safeGet(a).size() > safeGet(b).size()) && descending) ? -1 : 1;
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

    public void writeTo(PrintStream out)
    {
        for (K key : this.keySet()) {
            out.println(key + ":");
            for (V value : this.get(key)) {
                out.println("\t" + value);
            }
        }
    }
}
