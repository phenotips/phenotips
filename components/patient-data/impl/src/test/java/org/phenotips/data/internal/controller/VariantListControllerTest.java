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

import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.ListClass;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link VariantListController} Component, only the overridden methods from
 * {@link AbstractComplexController} are tested here.
 */
public class VariantListControllerTest
{
    private static final String VARIANTS_STRING = "variants";

    private static final String CONTROLLER_NAME = VARIANTS_STRING;

    private static final String VARIANTS_ENABLING_FIELD_NAME = "genes";

    private static final String VARIANT_KEY = "cdna";

    private static final String GENE_KEY = "gene";

    private static final String PROTEIN_KEY = "protein";

    private static final String TRANSCRIPT_KEY = "transcript";

    private static final String DBSNP_KEY = "dbsnp";

    private static final String ZYGOSITY_KEY = "zygosity";

    private static final String EFFECT_KEY = "effect";

    private static final String INTERPRETATION_KEY = "interpretation";

    private static final String INHERITANCE_KEY = "inheritance";

    private static final String EVIDENCE_KEY = "evidence";

    private static final String SEGREGATION_KEY = "segregation";

    private static final String SANGER_KEY = "sanger";

    private static final String CHROMOSOME_KEY = "chromosome";

    private static final String START_POSITION_KEY = "start_position";

    private static final String END_POSITION_KEY = "end_position";

    private static final String REFERENCE_GENOME_KEY = "reference_genome";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Map<String, String>>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Map<String, String>>>(VariantListController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    private List<BaseObject> variantXWikiObjects;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();

        this.variantXWikiObjects = new LinkedList<>();
        doReturn(this.variantXWikiObjects).when(this.doc).getXObjects(any(EntityReference.class));
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

        Assert.assertEquals(16, result.size());
        Assert.assertThat(result, Matchers.hasItem(VARIANT_KEY));
        Assert.assertThat(result, Matchers.hasItem(GENE_KEY));
        Assert.assertThat(result, Matchers.hasItem(PROTEIN_KEY));
        Assert.assertThat(result, Matchers.hasItem(TRANSCRIPT_KEY));
        Assert.assertThat(result, Matchers.hasItem(DBSNP_KEY));
        Assert.assertThat(result, Matchers.hasItem(ZYGOSITY_KEY));
        Assert.assertThat(result, Matchers.hasItem(EFFECT_KEY));
        Assert.assertThat(result, Matchers.hasItem(INTERPRETATION_KEY));
        Assert.assertThat(result, Matchers.hasItem(INHERITANCE_KEY));
        Assert.assertThat(result, Matchers.hasItem(EVIDENCE_KEY));
        Assert.assertThat(result, Matchers.hasItem(SEGREGATION_KEY));
        Assert.assertThat(result, Matchers.hasItem(SANGER_KEY));
        Assert.assertThat(result, Matchers.hasItem(CHROMOSOME_KEY));
        Assert.assertThat(result, Matchers.hasItem(START_POSITION_KEY));
        Assert.assertThat(result, Matchers.hasItem(END_POSITION_KEY));
        Assert.assertThat(result, Matchers.hasItem(REFERENCE_GENOME_KEY));
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
    public void loadCatchesInvalidDocument() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getXDocument();

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenPatientDoesNotHaveVariantClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenVariantListEmpty() throws ComponentLookupException
    {
        doReturn(new LinkedList<BaseObject>()).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenFields() throws ComponentLookupException
    {
        BaseObject obj = mock(BaseObject.class);
        doReturn(null).when(obj).getField(anyString());
        this.variantXWikiObjects.add(obj);

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadIgnoresNullVariants() throws ComponentLookupException
    {
        // Deleted objects appear as nulls in XWikiObjects list
        this.variantXWikiObjects.add(null);
        addVariantFields(INTERPRETATION_KEY, new String[] { "pathogenic" });
        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(1, result.size());
    }

    @Test
    public void checkLoadParsingOfInterpretationKey() throws ComponentLookupException
    {
        addVariantFields(INTERPRETATION_KEY, new String[] {
            "pathogenic", "likely_pathogenic", "likely_benign", "benign", "variant_u_s", "investigation_n" });

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals("pathogenic", result.get(0).get(INTERPRETATION_KEY));
        Assert.assertEquals("likely_pathogenic", result.get(1).get(INTERPRETATION_KEY));
        Assert.assertEquals("likely_benign", result.get(2).get(INTERPRETATION_KEY));
        Assert.assertEquals("benign", result.get(3).get(INTERPRETATION_KEY));
        Assert.assertEquals("variant_u_s", result.get(4).get(INTERPRETATION_KEY));
        Assert.assertEquals("investigation_n", result.get(5).get(INTERPRETATION_KEY));
    }

    @Test
    public void checkLoadParsingOfSegregationKey() throws ComponentLookupException
    {
        addVariantFields(SEGREGATION_KEY, new String[] { "complete", "in_progress", "not_done" });

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals("complete", result.get(0).get(SEGREGATION_KEY));
        Assert.assertEquals("in_progress", result.get(1).get(SEGREGATION_KEY));
        Assert.assertEquals("not_done", result.get(2).get(SEGREGATION_KEY));
    }

    @Test
    public void checkLoadParsingOfSangerKey() throws ComponentLookupException
    {
        addVariantFields(SANGER_KEY, new String[] { "complete", "in_progress", "not_done" });

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals("complete", result.get(0).get(SANGER_KEY));
        Assert.assertEquals("in_progress", result.get(1).get(SANGER_KEY));
        Assert.assertEquals("not_done", result.get(2).get(SANGER_KEY));
    }

    @Test
    public void checkLoadParsingOfEvidenceKey() throws ComponentLookupException
    {
        addVariantFields(EVIDENCE_KEY, new String[] { "rare", "predicted", "reported", "segregates" });

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals("rare", result.get(0).get(EVIDENCE_KEY));
        Assert.assertEquals("predicted", result.get(1).get(EVIDENCE_KEY));
        Assert.assertEquals("reported", result.get(2).get(EVIDENCE_KEY));
        Assert.assertEquals("segregates", result.get(3).get(EVIDENCE_KEY));
    }

    @Test
    public void checkLoadParsingOfEffectKey() throws ComponentLookupException
    {
        addVariantFields(EFFECT_KEY, new String[] {
            "insertion_in_frame", "insertion_frameshift", "deletion_in_frame", "deletion_frameshift", "indel_in_frame",
            "indel_frameshift", "repeat_expansion", "synonymous" });

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals("insertion_in_frame", result.get(0).get(EFFECT_KEY));
        Assert.assertEquals("insertion_frameshift", result.get(1).get(EFFECT_KEY));
        Assert.assertEquals("deletion_in_frame", result.get(2).get(EFFECT_KEY));
        Assert.assertEquals("deletion_frameshift", result.get(3).get(EFFECT_KEY));
        Assert.assertEquals("indel_in_frame", result.get(4).get(EFFECT_KEY));
        Assert.assertEquals("indel_frameshift", result.get(5).get(EFFECT_KEY));
        Assert.assertEquals("repeat_expansion", result.get(6).get(EFFECT_KEY));
        Assert.assertEquals("synonymous", result.get(7).get(EFFECT_KEY));
    }

    @Test
    public void checkLoadParsingOfInheritanceKey() throws ComponentLookupException
    {
        addVariantFields(INHERITANCE_KEY, new String[] { "denovo_germline", "denovo_s_mosaicism", "maternal" });

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals("denovo_germline", result.get(0).get(INHERITANCE_KEY));
        Assert.assertEquals("denovo_s_mosaicism", result.get(1).get(INHERITANCE_KEY));
        Assert.assertEquals("maternal", result.get(2).get(INHERITANCE_KEY));
    }

    @Test
    public void checkLoadParsingOfOtherKeys() throws ComponentLookupException
    {
        BaseObject obj = mock(BaseObject.class);
        BaseStringProperty property = mock(BaseStringProperty.class);
        doReturn("Variant").when(property).getValue();
        doReturn(property).when(obj).getField(VARIANT_KEY);

        property = mock(BaseStringProperty.class);
        doReturn("Gene").when(property).getValue();
        doReturn(property).when(obj).getField(GENE_KEY);

        property = mock(BaseStringProperty.class);
        doReturn("Protein").when(property).getValue();
        doReturn(property).when(obj).getField(PROTEIN_KEY);

        property = mock(BaseStringProperty.class);
        doReturn("Transcript").when(property).getValue();
        doReturn(property).when(obj).getField(TRANSCRIPT_KEY);

        property = mock(BaseStringProperty.class);
        doReturn("DBSNP").when(property).getValue();
        doReturn(property).when(obj).getField(DBSNP_KEY);

        property = mock(BaseStringProperty.class);
        doReturn("Zygosity").when(property).getValue();
        doReturn(property).when(obj).getField(ZYGOSITY_KEY);

        List<String> list = new ArrayList<>();
        list.add("variants");
        doReturn(list).when(obj).getFieldList();

        this.variantXWikiObjects.add(obj);

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals("Variant", result.get(0).get(VARIANT_KEY));
        Assert.assertEquals("Gene", result.get(0).get(GENE_KEY));
        Assert.assertEquals("Protein", result.get(0).get(PROTEIN_KEY));
        Assert.assertEquals("Transcript", result.get(0).get(TRANSCRIPT_KEY));
        Assert.assertEquals("DBSNP", result.get(0).get(DBSNP_KEY));
        Assert.assertEquals("Zygosity", result.get(0).get(ZYGOSITY_KEY));
    }

    // --------------------writeJSON() is Overridden from AbstractSimpleController--------------------

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNotNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(VARIANTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);
    }

    @Test
    public void writeJSONReturnsWhenDataIsNotEmpty() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(VARIANTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(CONTROLLER_NAME));
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

        Assert.assertFalse(json.has(CONTROLLER_NAME));
    }

    @Test
    public void writeJSONIgnoresItemsWhenCDNAIsBlank() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(VARIANT_KEY, "");
        internalList.add(item);

        item = new LinkedHashMap<>();
        item.put(VARIANT_KEY, null);
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(VARIANTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        Assert.assertEquals(0, json.getJSONArray(CONTROLLER_NAME).length());
    }

    @Test
    public void writeJSONAddsContainerWithAllValuesWhenSelectedFieldsNull() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(VARIANT_KEY, "variantName");
        item.put(GENE_KEY, "");
        item.put(PROTEIN_KEY, null);
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        JSONObject result = json.getJSONArray(CONTROLLER_NAME).getJSONObject(0);
        Assert.assertEquals("variantName", result.get(VARIANT_KEY));
    }

    @Test
    public void writeJSONAddsContainerWithOnlySelectedFields() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();

        Map<String, String> item = new LinkedHashMap<>();
        item.put(VARIANT_KEY, "variantName");
        item.put(GENE_KEY, "gene");
        item.put(PROTEIN_KEY, "Protein");
        item.put(TRANSCRIPT_KEY, "Transcript");
        item.put(DBSNP_KEY, "DBSNP");
        item.put(EFFECT_KEY, "Effect");
        item.put(INTERPRETATION_KEY, "Interpretation");
        item.put(INHERITANCE_KEY, "Inheritance");
        item.put(EVIDENCE_KEY, "Evidence");
        item.put(SANGER_KEY, "Sanger");
        item.put(SEGREGATION_KEY, "Segregation");
        item.put(ZYGOSITY_KEY, "Zygosity");
        internalList.add(item);

        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(VARIANTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        JSONObject result = json.getJSONArray(CONTROLLER_NAME).getJSONObject(0);
        Assert.assertEquals("variantName", result.get(VARIANT_KEY));
        Assert.assertEquals("gene", result.get(GENE_KEY));
        Assert.assertEquals("Protein", result.get(PROTEIN_KEY));
        Assert.assertEquals("Transcript", result.get(TRANSCRIPT_KEY));
        Assert.assertEquals("DBSNP", result.get(DBSNP_KEY));
        Assert.assertEquals("Zygosity", result.get(ZYGOSITY_KEY));

        json = new JSONObject();
        internalList = new LinkedList<>();
        item = new LinkedHashMap<>();
        item.put(VARIANT_KEY, "variantName");
        item.put(GENE_KEY, "gene");
        item.put(PROTEIN_KEY, "Protein");
        item.put(TRANSCRIPT_KEY, "Transcript");
        item.put(DBSNP_KEY, "DBSNP");
        item.put(EFFECT_KEY, "Effect");
        item.put(INTERPRETATION_KEY, "Interpretation");
        item.put(INHERITANCE_KEY, "Inheritance");
        item.put(EVIDENCE_KEY, "Evidence");
        item.put(SANGER_KEY, "Sanger");
        item.put(SEGREGATION_KEY, "Segregation");
        item.put(ZYGOSITY_KEY, "Zygosity");
        internalList.add(item);
        patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        selectedFields = new LinkedList<>();
        selectedFields.add(VARIANTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        result = json.getJSONArray(CONTROLLER_NAME).getJSONObject(0);
        Assert.assertEquals("variantName", result.get(VARIANT_KEY));
        Assert.assertEquals(12, result.length());
    }

    // ----------------------------------------Private methods----------------------------------------

    private void addVariantFields(String key, String[] fieldValues)
    {
        BaseObject obj;
        if (EVIDENCE_KEY.equals(key)) {
            StringListProperty property;
            for (String value : fieldValues) {
                obj = mock(BaseObject.class);
                property = mock(StringListProperty.class);
                List<String> values = new ArrayList<>();
                values.add(value);
                doReturn(property).when(obj).getField(key);
                doReturn(values).when(obj).getFieldList();
                doReturn(values).when(property).getList();
                doReturn(ListClass.getStringFromList(values, ListClass.DEFAULT_SEPARATOR)).when(property)
                    .getTextValue();
                this.variantXWikiObjects.add(obj);
            }
        } else {
            BaseStringProperty property;
            for (String value : fieldValues) {
                obj = mock(BaseObject.class);
                property = mock(BaseStringProperty.class);
                List<String> list = new ArrayList<>();
                list.add(value);
                doReturn(value).when(property).getValue();
                doReturn(property).when(obj).getField(key);
                doReturn(list).when(obj).getFieldList();
                this.variantXWikiObjects.add(obj);
            }
        }

    }
}
