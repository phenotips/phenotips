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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Holds various static functions which style {@link DataCell}
 */
public class CellStyler
{
    Workbook wBook;

    public CellStyler(Workbook wBook)
    {
        this.wBook = wBook;
    }

    public void header(List<DataCell> headers)
    {
        for (DataCell header : headers) {
            header.getStyle(wBook).setAlignment(CellStyle.ALIGN_CENTER);
            header.getStyle(wBook).setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        }
    }

    public void patientSeparationBorder(List<DataCell> body, Set<Integer> _patientBottomBorders, Integer maxCols)
    {
        List<Integer> patientBottomBorders = new LinkedList<Integer>();
        patientBottomBorders.addAll(_patientBottomBorders);
        Collections.sort(patientBottomBorders);

        Set<DataCell> bodyCopy = new HashSet<DataCell>();
        bodyCopy.addAll(body);
        for (Integer bY : patientBottomBorders) {
            Set<Short> notFound = new HashSet<Short>();
            Set<DataCell> toRemove = new HashSet<DataCell>();
            for (short x = 0; x < maxCols; x++) {
                notFound.add(x);
            }
            for (DataCell cell : bodyCopy) {
                if (cell.y < bY && (cell.getYBoundry() != null && cell.getYBoundry() > bY)) {
                    toRemove.add(cell);
                } else if ((int) cell.y == bY) {
                    cell.getStyle(wBook).setBorderBottom(CellStyle.BORDER_THIN);
                    cell.getStyle(wBook).setBottomBorderColor(IndexedColors.BLACK.getIndex());
                    notFound.remove(cell.x);
                } else if (cell.getYBoundry() != null && (int) cell.getYBoundry() == bY){
                    for (int y=cell.getYBoundry(); y > cell.y; y--) {
                        DataCell emptyCell = new DataCell("", cell.x, (short) y);
                        emptyCell.getStyle(wBook).setBorderBottom(CellStyle.BORDER_THIN);
                        emptyCell.getStyle(wBook).setBottomBorderColor(IndexedColors.BLACK.getIndex());
                        body.add(emptyCell);
                    }
                    cell.getStyle(wBook).setBorderBottom(CellStyle.BORDER_THIN);
                    cell.getStyle(wBook).setBottomBorderColor(IndexedColors.BLACK.getIndex());
                    notFound.remove(cell.x);
                }
            }
            for (DataCell remove : toRemove) {
                bodyCopy.remove(remove);
            }
            for (Short x : notFound) {
                DataCell cell = new DataCell("-", x, (short) ((int) bY));
                cell.getStyle(wBook).setBorderBottom(CellStyle.BORDER_THIN);
                cell.getStyle(wBook).setBottomBorderColor(IndexedColors.BLACK.getIndex());
                body.add(cell);
            }
        }
    }

    public void body(List<DataCell> body)
    {
        for (DataCell cell : body) {
            cell.getStyle(wBook).setVerticalAlignment(CellStyle.VERTICAL_CENTER);
            //FIXME Doesn't work anyways;
            if (cell.styleRequests.contains("shade")) {
                cell.getStyle(wBook).setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
        }
    }
}
