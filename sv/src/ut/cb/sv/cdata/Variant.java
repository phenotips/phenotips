package ut.cb.sv.cdata;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import ut.cb.sv.gene.Chromosome;
import ut.cb.sv.gene.Location;

public class Variant extends Location
{

    private Set<String> goFunctions;

    private final VariantType type;

    public Variant(String chr, int start, int end, String type)
    {
        super(chr, start, end);
        this.type = VariantType.getValue(type);
        this.goFunctions = new TreeSet<String>();
    }

    public Variant(Chromosome chr, int start, int end, VariantType type)
    {
        super(chr, start, end);
        this.type = type;
        this.goFunctions = new TreeSet<String>();
    }

    public VariantType gettype()
    {
        return this.type;
    }

    public boolean addGoFunction(String goFunction)
    {
        return this.goFunctions.add(goFunction);
    }

    public boolean addGoFunctions(Collection<String> goFunctions)
    {
        return this.goFunctions.addAll(goFunctions);
    }

    public boolean hasGoFunction(String goFunction)
    {
        return this.goFunctions.contains(goFunction);
    }

    public Set<String> getGoFunctions()
    {
        Set<String> clone = new TreeSet<String>();
        clone.addAll(this.goFunctions);
        return clone;

    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Variant other = (Variant) obj;
        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!this.type.equals(other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append(this.getChr()).append("(").append(this.getStart()).append("-").append(this.getEnd()).append(")")
            .append(this.type).append("\n");
        /*
         * for (String f : this.goFunctions) { str.append("        ").append(f); }
         */
        return str.toString();
    }
}
