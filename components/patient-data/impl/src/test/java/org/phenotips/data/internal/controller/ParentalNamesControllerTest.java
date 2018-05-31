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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for the {@link ParentalNamesController} Component, only the overridden methods from
 * {@link AbstractSimpleController} are tested here.
 */
public class ParentalNamesControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(ParentalNamesController.class);

    private PatientDataController<String> component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        this.component = this.mocker.getComponentUnderTest();
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals("parentalNames", this.component.getName());
    }

    @Test
    public void checkGetJsonPropertyName()
    {
        Assert.assertEquals("parental_names",
            ((AbstractSimpleController) this.component).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties()
    {
        List<String> result = ((AbstractSimpleController) this.component).getProperties();

        Assert.assertEquals(4, result.size());
        Assert.assertThat(result, Matchers.hasItem("maternal_last_name"));
        Assert.assertThat(result, Matchers.hasItem("maternal_first_name"));
        Assert.assertThat(result, Matchers.hasItem("paternal_last_name"));
        Assert.assertThat(result, Matchers.hasItem("paternal_first_name"));
    }
}
