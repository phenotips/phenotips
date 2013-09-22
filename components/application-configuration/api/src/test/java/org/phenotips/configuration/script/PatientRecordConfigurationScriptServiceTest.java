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
package org.phenotips.configuration.script;

import org.phenotips.configuration.PatientRecordConfiguration;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientRecordConfiguration} script service, {@link PatientRecordConfigurationScriptService}.
 * 
 * @version $Id$
 */
public class PatientRecordConfigurationScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<ScriptService> mocker = new MockitoComponentMockingRule<ScriptService>(
        PatientRecordConfigurationScriptService.class);

    /** Basic tests for {@link PatientRecordConfigurationScriptService#getEnabledFieldNames()}. */
    @Test
    public void getEnabledFieldNames() throws ComponentLookupException
    {
        List<String> expectedFields = new LinkedList<String>();
        expectedFields.add("external_id");
        expectedFields.add("first_name");
        expectedFields.add("last_name");
        expectedFields.add("gender");
        expectedFields.add("gestation");
        expectedFields.add("unaffected");
        expectedFields.add("phenotype");
        expectedFields.add("negative_phenotype");
        PatientRecordConfiguration config = this.mocker.getInstance(PatientRecordConfiguration.class);
        when(config.getEnabledFieldNames()).thenReturn(expectedFields);
        Assert.assertEquals(expectedFields,
            ((PatientRecordConfigurationScriptService) this.mocker.getComponentUnderTest()).getEnabledFieldNames());
    }

    /** {@link PatientRecordConfigurationScriptService#getEnabledFieldNames()} catches exception. */
    @Test
    public void getEnabledFieldNamesWithException() throws ComponentLookupException
    {
        PatientRecordConfiguration config = this.mocker.getInstance(PatientRecordConfiguration.class);
        when(config.getEnabledFieldNames()).thenThrow(new NullPointerException());
        Assert.assertTrue(((PatientRecordConfigurationScriptService) this.mocker.getComponentUnderTest())
            .getEnabledFieldNames().isEmpty());
    }

    /** Basic tests for {@link PatientRecordConfigurationScriptService#getAllFieldNames()}. */
    @Test
    public void getAllFieldNames() throws ComponentLookupException
    {
        List<String> expectedFields = new LinkedList<String>();
        expectedFields.add("external_id");
        expectedFields.add("first_name");
        expectedFields.add("last_name");
        expectedFields.add("gender");
        expectedFields.add("gestation");
        expectedFields.add("unaffected");
        expectedFields.add("phenotype");
        expectedFields.add("negative_phenotype");
        PatientRecordConfiguration config = this.mocker.getInstance(PatientRecordConfiguration.class);
        when(config.getAllFieldNames()).thenReturn(expectedFields);
        Assert.assertEquals(expectedFields,
            ((PatientRecordConfigurationScriptService) this.mocker.getComponentUnderTest()).getAllFieldNames());
    }

    /** {@link PatientRecordConfigurationScriptService#getAllFieldNames()} catches exception. */
    @Test
    public void getAllFieldNamesWithException() throws ComponentLookupException
    {
        PatientRecordConfiguration config = this.mocker.getInstance(PatientRecordConfiguration.class);
        when(config.getAllFieldNames()).thenThrow(new NullPointerException());
        Assert.assertTrue(((PatientRecordConfigurationScriptService) this.mocker.getComponentUnderTest())
            .getAllFieldNames().isEmpty());
    }

    /** Basic tests for {@link PatientRecordConfigurationScriptService#getDateOfBirthFormat()}. */
    @Test
    public void getDateOfBirthFormat() throws ComponentLookupException
    {
        PatientRecordConfiguration config = this.mocker.getInstance(PatientRecordConfiguration.class);
        when(config.getDateOfBirthFormat()).thenReturn("MMMM yyyy");
        Assert.assertEquals("MMMM yyyy",
            ((PatientRecordConfigurationScriptService) this.mocker.getComponentUnderTest()).getDateOfBirthFormat());
    }
}
