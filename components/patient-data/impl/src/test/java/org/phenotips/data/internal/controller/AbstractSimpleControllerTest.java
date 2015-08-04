package org.phenotips.data.internal.controller;

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.sf.json.JSONObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * test
 */
public class AbstractSimpleControllerTest
{

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

    private final String DATA_NAME = AbstractSimpleControllerTestImplementation.DATA_NAME;

    private final String PROPERTY_1 = AbstractSimpleControllerTestImplementation.PROPERTY_1;

    private final String PROPERTY_2 = AbstractSimpleControllerTestImplementation.PROPERTY_2;

    private final String PROPERTY_3 = AbstractSimpleControllerTestImplementation.PROPERTY_3;

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
        doReturn(datum1).when(this.data).getStringValue(PROPERTY_1);
        doReturn(datum2).when(this.data).getStringValue(PROPERTY_2);
        doReturn(datum3).when(this.data).getStringValue(PROPERTY_3);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(datum1, result.get(PROPERTY_1));
        Assert.assertEquals(datum2, result.get(PROPERTY_2));
        Assert.assertEquals(datum3, result.get(PROPERTY_3));
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void loadIgnoresBlankFields() throws ComponentLookupException
    {
        String datum = "datum";
        doReturn(" ").when(this.data).getStringValue(PROPERTY_1);
        doReturn(null).when(this.data).getStringValue(PROPERTY_2);
        doReturn(datum).when(this.data).getStringValue(PROPERTY_3);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(datum, result.get(PROPERTY_3));
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
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
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
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(this.DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.xWiki).saveDocument(this.doc, "Updated test from JSON", true, this.xcontext);
        verify(this.data).setStringValue(PROPERTY_1, "datum1");
        verify(this.data).setStringValue(PROPERTY_2, "datum2");
        verify(this.data).setStringValue(PROPERTY_3, "datum3");
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
    public void writeJSONAddsContainerWithAllValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsContainerWithAllValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_2);
        selectedFields.add(PROPERTY_3);
        

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_3);


        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
        Assert.assertNull(container.get(PROPERTY_2));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsContainerWithAllValuesWhenSelectedFieldsNull()
        throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();


        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void readJSONReturnsNullWhenPassedEmptyJSONObject() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsNullWhenDataContainerIsNotAJSONObject() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, "datum");
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void readJSONReadsAllProperties() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        JSONObject container = new JSONObject();
        container.put(PROPERTY_1, "datum1");
        container.put(PROPERTY_2, "datum2");
        container.put(PROPERTY_3, "datum3");
        json.put(DATA_NAME, container);

        PatientData<String> result = this.mocker.getComponentUnderTest().readJSON(json);

        Assert.assertTrue(result.isNamed());
        Assert.assertEquals(DATA_NAME, result.getName());
        Assert.assertEquals("datum1", result.get(PROPERTY_1));
        Assert.assertEquals("datum2", result.get(PROPERTY_2));
        Assert.assertEquals("datum3", result.get(PROPERTY_3));
    }
}
