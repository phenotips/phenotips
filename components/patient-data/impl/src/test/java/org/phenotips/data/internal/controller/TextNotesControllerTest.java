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
 * Test for the {@link TextNotesController} Component, only the overridden methods from {@link AbstractSimpleController}
 * are tested here
 */
public class TextNotesControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(TextNotesController.class);

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals("notes", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals("notes",
            ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result = ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertEquals(5, result.size());
        Assert.assertThat(result, Matchers.hasItem("indication_for_referral"));
        Assert.assertThat(result, Matchers.hasItem("family_history"));
        Assert.assertThat(result, Matchers.hasItem("prenatal_development"));
        Assert.assertThat(result, Matchers.hasItem("medical_history"));
        Assert.assertThat(result, Matchers.hasItem("diagnosis_notes"));
    }
}
