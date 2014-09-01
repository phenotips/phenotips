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

import org.phenotips.data.Patient;
import org.phenotips.data.internal.PhenoTipsPatient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
 * @since 1.0RC1
 */
public class SpreadsheetExporter
{
    protected Workbook wBook;

    protected OutputStream wOutputStream;

    protected Map<String, Sheet> sheets = new HashMap<String, Sheet>();

    public void export(String[] _enabledFields, List<XWikiDocument> patients, OutputStream outputStream)
        throws Exception
    {
        if (_enabledFields == null) {
            return;
        }
        Set<String> enabledFields = new HashSet<String>(Arrays.asList(_enabledFields));
        try {
            createBlankWorkbook();
            processMainSheet(enabledFields, patients);
            wBook.write(outputStream);
            outputStream.flush();
//            wBook.write(wOutputStream);
        } catch (IOException ex) {
            //FIXME
        } catch (Exception ex) {
            //Nothing that can be really done, so just rethrow it.
            throw ex;
        } finally {
            /* if (wOutputStream != null) {
                try {
                    wOutputStream.close();
                } catch (IOException ex) {
                    //If this happens, something went very wrong.
                }
            } */
//            return wOutputStream;
        }
    }

    protected void processMainSheet(Set<String> enabledFields, List<XWikiDocument> patients) throws Exception
    {
        String sheetName = "main";
        Sheet sheet = wBook.createSheet("Patient Sheet");
        sheets.put(sheetName, sheet);

        SheetAssembler assembler = runAssembler(enabledFields, patients);
//        styleCells();
        write(assembler.getAssembled(), sheet);
        freezeHeader(assembler.headerHeight.shortValue(), sheet);
    }

    /*
    protected void styleCells()
    {
        CellStyler styler = new CellStyler(wBook);
        styler.header(formatter.getHeaders());
        styler.patientSeparationBorder(formatter.getBody(), formatter.getPatientBottomBorders(),
            formatter.getMaxRowLength());
        styler.body(formatter.getBody());
    }
    */

    protected void freezeHeader(Short height, Sheet sheet)
    {
        sheet.createFreezePane(0, height);
    }

    protected SheetAssembler runAssembler(Set<String> enabledFields, List<XWikiDocument> _patients)
        throws Exception
    {
        List<Patient> patients = new LinkedList<Patient>();
        Map<Patient, XWikiDocument> patientXWikiDocumentMap = new HashMap<>();
        for (XWikiDocument patientDoc : _patients) {
            Patient patientInstance = new PhenoTipsPatient(patientDoc);
            patients.add(patientInstance);
            patientXWikiDocumentMap.put(patientInstance, patientDoc);
        }
        return new SheetAssembler(enabledFields, patients, patientXWikiDocumentMap);
    }

    protected void write(DataSection section, Sheet sheet)
    {
        DataCell[][] cells = section.getMatrix();
        Styler styler = new Styler();

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
                styler.style(dataCell, cell, wBook);

                if (dataCell.getNumberOfLines() != null) {
                    maxLines = maxLines < dataCell.getNumberOfLines() ? dataCell.getNumberOfLines() : maxLines;
                }
            }
            if (maxLines > 1) {
                Integer height = maxLines * 400;
                row.setHeight(height.shortValue());
            }
        }
        for (int col = 0; section.getMaxX() >= col; col++) {
            sheet.autoSizeColumn(col);
            if (sheet.getColumnWidth(col) > (DataToCellConverter.charactersPerLine * 210)) {
                sheet.setColumnWidth(col, DataToCellConverter.charactersPerLine * 210);
            }
        }

        /** Merging has to be done after autosizing because otherwise autosizing breaks */
        for (Integer y = 0; y <= section.getMaxY(); y++) {
            for (Integer x = 0; x <= section.getMaxX(); x++) {
                DataCell dataCell = cells[x][y];
                if (dataCell != null && dataCell.getMergeX() != null) {
                    sheet.addMergedRegion(new CellRangeAddress(y, y, x, x + dataCell.getMergeX()));
                }
                /* No longer will be merging cells on the Y axis, but keep this code for future reference.
                if (dataCell.getYBoundry() != null) {
                    sheet.addMergedRegion(new CellRangeAddress(dataCell.y, dataCell.getYBoundry(), dataCell.x, dataCell.x));
                } */
            }
        }
    }

    protected void createBlankWorkbook() throws IOException
    {
        wBook = new XSSFWorkbook();
//        wOutputStream = new FileOutputStream("/home/anton/Documents/workbook.xlsx");
        wOutputStream = new ByteArrayOutputStream();
    }
}
