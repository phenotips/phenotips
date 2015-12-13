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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class DataCellTest
{
    @Test
    public void mergeTest()
    {
        DataCell cell = new DataCell("", 0, 0);
        cell.addMergeX();

        Assert.assertEquals((Integer) 1, cell.getMergeX());

        cell.addStyle(StyleOption.FEATURE_SEPARATOR);
        Collection<DataCell> generated = cell.generateMergedCells();
        Assert.assertEquals(1, generated.size());
        for (DataCell generatedCell : generated) {
            Assert.assertTrue(generatedCell.isChild());
            Assert.assertTrue(generatedCell.getStyles().contains(StyleOption.FEATURE_SEPARATOR));
        }
    }

    @Test
    public void addStyles()
    {
        DataCell cell = new DataCell("", 0, 0);
        List<StyleOption> styleList = new LinkedList<>();
        styleList.add(StyleOption.HEADER);
        styleList.add(StyleOption.HEADER);
        cell.addStyle(StyleOption.FEATURE_SEPARATOR);
        cell.addStyle(StyleOption.FEATURE_SEPARATOR);

        Assert.assertTrue(cell.getStyles().size() == 1);

        cell.addStyles(styleList);
        Assert.assertTrue(cell.getStyles().size() == 2);
    }

    @Test
    public void linesTest()
    {
        DataCell cell = new DataCell("l\nl\n", 0, 0);
        cell.setMultiline();
        Assert.assertEquals((Integer) 2, cell.getNumberOfLines());

        cell = new DataCell("l\nl\nl", 0, 0);
        cell.setMultiline();
        Assert.assertEquals((Integer) 3, cell.getNumberOfLines());

        cell = new DataCell(null, 0, 0);
        cell.setMultiline();
    }

    @Test
    public void zeroMerge()
    {
        DataCell cell = new DataCell("", 0, 0);
        Assert.assertTrue(cell.generateMergedCells().isEmpty());
    }
}
