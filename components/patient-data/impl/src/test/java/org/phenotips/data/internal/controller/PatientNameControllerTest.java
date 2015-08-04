package org.phenotips.data.internal.controller;

import org.phenotips.data.PatientDataController;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;

/**
 * Test for the {@link PatientNameController} Component,
 * only the overridden abstract methods from {@link AbstractSimpleController} are tested here
 */
public class PatientNameControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(PatientNameController.class);

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals("patientName", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals("patient_name",
            ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result = ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertEquals(2, result.size());
        Assert.assertThat(result, hasItem("first_name"));
        Assert.assertThat(result, hasItem("last_name"));
    }
}
