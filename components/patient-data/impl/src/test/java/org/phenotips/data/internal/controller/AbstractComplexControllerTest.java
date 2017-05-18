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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;
import org.phenotips.data.VocabularyProperty;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link AbstractComplexController} defined methods (load, save, writeJSON, readJSON). These methods are
 * tested using a mock implementation of {@link AbstractComplexController} that provides simple definitions of the
 * abstract methods getName, getProperties, and getJsonPropertyName
 */
public class AbstractComplexControllerTest
{
    private static final String DATA_NAME = AbstractComplexControllerTestImplementation.DATA_NAME;

    private static final String CODE_FIELDS_DATA_NAME = AbstractComplexControllerCodeFieldsTestImplementation.DATA_NAME;

    private static final String PROPERTY_1 = AbstractComplexControllerTestImplementation.PROPERTY_1;

    private static final String PROPERTY_2 = AbstractComplexControllerTestImplementation.PROPERTY_2;

    private static final String PROPERTY_3 = AbstractComplexControllerTestImplementation.PROPERTY_3;

    private static final String PROPERTY_4 = AbstractComplexControllerTestImplementation.PROPERTY_4;

    private static final String PROPERTY_5 = AbstractComplexControllerTestImplementation.PROPERTY_5;

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(
            AbstractComplexControllerTestImplementation.class);

    @Rule
    public MockitoComponentMockingRule<PatientDataController<List<VocabularyProperty>>> codeFieldImplMocker =
        new MockitoComponentMockingRule<PatientDataController<List<VocabularyProperty>>>(
            AbstractComplexControllerCodeFieldsTestImplementation.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty1;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty2;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty3;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty4;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty5;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        doReturn(this.baseProperty1).when(this.data).getField(PROPERTY_1);
        doReturn(this.baseProperty2).when(this.data).getField(PROPERTY_2);
        doReturn(this.baseProperty3).when(this.data).getField(PROPERTY_3);
        doReturn(this.baseProperty4).when(this.data).getField(PROPERTY_4);
        doReturn(this.baseProperty5).when(this.data).getField(PROPERTY_5);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void verifyDefaultTestImplementationIsNotCodeFieldsOnly() throws ComponentLookupException
    {
        AbstractComplexController<String> controller =
            (AbstractComplexController<String>) this.mocker.getComponentUnderTest();
        Assert.assertFalse(controller.isCodeFieldsOnly());
    }

    @Test
    public void verifyCodeFieldsOnlyImplementationIsCodeFieldsOnly() throws ComponentLookupException
    {
        AbstractComplexController<List<VocabularyProperty>> controller =
            (AbstractComplexController<List<VocabularyProperty>>) this.codeFieldImplMocker.getComponentUnderTest();
        Assert.assertTrue(controller.isCodeFieldsOnly());
    }

    // -----------------------------------load() tests-----------------------------------

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        doReturn(null).when(this.patient).getXDocument();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAllData() throws ComponentLookupException
    {
        String datum1 = "datum1";
        String datum2 = "datum2";
        String datum3 = "datum3";
        String datum4 = "datum4";
        String datum5 = "datum5";
        doReturn(datum1).when(this.baseProperty1).getValue();
        doReturn(datum2).when(this.baseProperty2).getValue();
        doReturn(datum3).when(this.baseProperty3).getValue();
        doReturn(datum4).when(this.baseProperty4).getValue();
        doReturn(datum5).when(this.baseProperty5).getValue();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(datum1, result.get(PROPERTY_1));
        Assert.assertEquals(datum2, result.get(PROPERTY_2));
        Assert.assertEquals(datum3, result.get(PROPERTY_3));
        Assert.assertEquals(datum4, result.get(PROPERTY_4));
        Assert.assertEquals(datum5, result.get(PROPERTY_5));
        Assert.assertEquals(5, result.size());
    }

    @Test
    public void loadConvertsCodeFieldsWhenControllerIsOnlyCodeFields() throws Exception
    {
        List<String> list1 = new LinkedList<>();
        list1.add("HP:00000015");
        doReturn(list1).when(this.baseProperty1).getValue();
        List<String> list2 = new LinkedList<>();
        list2.add("HP:00000120");
        doReturn(list2).when(this.baseProperty2).getValue();

        PatientData<List<VocabularyProperty>> result =
            this.codeFieldImplMocker.getComponentUnderTest().load(this.patient);

        List<VocabularyProperty> propertyOneList = result.get(PROPERTY_1);
        List<VocabularyProperty> propertyTwoList = result.get(PROPERTY_2);
        Assert.assertNotNull(propertyOneList);
        Assert.assertNotNull(propertyTwoList);
        Assert.assertThat(propertyOneList, hasSize(1));
        Assert.assertThat(propertyTwoList, hasSize(1));
        Assert.assertThat(propertyOneList, contains(hasProperty("id", is("HP:00000015"))));
        Assert.assertThat(propertyTwoList, contains(hasProperty("id", is("HP:00000120"))));
    }

    // -----------------------------------writeJSON() tests-----------------------------------

    @Test
    public void writeJSONDoesNotWriteNullData() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsDoesNotWriteNullData() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONChecksThatDataIsKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsChecksThatDataIsKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsContainerWithAllValues() throws ComponentLookupException
    {
        String datum1 = "datum1";
        String datum2 = "datum2";
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, datum1);
        map.put(PROPERTY_2, datum2);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(datum1, container.get(PROPERTY_1));
        Assert.assertEquals(datum2, container.get(PROPERTY_2));
    }

    @Test
    public void writeJSONWithAllSelectedFieldsAddsContainerWithAllValues() throws ComponentLookupException
    {
        String datum1 = "datum1";
        String datum2 = "datum2";
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, datum1);
        map.put(PROPERTY_2, datum2);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_2);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(datum1, container.get(PROPERTY_1));
        Assert.assertEquals(datum2, container.get(PROPERTY_2));
    }

    @Test
    public void writeJSONConvertsBooleanValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_3, "1");
        map.put(PROPERTY_4, "0");
        map.put(PROPERTY_5, "SOME_NON_BOOL_STRING");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(true, container.get(PROPERTY_3));
        Assert.assertEquals(false, container.get(PROPERTY_4));
        Assert.assertFalse(container.has(PROPERTY_5));
    }

    @Test
    public void writeJSONWithSelectedFieldsConvertsBooleanValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_3, "1");
        map.put(PROPERTY_4, "0");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_3);
        selectedFields.add(PROPERTY_4);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(true, container.get(PROPERTY_3));
        Assert.assertEquals(false, container.get(PROPERTY_4));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues() throws ComponentLookupException
    {
        String datum1 = "datum1";
        String datum2 = "datum2";
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, datum1);
        map.put(PROPERTY_2, datum2);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(datum1, container.get(PROPERTY_1));
        Assert.assertFalse(container.has(PROPERTY_2));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsContainerWithAllValuesWhenSelectedFieldsNull()
        throws ComponentLookupException
    {
        String datum1 = "datum1";
        String datum2 = "datum2";
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, datum1);
        map.put(PROPERTY_2, datum2);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(datum1, container.get(PROPERTY_1));
        Assert.assertEquals(datum2, container.get(PROPERTY_2));
    }

    @Test
    public void writeJSONDoesNotOverwriteContainer() throws ComponentLookupException
    {
        String datum1 = "datum1";
        String datum2 = "datum2";
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, datum1);
        map.put(PROPERTY_2, datum2);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(datum1, container.get(PROPERTY_1));
        Assert.assertFalse(container.has(PROPERTY_2));

        selectedFields.clear();
        selectedFields.add(PROPERTY_2);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(datum1, container.get(PROPERTY_1));
        Assert.assertEquals(datum2, container.get(PROPERTY_2));
    }

    @Test
    public void writeJSONConvertsCodeFields() throws Exception
    {
        @SuppressWarnings("unchecked")
        Provider<ComponentManager> cmProvider = mock(Provider.class);
        ComponentManager contextComponentManager = mock(ComponentManager.class);
        VocabularyManager vm = mock(VocabularyManager.class);
        VocabularyTerm term = mock(VocabularyTerm.class);

        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", cmProvider);
        doReturn(contextComponentManager).when(cmProvider).get();
        doReturn(vm).when(contextComponentManager).getInstance(VocabularyManager.class);
        doReturn(term).when(vm).resolveTerm("HP:0009927");
        doReturn("Congenital absence of nose").when(term).getName();
        term = mock(VocabularyTerm.class);
        doReturn(term).when(vm).resolveTerm("HP:0002223");
        doReturn("Absent eyebrow").when(term).getName();

        Map<String, List<VocabularyProperty>> map = new LinkedHashMap<>();
        List<VocabularyProperty> list = new LinkedList<>();
        list.add(new AbstractComplexController.QuickVocabularyProperty("HP:0009927"));
        map.put(PROPERTY_1, list);
        list = new LinkedList<>();
        list.add(new AbstractComplexController.QuickVocabularyProperty("HP:0002223"));
        map.put(PROPERTY_2, list);
        PatientData<List<VocabularyProperty>> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(CODE_FIELDS_DATA_NAME);
        JSONObject json = new JSONObject();

        this.codeFieldImplMocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(CODE_FIELDS_DATA_NAME));
        Assert.assertTrue(json.get(CODE_FIELDS_DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(CODE_FIELDS_DATA_NAME);
        JSONObject item1 = container.getJSONArray(PROPERTY_1).getJSONObject(0);
        JSONObject item2 = container.getJSONArray(PROPERTY_2).getJSONObject(0);
        Assert.assertNotNull(item1);
        Assert.assertEquals("HP:0009927", item1.get("id"));
        Assert.assertEquals("Congenital absence of nose", item1.get("label"));
        Assert.assertNotNull(item2);
        Assert.assertEquals("HP:0002223", item2.get("id"));
        Assert.assertEquals("Absent eyebrow", item2.get("label"));
    }
}
