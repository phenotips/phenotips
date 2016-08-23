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
package org.phenotips.measurements.data;

import org.xwiki.stability.Unstable;

import java.util.Date;

/**
 * A class that represents a measurement entry with easy access.
 *
 * @version $Id$
 * @since 1.3M4
 */
@Unstable
public final class MeasurementEntry
{
    private Date date;

    private String age;

    private String type;

    private String side;

    private Double value;

    private String units;

    /**
     * The default constructor that takes all of the data stored in the patient record.
     *
     * @param date see the doc for the respective method
     * @param age see the doc for the respective method
     * @param type see the doc for the respective method
     * @param side see the doc for the respective method
     * @param value see the doc for the respective method
     * @param units see the doc for the respective method
     */
    public MeasurementEntry(Date date, String age, String type, String side, Double value, String units)
    {
        this.date = date;
        this.age = age;
        this.type = type;
        this.side = side;
        this.value = value;
        this.units = units;
    }

    /** @return the date of the measurement */
    public Date getDate()
    {
        return this.date;
    }

    /** @return string representing age */
    public String getAge()
    {
        return this.age;
    }

    /** @return the name of the measurement handler */
    public String getType()
    {
        return this.type;
    }

    /** @return a letter representing the side, if applicable */
    public String getSide()
    {
        return this.side;
    }

    /** @return the measurement itself */
    public Double getValue()
    {
        return this.value;
    }

    /** @return the units that the value is measured in */
    public String getUnits()
    {
        return this.units;
    }
}
