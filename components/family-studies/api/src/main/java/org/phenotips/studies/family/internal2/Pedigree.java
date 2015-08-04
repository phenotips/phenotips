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
package org.phenotips.studies.family.internal2;

import net.sf.json.JSONObject;

/**
 * Pedigree DTO.
 *
 * @version $Id$
 */
public class Pedigree
{
    /**
     * Code for data.
     */
    public static final String DATA = "data";

    /**
     * Code for image.
     */
    public static final String IMAGE = "image";

    private JSONObject data;

    private String image = "";

    /**
     * Create a new empty pedigree.
     */
    public Pedigree()
    {

    }

    // /**
    // * Create a new pedigree with data and image
    // * @param data
    // * @param image
    // */
    // public Pedigree(JSONObject data, String image)
    // {
    // this.data = data;
    // this.image = image;
    // }

    /**
     * Checks if the `data` field is empty.
     *
     * @return true if data is {@link null} or if {@link JSONObject#isEmpty()} returns true
     */
    public boolean isEmpty()
    {
        return this.data == null || this.data.isEmpty();
    }

    /**
     * Getter for `data` which holds all of a pedigree's JSON.
     *
     * @return could be null
     */
    public JSONObject getData()
    {
        return this.data;
    }

    /**
     * Setter for 'data' which holds all of a pedigree's JSON.
     *
     * @param data the JSON data
     */
    public void setData(JSONObject data)
    {
        this.data = data;
    }

    /**
     * Getter for `image` string (SVG).
     *
     * @return can not be null
     */
    public String getImage()
    {
        return this.image;
    }

    /**
     * Setter for 'image' string (SVG).
     *
     * @param image SVG image
     */
    public void setImage(String image)
    {
        this.image = image;
    }
}
