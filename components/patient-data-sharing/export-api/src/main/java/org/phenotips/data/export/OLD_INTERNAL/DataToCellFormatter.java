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
package org.phenotips.data.export.OLD_INTERNAL;

import org.phenotips.data.Patient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataToCellFormatter
{
    private List<DataCell> headers = new LinkedList<DataCell>();

    private List<DataCell> body = new LinkedList<DataCell>();

    private List<Set<DataCell>> patientCellSets = new LinkedList<Set<DataCell>>();

    private Short headerHeight = 0;

    private Integer maxRowLength = 0;

    /**
     * Holds each data row's (not spreadsheet row) height.
     */
    private List<Map<Integer, Short>> patientRowMetaData = new LinkedList<Map<Integer, Short>>();

    /** Holds each headers data row's height */
    private Map<Integer, Short> headerRowMetaData = new HashMap<Integer, Short>();

    /** Holds the current ending column number for each data row */
    private Map<Integer, Short> patientColumnMetaData = new HashMap<Integer, Short>();

    private Map<Integer, Short> patientDataRowPositions;

    private Integer totalDataRows;

    public Integer getTotalDataRows()
    {
        return totalDataRows;
    }

    public Set<Integer> getPatientBottomBorders()
    {
        Set<Integer> bottoms = new HashSet<Integer>();
        Integer y = 0;
        Boolean first = true;
        for (Integer rowNum : patientDataRowPositions.keySet()) {
            if (first) {
                first = false;
                continue;
            }
            if (y == totalDataRows - 1) {
                y = 0;
                bottoms.add(patientDataRowPositions.get(rowNum) - 1);
            } else {
                y++;
            }
        }
        return bottoms;
    }

    public List<DataCell> getHeaders()
    {
        return headers;
    }

    public List<DataCell> getBody()
    {
        return body;
    }

    public Short getHeaderHeight()
    {
        //Fixme. This might no be the best way to get the header height.
        if (headers.isEmpty()) {
            return 0;
        }
        if (headerHeight == 0) {
            for (DataCell cell : headers) {
                headerHeight = cell.y;
            }
            headerHeight = (short) (1 + headerHeight);
            return headerHeight;
        }
        return headerHeight;
    }

    public Integer getMaxRowLength()
    {
       if (maxRowLength > 0) {
           return maxRowLength;
       }
       for (Short rowNum : patientColumnMetaData.values()) {
           if (rowNum > maxRowLength) {
               maxRowLength = (int) rowNum;
           }
       }
       return maxRowLength;
    }

    public Set<DataCell> getAllCells()
    {
        Short hHeight = getHeaderHeight();
        for (DataCell bodyCell : getBody()) {
            bodyCell.y = (short) (bodyCell.y + hHeight);
        }
        Set<DataCell> all = new HashSet<DataCell>();
        all.addAll(getHeaders());
        all.addAll(getBody());
        return all;
    }

    protected void addSection(List<Set<DataCell>> cellSet, Integer dataRow)
    {
        Short xOffset = patientColumnMetaData.get(dataRow);
        Short maxX = 0;
        Integer counter = 0;
        for (Set<DataCell> patientCells : cellSet) {
            Map<Integer, Set<Integer>> relativeFillMap = new HashMap<Integer, Set<Integer>>();

            if (counter >= this.patientCellSets.size()) {
                this.patientCellSets.add(patientCells);
            } else {
                this.patientCellSets.get(counter).addAll(patientCells);
            }
            //Fixme. If initializeRowData does not fill patientColumnMetaData properly, this will throw an error.
            Short maxY = 0;
            for (DataCell cell : patientCells) {
                Set<Integer> rowFillMap;
                rowFillMap = relativeFillMap.get((int) cell.y);
                if (rowFillMap == null) {
                    rowFillMap = new HashSet<Integer>();
                }

                if (maxX < cell.x) {
                    maxX = cell.x;
                }
                if (maxY < cell.y) {
                    maxY = cell.y;
                }
                cell.x = (short) (cell.x + xOffset);

                rowFillMap.add((int) cell.x);
                relativeFillMap.put((int) cell.y, rowFillMap);
            }
            for (DataCell cell : patientCells) {
                /** Merging along the y axis and checking if the cell is at the bottom of the data row */ //FIXME MOVE DOWN
                Integer yMerge = 0;
                cell.bottomOfDataRow = true;
                for (int y=cell.y + 1; y <= maxY; y++) {
                    cell.bottomOfDataRow = false;
                    if (!relativeFillMap.get(y).contains((int) cell.x)) {
                        yMerge++;
                    } else {
                        break;
                    }
                }
                if (!cell.bottomOfDataRow) {
                    cell.setYMerge(yMerge);
                }
            }

            patientColumnMetaData.put(dataRow, (short) (xOffset + maxX + 1));
            Map<Integer, Short> dataRowMap = patientRowMetaData.get(counter);
            if (dataRowMap.get(dataRow) < maxY + 1) {
                dataRowMap.put(dataRow, (short) (maxY + 1));
            }
            counter++;
        }
    }

    protected void addHeaderSection(Set<DataCell> cellSet, Integer dataRow)
    {
        Map<Integer, Set<Integer>> relativeFillMap = new HashMap<Integer, Set<Integer>>();
        Short xOffset = patientColumnMetaData.get(dataRow);
        Short maxY = headerRowMetaData.get(dataRow);
        Short maxX = 0;
        Short absoluteMaxX = 0;
        for (DataCell cell : cellSet) {
            Set<Integer> rowFillMap;
            rowFillMap = relativeFillMap.get((int) cell.y);
            if (rowFillMap == null) {
                rowFillMap = new HashSet<Integer>();
            }

            if (cell.x > maxX) {
                maxX = cell.x;
            }
            if (maxY < cell.y) {
                maxY = cell.y;
            }
            cell.x = (short) (cell.x + xOffset);
            if (cell.x > absoluteMaxX) {
                absoluteMaxX = cell.x;
            }

            headers.add(cell);
            rowFillMap.add((int) cell.x);
            relativeFillMap.put((int) cell.y, rowFillMap);
        }
        for (DataCell cell: cellSet) {
            /** Merging along the x axis */
            Set<Integer> rowFill = relativeFillMap.get((int) cell.y);
            Integer xMerge = 0;
            for (int x=1; x <= maxX; x++) {
                Integer next = cell.x + x;
                if (!rowFill.contains(next) && next <= absoluteMaxX) {
                    xMerge++;
                } else {
                    break;
                }
            }
            cell.xMerge = xMerge;
            /** Merging along the y axis and checking if the cell is at the bottom of the data row */
            Integer yMerge = 0;
            cell.bottomOfDataRow = true;
            for (int y=cell.y + 1; y <= maxY; y++) {
                cell.bottomOfDataRow = false;
                if (!relativeFillMap.get(y).contains((int) cell.x)) {
                    yMerge++;
                } else {
                    break;
                }
            }
            if (!cell.bottomOfDataRow) {
                cell.setYMerge(yMerge);
            }
        }
        headerRowMetaData.put(dataRow, (short) (maxY + 1));
    }

    protected void assembleHeaders()
    {
        //This is just for compatibility with globalDataRowPositions
        List<Map<Integer, Short>> fakeHRMD= new LinkedList<Map<Integer, Short>>();
        fakeHRMD.add(headerRowMetaData);
        Map<Integer, Short> globalPositions = globalDataRowPositions(fakeHRMD);
        for (DataCell headerCell : headers) {
            /** First expand the y merge for bottom cells */
            if (headerCell.bottomOfDataRow) {
                headerCell.setYMerge(headerRowMetaData.get(headerCell.dataRow) - headerCell.y - 1);
            }
            /** Then give them the absolute y position */
            headerCell.y = (short) (headerCell.y + globalPositions.get(headerCell.dataRow));
        }
    }

    protected void assemble()
    {
        Map<Integer, Short> globalPositions = globalDataRowPositions(patientRowMetaData);
        patientDataRowPositions = globalPositions;
        Integer patientTopGlobalOffset = 0;
        Integer patientCounter = 0;
        for (Set<DataCell> patientCells : this.patientCellSets) {
            Integer maxDataRows = 0;
            if (patientCells.isEmpty()) {
                continue;
            }
            for (DataCell cell : patientCells) {
                /** If when the cell's section was added, it was at the bottom of the data row, give it a yMerge */
                if (cell.bottomOfDataRow) {
                    cell.setYMerge(patientRowMetaData.get(patientCounter).get(cell.dataRow) - cell.y - 1);
                }
                /** Global positioning has to be done after calculating merge */
                cell.y = (short) (cell.y + globalPositions.get(patientTopGlobalOffset + cell.dataRow));
                if (cell.dataRow > maxDataRows) {
                    maxDataRows = cell.dataRow;
                }
            }
            body.addAll(patientCells);
            patientTopGlobalOffset += maxDataRows + 1;
            patientCounter++;
        }
    }

    protected Map<Integer, Short> globalDataRowPositions(List<Map<Integer, Short>> rowMetaData) {
        Map<Integer, Short> globalMap = new HashMap<Integer, Short>();
        Integer dataRowCounter = 1;
        Short offset = 0;
        for (Map<Integer, Short> patientMeta : rowMetaData) {
            for (Integer key : patientMeta.keySet()) {
                offset = (short) (offset + patientMeta.get(key));
                globalMap.put(dataRowCounter, offset);
                dataRowCounter++;
            }
        }
        globalMap.put(0, (short) 0);
        return globalMap;
    }

    public DataToCellFormatter(Set<String> enabledFields, List<Patient> patients)
    {
        this.totalDataRows = 1;
        initializeRowData(patients, getTotalDataRows());

        DataToCellConverter converter = new DataToCellConverter(this);
        converter.convertIDs(enabledFields, patients, 0);
        converter.convertFeatures(enabledFields, patients, 0);

        assemble();
        assembleHeaders();
    }

    protected void initializeRowData(List<Patient> patients, Integer totalDataRows) {
        //FIXME this should go through all rows
        for (int i=0; i < totalDataRows; i++) {
            patientColumnMetaData.put(i, (short) 0);
            headerRowMetaData.put(i, (short) 0);
        }
        for (int i = 0; i < patients.size(); i++) {
            Map<Integer, Short> temp = new HashMap<Integer, Short>();
            temp.put(0, (short) 0);
            patientRowMetaData.add(temp);
        }
    }
}
