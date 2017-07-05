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

package org.phenotips.export.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds the value of the cell, its styling, instructions on merging and size. A
 * {@link org.phenotips.export.internal.DataCell} does not necessarily correspond only to one spreadsheet cell; it can
 * represent several of those at a time. The internal position variables should not be depended upon; they are used for
 * convenience of classes whose job is to assemble the spreadsheet.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class DataCell
{
    /** The contents of this {@link org.phenotips.export.internal.DataCell}. */
    private String value;

    /**
     * These styles are not applied directly. This set holds a list of styles that a
     * {@link org.phenotips.export.internal.Styler} reads and applies.
     */
    private Set<StyleOption> styles;

    /**
     * An arbitrary x coordinate assigned to this cell. This coordinate should not be used when committing cells to a
     * spreadsheet.
     */
    private Integer x;

    /** Same as {@link #x}, except that this is a y coordinate. */
    private Integer y;

    /**
     * How many additional spreadsheet cells on the x axis this {@link org.phenotips.export.internal.DataCell} occupies.
     */
    private Integer mergeX;

    /**
     * True if this is a generated {@link org.phenotips.export.internal.DataCell}. Generated cells are used to support
     * their parent {@link org.phenotips.export.internal.DataCell}; for example to assure correct alignment of cells
     * when merging.
     */
    private Boolean isChild;

    /**
     * A set of {@link org.phenotips.export.internal.DataCell} that were generated to support this
     * {@link org.phenotips.export.internal.DataCell}; for example, these cells are used for merging.
     */
    private Set<DataCell> generated;

    /**
     * Holds the number of lines in cases where the {@link #value} of this
     * {@link org.phenotips.export.internal.DataCell} contains several lines.
     */
    private Integer numberOfLines;

    /**
     * @param value see other constructor
     * @param x see {@link #x}
     * @param y see {@link #y}
     * @see #DataCell(String, Integer, Integer, StyleOption)
     */
    public DataCell(String value, Integer x, Integer y)
    {
        this.value = value != null ? value : "";
        this.x = x;
        this.y = y;
    }

    /**
     * @param value a string holding the content of the cell. Could be null, but should not be in majority of use cases
     * @param x see {@link #x}
     * @param y see {@link #y}
     * @param style a {@link org.phenotips.export.internal.StyleOption} which will be applied when committing this
     *            {@link org.phenotips.export.internal.DataCell} to a spreadsheet.
     */
    public DataCell(String value, Integer x, Integer y, StyleOption style)
    {
        this.value = value != null ? value : "";
        this.x = x;
        this.y = y;
        addStyle(style);
    }

    /**
     * @param style added to {@link #styles} of this {@link org.phenotips.export.internal.DataCell} and all of its
     *            children
     */
    public void addStyle(StyleOption style)
    {
        if (this.styles == null) {
            this.styles = new HashSet<>();
        }
        this.styles.add(style);
        if (this.generated != null) {
            for (DataCell child : this.generated) {
                child.addStyle(style);
            }
        }
    }

    /**
     * @param styles same as in {@link #addStyle(StyleOption)}, but in batch
     */
    public void addStyles(Collection<StyleOption> styles)
    {
        if (styles == null) {
            return;
        }
        if (this.styles == null) {
            this.styles = new HashSet<>();
        }
        this.styles.addAll(styles);
        if (this.generated != null) {
            for (DataCell child : this.generated) {
                child.addStyles(styles);
            }
        }
    }

    /**
     * @param styles that will be removed from the internal {@link #styles}
     */
    public void removeStyles(Collection<StyleOption> styles)
    {
        if (this.styles != null) {
            this.styles.removeAll(styles);
        }
    }

    /**
     * @return {@link #x}
     */
    public Integer getX()
    {
        return this.x;
    }

    /**
     * @return {@link #y}
     */
    public Integer getY()
    {
        return this.y;
    }

    /**
     * @param x to which {@link #x} will be set to
     */
    public void setX(Integer x)
    {
        this.x = x;
    }

    /**
     * @param y to which {@link #y} will be set to
     */
    public void setY(Integer y)
    {
        this.y = y;
    }

    /**
     * @return The content of the cell held in {@link #value}
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * @return {@link #mergeX}
     */
    public Integer getMergeX()
    {
        return this.mergeX;
    }

    /**
     * Increases the number of spreadsheet cells this {@link org.phenotips.export.internal.DataCell} will occupy.
     */
    public void addMergeX()
    {
        if (this.mergeX == null) {
            this.mergeX = 0;
        }
        this.mergeX++;
    }

    /**
     * Generates {@link org.phenotips.export.internal.DataCell}s which will occupy space that will eventually be merged
     * into one spreadsheet cell, and copies the styling to each generated cell.
     *
     * @return a set of {@link org.phenotips.export.internal.DataCell}s that have been generated
     */
    public Set<DataCell> generateMergedCells()
    {
        if (this.mergeX == null) {
            return Collections.emptySet();
        }
        if (this.generated != null) {
            return this.generated;
        }

        this.generated = new HashSet<>();
        for (int gx = 1; gx <= this.mergeX; gx++) {
            DataCell cell = new DataCell("", this.x + gx, this.getY());
            cell.setIsChild(true);
            cell.addStyles(this.styles);
            this.generated.add(cell);
        }

        return this.generated;
    }

    /**
     * @return {@link #styles}
     */
    public Set<StyleOption> getStyles()
    {
        return this.styles;
    }

    /**
     * @return {@link #isChild} if not null, false otherwise
     */
    public Boolean isChild()
    {
        if (this.isChild == null || !this.isChild) {
            return false;
        }
        return true;
    }

    /**
     * @param isChild to which {@link #isChild} will be set to
     */
    public void setIsChild(Boolean isChild)
    {
        this.isChild = isChild;
    }

    /**
     * Calculates and stores the number of lines the content of this {@link org.phenotips.export.internal.DataCell}
     * occupies.
     */
    public void setMultiline()
    {
        this.numberOfLines = this.value.split("\n").length;
    }

    /**
     * @return {@link #numberOfLines}
     */
    public Integer getNumberOfLines()
    {
        return this.numberOfLines;
    }
}
