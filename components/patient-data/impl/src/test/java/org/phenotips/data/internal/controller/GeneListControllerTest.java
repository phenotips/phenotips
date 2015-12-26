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

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link GeneListController} Component, only the overridden methods from {@link AbstractComplexController}
 * are tested here
 */
public class GeneListControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Map<String, String>>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Map<String, String>>>(GeneListController.class);

    @Mock
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    private List<BaseObject> geneXWikiObjects;

    private static final String GENES_STRING = "genes";

    private static final String CONTROLLER_NAME = GENES_STRING;

    private static final String GENES_ENABLING_FIELD_NAME = GENES_STRING;

    private static final String GENES_COMMENTS_ENABLING_FIELD_NAME = "genes_comments";

    private static final String GENE_KEY = "gene";

    private static final String COMMENTS_KEY = "comments";

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        this.geneXWikiObjects = new LinkedList<>();
        doReturn(this.geneXWikiObjects).when(this.doc).getXObjects(any(EntityReference.class));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME,
            ((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest())
                .getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result =
            ((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertTrue(result.contains(GENE_KEY));
        Assert.assertTrue(result.contains(COMMENTS_KEY));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest()).getBooleanFields()
                .isEmpty());
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest())
            .getCodeFields().isEmpty());
    }

    // --------------------load() is Overridden from AbstractSimpleController--------------------

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        Exception exception = new Exception();
        doThrow(exception).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen "
            + "error has occurred during controller loading ", exception.getMessage());
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHaveGeneClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenGeneIsEmpty() throws ComponentLookupException
    {
        doReturn(new LinkedList<BaseObject>()).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullFields() throws ComponentLookupException
    {
        BaseObject obj = mock(BaseObject.class);
        doReturn(null).when(obj).getField(anyString());
        this.geneXWikiObjects.add(obj);

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullGenes() throws ComponentLookupException
    {
        // Deleted objects appear as nulls in XWikiObjects list
        this.geneXWikiObjects.add(null);
        addGeneFields(GENE_KEY, new String[] { "SRCAP" });
        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(1, result.size());
    }

    @Test
    public void checkLoadParsingOfGeneKey() throws ComponentLookupException
    {
        String[] genes = new String[] { "A", "<!'>;", "two words", " ", "" };
        addGeneFields(GENE_KEY, genes);

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        for (int i = 0; i < genes.length; i++) {
            Assert.assertEquals(genes[i], result.get(i).get(GENE_KEY));
        }
    }

    @Test
    public void checkLoadParsingOfCommentsKey() throws ComponentLookupException
    {
        String[] comments = new String[] { "Hello world!", "<script></script>", "", "{{html}}" };
        addGeneFields(COMMENTS_KEY, comments);

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result);
        for (int i = 0; i < comments.length; i++) {
            Assert.assertEquals(comments[i], result.get(i).get(COMMENTS_KEY));
        }
    }

    // --------------------writeJSON() is Overridden from AbstractSimpleController--------------------

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONReturnsWhenDataIsEmpty() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONReturnsWhenSelectedFieldsDoesNotContainGeneEnabler() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("some_string");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(CONTROLLER_NAME));
    }

    @Test
    public void writeJSONIgnoresItemsWhenGeneIsBlank() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(GENE_KEY, "");
        internalList.add(item);

        item = new LinkedHashMap<>();
        item.put(GENE_KEY, null);
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        Assert.assertTrue(json.getJSONArray(CONTROLLER_NAME).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeJSONAddsContainerWithAllValuesWhenSelectedFieldsNull() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(GENE_KEY, "geneName");
        item.put(COMMENTS_KEY, null);
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        item = (Map<String, String>) json.getJSONArray(CONTROLLER_NAME).get(0);
        Assert.assertEquals("geneName", item.get(GENE_KEY));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeJSONAddsContainerWithOnlySelectedFields() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(GENE_KEY, "GENE");
        item.put(COMMENTS_KEY, "Comment");
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);
        selectedFields.add(GENES_COMMENTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        item = (Map<String, String>) json.getJSONArray(CONTROLLER_NAME).get(0);
        Assert.assertEquals("GENE", item.get(GENE_KEY));
        Assert.assertEquals("Comment", item.get(COMMENTS_KEY));

        json = new JSONObject();
        internalList = new LinkedList<>();
        item = new LinkedHashMap<>();
        item.put(GENE_KEY, "GENE");
        item.put(COMMENTS_KEY, "Comment");
        internalList.add(item);
        patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        item = (Map<String, String>) json.getJSONArray(CONTROLLER_NAME).get(0);
        Assert.assertEquals("GENE", item.get(GENE_KEY));
        Assert.assertEquals(1, item.size());
    }

    // ----------------------------------------Private methods----------------------------------------

    private void addGeneFields(String key, String[] fieldValues)
    {
        BaseObject obj;
        BaseStringProperty property;
        for (String value : fieldValues) {
            obj = mock(BaseObject.class);
            property = mock(BaseStringProperty.class);
            List<String> list = new ArrayList<>();
            list.add(value);
            doReturn(value).when(property).getValue();
            doReturn(property).when(obj).getField(key);
            doReturn(list).when(obj).getFieldList();
            this.geneXWikiObjects.add(obj);
        }
    }
}
