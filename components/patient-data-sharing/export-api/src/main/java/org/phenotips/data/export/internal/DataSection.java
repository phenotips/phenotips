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
 * Holds all the DataCells of a section of information about a patient, such as phenotype.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class DataSection
{
    private String sectionName;

    private Set<DataCell> bufferList = new HashSet<DataCell>();

    /** Populated after all the cells are in the buffer list */
    private DataCell[][] finalList;

    private Integer maxX = 0;

    private Integer maxY = 0;

    public DataSection(String name)
    {
        sectionName = name;
    }

    /** This constructor should be used only when combining several sections into one */
    public DataSection()
    {
    }

    /** Need the buffer to determine the size of the matrix first */
    public void addToBuffer(DataCell cell)
    {
        if (cell.getX() > maxX) {
            maxX = cell.getX();
        }
        if (cell.getY() > maxY) {
            maxY = cell.getY();
        }
        bufferList.add(cell);
    }

    public void _finalize() throws Exception
    {
        if (maxX == null || maxY == null) {
            throw new Exception("The maximum values should be initialized");
        }

        /* From now on the cell positioning can be read from the 2D array's points,
        rather then the positioning stored within the cell. */
        finalList = new DataCell[maxX + 1][maxY + 1];
        for (DataCell cell : bufferList) {
            finalList[cell.getX()][cell.getY()] = cell;

            /* Some cells will be later merged, and to preserve styles they need to generate a list of empty
            cells */
            if (cell.generateMergedCells() == null) {
                continue;
            }
            for (DataCell emptyCell : cell.generateMergedCells()) {
                finalList[emptyCell.getX()][emptyCell.getY()] = emptyCell;
            }
        }
    }

    public void mergeX() throws Exception
    {
        _finalize();
        for (Integer y = 0; y <= getMaxY(); y++) {
            for (Integer x = 0; x <= getMaxX(); x++) {
                DataCell cell = finalList[x][y];
                if (cell == null) {
                    continue;
                }
                Integer nextX = x + 1;
                while (nextX <= getMaxX() && finalList[nextX][y] == null) {
                    cell.addMergeX();
                    nextX++;
                }
            }
        }
    }

    public Set<DataCell> getBufferList()
    {
        return bufferList;
    }

    public DataCell[][] getFinalList()
    {
        return finalList;
    }

    public Integer getMaxX()
    {
        return maxX;
    }

    public Integer getMaxY()
    {
        return maxY;
    }
}
