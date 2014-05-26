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
import org.phenotips.data.internal.PhenoTipsPatient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * FIXME
 *
 * @version $Id$
 * @since 1.0M11
 */

public class SpreadsheetExporter
{
    protected Workbook wBook;

    protected FileOutputStream wFile;

    protected Map<String, Sheet> sheets = new HashMap<String, Sheet>();

    protected Short currentGlobalRow = 0;

    protected DataToCellFormatter formatter;

    public void export(String[] _enabledFields, List<XWikiDocument> patients)
    {
        if (_enabledFields == null) {
            return;
        }
        Set<String> enabledFields = new HashSet<String>(Arrays.asList(_enabledFields));
        try {
            createBlankWorkbook();
            processMainSheet(enabledFields, patients);
            wBook.write(wFile);
        } catch (IOException ex) {
            //FIXME
        } finally {
            if (wFile != null) {
                try {
                    wFile.close();
                } catch (IOException ex) {
                    //If this happens, something went very wrong.
                }
            }
        }
    }

    protected void processMainSheet(Set<String> enabledFields, List<XWikiDocument> patients)
    {
        String sheetName = "main";
        Sheet sheet = wBook.createSheet("Page 1");
        sheets.put(sheetName, sheet);

        convertDataToCells(enabledFields, patients);
        styleCells();
        writeCells(formatter.getAllCells(), sheet);
        freezeHeader(formatter.getHeaderHeight(), sheet);
    }

    protected void styleCells()
    {
        CellStyler styler = new CellStyler(wBook);
        styler.header(formatter.getHeaders());
        styler.patientSeparationBorder(formatter.getBody(), formatter.getPatientBottomBorders(), formatter.getMaxRowLength());
        styler.body(formatter.getBody());
    }

    protected void freezeHeader(Short height, Sheet sheet)
    {
        sheet.createFreezePane(0, height);
    }

    protected void convertDataToCells(Set<String> enabledFields, List<XWikiDocument> _patients)
    {
        List<Patient> patients = new LinkedList<Patient>();
        for (XWikiDocument patient : _patients) {
            patients.add(new PhenoTipsPatient(patient));
        }
        formatter = new DataToCellFormatter(enabledFields, patients);
    }

    protected void writeCells(Iterable<DataCell> dataCells, Sheet sheet)
    {
        Map<Short, Row> rowMap = new HashMap<Short, Row>();

        Short currentEntryRow = -1;
        Row row = null;
        for (DataCell dataCell : dataCells) {
            if (dataCell.y != currentEntryRow) {
                currentEntryRow = dataCell.y;
                try {
                    row = rowMap.get(currentEntryRow);
                    if (row == null) {
                        row = sheet.createRow(currentEntryRow);
                        rowMap.put(currentEntryRow, row);
                    }
                } catch (Exception ex) {
                    row = sheet.createRow(currentEntryRow);
                    rowMap.put(currentEntryRow, row);
                }
            }

            Cell cell = row.createCell(dataCell.x);
            cell.setCellValue(dataCell.value);

            cell.setCellStyle(dataCell.getStyle(wBook));
        }

        for (int col = 0; formatter.getMaxRowLength() > col; col++) {
            sheet.autoSizeColumn(col);
        }

        /** Merging has to be done after autosizing because otherwise autosizing breaks */
        for (DataCell dataCell : dataCells) {
            if (dataCell.getXBoundry() != null) {
                sheet.addMergedRegion(new CellRangeAddress(dataCell.y, dataCell.y, dataCell.x, dataCell.getXBoundry()));
            }
            if (dataCell.getYBoundry() != null) {
                sheet.addMergedRegion(new CellRangeAddress(dataCell.y, dataCell.getYBoundry(), dataCell.x, dataCell.x));
            }
        }
    }

    protected void createBlankWorkbook() throws IOException
    {
        wBook = new XSSFWorkbook();
        wFile = new FileOutputStream("/home/anton/Documents/workbook.xlsx");
    }
}
