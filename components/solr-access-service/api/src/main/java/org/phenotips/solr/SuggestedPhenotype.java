/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.solr;

/**
 * @version $Id$
 */
public class SuggestedPhenotype implements Comparable<SuggestedPhenotype>
{
    /** HPO identifier of the phenotype. */
    private final String id;

    /** The name of the phenotype in the HPO ontology. */
    private final String name;

    /** "Score" of this suggestion, the information gain provided by it. */
    private final double score;

    /**
     * Constructor initializing all the required fields.
     *
     * @param id see {@link #id}
     * @param name see {@link #name}
     * @param score see {@link #score}
     */
    public SuggestedPhenotype(String id, String name, double score)
    {
        this.id = id;
        this.name = name;
        this.score = score;
    }

    /**
     * @return HPO identifier of the phenotype, in the {@code HP:1234567} format
     * @see #id
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the name of the phenotype in the HPO ontology
     * @see #name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the information gain provided by this phenotype, a number between 0 and 1
     * @see #score
     */
    public double getScore()
    {
        return this.score;
    }

    @Override
    public int compareTo(SuggestedPhenotype other)
    {
        if (other == null) {
            return 0;
        }
        return (int) -Math.signum(this.score - (other).getScore());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        long temp;
        temp = Double.doubleToLongBits(this.score);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

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
        SuggestedPhenotype other = (SuggestedPhenotype) obj;
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return this.id + '\t' + this.name + '\t' + this.score;
    }
}
