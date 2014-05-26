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

import java.util.HashSet;
import java.util.Set;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

public class DataCell
{
    public String value;

    public Short x = 0;

    public Short y = 0;

    public Integer dataRow = 0;

    public Integer patientIndex = 0;

    /** These are the number of cells that this cell should be merged with */
    public Integer xMerge = 0;

    private Integer yMerge = null;

    public Boolean bottomOfDataRow = false;

    private CellStyle style = null; //FIXME Setter method

    public Set<String> styleRequests = new HashSet<String>();

    public CellStyle getStyle(Workbook wBook)
    {
        if (style == null) {
            style = wBook.createCellStyle();
        }
        return style;
    }

    public void setYMerge(Integer yMerge)
    {
        if (this.yMerge == null || this.yMerge > yMerge) {
            this.yMerge = yMerge;
        }
    }

    public Short getXBoundry()
    {
        if (xMerge == 0) {
            return null;
        }
        return (short) (x + xMerge);
    }

    public Short getYBoundry()
    {
        if (yMerge == null || yMerge == 0) {
            return null;
        }
        return (short) (y + yMerge);
    }

    public DataCell(String v, Short x, Short y, Integer patientIndex, Integer row)
    {
        value = v;
        this.x = x;
        this.y = y;
        this.patientIndex = patientIndex;
        this.dataRow = row;
    }

    public DataCell(String v, Short x, Short y, Integer row)
    {
        value = v;
        this.x = x;
        this.y = y;
        this.dataRow = row;
    }


    public DataCell(String v, Short x, Short y)
    {
        value = v;
        this.x = x;
        this.y = y;
    }

    public DataCell(String v)
    {
        value = v;
    }
}

