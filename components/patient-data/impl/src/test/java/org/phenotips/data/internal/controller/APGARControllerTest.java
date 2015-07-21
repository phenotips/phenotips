package org.phenotips.data.internal.controller;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.obo2solr.maps.IntegerMap;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link APGARController} Component,
 * implementation of the {@link org.phenotips.data.PatientDataController} interface
 */
public class APGARControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController> mocker =
            new MockitoComponentMockingRule<PatientDataController>(APGARController.class);

    private static final String DATA_NAME = "apgar";

    private Logger logger;

    private DocumentAccessBridge documentAccessBridge;

    private APGARController controller;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.controller = (APGARController)this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<Integer> result = this.controller.load(this.patient);

        verify(this.logger).error("Could not find requested document or some unforeseen"
                        + " error has occurred during controller loading ",
                PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
        Assert.assertNull(result);
    }

    @Test
    public void loadDoesNotReturnNullIntegers()
    {
        doReturn(null).when(this.data).getStringValue(anyString());

        PatientData<Integer> result = this.controller.load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadDoesNotReturnNonIntegerStrings()
    {
        doReturn("STRING").when(this.data).getStringValue(anyString());

        PatientData<Integer> result = this.controller.load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadReturnsExpectedIntegers()
    {
        doReturn("1234").when(this.data).getStringValue("apgar1");
        doReturn("4321").when(this.data).getStringValue("apgar5");

        PatientData<Integer> result = this.controller.load(this.patient);

        Assert.assertEquals(Integer.valueOf(1234), result.get("apgar1"));
        Assert.assertEquals(Integer.valueOf(4321), result.get("apgar5"));
        Assert.assertEquals(2, result.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void saveIsUnsupported(){
        this.controller.save(this.patient);
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add("apgar1");
        selectedFields.add("apgar5");
        
        this.controller.writeJSON(this.patient, json, selectedFields);
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.controller.getName());
    }
}
