/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data;

import java.util.Date;

/**
 * Patient specificity, a score estimating how "good" a patient record is, along with metadata about the score
 * computation.
 *
 * @version $Id$
 * @since 1.0M12
 */
public class PatientSpecificity
{
    /** @see #getScore() */
    private double score;

    /** @see #getComputationDate() */
    private Date computationDate;

    /** @see #getComputingMethod() */
    private String computingMethod;

    /**
     * Default constructor passing all the required information.
     *
     * @param score the profile score, must be a number between 0 and 1 (inclusive)
     * @param computationDate the date when the score was computed
     * @param computingMethod the method used for computing the score
     */
    public PatientSpecificity(final double score, final Date computationDate, final String computingMethod)
    {
        this.score = score;
        this.computationDate = computationDate;
        this.computingMethod = computingMethod;
    }

    /**
     * The score assigned to this patient record, where 0 means the record isn't informative at all, and 1 means that
     * this is a perfect patient description.
     *
     * @return a number between {@code 0.0} and {@code 1.0} (inclusive), or {@code -1} if a score couldn't be computed
     */
    public double getScore()
    {
        return this.score;
    }

    /**
     * The date when this score was computed.
     *
     * @return a date object
     */
    public Date getComputationDate()
    {
        return this.computationDate;
    }

    /**
     * The method used for computing this score.
     *
     * @return a method identifier, e.g. {@code monarchinitiative.org} or {@code local-omim}
     */
    public String getComputingMethod()
    {
        return this.computingMethod;
    }
}
