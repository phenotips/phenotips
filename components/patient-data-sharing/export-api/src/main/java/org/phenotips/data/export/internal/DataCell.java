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
package org.phenotips.data.export.internal;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds the value of the cell and its styling. The position is held as indices of a 2D array in whatever class is
 * referencing the cell.
 */
public class DataCell
{
    private String value;

    private Set<StyleOption> styles;

    private Integer x;

    private Integer y;

    private Integer mergeX;

    public DataCell(String value, Integer x, Integer y)
    {
        this.value = value;
        this.x = x;
        this.y = y;
    }

    public DataCell(String value, Integer x, Integer y, StyleOption style)
    {
        this.value = value;
        this.x = x;
        this.y = y;
        addStyle(style);
    }

    public void addStyle(StyleOption style)
    {
        if (styles == null) {
            styles = new HashSet<StyleOption>();
        }
        styles.add(style);
    }

    public Set<StyleOption> readStyles()
    {
        return styles;
    }

    public Integer getX()
    {
        return x;
    }

    public Integer getY()
    {
        return y;
    }

    public void setX(Integer x)
    {
        this.x = x;
    }

    public void setY(Integer y)
    {
        this.y = y;
    }

    public String getValue()
    {
        return value;
    }

    public Integer getMergeX()
    {
        return mergeX;
    }

    public void addMergeX()
    {
        if (mergeX == null) {
            mergeX = 0;
        }
        mergeX++;
    }
}
