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

import java.util.Collections;
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
import org.apache.poi.xssf.usermodel.XSSFFont;

/**
 * Takes care of giving cells the right style, and applying styles to the output spreadsheet.
 */
public class Styler
{
    Map<Set<StyleOption>, CellStyle> styleCache = new HashMap<Set<StyleOption>, CellStyle>();

    Font defaultFont = null;

    /** Use only on finalized sections */
    public static void styleSectionBottom(DataSection section, StyleOption style) throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception("The section has not been converted to a matrix");
        }

        /* In case the border passes through non-existent cells */
        for (int x = 0; x <= section.getMaxX(); x++) {
            DataCell cell = cellMatrix[x][section.getMaxY()];
            if (cell == null) {
                cell = new DataCell("", x, section.getMaxY());
                section.addCell(cell);
            }
            cell.addStyle(style);
        }
    }

    public static void styleSectionBorder(DataSection section, StyleOption styleLeft, StyleOption styleRight)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception("The section has not been converted to a matrix");
        }

        /* TODO determine if setting border on both sides is needed */
        /* In case the border passes through non-existent cells */
        for (int y = 0; y <= section.getMaxY(); y++) {
            DataCell cellLeft = cellMatrix[0][y];
            DataCell cellRight = cellMatrix[section.getMaxX()][y];
            if (cellLeft == null) {
                cellLeft = new DataCell("", 0, y);
                section.addCell(cellLeft);
            }
            if (cellRight == null) {
                cellRight = new DataCell("", section.getMaxX(), y);
                section.addCell(cellRight);
            }
            cellLeft.addStyle(styleLeft);
            cellRight.addStyle(styleRight);
        }
    }


    public static void extendStyleHorizontally(DataSection section, StyleOption... styles)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception("The section has not been converted to a matrix");
        }

        /* In case the border passes through non-existent cells */
        for (int y = 0; y <= section.getMaxY(); y++) {
            Set<StyleOption> toExtend = new HashSet<StyleOption>();
            Integer startingX = 0;
            Boolean found = false;
            for (int x = 0; x <= section.getMaxX(); x++) {
                found = false;
                DataCell cell = cellMatrix[x][y];
                if (cell == null) {
                    continue;
                }
                for (StyleOption style : styles) {
                    if (cell.getStyles() != null && cell.getStyles().contains(style)) {
                        toExtend.add(style);
                        found = true;
                    }
                }
                if (found) {
                    startingX = x;
                    break;
                }
            }
            if (!found) {
                continue;
            }
            for (int x = startingX + 1; x <= section.getMaxX(); x++) {
                DataCell cell = cellMatrix[x][y];
                if (cell == null) {
                    cell = new DataCell("", x, y);
                    section.addCell(cell);
                }
                cell.addStyles(toExtend);
            }
        }
    }

    public static void extendStyleVertically(DataSection section, StyleOption... styles)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception("The section has not been converted to a matrix");
        }

        /* In case the border passes through non-existent cells */
        for (int x = 0; x <= section.getMaxX(); x++) {
            Set<StyleOption> toExtend = new HashSet<StyleOption>();
            Integer startingY = 0;
            Boolean found = false;
            for (int y = 0; y <= section.getMaxY(); y++) {
                found = false;
                DataCell cell = cellMatrix[x][y];
                if (cell == null) {
                    continue;
                }
                for (StyleOption style : styles) {
                    if (!cell.isChild() && cell.getStyles() != null && cell.getStyles().contains(style)) {
                        toExtend.add(style);
                        found = true;
                    }
                }
                if (found) {
                    startingY = y;
                    break;
                }
            }
            if (!found) {
                continue;
            }
//            for (int y = startingY + 1; y <= section.getMaxY(); y++) {
            for (int y = 0; y <= section.getMaxY(); y++) {
                DataCell cell = cellMatrix[x][y];
                if (cell == null) {
                    cell = new DataCell("", x, y);
                    section.addCell(cell);
                }
                cell.addStyles(toExtend);
            }
        }
    }

    public void style(DataCell dataCell, Cell cell, Workbook wBook)
    {
        Set<StyleOption> styles = dataCell.getStyles();
        CellStyle cellStyle = wBook.createCellStyle();
        /* For \n to work properly set to true */
        cellStyle.setWrapText(true);
        if (defaultFont == null) {
            defaultFont = createDefaultFont(wBook);
        }
        cellStyle.setFont(defaultFont);
        if (styles == null) {
            if (styleCache.containsKey(Collections.<StyleOption>emptySet())) {
                cell.setCellStyle(styleCache.get(Collections.<StyleOption>emptySet()));
                return;
            }
            cell.setCellStyle(cellStyle);
            styleCache.put(Collections.<StyleOption>emptySet(), cellStyle);
            return;
        }

        if (styleCache.containsKey(styles)) {
            cell.setCellStyle(styleCache.get(styles));
            return;
        }

        /* Priority can be coded in by placing the if statement lower, for higher priority */
        /** Font styles */
        Font headerFont = null;
        if (styles.contains(StyleOption.HEADER)) {
            headerFont = wBook.createFont();
            headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
            cellStyle.setFont(headerFont);
            cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
            cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.LARGE_HEADER)) {
            if (headerFont == null) {
                headerFont = wBook.createFont();
                headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
            }
            headerFont.setFontHeightInPoints((short) 12);
            cellStyle.setFont(headerFont);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.YES)) {
            Font font = createDefaultFont(wBook);
            font.setColor(HSSFColor.GREEN.index);
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);
        }
        if (styles.contains(StyleOption.NO)) {
            Font font = createDefaultFont(wBook);
            font.setColor(HSSFColor.DARK_RED.index);
            font.setBoldweight(Font.BOLDWEIGHT_BOLD);
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);
        }

        /** Border styles */
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
            cellStyle.setBorderTop(CellStyle.BORDER_DASHED);
            cellStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
            cell.setCellStyle(cellStyle);
        }

        /* Keep this as the last statement. */
        styleCache.put(styles, cellStyle);
    }

    private Font createDefaultFont(Workbook wBook) {
        Font font = wBook.createFont();
        font.setFontHeightInPoints((short) 9);
        font.setFontName(XSSFFont.DEFAULT_FONT_NAME);
        return font;
    }
}
