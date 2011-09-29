package ut.cb.sv.gene;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import ut.cb.util.maps.SetMap;

public class RelativeLocationMap<V extends Location> extends
    LinkedHashMap<Integer, SetMap<RelativeLocationMap.RelativePosition, V>>
{
    public enum RelativePosition
    {
        BEFORE(false),
        START(true),
        IN(true),
        END(true, START),
        AFTER(false, BEFORE),
        OUT(false, IN);

        private RelativePosition opposite = null;

        private boolean inside;

        RelativePosition(boolean inside)
        {
        }

        RelativePosition(boolean inside, RelativePosition op)
        {
            this.opposite = op;
            if (op != null) {
                op.opposite = this;
            }
        }

        public static RelativePosition getDefault()
        {
            return IN;
        }

        public RelativePosition getOpposite()
        {
            return this.opposite;
        }

        public boolean isInside()
        {
            return this.inside;
        }

        public boolean isOutside()
        {
            return !this.inside;
        }
    }

    public RelativeLocationMap()
    {
        super();
    }

    public RelativeLocationMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    public boolean addTo(Integer key, V value)
    {
        return addTo(key, RelativePosition.getDefault(), value);
    }

    public boolean addTo(Integer key, RelativePosition position, V value)
    {
        SetMap<RelativePosition, V> crtValue = this.get(key);
        if (crtValue == null) {
            crtValue = new SetMap<RelativePosition, V>();
            this.put(key, crtValue);
        }
        return crtValue.addTo(position, value);
    }

    public boolean addTo(Integer key, Collection<V> values)
    {
        return addTo(key, RelativePosition.getDefault(), values);
    }

    public boolean addTo(Integer key, RelativePosition position, Collection<V> values)
    {
        SetMap<RelativePosition, V> crtValue = this.get(key);
        if (crtValue == null) {
            crtValue = new SetMap<RelativePosition, V>();
            this.put(key, crtValue);
        }
        return crtValue.addTo(position, values);
    }

    public void reset(Integer key)
    {
        if (this.get(key) == null) {
            this.put(key, new SetMap<RelativePosition, V>());
        }
        this.get(key).clear();
    }

    public SetMap<RelativePosition, V> safeGet(Integer key)
    {
        SetMap<RelativePosition, V> value = this.get(key);
        return value == null ? new SetMap<RelativePosition, V>() : value;
    }

    public Collection<V> get(Integer key, RelativePosition pos)
    {
        if (this.get(key) != null) {
            return this.get(key).get(pos);
        }
        return null;
    }

    public Collection<V> safeGet(Integer key, RelativePosition pos)
    {
        if (this.get(key) != null && this.get(key).get(pos) != null) {
            return this.get(key).get(pos);
        }
        return new LinkedHashSet<V>();
    }

    public Set<V> getAll(Integer key)
    {
        SetMap<RelativePosition, V> value = this.get(key);
        Set<V> result = new LinkedHashSet<V>();
        if (value != null) {
            for (Collection<V> set : value.values()) {
                result.addAll(set);
            }
        }
        return result;
    }

    public void writeTo(PrintStream out)
    {
        for (Integer key : this.keySet()) {
            out.println(key + ":");
            SetMap<RelativePosition, V> mapping = this.get(key);
            for (RelativePosition pos : mapping.keySet()) {
                out.println("\t" + key + ":");
                for (V value : mapping.get(pos)) {
                    out.println("\t" + value);
                }
            }
        }
    }
}
