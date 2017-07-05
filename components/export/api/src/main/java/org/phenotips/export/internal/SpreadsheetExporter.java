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

import org.phenotips.data.Patient;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Abstracts all export functionality by exposing a single function {@link #export(String[], java.util.List,
 * java.io.OutputStream)}.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class SpreadsheetExporter
{
    protected Workbook wBook;

    /**
     * Holds workbook sheets names mapped to their sheet object. Although it is currently not used, this map will be
     * needed if working with several sheets.
     */
    protected Map<String, Sheet> sheets = new HashMap<String, Sheet>();

    /**
     * For the list of patients, completes an export limited by the list of fields that are requested, and writes the
     * result to the output stream.
     *
     * @param enabledFieldsArray array of field ids that should be present in the export
     * @param patients list of patients whose information should be present in the export
     * @param outputStream stream to which the export will be written to
     * @throws Exception an attempt to close outputStream will be made, but the exception will not be handled
     */
    public void export(String[] enabledFieldsArray, List<Patient> patients, OutputStream outputStream)
        throws Exception
    {
        if (enabledFieldsArray == null || outputStream == null) {
            return;
        }
        Set<String> enabledFields = new HashSet<String>(Arrays.asList(enabledFieldsArray));
        try {
            this.wBook = createNewWorkbook();
            processMainSheet(enabledFields, patients);
            this.wBook.write(outputStream);
            outputStream.flush();
        } finally {
            try {
                outputStream.close();
            } catch (IOException ex) {
                //If this happens,something went very wrong.
            }
        }
    }

    protected Workbook createNewWorkbook()
    {
        return new XSSFWorkbook();
    }

    /**
     * Creates the main sheet in the workbook, calculates the positioning of the cells, and commits them into the
     * workbook.
     */
    protected void processMainSheet(Set<String> enabledFields, List<Patient> patients) throws Exception
    {
        String sheetName = "main";
        Sheet sheet = this.wBook.createSheet("Patient Sheet");
        this.sheets.put(sheetName, sheet);

        SheetAssembler assembler = runAssembler(enabledFields, patients);
        commit(assembler.getAssembled(), sheet);
        freezeHeader(assembler.getHeaderHeight().shortValue(), sheet);
    }

    protected void freezeHeader(Short height, Sheet sheet)
    {
        sheet.createFreezePane(0, height);
    }

    protected SheetAssembler runAssembler(Set<String> enabledFields, List<Patient> patients)
        throws Exception
    {
        return new SheetAssembler(enabledFields, patients);
    }

    /**
     * Commits cells row by row, sets row height and column width, and merges cells.
     *
     * @param section usually is a single section encompassing all the data on a given sheet
     * @param sheet a workbook sheet to which the cells from the section will be written
     */
    protected void commit(DataSection section, Sheet sheet)
    {
        DataCell[][] cells = section.getMatrix();
        Styler styler = new Styler();

        commitRows(section, sheet, styler);

        for (int col = 0; section.getMaxX() >= col; col++) {
            sheet.autoSizeColumn(col);
            int correctWidth = DataToCellConverter.MAX_CHARACTERS_PER_LINE * 210;
            if (sheet.getColumnWidth(col) > correctWidth) {
                sheet.setColumnWidth(col, correctWidth);
            }
        }

        /* Merging has to be done after autosizing because otherwise autosizing breaks */
        for (Integer y = 0; y <= section.getMaxY(); y++) {
            for (Integer x = 0; x <= section.getMaxX(); x++) {
                DataCell dataCell = cells[x][y];
                if (dataCell != null && dataCell.getMergeX() != null) {
                    sheet.addMergedRegion(new CellRangeAddress(y, y, x, x + dataCell.getMergeX()));
                }
                /*
                 * No longer will be merging cells on the Y axis, but keep this code for future reference.
                 * if (dataCell.getYBoundry() != null) { sheet.addMergedRegion(new CellRangeAddress(dataCell.y,
                 * dataCell.getYBoundry(), dataCell.x, dataCell.x)); }
                 */
            }
        }
    }

    protected void commitRows(DataSection section, Sheet sheet, Styler styler)
    {
        DataCell[][] cells = section.getMatrix();
        Row row;
        for (Integer y = 0; y <= section.getMaxY(); y++) {
            row = sheet.createRow(y);
            Integer maxLines = 0;

            for (Integer x = 0; x <= section.getMaxX(); x++) {
                DataCell dataCell = cells[x][y];
                if (dataCell == null) {
                    continue;
                }
                Cell cell = row.createCell(x);
                cell.setCellValue(dataCell.getValue());
                styler.style(dataCell, cell, this.wBook);

                if (dataCell.getNumberOfLines() != null) {
                    maxLines = maxLines < dataCell.getNumberOfLines() ? dataCell.getNumberOfLines() : maxLines;
                }
            }
            if (maxLines > 1) {
                Integer height = maxLines * 400;
                row.setHeight(height.shortValue());
            }
        }
    }
}
