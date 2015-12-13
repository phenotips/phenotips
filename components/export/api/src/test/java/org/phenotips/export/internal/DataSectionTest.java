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

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataSectionTest
{
    /**
     * Adds a cell, finalizes, and adds two other cells (one within the matrix, one outside), and then adds a cell that
     * overwrites an existing cell.
     */
    @Test
    public void addCellFinalizeAddAgainOverwrite() throws Exception
    {
        Integer xOne = 1;
        Integer yOne = 3;
        DataSection section = new DataSection();
        DataCell cellOne = mock(DataCell.class);
        when(cellOne.getX()).thenReturn(xOne);
        when(cellOne.getY()).thenReturn(yOne);

        section.addCell(cellOne);
        Assert.assertNull(section.getMatrix());
        Assert.assertThat(section.getCellList().isEmpty(), is(false));
        Assert.assertThat(section.getCellList().contains(cellOne), is(true));
        Assert.assertEquals(xOne, section.getMaxX());
        Assert.assertEquals(yOne, section.getMaxY());

        section.finalizeToMatrix();
        Assert.assertNotNull(section.getMatrix());
        Assert.assertEquals(xOne + 1, section.getMatrix().length);
        Assert.assertEquals(yOne + 1, section.getMatrix()[0].length);
        Assert.assertEquals(cellOne, section.getMatrix()[xOne][yOne]);

        /* This cell should not end up in the matrix. */
        Integer xTwo = 2;
        Integer yTwo = 1;
        DataCell cellTwo = mock(DataCell.class);
        when(cellTwo.getX()).thenReturn(xTwo);
        when(cellTwo.getY()).thenReturn(yTwo);

        section.addCell(cellTwo);
        Assert.assertEquals(xTwo, section.getMaxX());
        Assert.assertEquals(yOne, section.getMaxY());
        /* Should contain both cells still. */
        Assert.assertThat(section.getCellList().contains(cellOne), is(true));
        Assert.assertThat(section.getCellList().contains(cellTwo), is(true));

        /*
         * One cell should overwrite another cell silently, since it could happen routinely, and is up to the assemblers
         * and converters to make sure that it all cells are positioned correctly. However, the cell that was added last
         * should be present in the matrix.
         */
        DataCell cellThree = mock(DataCell.class);
        when(cellThree.getX()).thenReturn(xTwo);
        when(cellThree.getY()).thenReturn(yTwo);

        section.addCell(cellThree);
        section.finalizeToMatrix();
        Assert.assertEquals(cellThree, section.getMatrix()[xTwo][yTwo]);

        /* If the cell has it's coordinates changed and re-added, it should not have a duplicate in cell list. */
        Integer newX = 0;
        Integer newY = 0;
        Assert.assertEquals(xTwo, section.getMaxX());
        Assert.assertEquals(yOne, section.getMaxY());
        when(cellThree.getX()).thenReturn(newX);
        when(cellThree.getY()).thenReturn(newY);
        section.addCell(cellThree);
        Assert.assertEquals(3, section.getCellList().size());
        /* Since the coodinates are within the current matrix size, the new cell should be inserted into the matrix. */
        Assert.assertEquals(cellThree, section.getMatrix()[newX][newY]);
    }

    @Test(expected = Exception.class)
    public void emptyMerge() throws Exception
    {
        (new DataSection()).mergeX();
    }

    @Test
    public void merge() throws Exception
    {
        Integer xOne = 2;
        Integer yOne = 3;
        Integer origin = 0;
        DataSection section = new DataSection();
        DataCell cellOne = mock(DataCell.class);
        DataCell cellTwo = mock(DataCell.class);
        when(cellOne.getX()).thenReturn(origin);
        when(cellOne.getY()).thenReturn(origin);
        when(cellTwo.getX()).thenReturn(xOne);
        when(cellTwo.getY()).thenReturn(yOne);

        section.addCell(cellOne);
        section.addCell(cellTwo);
        section.finalizeToMatrix();
        section.mergeX();

        Integer minus = origin == yOne ? 1 : 0;
        verify(cellOne, times(xOne - origin - minus)).addMergeX();
    }

    @Test
    public void empty() throws Exception
    {
        (new DataSection()).finalizeToMatrix();
    }
}
