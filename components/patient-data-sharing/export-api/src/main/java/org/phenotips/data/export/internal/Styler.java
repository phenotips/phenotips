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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Takes care of giving cells the right style, and applying styles to the output spreadsheet.
 */
public class Styler
{
    Map<Set<StyleOption>, CellStyle> styleCache = new HashMap<Set<StyleOption>, CellStyle>();

    /** Use only on finalized sections */
    public static void styleSectionBottom(DataSection section, StyleOption style) throws Exception
    {
        DataCell[][] cellMatrix = section.getFinalList();
        if (cellMatrix == null) {
            throw new Exception("The section has not been finalized");
        }

        /* In case the border passes through non-existent cells */
        for (int x = 0; x <= section.getMaxX(); x++) {
            DataCell cell = cellMatrix[x][section.getMaxY()];
            if (cell == null) {
                cell = new DataCell("", x, section.getMaxY());
                section.addToBuffer(cell);
            }
            cell.addStyle(style);
        }
    }

    public static void styleSectionBorder(DataSection section, StyleOption styleLeft, StyleOption styleRight)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getFinalList();
        if (cellMatrix == null) {
            throw new Exception("The section has not been finalized");
        }

        /* TODO determine if setting border on both sides is needed */
        /* In case the border passes through non-existent cells */
        for (int y = 0; y <= section.getMaxY(); y++) {
            DataCell cellLeft = cellMatrix[0][section.getMaxY()];
            DataCell cellRight = cellMatrix[section.getMaxX()][section.getMaxY()];
            if (cellLeft == null) {
                cellLeft = new DataCell("", 0, y);
                section.addToBuffer(cellLeft);
            }
            if (cellRight == null) {
                cellRight = new DataCell("", section.getMaxX(), y);
                section.addToBuffer(cellLeft);
            }
            cellLeft.addStyle(styleLeft);
            cellRight.addStyle(styleRight);
        }
    }


    public static void extendStyleHorizontally(DataSection section, StyleOption... styles)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getFinalList();
        if (cellMatrix == null) {
            throw new Exception("The section has not been finalized");
        }

        /* In case the border passes through non-existent cells */
        for (int y = 0; y <= section.getMaxY(); y++) {
            Set<StyleOption> toExtend = new HashSet<StyleOption>();
            for (int x = 0; x <= section.getMaxX(); x++) {
                Boolean _break = false;
                DataCell cell = cellMatrix[x][y];
                if (cell == null) {
                    continue;
                }
                for (StyleOption style : styles) {
                    if (cell.getStyles().contains(style)) {
                        toExtend.add(style);
                        _break = true;
                    }
                }

                if (_break) {
                    break;
                }
            }
            for (int x = 0; x <= section.getMaxX(); x++) {
                DataCell cell = cellMatrix[x][y];
                if (cell == null) {
                    cell = new DataCell("", x, y);
                }
                cell.transferStyle(toExtend);
            }
        }
    }

    public void style(DataCell dataCell, Cell cell, Workbook wBook)
    {
        Set<StyleOption> styles = dataCell.getStyles();
        if (styles == null) {
            return;
        }

        if (styleCache.containsKey(styles)) {
            cell.setCellStyle(styleCache.get(styles));
            return;
        }

        /* Priority can be coded in by placing the if statement lower, for higher priority */
        /* Font styles */
        CellStyle cellStyle = wBook.createCellStyle();
        if (styles.contains(StyleOption.HEADER)) {
            Font font = wBook.createFont();
            font.setBoldweight(Font.BOLDWEIGHT_BOLD);
            cellStyle.setFont(font);
            cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
            cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.LARGE_HEADER)) {
            Font font = wBook.createFont();
            font.setFontHeightInPoints((short) 18);
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.YES)) {
            Font font = wBook.createFont();
            font.setColor(HSSFColor.GREEN.index);
            font.setFontHeightInPoints((short) 12);
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.NO)) {
            Font font = wBook.createFont();
            font.setColor(HSSFColor.RED.index);
            font.setFontHeightInPoints((short) 12);
            font.setBoldweight(Font.BOLDWEIGHT_BOLD);
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);
        }

        /* Border styles */
        if (styles.contains(StyleOption.HEADER_BOTTOM)) {
            cellStyle.setBorderBottom(CellStyle.BORDER_MEDIUM);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.SECTION_BORDER_LEFT)) {
            cellStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.SECTION_BORDER_RIGHT)) {
            cellStyle.setBorderRight(CellStyle.BORDER_MEDIUM);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.PATIENT_BORDER)) {
            cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.FEATURE_SEPARATOR)) {
            cellStyle.setBorderTop(CellStyle.BORDER_THIN);
            cellStyle.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.YES_NO_SEPARATOR)) {
            cellStyle.setBorderTop(CellStyle.BORDER_DOTTED);
            cellStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
            cell.setCellStyle(cellStyle);
        }

        /* Keep this as the last statement. */
        styleCache.put(styles, cellStyle);
    }
}
