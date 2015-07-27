package org.phenotips.data.internal.controller;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.*;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * test
 */
public class AbstractSimpleControllerTest {

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
            new MockitoComponentMockingRule<PatientDataController<String>>(AbstractSimpleControllerTestImplementation.class);

    protected DocumentAccessBridge documentAccessBridge;

    protected XWikiContext xcontext;

    @Mock
    protected XWiki xWiki;

    @Mock
    protected Patient patient;

    @Mock
    protected XWikiDocument doc;

    @Mock
    protected BaseObject data;

    private final String DATA_NAME = "test";

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xcontext = provider.get();
        doReturn(this.xWiki).when(this.xcontext).getWiki();

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", (String)null);
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen"
                        + " error has occurred during controller loading ",
                PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAllData() throws ComponentLookupException
    {
        String datum1 = "datum2";
        String datum2 = "datum2";
        String datum3 = "datum3";
        doReturn(datum1).when(this.data).getStringValue("property1");
        doReturn(datum2).when(this.data).getStringValue("property2");
        doReturn(datum3).when(this.data).getStringValue("property3");

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(datum1, result.get("property1"));
        Assert.assertEquals(datum2, result.get("property2"));
        Assert.assertEquals(datum3, result.get("property3"));
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void loadIgnoresBlankFields() throws ComponentLookupException
    {
        String datum = "datum";
        doReturn(" ").when(this.data).getStringValue("property1");
        doReturn(null).when(this.data).getStringValue("property2");
        doReturn(datum).when(this.data).getStringValue("property3");

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(datum, result.get("property3"));
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void saveCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        mocker.getComponentUnderTest().save(this.patient);

        verify(this.mocker.getMockedLogger()).error("Failed to save {}: [{}]", this.DATA_NAME, null);
    }

    @Test
    public void saveCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        mocker.getComponentUnderTest().save(this.patient);

        verify(mocker.getMockedLogger()).error("Failed to save {}: [{}]", this.DATA_NAME,
                PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
    }

    @Test
    public void saveCatchesExceptionFromSaveDocument() throws XWikiException, ComponentLookupException
    {
        XWikiException exception = new XWikiException();
        doThrow(exception).when(this.xWiki).saveDocument(any(XWikiDocument.class),
                anyString(), anyBoolean(), any(XWikiContext.class));
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("property1", "datum1");
        map.put("property2", "datum2");
        map.put("property3", "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(this.DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.xWiki).saveDocument(any(XWikiDocument.class),
                anyString(), anyBoolean(), any(XWikiContext.class));
        verify(this.mocker.getMockedLogger()).error("Failed to save {}: [{}]", this.DATA_NAME, exception.getMessage());
    }

    @Test
    public void saveReturnsWithoutSavingWhenDataIsNotKeyValueBased() throws ComponentLookupException, XWikiException
    {
        PatientData<String> patientData = new SimpleValuePatientData<String>(this.DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(this.DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data, never()).setStringValue(anyString(), anyString());
        verify(this.xWiki, never()).saveDocument(any(XWikiDocument.class),
                anyString(), anyBoolean(), any(XWikiContext.class));
    }

    @Test
    public void saveSetsAllFields() throws XWikiException, ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("property1", "datum1");
        map.put("property2", "datum2");
        map.put("property3", "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(this.DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.xWiki).saveDocument(this.doc, "Updated test from JSON", true, this.xcontext);
        verify(this.data).setStringValue("property1", "datum1");
        verify(this.data).setStringValue("property2", "datum2");
        verify(this.data).setStringValue("property3", "datum3");
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void readJSONEmptyJsonReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }
}
