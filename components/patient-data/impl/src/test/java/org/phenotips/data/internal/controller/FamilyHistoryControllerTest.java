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
 * Test for the {@link FamilyHistoryController} Component,
 * only the overridden methods from {@link AbstractComplexController} are tested here
 */
public class FamilyHistoryControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
            new MockitoComponentMockingRule<PatientDataController<Integer>>(FamilyHistoryController.class);

    private static final String CONSANGUINITY = "consanguinity";

    private static final String MISCARRIAGES = "miscarriages";

    private static final String AFFECTED_RELATIVES = "affectedRelatives";

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals("familyHistory", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals("family_history",
                ((AbstractComplexController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result =
                ((AbstractComplexController<Integer>) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertEquals(3, result.size());
        Assert.assertThat(result, Matchers.hasItem(CONSANGUINITY));
        Assert.assertThat(result, Matchers.hasItem(MISCARRIAGES));
        Assert.assertThat(result, Matchers.hasItem(AFFECTED_RELATIVES));
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        List<String> result =
                ((AbstractComplexController<Integer>) this.mocker.getComponentUnderTest()).getBooleanFields();

        Assert.assertEquals(3, result.size());
        Assert.assertThat(result, Matchers.hasItem(CONSANGUINITY));
        Assert.assertThat(result, Matchers.hasItem(MISCARRIAGES));
        Assert.assertThat(result, Matchers.hasItem(AFFECTED_RELATIVES));
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(((AbstractComplexController) this.mocker.getComponentUnderTest()).getCodeFields().isEmpty());
    }
}
