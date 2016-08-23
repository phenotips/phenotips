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
package org.phenotips.measurements.internal.controller;

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;
import org.phenotips.measurements.MeasurementHandler;
import org.phenotips.measurements.data.MeasurementEntry;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link MeasurementsController} Component, only the overridden methods from {@link PatientDataController}
 * are tested here
 */
public class MeasurementsControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController<MeasurementEntry>> mocker =
        new MockitoComponentMockingRule<PatientDataController<MeasurementEntry>>(MeasurementsController.class);

    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject obj1;

    @Mock
    private BaseObject obj2;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiContext xcontext;

    private DocumentReferenceResolver<String> stringResolver;

    @Mock
    private MeasurementHandler footHandler;

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mProvider;

    private List<BaseObject> measurementXWikiObjects;

    private static final String MEASUREMENTS_STRING = "measurements";

    private static final String CONTROLLER_NAME = MEASUREMENTS_STRING;

    private static final String MEASUREMENT_ENABLING_FIELD_NAME = MEASUREMENTS_STRING;

    private static final DocumentReference MEASUREMENTS_CLASS = new DocumentReference("xwiki", "PhenoTips",
        "MeasurementsClass");

    private static final String DATE_KEY = "date";

    private static final String AGE_KEY = "age";

    private static final String TYPE_KEY = "type";

    private static final String SIDE_KEY = "side";

    private static final String VALUE_KEY = "value";

    private static final String UNIT_KEY = "unit";

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);

        this.measurementXWikiObjects = new LinkedList<>();
        this.measurementXWikiObjects.add(this.obj1);
        this.measurementXWikiObjects.add(this.obj2);
        doReturn(this.measurementXWikiObjects).when(this.doc).getXObjects(any(EntityReference.class));

        this.mocker.registerComponent(MeasurementHandler.class, "foot", this.footHandler);
        when(this.footHandler.getName()).thenReturn("foot");
        when(this.footHandler.getUnit()).thenReturn("cm");
        this.mocker.registerComponent(ComponentManager.class, "context", this.mocker);

        this.stringResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    public void getNameTest() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME, this.mocker.getComponentUnderTest().getName());
    }

    // ------------Load Tests------------

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        Exception exception = new Exception();
        doThrow(exception).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen "
            + "error has occurred during measurements controller loading ", exception.getMessage());
    }

    @Test
    public void loadReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObjects(any(EntityReference.class));
        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullFields() throws ComponentLookupException
    {
        BaseObject obj = mock(BaseObject.class);
        doReturn(null).when(obj).getField(anyString());
        this.measurementXWikiObjects.add(obj);

        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNotNull(result);
    }

    @Test
    public void typeThrowsComponentLookupException() throws ComponentLookupException
    {
        String age = "67";
        String type = "armspan";
        String side = "l";
        Double value = 35.2;

        when(this.obj1.getStringValue(AGE_KEY)).thenReturn(age);
        when(this.obj1.getStringValue(TYPE_KEY)).thenReturn(type);
        when(this.obj1.getStringValue(SIDE_KEY)).thenReturn(side);
        when(this.obj1.getDoubleValue(VALUE_KEY)).thenReturn(value);

        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNotNull(result);
    }

    @Test
    public void typeIsEmptyTest() throws Exception
    {
        Date date = new Date(1999 - 03 - 03);
        String age = "67";
        String side = "l";
        Double value = 35.2;

        when(this.obj1.getStringValue(AGE_KEY)).thenReturn(age);
        when(this.obj1.getDateValue(DATE_KEY)).thenReturn(date);
        when(this.obj1.getStringValue(SIDE_KEY)).thenReturn(side);
        when(this.obj1.getDoubleValue(VALUE_KEY)).thenReturn(value);
        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void dateMissingTest() throws Exception
    {
        String age = "67";
        String type = "foot";
        String side = "l";
        Double value = 35.2;
        when(this.obj1.getStringValue(AGE_KEY)).thenReturn(age);
        when(this.obj1.getStringValue(TYPE_KEY)).thenReturn(type);
        when(this.obj1.getStringValue(SIDE_KEY)).thenReturn(side);
        when(this.obj1.getDoubleValue(VALUE_KEY)).thenReturn(value);
        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().load(this.patient);

        MeasurementEntry m = result.get(0);
        Assert.assertNull(m.getDate());
    }

    @Test
    public void loadTest() throws Exception, ComponentLookupException
    {

        String age = "67";
        Date date = new Date(1999 - 03 - 03);
        String type = "foot";
        String side = "l";
        Double value = 35.2;

        when(this.obj1.getStringValue(AGE_KEY)).thenReturn(age);
        when(this.obj1.getDateValue(DATE_KEY)).thenReturn(date);
        when(this.obj1.getStringValue(TYPE_KEY)).thenReturn(type);
        when(this.obj1.getStringValue(SIDE_KEY)).thenReturn(side);
        when(this.obj1.getDoubleValue(VALUE_KEY)).thenReturn(value);

        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(1, result.size());
        MeasurementEntry result1 = result.get(0);
        Assert.assertEquals("l", result1.getSide());
        Assert.assertEquals("foot", result1.getType());
        Assert.assertEquals("cm", result1.getUnits());
        Assert.assertEquals(0.0001, 35.2, result1.getValue());
        Assert.assertEquals(date, result1.getDate());
        Assert.assertEquals("67", result1.getAge());
    }

    // ------------Write Tests------------

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(MEASUREMENT_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONWithNullFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void gettingHandlerCatchesException() throws ComponentLookupException
    {
        List<MeasurementEntry> internalList = new LinkedList<>();
        String age = "67";
        Date date = new Date(1999 - 03 - 03);
        String type = "foot";
        String side = "l";
        Double value = 35.2;
        String units = "cm";

        MeasurementEntry entry = new MeasurementEntry(date, age, type, side, value, units);
        internalList.add(entry);

        PatientData<MeasurementEntry> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);

        JSONObject json = new JSONObject();

        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(MEASUREMENT_ENABLING_FIELD_NAME);

        // Does not catch getHandler() ComponentLookupException, to be fixed
        when(this.mProvider.get()).thenReturn(this.cm);
        doThrow(ComponentLookupException.class).when(this.cm).getInstanceList(MeasurementHandler.class);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);
    }

    @Test
    public void writeJSONhasNext() throws ComponentLookupException
    {
        List<MeasurementEntry> internalList = new LinkedList<>();
        String age = "67";
        Date date = new Date(1999 - 03 - 03);
        String type = "foot";
        String side = "l";
        Double value = 35.2;
        String units = "cm";
        MeasurementEntry entry = new MeasurementEntry(date, age, type, side, value, units);
        internalList.add(entry);

        Assert.assertEquals("67", entry.getAge());

        PatientData<MeasurementEntry> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);

        JSONObject json = new JSONObject();

        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(MEASUREMENT_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
    }

    @Test
    public void writeWithNullValues() throws ComponentLookupException
    {
        List<MeasurementEntry> internalList = new LinkedList<>();
        String age = null;
        Date date = new Date(1999 - 03 - 03);
        String type = null;
        String side = "l";
        Double value = null;
        String units = "cm";
        MeasurementEntry entry = new MeasurementEntry(date, age, type, side, value, units);
        internalList.add(entry);

        Assert.assertNull(entry.getAge());
        Assert.assertNull(entry.getType());

        PatientData<MeasurementEntry> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);

        JSONObject json = new JSONObject();

        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(MEASUREMENT_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
    }

    @Test
    public void writeJSONWithSexDataTest() throws ComponentLookupException
    {
        List<MeasurementEntry> internalList = new LinkedList<>();
        Date date = new Date(1999 - 03 - 03);
        String age = "2y";
        String type = "foot";
        String side = "l";
        Double value = 3.5;
        String units = "cm";
        String male = "M";

        MeasurementEntry entry = new MeasurementEntry(date, age, type, side, value, units);
        internalList.add(entry);
        PatientData<MeasurementEntry> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);

        PatientData<String> sexData = new SimpleValuePatientData<>("sex", male);
        when(this.patient.<String>getData("sex")).thenReturn(sexData);

        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(CONTROLLER_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);
    }

    // ------------Read Tests------------

    @Test
    public void readWithEmptyDataDoesNothing() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, new JSONArray());
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void readWithNullJsonDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(null));
    }

    @Test
    public void readWithNoDataDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readWithWrongDataDoesNothing() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, "No");
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void readWorksCorrectly() throws ComponentLookupException
    {
        JSONArray data = new JSONArray();
        JSONObject item = new JSONObject();
        item.put(DATE_KEY, "1993-01-02");
        item.put(AGE_KEY, 13);
        item.put(TYPE_KEY, "foot");
        item.put(SIDE_KEY, "");
        item.put(VALUE_KEY, 23.5);
        item.put(UNIT_KEY, "cm");
        data.put(item);
        item = new JSONObject();
        item.put(DATE_KEY, "1994-01-02");
        item.put(AGE_KEY, 13);
        item.put(TYPE_KEY, "weight");
        item.put(SIDE_KEY, "");
        item.put(VALUE_KEY, 23.5);
        item.put(UNIT_KEY, "kg");
        data.put(item);
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, data);
        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.isIndexed());
    }

    @Test
    public void jsonEntryReturnsNull() throws ComponentLookupException
    {
        JSONArray data = new JSONArray();
        JSONObject item = new JSONObject();
        item.put(AGE_KEY, 13);
        item.put(SIDE_KEY, "");
        item.put(VALUE_KEY, "2");
        item.put(UNIT_KEY, "cm");
        data.put(item);
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, data);
        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNull(result);
    }

    @Test
    public void DuplicateDateTest() throws ComponentLookupException
    {
        JSONArray data = new JSONArray();
        JSONObject item = new JSONObject();
        item.put(DATE_KEY, "1993-01-02");
        item.put(AGE_KEY, 13);
        item.put(TYPE_KEY, "foot");
        item.put(SIDE_KEY, "");
        item.put(VALUE_KEY, 23.5);
        item.put(UNIT_KEY, "cm");
        data.put(item);
        item = new JSONObject();
        item.put(DATE_KEY, "1993-01-02");
        item.put(AGE_KEY, 13);
        item.put(TYPE_KEY, "weight");
        item.put(SIDE_KEY, "");
        item.put(VALUE_KEY, 23.5);
        item.put(UNIT_KEY, "kg");
        data.put(item);
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, data);
        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.isIndexed());
    }

    @Test
    public void DuplicateTest() throws ComponentLookupException
    {
        JSONArray data = new JSONArray();
        JSONObject item = new JSONObject();
        item.put(DATE_KEY, "1993-01-02");
        item.put(AGE_KEY, 13);
        item.put(TYPE_KEY, "ear");
        item.put(SIDE_KEY, "l");
        item.put(VALUE_KEY, 23.5);
        item.put(UNIT_KEY, "cm");
        data.put(item);
        item = new JSONObject();
        item.put(DATE_KEY, "1993-01-02");
        item.put(AGE_KEY, 13);
        item.put(TYPE_KEY, "ear");
        item.put(SIDE_KEY, "l");
        item.put(VALUE_KEY, 23.5);
        item.put(UNIT_KEY, "cm");
        data.put(item);
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, data);
        PatientData<MeasurementEntry> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNotNull(result);
    }

    // ------------Save Tests------------

    @Test
    public void saveWithNoDataDoesNothing() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithWrongTypeOfDataDoesNothing() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(new SimpleValuePatientData<Object>("a", "b"));
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithEmptyDataClearsMeasurements() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = xcontextProvider.get();
        when(context.getWiki()).thenReturn(this.xwiki);
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
    }

    @Test
    public void saveWithIterationsTest() throws Exception
    {
        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = xcontextProvider.get();
        when(context.getWiki()).thenReturn(this.xwiki);
        when(this.stringResolver.resolve("PhenoTips.MeasurementClass")).thenReturn(MEASUREMENTS_CLASS);
        Date d = new Date();
        MeasurementEntry entry = new MeasurementEntry(d, "5y", "armspan", "l", 4.2, "cm");
        when(this.patient.<MeasurementEntry>getData("measurements"))
            .thenReturn(new IndexedPatientData<>("measurements", Collections.singletonList(entry)));
        when(this.doc.newXObject(eq(MEASUREMENTS_CLASS), any(XWikiContext.class))).thenReturn(this.obj1, this.obj2);
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        verify(this.obj1).set(DATE_KEY, entry.getDate(), context);
        verify(this.obj1).set(AGE_KEY, entry.getAge(), context);
        verify(this.obj1).set(TYPE_KEY, entry.getType(), context);
        verify(this.obj1).set(SIDE_KEY, entry.getSide(), context);
        verify(this.obj1).set(VALUE_KEY, entry.getValue(), context);
    }
}
