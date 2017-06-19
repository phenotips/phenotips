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
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.json.JSONArray;
import org.json.JSONObject;
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
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.StringListProperty;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link GeneListController} Component, only the overridden methods from {@link AbstractComplexController}
 * are tested here.
 */
public class GeneListControllerTest
{
    private static final String GENES_STRING = "genes";

    private static final String CONTROLLER_NAME = GENES_STRING;

    private static final String GENES_ENABLING_FIELD_NAME = GENES_STRING;

    private static final String GENE_KEY = "gene";

    private static final String GENE_VALUE = "GENE";

    private static final String STATUS_KEY = "status";

    private static final String STRATEGY_KEY = "strategy";

    private static final String COMMENTS_KEY = "comments";

    private static final String JSON_GENE_ID = "id";

    private static final String JSON_GENE_SYMBOL = GENE_KEY;

    private static final String JSON_STATUS_KEY = STATUS_KEY;

    private static final String JSON_STRATEGY_KEY = STRATEGY_KEY;

    private static final String JSON_COMMENTS_KEY = COMMENTS_KEY;

    private static final String JSON_OLD_REJECTED_GENE_KEY = "rejectedGenes";

    private static final String JSON_OLD_SOLVED_GENE_KEY = "solved";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Map<String, String>>> mocker =
        new MockitoComponentMockingRule<>(GeneListController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XWikiContext context;

    private List<BaseObject> geneXWikiObjects;

    private PatientDataController<Map<String, String>> component;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);

        this.component = this.mocker.getComponentUnderTest();
        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();

        this.geneXWikiObjects = new LinkedList<>();
        doReturn(this.geneXWikiObjects).when(this.doc).getXObjects(any(EntityReference.class));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME, this.component.getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME,
            ((AbstractComplexController<Map<String, String>>) this.component)
                .getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result =
            ((AbstractComplexController<Map<String, String>>) this.component).getProperties();

        Assert.assertTrue(result.contains(GENE_KEY));
        Assert.assertTrue(result.contains(STATUS_KEY));
        Assert.assertTrue(result.contains(STRATEGY_KEY));
        Assert.assertTrue(result.contains(COMMENTS_KEY));
        Assert.assertEquals(4, result.size());
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController<Map<String, String>>) this.component).getBooleanFields()
                .isEmpty());
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(((AbstractComplexController<Map<String, String>>) this.component)
            .getCodeFields().isEmpty());
    }

    // --------------------load() is Overridden from AbstractSimpleController--------------------

    @Test
    public void loadWorks() throws Exception
    {
        for (int i = 0; i < 3; ++i) {
            BaseObject gene = mock(BaseObject.class);
            this.geneXWikiObjects.add(gene);

            BaseStringProperty geneString = mock(BaseStringProperty.class);
            doReturn("gene" + i).when(geneString).getValue();
            doReturn(geneString).when(gene).getField(GENE_KEY);

            BaseStringProperty statusString = mock(BaseStringProperty.class);
            doReturn("status" + i).when(statusString).getValue();
            doReturn(statusString).when(gene).getField(STATUS_KEY);

            BaseStringProperty commentString = mock(BaseStringProperty.class);
            doReturn("comment" + i).when(commentString).getValue();
            doReturn(commentString).when(gene).getField(COMMENTS_KEY);

            StringListProperty strategyString = mock(StringListProperty.class);
            doReturn("strategy" + i).when(strategyString).getTextValue();
            doReturn(Collections.singletonList("strategy" + i)).when(strategyString).getList();
            doReturn(strategyString).when(gene).getField(STRATEGY_KEY);

            doReturn(Arrays.asList(geneString, statusString, commentString, strategyString)).when(gene).getFieldList();
        }

        PatientData<Map<String, String>> result = this.component.load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isIndexed());
        Assert.assertEquals(3, result.size());
        for (int i = 0; i < 3; ++i) {
            Map<String, String> item = result.get(i);
            Assert.assertEquals("gene" + i, item.get(GENE_KEY));
            Assert.assertEquals("status" + i, item.get(STATUS_KEY));
            Assert.assertEquals("comment" + i, item.get(COMMENTS_KEY));
            Assert.assertEquals("strategy" + i, item.get(STRATEGY_KEY));
        }
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        Exception exception = new RuntimeException();
        doThrow(exception).when(this.patient).getXDocument();

        PatientData<Map<String, String>> result = this.component.load(this.patient);

        Assert.assertNull(result);
        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHaveGeneClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Map<String, String>> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenGeneIsEmpty() throws ComponentLookupException
    {
        doReturn(new LinkedList<BaseObject>()).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Map<String, String>> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullFields() throws ComponentLookupException
    {
        BaseObject obj = mock(BaseObject.class);
        doReturn(null).when(obj).getField(anyString());
        this.geneXWikiObjects.add(obj);

        PatientData<Map<String, String>> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullGenes() throws ComponentLookupException
    {
        // Deleted objects appear as nulls in XWikiObjects list
        this.geneXWikiObjects.add(null);
        addGeneFields(GENE_KEY, new String[] { "SRCAP" });
        PatientData<Map<String, String>> result = this.component.load(this.patient);

        Assert.assertEquals(1, result.size());
    }

    @Test
    public void checkLoadParsingOfGeneKey() throws ComponentLookupException
    {
        String[] genes = new String[] { "A", "<!'>;", "two words", " ", "" };
        addGeneFields(GENE_KEY, genes);

        PatientData<Map<String, String>> result = this.component.load(this.patient);

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

        PatientData<Map<String, String>> result = this.component.load(this.patient);

        Assert.assertNotNull(result);
        for (int i = 0; i < comments.length; i++) {
            Assert.assertEquals(comments[i], result.get(i).get(COMMENTS_KEY));
        }
    }

    // --------------------writeJSON() is Overridden from AbstractSimpleController--------------------

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNotNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
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

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    /*
     * Tests that the passed JSON will not be affected by writeJSON in this controller if selected fields is not null,
     * and does not contain GeneListController.GENES_ENABLING_FIELD_NAME
     */
    @Test
    public void writeJSONReturnsWhenSelectedFieldsDoesNotContainGeneEnabler() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        // selectedFields could contain any number of random strings; it should not affect the behavior in this case
        selectedFields.add("some_string");

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
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

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        Assert.assertEquals(0, json.getJSONArray(CONTROLLER_NAME).length());
    }

    @Test
    public void writeJSONAddsContainerWithAllValuesWhenSelectedFieldsNull() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(GENE_KEY, "geneName");
        item.put(STATUS_KEY, "");
        item.put(STRATEGY_KEY, "");
        item.put(COMMENTS_KEY, null);
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        Assert.assertEquals("geneName", json.getJSONArray(CONTROLLER_NAME).getJSONObject(0).get(JSON_GENE_ID));
        Assert.assertEquals("geneName", json.getJSONArray(CONTROLLER_NAME).getJSONObject(0).get(JSON_GENE_SYMBOL));
    }

    @Test
    public void writeJSONWorksCorrectly() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(GENE_KEY, GENE_VALUE);
        item.put(STATUS_KEY, "Status");
        item.put(STRATEGY_KEY, "Strategy");
        item.put(COMMENTS_KEY, "Comment");
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        JSONObject result = json.getJSONArray(CONTROLLER_NAME).getJSONObject(0);
        Assert.assertEquals(GENE_VALUE, result.get(JSON_GENE_SYMBOL));
        Assert.assertEquals(GENE_VALUE, result.get(JSON_GENE_ID));
        Assert.assertEquals("Status", result.get(JSON_STATUS_KEY));
        String[] strategyArray = { "Strategy" };
        Assert.assertEquals(new JSONArray(strategyArray).get(0), ((JSONArray) result.get(JSON_STRATEGY_KEY)).get(0));
        Assert.assertEquals("Comment", result.get(JSON_COMMENTS_KEY));
        // id, gene, status, strategy, comment
        Assert.assertEquals(5, result.length());
    }

    @Test
    public void readWithNullJsonDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.component.readJSON(null));
    }

    @Test
    public void readWithNoDataDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.component.readJSON(new JSONObject()));
    }

    @Test
    public void readWithWrongDataDoesNothing() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, "No");
        PatientData<Map<String, String>> result = this.component.readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readWithEmptyDataDoesNothing() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, new JSONArray());
        PatientData<Map<String, String>> result = this.component.readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readWorksCorrectly() throws ComponentLookupException
    {
        JSONArray data = new JSONArray();
        JSONObject item = new JSONObject();
        item.put(JSON_GENE_SYMBOL, "GENE1");
        item.put(JSON_COMMENTS_KEY, "Notes1");
        data.put(item);
        item = new JSONObject();
        item.put(JSON_GENE_SYMBOL, "GENE2");
        item.put(JSON_STATUS_KEY, "rejected");
        data.put(item);
        item = new JSONObject();
        item.put(JSON_GENE_ID, "ENSG00000123456");
        data.put(item);
        item = new JSONObject();
        item.put(JSON_GENE_ID, "ENSG00000098765");
        item.put(JSON_STATUS_KEY, "incorrect_status");
        data.put(item);
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, data);
        PatientData<Map<String, String>> result = this.component.readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());
        Assert.assertTrue(result.isIndexed());
        Iterator<Map<String, String>> it = result.iterator();
        Map<String, String> gene = it.next();
        Assert.assertEquals("GENE1", gene.get(GENE_KEY));
        Assert.assertEquals("Notes1", gene.get(COMMENTS_KEY));
        // "candidate" is the default status added to any gene without explicitly specified status
        Assert.assertEquals("candidate", gene.get(STATUS_KEY));
        Assert.assertFalse(gene.containsKey(STRATEGY_KEY));
        gene = it.next();
        Assert.assertEquals("GENE2", gene.get(GENE_KEY));
        Assert.assertEquals("rejected", gene.get(STATUS_KEY));
        Assert.assertFalse(gene.containsKey(COMMENTS_KEY));
        Assert.assertFalse(gene.containsKey(STRATEGY_KEY));
        gene = it.next();
        Assert.assertEquals("ENSG00000123456", gene.get(GENE_KEY));
        Assert.assertEquals("candidate", gene.get(STATUS_KEY));
        Assert.assertFalse(gene.containsKey(COMMENTS_KEY));
        Assert.assertFalse(gene.containsKey(STRATEGY_KEY));
        gene = it.next();
        Assert.assertEquals("ENSG00000098765", gene.get(GENE_KEY));
        // any incorrect status should be replaced with "candidate"
        Assert.assertEquals("candidate", gene.get(STATUS_KEY));
    }

    @Test
    public void readParsedsOldJSONCorrectly() throws ComponentLookupException
    {
        JSONArray data = new JSONArray();
        JSONObject item = new JSONObject();
        item.put(JSON_GENE_SYMBOL, "GENE_1");
        item.put(JSON_STATUS_KEY, "candidate");
        data.put(item);
        item = new JSONObject();
        // this gene is duplicated 2 times - in candidate and in rejected sections. Should become rejected
        item.put(JSON_GENE_SYMBOL, "GENE_TO_BECOME_REJECTED");
        item.put(JSON_STATUS_KEY, "candidate");
        data.put(item);
        item = new JSONObject();
        // this gene is duplicated 3 times - in candidate, rejected and solved sections. Should become solved
        item.put(JSON_GENE_SYMBOL, "GENE_TO_BECOME_SOLVED");
        item.put(JSON_STATUS_KEY, "candidate");
        data.put(item);
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, data);
        data = new JSONArray();
        item = new JSONObject();
        item.put(JSON_GENE_SYMBOL, "GENE_TO_BECOME_REJECTED");
        item.put(JSON_STATUS_KEY, "rejected");
        data.put(item);
        item = new JSONObject();
        item.put(JSON_GENE_SYMBOL, "GENE_TO_BECOME_SOLVED");
        item.put(JSON_STATUS_KEY, "rejected");
        data.put(item);
        json.put(JSON_OLD_REJECTED_GENE_KEY, data);
        item = new JSONObject();
        item.put(JSON_GENE_SYMBOL, "GENE_TO_BECOME_SOLVED");
        json.put(JSON_OLD_SOLVED_GENE_KEY, item);

        PatientData<Map<String, String>> result = this.component.readJSON(json);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result.isIndexed());
        Iterator<Map<String, String>> it = result.iterator();
        Map<String, String> gene = it.next();
        Assert.assertEquals("GENE_1", gene.get(GENE_KEY));
        Assert.assertEquals("candidate", gene.get(STATUS_KEY));
        gene = it.next();
        Assert.assertEquals("GENE_TO_BECOME_REJECTED", gene.get(GENE_KEY));
        Assert.assertEquals("rejected", gene.get(STATUS_KEY));
        gene = it.next();
        Assert.assertEquals("GENE_TO_BECOME_SOLVED", gene.get(GENE_KEY));
        Assert.assertEquals("solved", gene.get(STATUS_KEY));
    }

    @Test
    public void saveWithNoDataDoesNothingWhenPolicyIsUpdate()
    {
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithNoDataDoesNothingWhenPolicyIsMerge()
    {
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithNoDataRemovesAllGenesWhenPolicyIsReplace()
    {
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).removeXObjects(GeneListController.GENE_CLASS_REFERENCE);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveWithWrongTypeOfDataDoesNothing()
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(new SimpleValuePatientData<>("a", "b"));
        this.component.save(this.patient);
        verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithEmptyDataClearsGenesWhenPolicyIsUpdate()
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        when(this.context.getWiki()).thenReturn(mock(XWiki.class));
        this.component.save(this.patient);
        verify(this.doc).removeXObjects(GeneListController.GENE_CLASS_REFERENCE);

        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveWithEmptyDataMergesWithStoredDataWhenPolicyIsMerge() throws XWikiException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        final BaseObject geneObject = mock(BaseObject.class);
        when(this.doc.getXObjects(GeneListController.GENE_CLASS_REFERENCE)).thenReturn(Collections.singletonList(geneObject));

        final BaseStringProperty geneProperty = mock(BaseStringProperty.class);
        final BaseStringProperty statusProperty = mock(BaseStringProperty.class);

        when(geneObject.getFieldList()).thenReturn(Arrays.asList(GENE_KEY, STATUS_KEY));
        when(geneObject.getField(GENE_KEY)).thenReturn(geneProperty);
        when(geneObject.getField(STATUS_KEY)).thenReturn(statusProperty);
        when(geneObject.getField(STRATEGY_KEY)).thenReturn(null);
        when(geneObject.getField(COMMENTS_KEY)).thenReturn(null);

        when(geneProperty.getValue()).thenReturn(GENE_VALUE);
        when(statusProperty.getValue()).thenReturn(STATUS_KEY);

        final BaseObject xwikiObject = mock(BaseObject.class);
        when(this.doc.newXObject(GeneListController.GENE_CLASS_REFERENCE, this.context)).thenReturn(xwikiObject);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).removeXObjects(GeneListController.GENE_CLASS_REFERENCE);
        verify(this.doc, times(1)).newXObject(GeneListController.GENE_CLASS_REFERENCE, this.context);
        verify(this.doc, times(1)).getXObjects(GeneListController.GENE_CLASS_REFERENCE);
        verify(xwikiObject, times(1)).set(GENE_KEY, GENE_VALUE, this.context);
        verify(xwikiObject, times(1)).set(STATUS_KEY, STATUS_KEY, this.context);
        verify(xwikiObject, never()).set(eq(STRATEGY_KEY), any(), eq(this.context));
        verify(xwikiObject, never()).set(eq(COMMENTS_KEY), any(), eq(this.context));

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(xwikiObject);
    }

    @Test
    public void saveWithEmptyDataClearsGenesWhenPolicyIsReplace()
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        when(this.context.getWiki()).thenReturn(mock(XWiki.class));
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc).removeXObjects(GeneListController.GENE_CLASS_REFERENCE);

        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveUpdatesGenesWhenPolicyIsUpdate() throws XWikiException
    {
        List<Map<String, String>> data = new LinkedList<>();
        Map<String, String> item = new HashMap<>();
        item.put(GENE_KEY, "GENE1");
        item.put(COMMENTS_KEY, "Notes1");
        data.add(item);
        item = new HashMap<>();
        item.put(GENE_KEY, "GENE2");
        data.add(item);
        when(this.patient.<Map<String, String>>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, data));

        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        BaseObject o1 = mock(BaseObject.class);
        BaseObject o2 = mock(BaseObject.class);
        when(this.doc.newXObject(GeneListController.GENE_CLASS_REFERENCE, this.context)).thenReturn(o1, o2);

        this.component.save(this.patient);

        verify(this.doc).removeXObjects(GeneListController.GENE_CLASS_REFERENCE);
        verify(o1).set(GENE_KEY, "GENE1", this.context);
        verify(o1).set(COMMENTS_KEY, "Notes1", this.context);
        verify(o2).set(GENE_KEY, "GENE2", this.context);
        verify(o2, never()).set(eq(COMMENTS_KEY), anyString(), eq(this.context));
    }

    @Test
    public void saveReplacesGenesWhenPolicyIsReplace() throws XWikiException
    {
        List<Map<String, String>> data = new LinkedList<>();
        Map<String, String> item = new HashMap<>();
        item.put(GENE_KEY, "GENE1");
        item.put(COMMENTS_KEY, "Notes1");
        data.add(item);
        item = new HashMap<>();
        item.put(GENE_KEY, "GENE2");
        data.add(item);
        when(this.patient.<Map<String, String>>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, data));

        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        BaseObject o1 = mock(BaseObject.class);
        BaseObject o2 = mock(BaseObject.class);
        when(this.doc.newXObject(GeneListController.GENE_CLASS_REFERENCE, this.context)).thenReturn(o1, o2);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc).removeXObjects(GeneListController.GENE_CLASS_REFERENCE);
        verify(o1).set(GENE_KEY, "GENE1", this.context);
        verify(o1).set(COMMENTS_KEY, "Notes1", this.context);
        verify(o2).set(GENE_KEY, "GENE2", this.context);
        verify(o2, never()).set(eq(COMMENTS_KEY), anyString(), eq(this.context));
    }

    @Test
    public void saveMergesGenesWhenPolicyIsMerge() throws XWikiException
    {
        List<Map<String, String>> data = new LinkedList<>();
        Map<String, String> item = new HashMap<>();
        item.put(GENE_KEY, "GENE1");
        item.put(COMMENTS_KEY, "Notes1");
        data.add(item);
        item = new HashMap<>();
        item.put(GENE_KEY, "GENE2");
        data.add(item);
        when(this.patient.<Map<String, String>>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, data));

        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        BaseObject o1 = mock(BaseObject.class);
        BaseObject o2 = mock(BaseObject.class);
        BaseObject o3 = mock(BaseObject.class);
        when(this.doc.getXObjects(GeneListController.GENE_CLASS_REFERENCE)).thenReturn(Collections.singletonList(o3));

        final BaseStringProperty geneProperty = mock(BaseStringProperty.class);
        final BaseStringProperty statusProperty = mock(BaseStringProperty.class);
        when(o3.getFieldList()).thenReturn(Arrays.asList(GENE_KEY, STATUS_KEY));
        when(o3.getField(GENE_KEY)).thenReturn(geneProperty);
        when(o3.getField(STATUS_KEY)).thenReturn(statusProperty);
        when(o3.getField(STRATEGY_KEY)).thenReturn(null);
        when(o3.getField(COMMENTS_KEY)).thenReturn(null);
        when(geneProperty.getValue()).thenReturn(GENE_VALUE);
        when(statusProperty.getValue()).thenReturn(STATUS_KEY);

        when(this.doc.newXObject(GeneListController.GENE_CLASS_REFERENCE, this.context)).thenReturn(o3, o1, o2);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).removeXObjects(GeneListController.GENE_CLASS_REFERENCE);
        verify(this.doc, times(3)).newXObject(GeneListController.GENE_CLASS_REFERENCE, this.context);
        verify(this.doc, times(1)).getXObjects(GeneListController.GENE_CLASS_REFERENCE);
        verify(o1).set(GENE_KEY, "GENE1", this.context);
        verify(o1).set(COMMENTS_KEY, "Notes1", this.context);
        verify(o2).set(GENE_KEY, "GENE2", this.context);
        verify(o2, never()).set(eq(COMMENTS_KEY), anyString(), eq(this.context));
        verify(o3, times(1)).set(GENE_KEY, GENE_VALUE, this.context);
        verify(o3, times(1)).set(STATUS_KEY, STATUS_KEY, this.context);
        verify(o3, never()).set(eq(STRATEGY_KEY), any(), eq(this.context));
        verify(o3, never()).set(eq(COMMENTS_KEY), any(), eq(this.context));
        verify(o3, times(1)).getFieldList();
        verify(o3, times(4)).getField(anyString());

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(o1);
        verifyNoMoreInteractions(o2);
        verifyNoMoreInteractions(o3);
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
