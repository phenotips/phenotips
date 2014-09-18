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

package org.phenotips.export.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds the value of the cell and its styling. The position is held as indices of a 2D array in whatever class is
 * referencing the cell.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class DataCell
{
    private String value;

    private Set<StyleOption> styles;

    private Integer x;

    private Integer y;

    private Integer mergeX;

    private Boolean isChild;

    private Set<DataCell> generated;

    private Integer numberOfLines;

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
        if (this.styles == null) {
            this.styles = new HashSet<StyleOption>();
        }
        this.styles.add(style);
        if (this.generated != null) {
            for (DataCell child : this.generated) {
                child.addStyle(style);
            }
        }
    }

    public void addStyles(Collection<StyleOption> styles)
    {
        if (styles == null) {
            return;
        }
        if (this.styles == null) {
            this.styles = new HashSet<StyleOption>();
        }
        this.styles.addAll(styles);
        if (this.generated != null) {
            for (DataCell child : this.generated) {
                child.addStyles(styles);
            }
        }
    }

    public Integer getX()
    {
        return this.x;
    }

    public Integer getY()
    {
        return this.y;
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
        return this.value;
    }

    public Integer getMergeX()
    {
        return this.mergeX;
    }

    public void addMergeX()
    {
        if (this.mergeX == null) {
            this.mergeX = 0;
        }
        this.mergeX++;
    }

    public Set<DataCell> generateMergedCells()
    {
        if (this.mergeX == null) {
            return Collections.emptySet();
        }
        if (this.generated != null) {
            return this.generated;
        }

        this.generated = new HashSet<DataCell>();
        for (int x = 1; x <= this.mergeX; x++) {
            DataCell cell = new DataCell("", this.x + x, this.getY());
            cell.setIsChild(true);
            cell.addStyles(this.styles);
            this.generated.add(cell);
        }

        return this.generated;
    }

    public Set<StyleOption> getStyles()
    {
        return this.styles;
    }

    public Boolean isChild()
    {
        if (this.isChild == null || !this.isChild) {
            return false;
        }
        return true;
    }

    public void setIsChild(Boolean isChild)
    {
        this.isChild = isChild;
    }

    public void setMultiline()
    {
        this.numberOfLines = this.value.split("\n").length;
    }

    public Integer getNumberOfLines()
    {
        return this.numberOfLines;
    }
}
