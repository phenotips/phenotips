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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 * Reads the {@link org.phenotips.export.internal.DataCell#styles} from DataCells, creates {@link
 * org.apache.poi.ss.usermodel.CellStyle}s and applies them to {@link org.apache.poi.ss.usermodel.Cell}. In other words
 * this class bridges the gap between {@link org.phenotips.export.internal.DataCell#styles}'s keeping track of the
 * cell's appearance, and actually committing the appearance to the spreadsheet. This class also contains some static
 * function which have to be used at later stages of committing cells to a spreadsheet.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class Styler
{
    private static final String NO_MATRIX_ERR_MSG = "The section has not been converted to a matrix";

    /**
     * The {@link org.apache.poi.ss.usermodel.Workbook} can have only a limited number of styles. The cache prevents
     * creation of duplicates.
     */
    private Map<Set<StyleOption>, CellStyle> styleCache = new HashMap<Set<StyleOption>, CellStyle>();

    /** Cached font. */
    private Font defaultFont;

    /**
     * In some corner cases, some styles should be removed from cells to prevent conflicts with styles in other cells.
     *
     * @param section cannot be null
     * @throws java.lang.Exception if the section was not finalized
     */
    public static void disallowBodyStyles(DataSection section) throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception(NO_MATRIX_ERR_MSG);
        }
        List<StyleOption> disallowedStyles =
            Arrays.asList(StyleOption.FEATURE_SEPARATOR, StyleOption.YES_NO_SEPARATOR);

        for (int x = 0; x <= section.getMaxX(); x++) {
            DataCell cell = cellMatrix[x][0];
            if (cell != null) {
                cell.removeStyles(disallowedStyles);
            }
        }
    }

    /**
     * Styles the bottom cells of the section. Creates new {@link org.phenotips.export.internal.DataCell}s, if missing.
     * This is a static function that is used outside of this class in the final stages of committing cells to a
     * spreadsheet. For example, {@link org.phenotips.export.internal.SheetAssembler#SheetAssembler(java.util.Set,
     * java.util.List)}.
     *
     * @param section cannot be null
     * @param style the style to apply
     * @throws java.lang.Exception if the section was not {@link DataSection#finalizeToMatrix()}
     */
    public static void styleSectionBottom(DataSection section, StyleOption style) throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception(NO_MATRIX_ERR_MSG);
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

    /**
     * Styles the leftmost and rightmost cells of the section. Creates new {@link org.phenotips.export.internal
     * .DataCell}s, if missing.
     *
     * @param section cannot be null
     * @param styleLeft the style to apply to the leftmost cells
     * @param styleRight the style to apply to the rightmost cells
     * @throws java.lang.Exception if the section was not {@link DataSection#finalizeToMatrix()}
     * @see #styleSectionBottom(DataSection, StyleOption)
     */
    public static void styleSectionBorder(DataSection section, StyleOption styleLeft, StyleOption styleRight)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception(NO_MATRIX_ERR_MSG);
        }

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

    /**
     * Looks for passed in styles in each row of the passed in section, determines which styles are present, and extends
     * them horizontally from the first cell in which a style from the passed in array of styles was found. In other
     * words, when the function finds a cell that has at least one style from the passed in styles array, it stops
     * searching, and starts extending.
     *
     * @param section cannot be null
     * @param styles an array of styles to look for
     * @throws Exception if the section was not {@link DataSection#finalizeToMatrix()}
     */
    public static void extendStyleHorizontally(DataSection section, StyleOption... styles)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception(NO_MATRIX_ERR_MSG);
        }

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

            /* In case the border passes through non-existent cells */
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

    /**
     * Same as {@link #extendStyleHorizontally(DataSection, StyleOption[])}, but in the vertical direction, and it is
     * not limited by the first occurrence of a style from the passed in styles. Unlike it's horizontal cousin, this
     * function always starts extending from the top of the section.
     *
     * @param section cannot be null
     * @param styles an array of styles to look for
     * @throws Exception if the section was not {@link DataSection#finalizeToMatrix()}
     */
    public static void extendStyleVertically(DataSection section, StyleOption... styles)
        throws Exception
    {
        DataCell[][] cellMatrix = section.getMatrix();
        if (cellMatrix == null) {
            throw new Exception(NO_MATRIX_ERR_MSG);
        }

        for (int x = 0; x <= section.getMaxX(); x++) {
            Set<StyleOption> toExtend = new HashSet<StyleOption>();
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
                    break;
                }
            }
            if (!found) {
                continue;
            }

            /* In case the border passes through non-existent cells */
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

    /**
     * Translates the internal styling into styling that {@link org.apache.poi.ss.usermodel.Workbook} can use.
     *
     * @param dataCell from which the styling should be read
     * @param cell to which the styling should be applied
     * @param wBook in which the "cell" resides
     */
    public void style(DataCell dataCell, Cell cell, Workbook wBook)
    {
        Set<StyleOption> styles = dataCell.getStyles();
        CellStyle cellStyle = wBook.createCellStyle();
        /* For \n to work properly set to true */
        cellStyle.setWrapText(true);
        if (this.defaultFont == null) {
            this.defaultFont = createDefaultFont(wBook);
        }
        cellStyle.setFont(this.defaultFont);
        cellStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);

        /* If the DataCell's styles is not set, gives default style, and indicates that this function should return. */
        if (this.setDefaultStyle(styles, cell, cellStyle)) {
            return;
        }
        if (this.styleCache.containsKey(styles)) {
            cell.setCellStyle(this.styleCache.get(styles));
            return;
        }

        /* Priority of styles can be coded in by placing the if statement lower within the corresponding function. */
        this.setFontStyles(styles, cell, cellStyle, wBook);
        this.setBorderStyles(styles, cell, cellStyle, wBook);

        /* Keep this as the last statement. */
        this.styleCache.put(styles, cellStyle);
    }

    /**
     * In case a {@link org.phenotips.export.internal.DataCell} does not have its styles set, applies the default
     * style.
     */
    private boolean setDefaultStyle(Set<StyleOption> styles, Cell cell, CellStyle cellStyle)
    {
        if (styles == null) {
            if (this.styleCache.containsKey(Collections.<StyleOption>emptySet())) {
                cell.setCellStyle(this.styleCache.get(Collections.<StyleOption>emptySet()));
                return true;
            }
            cell.setCellStyle(cellStyle);
            this.styleCache.put(Collections.<StyleOption>emptySet(), cellStyle);
            return true;
        }
        return false;
    }

    /**
     * Creates new {@link org.apache.poi.ss.usermodel.Font}s. Priority can be coded in by placing the if statement
     * lower, for higher priority.
     */
    private void setFontStyles(Set<StyleOption> styles, Cell cell, CellStyle cellStyle, Workbook wBook)
    {
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
    }

    /**
     * Converts {@link org.phenotips.export.internal.StyleOption} enum to {@link org.apache.poi.ss.usermodel.CellStyle}
     * enum. Priority can be coded in by placing the if statement lower, for higher priority.
     */
    private void setBorderStyles(Set<StyleOption> styles, Cell cell, CellStyle cellStyle, Workbook wBook)
    {
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
    }

    private Font createDefaultFont(Workbook wBook)
    {
        Font font = wBook.createFont();
        font.setFontHeightInPoints((short) 9);
        font.setFontName(XSSFFont.DEFAULT_FONT_NAME);
        return font;
    }
}
