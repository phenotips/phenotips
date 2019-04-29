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
import org.phenotips.data.Gene;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;
import org.phenotips.data.internal.PhenoTipsGene;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.collections4.CollectionUtils;
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
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.web.Utils;

import net.jcip.annotations.NotThreadSafe;

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
@NotThreadSafe
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

    private static final String JSON_OLD_CANDIDATE_GENE_KEY = "candidate";

    private static final List<String> STATUS_VALUES = Arrays.asList("candidate", "rejected", "rejected_candidate",
        "solved", "carrier", "candidate>novel_disease", "candidate>novel_phen", "umc", "umc>vus", "umc>msv");

    private static final List<String> STRATEGY_VALUES = Arrays.asList("sequencing", "deletion", "familial_mutation",
        "common_mutations");

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Gene>> mocker =
        new MockitoComponentMockingRule<>(GeneListController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XWikiContext context;

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private VocabularyManager vm;

    @Mock
    private XWiki xwiki;

    @Mock
    private Provider<XWikiContext> provider;

    private List<BaseObject> geneXWikiObjects;

    private PatientDataController<Gene> component;

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

        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(VocabularyManager.class)).thenReturn(this.vm);

        when(this.cm.getInstance(XWikiContext.TYPE_PROVIDER)).thenReturn(this.provider);
        when(this.provider.get()).thenReturn(this.context);
        XWiki x = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(x);

        XWikiDocument geneDoc = mock(XWikiDocument.class);
        when(x.getDocument(Gene.GENE_CLASS, this.context)).thenReturn(geneDoc);
        geneDoc.setNew(false);
        BaseClass c = mock(BaseClass.class);
        when(geneDoc.getXClass()).thenReturn(c);
        StaticListClass lc1 = mock(StaticListClass.class);
        StaticListClass lc2 = mock(StaticListClass.class);
        when(c.get(STATUS_KEY)).thenReturn(lc1);
        when(c.get(STRATEGY_KEY)).thenReturn(lc2);
        when(lc1.getList(this.context)).thenReturn(STATUS_VALUES);
        when(lc2.getList(this.context)).thenReturn(STRATEGY_VALUES);

        this.geneXWikiObjects = new LinkedList<>();
        doReturn(this.geneXWikiObjects).when(this.doc).getXObjects(any(EntityReference.class));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME, this.component.getName());
    }

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
            doReturn(STATUS_VALUES.get(i)).when(statusString).getValue();
            doReturn(statusString).when(gene).getField(STATUS_KEY);

            StringListProperty strategyString = mock(StringListProperty.class);
            doReturn(STRATEGY_VALUES.get(i)).when(strategyString).getTextValue();
            doReturn(Arrays.asList("strategy" + i)).when(strategyString).getList();
            doReturn(strategyString).when(gene).getField(STRATEGY_KEY);

            BaseStringProperty commentString = mock(BaseStringProperty.class);
            doReturn("comment" + i).when(commentString).getValue();
            doReturn(commentString).when(gene).getField(COMMENTS_KEY);

            doReturn(Arrays.asList(geneString, statusString, commentString, strategyString)).when(gene).getFieldList();
        }

        PatientData<Gene> result = this.component.load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        for (int i = 0; i < 3; ++i) {
            Gene item = result.get(i);
            Assert.assertEquals("gene" + i, item.getName());
            Assert.assertEquals(STATUS_VALUES.get(i), item.getStatus());
            Assert.assertTrue(
                CollectionUtils.isEqualCollection(Collections.singletonList("strategy" + i), item.getStrategy()));
            Assert.assertEquals("comment" + i, item.getComment());
        }
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        Exception exception = new RuntimeException();
        doThrow(exception).when(this.patient).getXDocument();

        PatientData<Gene> result = this.component.load(this.patient);

        Assert.assertNull(result);
        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHaveGeneClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Gene> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenGeneIsEmpty() throws ComponentLookupException
    {
        doReturn(new LinkedList<BaseObject>()).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Gene> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullFields() throws ComponentLookupException
    {
        BaseObject obj = mock(BaseObject.class);
        doReturn(null).when(obj).getField(anyString());
        this.geneXWikiObjects.add(obj);

        PatientData<Gene> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullGenes() throws ComponentLookupException
    {
        // Deleted objects appear as nulls in XWikiObjects list
        this.geneXWikiObjects.add(null);
        addGeneFields(GENE_KEY, new String[] { "SRCAP" });
        PatientData<Gene> result = this.component.load(this.patient);

        Assert.assertEquals(1, result.size());
    }

    @Test
    public void checkLoadParsingOfGeneAndCommentsKeys() throws ComponentLookupException
    {
        String[] genes = new String[] { "A", "<!'>;", "two words" };
        String[] comments = new String[] { "Hello world!", "<script></script>", "{{html}}" };

        for (int i = 0; i < 3; ++i) {
            BaseObject gene = mock(BaseObject.class);
            this.geneXWikiObjects.add(gene);

            BaseStringProperty geneString = mock(BaseStringProperty.class);
            doReturn(genes[i]).when(geneString).getValue();
            doReturn(geneString).when(gene).getField(GENE_KEY);

            BaseStringProperty commentString = mock(BaseStringProperty.class);
            doReturn(comments[i]).when(commentString).getValue();
            doReturn(commentString).when(gene).getField(COMMENTS_KEY);

            when(gene.getField(STRATEGY_KEY)).thenReturn(null);
            when(gene.getField(STATUS_KEY)).thenReturn(null);

            doReturn(Arrays.asList(geneString, commentString)).when(gene).getFieldList();
        }

        PatientData<Gene> result = this.component.load(this.patient);

        Assert.assertNotNull(result);
        for (int i = 0; i < comments.length; i++) {
            Gene gene = result.get(i);
            Assert.assertEquals(genes[i], gene.getName());
            Assert.assertEquals(comments[i], gene.getComment());
        }
    }

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
        List<Gene> internalList = new LinkedList<>();
        PatientData<Gene> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
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
        List<Gene> internalList = new LinkedList<>();
        PatientData<Gene> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        // selectedFields could contain any number of random strings; it should not affect the behavior in this case
        selectedFields.add("some_string");

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(CONTROLLER_NAME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeJSONIgnoresItemsWhenGeneIsBlank() throws ComponentLookupException
    {
        List<Gene> internalList = new LinkedList<>();
        Gene gene = new PhenoTipsGene("", null, null, null, null);
        internalList.add(gene);
        PatientData<Gene> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
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
        List<Gene> internalList = new LinkedList<>();

        Gene item = new PhenoTipsGene(null, "geneName", "", Collections.singleton(""), null);
        internalList.add(item);

        PatientData<Gene> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
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
        List<Gene> internalList = new LinkedList<>();

        Gene item =
            new PhenoTipsGene(null, GENE_VALUE, "Status", Collections.singleton("familial_mutation"), "Comment");
        internalList.add(item);

        PatientData<Gene> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
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
        Assert.assertEquals(null, result.opt(JSON_STATUS_KEY));
        String[] strategyArray = { "familial_mutation" };
        Assert.assertEquals(new JSONArray(strategyArray).get(0), ((JSONArray) result.get(JSON_STRATEGY_KEY)).get(0));
        Assert.assertEquals("Comment", result.get(JSON_COMMENTS_KEY));
        // id, gene, strategy, comment
        Assert.assertEquals(4, result.length());
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

        PatientData<Gene> result = this.component.readJSON(json);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readWithEmptyDataDoesNothing() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(CONTROLLER_NAME, new JSONArray());

        PatientData<Gene> result = this.component.readJSON(json);

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
        item.put(JSON_GENE_SYMBOL, "GENE3");
        item.put(JSON_STATUS_KEY, "rejected_candidate");
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

        PatientData<Gene> result = this.component.readJSON(json);

        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.size());
        Gene gene = result.get(0);
        Assert.assertEquals("GENE1", gene.getName());
        Assert.assertEquals("Notes1", gene.getComment());
        Assert.assertTrue(gene.getStrategy().isEmpty());
        gene = result.get(1);
        Assert.assertEquals("GENE2", gene.getName());
        Assert.assertEquals("rejected", gene.getStatus());
        Assert.assertNull(gene.getComment());
        Assert.assertTrue(gene.getStrategy().isEmpty());
        gene = result.get(2);
        Assert.assertEquals("GENE3", gene.getName());
        Assert.assertEquals("rejected_candidate", gene.getStatus());
        Assert.assertNull(gene.getComment());
        Assert.assertTrue(gene.getStrategy().isEmpty());
        gene = result.get(3);
        Assert.assertEquals("ENSG00000123456", gene.getId());
        Assert.assertNull(gene.getStatus());
        Assert.assertNull(gene.getComment());
        Assert.assertTrue(gene.getStrategy().isEmpty());
        gene = result.get(4);
        Assert.assertEquals("ENSG00000098765", gene.getId());
        // any incorrect status should be replaced with "candidate"
        Assert.assertNull(gene.getStatus());
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

        PatientData<Gene> result = this.component.readJSON(json);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Gene gene = result.get(0);
        Assert.assertEquals("GENE_1", gene.getName());
        Assert.assertEquals("candidate", gene.getStatus());
        gene = result.get(1);
        Assert.assertEquals("GENE_TO_BECOME_REJECTED", gene.getName());
        Assert.assertEquals("rejected", gene.getStatus());
        gene = result.get(2);
        Assert.assertEquals("GENE_TO_BECOME_SOLVED", gene.getName());
        Assert.assertEquals("solved", gene.getStatus());
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
        verify(this.doc, times(1)).removeXObjects(Gene.GENE_CLASS);
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
        verify(this.doc).removeXObjects(Gene.GENE_CLASS);

        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveWithEmptyDataMergesWithStoredDataWhenPolicyIsMerge() throws XWikiException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        final BaseObject geneObject = mock(BaseObject.class);
        when(this.doc.getXObjects(Gene.GENE_CLASS))
            .thenReturn(Collections.singletonList(geneObject));

        final BaseStringProperty geneProperty = mock(BaseStringProperty.class);
        final BaseStringProperty statusProperty = mock(BaseStringProperty.class);

        when(geneObject.getFieldList()).thenReturn(Arrays.asList(GENE_KEY, STATUS_KEY));
        when(geneObject.getField(GENE_KEY)).thenReturn(geneProperty);
        when(geneObject.getField(STATUS_KEY)).thenReturn(statusProperty);
        when(geneObject.getField(STRATEGY_KEY)).thenReturn(null);
        when(geneObject.getField(COMMENTS_KEY)).thenReturn(null);

        when(geneProperty.getValue()).thenReturn(GENE_VALUE);
        when(statusProperty.getValue()).thenReturn(JSON_OLD_SOLVED_GENE_KEY);

        final BaseObject xwikiObject = mock(BaseObject.class);
        when(this.doc.newXObject(Gene.GENE_CLASS, this.context)).thenReturn(xwikiObject);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).newXObject(Gene.GENE_CLASS, this.context);
        verify(this.doc, times(1)).removeXObjects(Gene.GENE_CLASS);
        verify(this.doc, times(1)).getXObjects(Gene.GENE_CLASS);
        verify(xwikiObject, times(1)).set(GENE_KEY, GENE_VALUE, this.context);
        verify(xwikiObject, times(1)).set(STATUS_KEY, JSON_OLD_SOLVED_GENE_KEY, this.context);
        verify(xwikiObject, times(1)).set(eq(STRATEGY_KEY), eq(Collections.emptyList()), eq(this.context));
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
        verify(this.doc).removeXObjects(Gene.GENE_CLASS);

        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveUpdatesGenesWhenPolicyIsUpdate() throws XWikiException
    {
        List<Gene> data = new LinkedList<>();
        Gene item = new PhenoTipsGene(null, "GENE1", null, null, "Notes1");
        data.add(item);
        item = new PhenoTipsGene(null, "GENE2", null, null, null);
        data.add(item);
        when(this.patient.<Gene>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, data));

        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        BaseObject o1 = mock(BaseObject.class);
        BaseObject o2 = mock(BaseObject.class);
        when(this.doc.newXObject(Gene.GENE_CLASS, this.context)).thenReturn(o1, o2);

        this.component.save(this.patient);

        verify(this.doc).removeXObjects(Gene.GENE_CLASS);
        verify(o1).set(GENE_KEY, "GENE1", this.context);
        verify(o1).set(COMMENTS_KEY, "Notes1", this.context);
        verify(o2).set(GENE_KEY, "GENE2", this.context);
        verify(o2, never()).set(eq(COMMENTS_KEY), anyString(), eq(this.context));
    }

    @Test
    public void saveReplacesGenesWhenPolicyIsReplace() throws XWikiException
    {
        List<Gene> data = new LinkedList<>();
        Gene gene = new PhenoTipsGene("GENE1", null, null, null, "Notes1");
        Gene gene2 = new PhenoTipsGene("GENE2", null, null, null, null);
        data.add(gene);
        data.add(gene2);
        when(this.patient.<Gene>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, data));

        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        BaseObject o1 = mock(BaseObject.class);
        BaseObject o2 = mock(BaseObject.class);
        when(this.doc.newXObject(Gene.GENE_CLASS, this.context)).thenReturn(o1, o2);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc).removeXObjects(Gene.GENE_CLASS);
        verify(o1).set(GENE_KEY, "GENE1", this.context);
        verify(o1).set(COMMENTS_KEY, "Notes1", this.context);
        verify(o2).set(GENE_KEY, "GENE2", this.context);
        verify(o2, never()).set(eq(COMMENTS_KEY), anyString(), eq(this.context));
    }

    @Test
    public void saveMergesGenesWhenPolicyIsMerge() throws XWikiException
    {
        List<Gene> data = new LinkedList<>();
        Gene gene = new PhenoTipsGene("GENE1", null, null, null, "Notes1");
        Gene gene2 = new PhenoTipsGene("GENE2", null, null, null, null);
        data.add(gene);
        data.add(gene2);
        when(this.patient.<Gene>getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, data));

        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        BaseObject o1 = mock(BaseObject.class);
        BaseObject o2 = mock(BaseObject.class);
        BaseObject o3 = mock(BaseObject.class);
        when(this.doc.getXObjects(Gene.GENE_CLASS)).thenReturn(Collections.singletonList(o3));

        final BaseStringProperty geneProperty = mock(BaseStringProperty.class);
        final BaseStringProperty statusProperty = mock(BaseStringProperty.class);
        when(o3.getFieldList()).thenReturn(Arrays.asList(GENE_KEY, STATUS_KEY));
        when(o3.getField(GENE_KEY)).thenReturn(geneProperty);
        when(o3.getField(STATUS_KEY)).thenReturn(statusProperty);
        when(o3.getField(STRATEGY_KEY)).thenReturn(null);
        when(o3.getField(COMMENTS_KEY)).thenReturn(null);
        when(geneProperty.getValue()).thenReturn(GENE_VALUE);
        when(statusProperty.getValue()).thenReturn(STATUS_KEY);

        when(this.doc.newXObject(Gene.GENE_CLASS, this.context)).thenReturn(o3, o1, o2);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).removeXObjects(Gene.GENE_CLASS);
        verify(this.doc, times(3)).newXObject(Gene.GENE_CLASS, this.context);
        verify(this.doc, times(1)).getXObjects(Gene.GENE_CLASS);
        verify(o1).set(GENE_KEY, "GENE1", this.context);
        verify(o1).set(COMMENTS_KEY, "Notes1", this.context);
        verify(o2).set(GENE_KEY, "GENE2", this.context);
        verify(o2, never()).set(eq(COMMENTS_KEY), anyString(), eq(this.context));
        verify(o3, times(1)).set(GENE_KEY, GENE_VALUE, this.context);
        verify(o3, times(1)).set(STATUS_KEY, JSON_OLD_CANDIDATE_GENE_KEY, this.context);
        verify(o3, times(1)).set(eq(STRATEGY_KEY), eq(Collections.emptyList()), eq(this.context));
        verify(o3, never()).set(eq(COMMENTS_KEY), any(), eq(this.context));
        verify(o3, times(1)).getFieldList();
        verify(o3, times(4)).getField(anyString());

        verifyNoMoreInteractions(this.doc);
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
