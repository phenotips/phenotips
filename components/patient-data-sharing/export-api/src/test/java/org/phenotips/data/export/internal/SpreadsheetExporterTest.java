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

import org.phenotips.data.export.OLD_INTERNAL.SpreadsheetExporter;

import org.junit.Before;

/**
 * FIXME
 *
 * @version $Id$
 * @since 1.0M11
 */

//xlsx
public class SpreadsheetExporterTest
{
    SpreadsheetExporter testClass;

    String[] testEnabledFields = { "First", "s,", "3" };

    @Before
    public void setUp()
    {
        testClass = new SpreadsheetExporter();
    }

    /*
    @Test
    public void export() throws IOException
    {

        testClass.export(testEnabledFields, );
    }

    @Test
    public void exportWithNull() throws IOException
    {
        testClass.export(null);
    }

    @Test
    public void exportWFileErrors() throws IOException
    {
        SpreadsheetExporter spyClass = Mockito.spy(testClass);
        FileOutputStream wFile = Mockito.mock(FileOutputStream.class);
        spyClass.wFile = wFile;
        Mockito.doThrow(IOException.class).when(spyClass).createBlankWorkbook();
        spyClass.export(testEnabledFields);
        Mockito.doThrow(IOException.class).when(wFile).close();
        spyClass.export(testEnabledFields);
    }

    @Test
    public void exportWFileNull() throws IOException
    {
        SpreadsheetExporter spyClass = Mockito.spy(testClass);
        spyClass.wFile = null;
        Mockito.doThrow(IOException.class).when(spyClass).createBlankWorkbook();
        spyClass.export(testEnabledFields);
    }
    */
}
