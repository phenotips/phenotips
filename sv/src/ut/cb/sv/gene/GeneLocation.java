package ut.cb.sv.gene;

public class GeneLocation extends Location implements Comparable<Location>
{
    private String id = "";

    public GeneLocation(String id, String chr, int start, int end)
    {
        super(chr, start, end);
        this.setId(id);
    }

    public GeneLocation(String id, String location)
    {
        super(location);
        this.setId(id);
    }

    /**
     * @param id the id to set
     */
    private void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the id
     */
    public String getId()
    {
        return this.id;
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
        if (!super.equals(obj)) {
            return false;
        }
        GeneLocation other = (GeneLocation) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.id + " " + super.toString();
    }

    @Override
    public boolean isValid()
    {
        return (!"".equals(this.id) && super.isValid());
    }

}
