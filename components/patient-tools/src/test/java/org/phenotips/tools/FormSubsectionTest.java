package org.phenotips.tools;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import static org.mockito.Mockito.mock;

public class FormSubsectionTest {

    private FormSubsection testSubsection;

    @Before
    public void setUp(){
        testSubsection = new FormSubsection("title", "type");
    }

    @Test
    public void testSubsectionDisplay(){
        Assert.assertEquals(testSubsection.display(DisplayMode.Edit, new String[]{ "phenotype", "negative_phenotype" }), "");
        FormField testFormField = mock(FormField.class);
        testSubsection.addElement(testFormField);
        Assert.assertNotNull(testSubsection.display(DisplayMode.Edit, new String[]{ "phenotype", "negative_phenotype" }));
    }

}
