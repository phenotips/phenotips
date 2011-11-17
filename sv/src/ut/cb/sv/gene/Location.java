package ut.cb.sv.gene;

public class Location implements Comparable<Location>
{
    public static final String INPUT_SEPARATOR = "\t";

    private Chromosome chr = Chromosome.UNKNOWN;

    private int start = -1;

    private int end = -1;

    private String meta = "";

    public Location(String chr, int start, int end)
    {
        super();
        this.setChr(chr);
        this.setStart(start);
        this.setEnd(end);
    }

    public Location(Chromosome chr, int start, int end)
    {
        super();
        this.setChr(chr);
        this.setStart(start);
        this.setEnd(end);
    }

    public Location(String line)
    {
        super();
        String pieces[] = line.split(INPUT_SEPARATOR);
        try {
            this.setChr(pieces[0].trim());
            this.setStart(pieces[1].trim());
            this.setEnd(pieces[2].trim());
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println(ex.getClass().getName() + ": " + ex.getMessage()
                + ". Not enough pieces of information in " + line);
        } catch (NumberFormatException ex) {
            System.out.println(ex.getClass().getName() + ": " + ex.getMessage()
                + ". Wrong limit format in " + line);
        }
    }

    /**
     * @param chr the chr to set
     */
    private void setChr(String chr)
    {
        int chromosomeMetaSeparatorPosition = chr.indexOf('_');
        if (chromosomeMetaSeparatorPosition >= 0) {
            this.chr = Chromosome.getValue(chr.substring(0, chromosomeMetaSeparatorPosition));
            this.meta = chr.substring(chromosomeMetaSeparatorPosition + 1);
        } else {
            this.chr = Chromosome.getValue(chr);
        }
    }

    private void setChr(Chromosome chr)
    {
        this.chr = chr;
    }

    /**
     * @param start the start to set
     */
    private void setStart(int start)
    {
        this.start = start;
    }

    /**
     * @param start the start to set
     */
    private void setStart(String start)
    {
        this.start = Integer.parseInt(start);
    }

    /**
     * @param end the end to set
     */
    private void setEnd(int end)
    {
        this.end = end;
    }

    /**
     * @param end the end to set
     */
    private void setEnd(String end)
    {
        this.end = Integer.parseInt(end);
    }

    /**
     * @return the chr
     */
    public Chromosome getChr()
    {
        return this.chr;
    }

    /**
     * @return the start
     */
    public int getStart()
    {
        return this.start;
    }

    /**
     * @return the end
     */
    public int getEnd()
    {
        return this.end;
    }

    /**
     * @return the end
     */
    public int getSize()
    {
        return this.end - this.start;
    }

    /**
     * @return the meta
     */
    public String getMeta()
    {
        return this.meta;
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
        result = prime * result + ((this.chr == null) ? 0 : this.chr.hashCode());
        result = prime * result + this.end;
        result = prime * result + this.start;
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
        Location other = (Location) obj;
        if (this.chr == null) {
            if (other.chr != null) {
                return false;
            }
        } else if (!this.chr.equals(other.chr)) {
            return false;
        }
        if (this.end != other.end) {
            return false;
        }
        if (this.start != other.start) {
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
        return "[" + this.chr + ("".equals(this.meta) ? " " : "(" + this.meta + ") ") + this.start + " - "
            + this.end + "]";
    }

    public boolean isValid()
    {
        return (this.chr != null && this.start >= 0 && this.end > this.start);
    }

    public int compareTo(Location o)
    {
        int result = 0;
        if (o == null) {
            return 1;
        }
        if (this.chr != null) {
            if (o.chr == null) {
                return 1;
            }
            if ((result = this.chr.compareTo(o.getChr())) != 0) {
                return result;
            }
        } else {
            if (o.chr != null) {
                return -1;
            }
        }

        if (this.start < o.getStart()) {
            return -1;
        }
        if (this.start > o.getStart()) {
            return 1;
        }
        if (this.end < o.getEnd()) {
            return -1;
        }
        if (this.end > o.getEnd()) {
            return 1;
        }
        return 0;
    }
}
