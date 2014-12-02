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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A container for {@link org.phenotips.export.internal.DataCell}s, that organizes its contents into a positional
 * matrix.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class DataSection
{
    private Set<DataCell> cellList = new LinkedHashSet<>();

    /**
     * Eventually the {@link org.phenotips.export.internal.DataCell}s end up in this matrix, with x and y coordinates
     * corresponding to the matrix's indices.
     */
    private DataCell[][] matrix;

    /** The size of the matrix along x axis. */
    private Integer matrixX = 0;

    /** @see #matrixX */
    private Integer matrixY = 0;

    /** Used for creating the {@link #matrix}. */
    private Integer maxX = 0;

    /** @see #maxX */
    private Integer maxY = 0;

    /**
     * Adds a new cell to the {@link #cellList} and attempts to include it into the {@link #matrix}.
     *
     * @param cell cannot be null, and must have coordinates set relative to the top-left corner of this {@linkplain
     * org.phenotips.export.internal.DataSection}
     */
    public void addCell(DataCell cell)
    {
        /* Add to matrix only if the cell fits within the boundaries and the current spot is empty */
        if (this.matrix != null) {
            if (cell.getX() < this.matrixX && cell.getY() < this.matrixY) {
                if (this.matrix[cell.getX()][cell.getY()] == null) {
                    this.matrix[cell.getX()][cell.getY()] = cell;
                }
            }
        }
        if (cell.getX() > this.maxX) {
            this.maxX = cell.getX();
        }
        if (cell.getY() > this.maxY) {
            this.maxY = cell.getY();
        }
        this.cellList.add(cell);
    }

    /**
     * Fills the {@link #matrix} with the cells from {@link #cellList}, putting each cell into the {@link #matrix}
     * according to the cell's coordinates.
     *
     * @throws Exception if {@link #maxX} or {@link #maxY} are null
     */
    public void finalizeToMatrix() throws Exception
    {
        if (this.maxX == null || this.maxY == null) {
            throw new Exception("The maximum values should be initialized");
        }

        // From now on the cell positioning can be read from the 2D array's points, rather then the positioning stored
        // within the cell.
        this.matrixX = this.maxX + 1;
        this.matrixY = this.maxY + 1;
        this.matrix = new DataCell[this.matrixX][this.matrixY];
        for (DataCell cell : this.cellList) {
            this.matrix[cell.getX()][cell.getY()] = cell;

            // Some cells will be later merged, and to preserve styles they need to generate a list of empty cells
            for (DataCell emptyCell : cell.generateMergedCells()) {
                this.matrix[emptyCell.getX()][emptyCell.getY()] = emptyCell;
            }
        }
    }

    /**
     * Checks for "holes" in the {@link #matrix}, and sets the number of spreadsheets cells a {@link
     * org.phenotips.export.internal.DataCell} should encompass.
     *
     * @throws Exception if this DataSection has not beet {@link #finalizeToMatrix()}
     * @see DataCell#addMergeX()
     */
    public void mergeX() throws Exception
    {
        if (this.matrix == null) {
            throw new Exception("The section has not been converted to a matrix");
        }
        for (Integer y = 0; y <= getMaxY(); y++) {
            for (Integer x = 0; x <= getMaxX(); x++) {
                DataCell cell = this.matrix[x][y];
                if (cell == null) {
                    continue;
                }
                Integer nextX = x + 1;
                while (nextX <= getMaxX() && this.matrix[nextX][y] == null) {
                    cell.addMergeX();
                    nextX++;
                }
            }
        }
    }

    /**
     * @return {@link #cellList}
     */
    public Set<DataCell> getCellList()
    {
        return this.cellList;
    }

    /**
     * @return {@link #matrix}
     */
    public DataCell[][] getMatrix()
    {
        return this.matrix;
    }

    /**
     * @return {@link #maxX}
     */
    public Integer getMaxX()
    {
        return this.maxX;
    }

    /**
     * @return {@link #maxY}
     */
    public Integer getMaxY()
    {
        return this.maxY;
    }
}
