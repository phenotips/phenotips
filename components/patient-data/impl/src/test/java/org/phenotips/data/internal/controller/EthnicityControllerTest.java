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
package org.phenotips.data.internal.controller;

import org.phenotips.data.PatientDataController;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for the {@link EthnicityController} Component,
 * only the overridden methods from {@link AbstractComplexController} are tested here
 */
public class EthnicityControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<List<String>>> mocker =
        new MockitoComponentMockingRule<PatientDataController<List<String>>>(EthnicityController.class);

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals("ethnicity", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals("ethnicity",
            ((AbstractComplexController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result =
            ((AbstractComplexController<List<String>>) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertEquals(2, result.size());
        Assert.assertThat(result, Matchers.hasItem("maternal_ethnicity"));
        Assert.assertThat(result, Matchers.hasItem("paternal_ethnicity"));
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController) this.mocker.getComponentUnderTest()).getBooleanFields().isEmpty());
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(((AbstractComplexController) this.mocker.getComponentUnderTest()).getCodeFields().isEmpty());
    }
}
