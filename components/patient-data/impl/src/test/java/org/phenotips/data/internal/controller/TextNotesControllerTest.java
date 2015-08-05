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
 * Test for the {@link TextNotesController} Component,
 * only the overridden methods from {@link AbstractSimpleController} are tested here
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
        Assert.assertThat(result, hasItem("indication_for_referral"));
        Assert.assertThat(result, hasItem("family_history"));
        Assert.assertThat(result, hasItem("prenatal_development"));
        Assert.assertThat(result, hasItem("medical_history"));
        Assert.assertThat(result, hasItem("diagnosis_notes"));
    }
}
