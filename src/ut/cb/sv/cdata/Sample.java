package ut.cb.sv.cdata;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ut.cb.sv.gene.Chromosome;
import ut.cb.sv.gene.Location;

public class Sample
{
    final private String id;

    final private Gender gender;

    private Set<String> phenotype;

    private List<Variant> variants;

    public Sample(String id, Gender gender)
    {
        this.id = id;
        this.gender = gender;
        this.phenotype = new TreeSet<String>();
        this.variants = new LinkedList<Variant>();
    }

    public String getId()
    {
        return this.id;
    }

    public Gender getGender()
    {
        return this.gender;
    }

    public boolean addPhenotype(String phenotype)
    {
        return this.phenotype.add(phenotype);
    }

    public boolean addPhenotype(Collection<String> phenotype)
    {
        return this.phenotype.addAll(phenotype);
    }

    public boolean hasPhenotype(String phenotype)
    {
        return this.phenotype.contains(phenotype);
    }

    public Set<String> getPhenotype()
    {
        Set<String> clone = new TreeSet<String>();
        clone.addAll(this.phenotype);
        return clone;

    }

    public boolean addVariant(Chromosome chr, int start, int end, VariantType type)
    {
        return this.variants.add(new Variant(chr, start, end, type));
    }

    public boolean addVariant(String chr, int start, int end, String type)
    {
        return this.variants.add(new Variant(chr, start, end, type));
    }

    public boolean addVariant(Location location, VariantType type)
    {
        return this.variants.add(new Variant(location.getChr(), location.getStart(), location.getEnd(), type));
    }

    public boolean addVariant(Variant variant)
    {
        return this.variants.add(variant);
    }

    public boolean hasVariant(Chromosome chr, int start, int end, VariantType type)
    {
        return this.variants.contains(new Variant(chr, start, end, type));
    }

    public boolean hasVariant(String chr, int start, int end, String type)
    {
        return this.variants.contains(new Variant(chr, start, end, type));
    }

    public boolean hasVariant(Location location, VariantType type)
    {
        return this.variants.contains(new Variant(location.getChr(), location.getStart(), location.getEnd(), type));
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.gender == null) ? 0 : this.gender.hashCode());
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
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
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Sample other = (Sample) obj;
        if (this.gender == null) {
            if (other.gender != null) {
                return false;
            }
        } else if (!this.gender.equals(other.gender)) {
            return false;
        }
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("ID:        ").append(this.id).append("\n");
        str.append("GENDER:    ").append(this.gender).append("\n");
        str.append("PHENOTYPE: ").append("\n");
        for (String p : this.phenotype) {
            str.append("    ").append(p).append("\n");
        }
        str.append("VARIANTS:  ");
        for (Variant v : this.variants) {
            str.append("    ").append(v);
        }
        return str.toString();
    }
}
