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

import java.util.HashSet;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class StylerTest
{
    @Test
    public void styleBottom() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption style = StyleOption.FEATURE_SEPARATOR;
        DataCell cell = mock(DataCell.class);
        DataCell[][] matrix = new DataCell[2][1];
        matrix[0][0] = cell;

        doReturn(matrix).when(section).getMatrix();
        doReturn(1).when(section).getMaxX();
        doReturn(0).when(section).getMaxY();

        Styler.styleSectionBottom(section, style);

        verify(section, times(1)).addCell(any(DataCell.class));
        verify(cell, times(1)).addStyle(style);
    }

    @Test
    public void styleBorder() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption styleLeft = StyleOption.SECTION_BORDER_LEFT;
        StyleOption styleRight = StyleOption.SECTION_BORDER_RIGHT;
        DataCell cellLeft = mock(DataCell.class);
        DataCell cellRight = mock(DataCell.class);
        DataCell[][] matrix = new DataCell[2][2];
        matrix[0][0] = cellLeft;
        matrix[1][0] = cellRight;

        doReturn(matrix).when(section).getMatrix();
        doReturn(1).when(section).getMaxX();
        doReturn(1).when(section).getMaxY();

        Styler.styleSectionBorder(section, styleLeft, styleRight);

        verify(section, times(2)).addCell(any(DataCell.class));
        verify(cellLeft, times(1)).addStyle(styleLeft);
        verify(cellRight, times(1)).addStyle(styleRight);
    }

    @Test
    public void extendHorizontally() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption styleLeft = StyleOption.SECTION_BORDER_LEFT;
        StyleOption styleRight = StyleOption.SECTION_BORDER_RIGHT;
        DataCell cellLeft = mock(DataCell.class);
        DataCell cellMiddle = mock(DataCell.class);
        DataCell cellRight = new DataCell("", 0, 0);
        DataCell cellRightSpy = spy(cellRight);
        DataCell[][] matrix = new DataCell[4][1];
        Set<StyleOption> styleSet = new HashSet<>();
        styleSet.add(styleRight);
        matrix[0][0] = cellLeft;
        matrix[1][0] = cellMiddle;
        matrix[3][0] = cellRightSpy;

        doReturn(matrix).when(section).getMatrix();
        doReturn(3).when(section).getMaxX();
        doReturn(0).when(section).getMaxY();
        doReturn(styleSet).when(cellMiddle).getStyles();

        Styler.extendStyleHorizontally(section, styleLeft, styleRight);

        verify(cellLeft, atMost(0)).addStyles(anySet());
        verify(cellMiddle, atMost(0)).addStyles(anySet());
        verify(cellRightSpy, times(1)).addStyles(anySet());
        Assert.assertTrue(cellRightSpy.getStyles().contains(styleRight));
        verify(section, times(1)).addCell(any(DataCell.class));
    }

    @Test
    public void extendHorizontallyNotFound() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption styleLeft = StyleOption.SECTION_BORDER_LEFT;
        StyleOption styleRight = StyleOption.SECTION_BORDER_RIGHT;
        DataCell cellLeft = mock(DataCell.class);
        DataCell cellMiddle = mock(DataCell.class);
        DataCell cellRight = mock(DataCell.class);
        DataCell[][] matrix = new DataCell[4][1];
        Set<StyleOption> styleSet = new HashSet<>();
        matrix[0][0] = cellLeft;
        matrix[1][0] = cellMiddle;
        matrix[3][0] = cellRight;

        doReturn(matrix).when(section).getMatrix();
        doReturn(3).when(section).getMaxX();
        doReturn(0).when(section).getMaxY();
        doReturn(styleSet).when(cellMiddle).getStyles();

        Styler.extendStyleHorizontally(section, styleLeft, styleRight);

        verify(cellLeft, atMost(0)).addStyles(anySet());
        verify(cellMiddle, atMost(0)).addStyles(anySet());
        verify(cellRight, times(0)).addStyles(anySet());
        verify(section, times(0)).addCell(any(DataCell.class));
    }

    @Test
    public void extendVertically() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption styleTop = StyleOption.SECTION_BORDER_LEFT;
        StyleOption styleBottom = StyleOption.SECTION_BORDER_RIGHT;
        DataCell cellTop = new DataCell("", 0, 0);
        DataCell cellTopSpy = spy(cellTop);
        DataCell cellMiddle = mock(DataCell.class);
        DataCell cellBottom = new DataCell("", 0, 0);
        DataCell cellBottomSpy = spy(cellBottom);
        DataCell[][] matrix = new DataCell[1][4];
        Set<StyleOption> styleSet = new HashSet<>();
        styleSet.add(styleBottom);
        matrix[0][0] = cellTopSpy;
        matrix[0][1] = cellMiddle;
        matrix[0][3] = cellBottomSpy;

        doReturn(matrix).when(section).getMatrix();
        doReturn(0).when(section).getMaxX();
        doReturn(3).when(section).getMaxY();
        doReturn(styleSet).when(cellMiddle).getStyles();

        Styler.extendStyleVertically(section, styleTop, styleBottom);

        verify(cellTopSpy, times(1)).addStyles(anySet());
        verify(cellMiddle, times(1)).addStyles(anySet());
        verify(cellBottomSpy, times(1)).addStyles(anySet());
        Assert.assertTrue(cellBottomSpy.getStyles().contains(styleBottom));
        Assert.assertTrue(cellTopSpy.getStyles().contains(styleBottom));
        verify(section, times(1)).addCell(any(DataCell.class));
    }

    @Test
    public void extendVerticallyNotFound() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption styleTop = StyleOption.SECTION_BORDER_LEFT;
        StyleOption styleBottom = StyleOption.SECTION_BORDER_RIGHT;
        DataCell cellTop = new DataCell("", 0, 0);
        DataCell cellTopSpy = spy(cellTop);
        DataCell cellMiddle = mock(DataCell.class);
        DataCell cellBottom = new DataCell("", 0, 0);
        DataCell cellBottomSpy = spy(cellBottom);
        DataCell[][] matrix = new DataCell[1][4];
        Set<StyleOption> styleSet = new HashSet<>();
        matrix[0][0] = cellTopSpy;
        matrix[0][1] = cellMiddle;
        matrix[0][3] = cellBottomSpy;

        doReturn(matrix).when(section).getMatrix();
        doReturn(0).when(section).getMaxX();
        doReturn(3).when(section).getMaxY();
        doReturn(styleSet).when(cellMiddle).getStyles();

        Styler.extendStyleVertically(section, styleTop, styleBottom);

        verify(cellTopSpy, atMost(0)).addStyles(anySet());
        verify(cellMiddle, atMost(0)).addStyles(anySet());
        verify(cellBottomSpy, atMost(0)).addStyles(anySet());
        verify(section, times(0)).addCell(any(DataCell.class));
    }

    /*
     * The style method could have much more thorough testing, but in reality it is useless. No matter how many test
     * there are, it is not possible to test if the spreadsheet has the correct appearance.
     */
    @Test
    public void styleNoStyles()
    {
        Styler styler = new Styler();
        DataCell dataCell = mock(DataCell.class);
        Cell cell = mock(Cell.class);
        Workbook workbook = mock(Workbook.class);
        CellStyle style = mock(CellStyle.class);
        Font font = mock(Font.class);

        doReturn(null).when(dataCell).getStyles();
        doReturn(style).when(workbook).createCellStyle();
        doReturn(font).when(workbook).createFont();

        styler.style(dataCell, cell, workbook);

        verify(dataCell).getStyles();
        verify(style, times(1)).setWrapText(true);
        verify(style, times(1)).setFont(any(Font.class));
        verify(style, times(1)).setVerticalAlignment(CellStyle.VERTICAL_TOP);
        verify(cell, times(1)).setCellStyle(style);
        verifyNoMoreInteractions(style);
        /* This is the only one that's important. */
        verifyNoMoreInteractions(cell);
        verifyNoMoreInteractions(dataCell);
    }

    @Test(expected = Exception.class)
    public void styleBottomNullMatrix() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption style = mock(StyleOption.class);

        doReturn(null).when(section).getMatrix();

        Styler.styleSectionBottom(section, style);

        verifyNoMoreInteractions(section);
    }

    @Test(expected = Exception.class)
    public void styleBorderNullMatrix() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption styleLeft = StyleOption.SECTION_BORDER_LEFT;
        StyleOption styleRight = StyleOption.SECTION_BORDER_RIGHT;

        doReturn(null).when(section).getMatrix();

        Styler.styleSectionBorder(section, styleLeft, styleRight);

        verifyNoMoreInteractions(section);
    }

    @Test(expected = Exception.class)
    public void extendHorizontallyNullMatrix() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption style = StyleOption.SECTION_BORDER_LEFT;

        doReturn(null).when(section).getMatrix();

        Styler.extendStyleHorizontally(section, style);

        verifyNoMoreInteractions(section);
    }

    @Test(expected = Exception.class)
    public void extendVerticallyNullMatrix() throws Exception
    {
        DataSection section = mock(DataSection.class);
        StyleOption style = StyleOption.SECTION_BORDER_LEFT;

        doReturn(null).when(section).getMatrix();

        Styler.extendStyleVertically(section, style);

        verifyNoMoreInteractions(section);
    }
}
