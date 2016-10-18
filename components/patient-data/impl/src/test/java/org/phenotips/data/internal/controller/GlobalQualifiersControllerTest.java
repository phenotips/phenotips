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

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class GlobalQualifiersControllerTest
{
    private static final String ONSET = "global_age_of_onset";

    private static final String NEONATAL = "HP:0003623";

    private static final String INHERITANCE = "global_mode_of_inheritance";

    private static final String GONOSOMAL = "HP:0010985";

    private static final String MITOCHONDRIAL = "HP:0001427";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<List<VocabularyTerm>>> mocker =
        new MockitoComponentMockingRule<PatientDataController<List<VocabularyTerm>>>(GlobalQualifiersController.class);

    private PatientDataController<List<VocabularyTerm>> tested;

    @Mock
    private Patient patient;

    @Mock
    private PatientData<List<VocabularyTerm>> mockPatientData;

    @Mock
    private Logger logger;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    @Mock
    private BaseClass patientClass;

    private Provider<XWikiContext> provider;

    private XWikiContext xWikiContext;

    @Mock
    private XWiki xwiki;

    @Mock
    private VocabularyTerm neonatalTerm;

    @Mock
    private VocabularyTerm gonosomalTerm;

    @Mock
    private VocabularyTerm mitochondrialTerm;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.tested = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        this.provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xWikiContext = this.provider.get();
        doReturn(this.xwiki).when(this.xWikiContext).getWiki();

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "P0000001");
        doReturn(patientDocument).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getDocument();

        VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);
        when(this.neonatalTerm.getId()).thenReturn(NEONATAL);
        when(vocabularyManager.resolveTerm(NEONATAL)).thenReturn(this.neonatalTerm);
        when(this.gonosomalTerm.getId()).thenReturn(GONOSOMAL);
        when(vocabularyManager.resolveTerm(GONOSOMAL)).thenReturn(this.gonosomalTerm);
        when(this.mitochondrialTerm.getId()).thenReturn(MITOCHONDRIAL);
        when(vocabularyManager.resolveTerm(MITOCHONDRIAL)).thenReturn(this.mitochondrialTerm);

        when(this.data.getXClass(this.xWikiContext)).thenReturn(this.patientClass);
        StringClass onsetClass = mock(StringClass.class);
        when(this.patientClass.get(ONSET)).thenReturn(onsetClass);
        when(onsetClass.newProperty()).thenReturn(new StringProperty());
        StringClass inheritanceClass = mock(StringClass.class);
        when(this.patientClass.get(INHERITANCE)).thenReturn(inheritanceClass);
        when(inheritanceClass.newProperty()).thenReturn(new DBStringListProperty());
    }

    @Test
    public void loadIgnoresEmptyPatient()
    {
        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));

        PatientData<List<VocabularyTerm>> testPatientData = this.tested.load(this.patient);

        Assert.assertNull(testPatientData);
    }

    @Test
    public void loadDefaultBehaviourTest() throws XWikiException
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(this.data);

        StringProperty onset = new StringProperty();
        onset.setValue(NEONATAL);
        when(this.data.get(ONSET)).thenReturn(onset);

        DBStringListProperty inheritance = new DBStringListProperty();
        inheritance.setValue(Arrays.asList(GONOSOMAL, MITOCHONDRIAL));
        when(this.data.get(INHERITANCE)).thenReturn(inheritance);

        PatientData<List<VocabularyTerm>> testPatientData = this.tested.load(this.patient);

        Assert.assertEquals("global-qualifiers", testPatientData.getName());
        Assert.assertEquals(2, testPatientData.size());

        List<VocabularyTerm> result = testPatientData.get(ONSET);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(NEONATAL, result.get(0).getId());

        result = testPatientData.get(INHERITANCE);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(GONOSOMAL, result.get(0).getId());
        Assert.assertEquals(MITOCHONDRIAL, result.get(1).getId());
    }

    @Test
    public void loadIgnoresEmptyValues() throws XWikiException
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(this.data);

        StringProperty onset = new StringProperty();
        onset.setValue("");
        when(this.data.get(ONSET)).thenReturn(onset);

        DBStringListProperty inheritance = new DBStringListProperty();
        inheritance.setValue(Collections.singletonList(""));
        when(this.data.get(INHERITANCE)).thenReturn(inheritance);

        PatientData<List<VocabularyTerm>> testPatientData = this.tested.load(this.patient);

        Assert.assertEquals("global-qualifiers", testPatientData.getName());
        Assert.assertEquals(2, testPatientData.size());
        Assert.assertTrue(testPatientData.get(ONSET).isEmpty());
        Assert.assertTrue(testPatientData.get(INHERITANCE).isEmpty());
    }

    @Test
    public void loadGeneratesEmptyData()
    {
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<List<VocabularyTerm>> testPatientData = this.tested.load(this.patient);

        Assert.assertEquals(2, testPatientData.size());
        Assert.assertTrue(testPatientData.get(ONSET).isEmpty());
        Assert.assertTrue(testPatientData.get(INHERITANCE).isEmpty());
    }

    @Test
    public void loadCatchesUnforeseenExceptions() throws Exception
    {
        NullPointerException testException = new NullPointerException("Test Exception");
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenThrow(testException);

        this.tested.load(this.patient);

        verify(this.logger).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading ", "Test Exception");
    }

    @Test
    public void saveHandlesEmptyPatientTest() throws XWikiException
    {
        when(this.patient.getData(this.tested.getName())).thenReturn(null);

        this.tested.save(this.patient, this.doc);

        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveDefaultBehaviourTest() throws XWikiException
    {
        when(this.patient.<List<VocabularyTerm>>getData(this.tested.getName()))
            .thenReturn(setupMockPatientData(this.neonatalTerm, this.gonosomalTerm, this.mitochondrialTerm));

        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE, true, this.xWikiContext);

        this.tested.save(this.patient, this.doc);
        verify(this.data).set(ONSET, NEONATAL, this.xWikiContext);
        verify(this.data).set(INHERITANCE, Arrays.asList(GONOSOMAL, MITOCHONDRIAL), this.xWikiContext);
    }

    @Test
    public void writeJSONWithoutSelectedFieldsTest()
    {
        when(this.patient.<List<VocabularyTerm>>getData(this.tested.getName()))
            .thenReturn(setupMockPatientData(this.neonatalTerm, this.gonosomalTerm, this.mitochondrialTerm));

        JSONObject json = new JSONObject();

        this.tested.writeJSON(this.patient, json);
        Assert.assertNotNull(json);
        Assert.assertTrue(
            new JSONObject("{\"global_mode_of_inheritance\":[{\"id\":\"HP:0010985\"},{\"id\":\"HP:0001427\"}],"
                + "\"global_age_of_onset\":[{\"id\":\"HP:0003623\"}]}")
                .similar(json));
    }

    @Test
    public void writeJSONWithoutGlobalInheritanceField()
    {
        when(this.patient.<List<VocabularyTerm>>getData(this.tested.getName()))
            .thenReturn(setupMockPatientData(this.neonatalTerm, this.gonosomalTerm, this.mitochondrialTerm));

        JSONObject json = new JSONObject();

        this.tested.writeJSON(this.patient, json, Collections.singletonList(ONSET));
        Assert.assertNotNull(json);
        Assert.assertTrue(new JSONObject("{\"global_age_of_onset\":[{\"id\":\"HP:0003623\"}]}").similar(json));
    }

    @Test
    public void writeJSONWithoutGlobalOnsetField()
    {
        when(this.patient.<List<VocabularyTerm>>getData(this.tested.getName()))
            .thenReturn(setupMockPatientData(this.neonatalTerm, this.gonosomalTerm, this.mitochondrialTerm));

        JSONObject json = new JSONObject();

        this.tested.writeJSON(this.patient, json, Collections.singletonList(INHERITANCE));
        Assert.assertNotNull(json);
        Assert.assertTrue(
            new JSONObject("{\"global_mode_of_inheritance\":[{\"id\":\"HP:0010985\"},{\"id\":\"HP:0001427\"}]}")
                .similar(json));
    }

    @Test
    public void writeJSONIgnoresMissingData()
    {
        JSONObject json = new JSONObject();
        this.tested.writeJSON(this.patient, json, null);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void readJSONObjectWithNoDataProducesMapWithNulls()
    {
        JSONObject json = new JSONObject();
        json.put(ONSET, 4);
        json.put(INHERITANCE, 2);

        PatientData<List<VocabularyTerm>> patientData = this.tested.readJSON(json);
        Assert.assertNotNull(patientData);
        Assert.assertEquals(2, patientData.size());
        Assert.assertNull(patientData.get(ONSET));
        Assert.assertNull(patientData.get(INHERITANCE));
    }

    @Test
    public void readJSONIgnoresNullParameter()
    {
        PatientData<List<VocabularyTerm>> patientData = this.tested.readJSON(null);
        Assert.assertNull(patientData);
    }

    @Test
    public void readJSONIgnoresEmptyParameter()
    {
        PatientData<List<VocabularyTerm>> patientData = this.tested.readJSON(new JSONObject());
        Assert.assertNotNull(patientData);
        Assert.assertEquals(2, patientData.size());
        Assert.assertNull(patientData.get(ONSET));
        Assert.assertNull(patientData.get(INHERITANCE));
    }

    @Test
    public void readJSONDefaultBehaviour()
    {
        JSONObject json =
            new JSONObject(setupMockPatientDataMap(this.neonatalTerm, this.gonosomalTerm, this.mitochondrialTerm));

        PatientData<List<VocabularyTerm>> patientData = this.tested.readJSON(json);

        Assert.assertEquals(this.tested.getName(), patientData.getName());
        Assert.assertEquals(2, patientData.size());

        List<VocabularyTerm> onsets = patientData.get(ONSET);
        Assert.assertEquals(1, onsets.size());
        Assert.assertEquals(this.neonatalTerm, onsets.get(0));

        List<VocabularyTerm> inheritances = patientData.get(INHERITANCE);
        Assert.assertEquals(2, inheritances.size());
        Assert.assertEquals(this.gonosomalTerm, inheritances.get(0));
        Assert.assertEquals(this.mitochondrialTerm, inheritances.get(1));
    }

    private Map<String, List<VocabularyTerm>> setupMockPatientDataMap(VocabularyTerm onset,
        VocabularyTerm... inheritance)
    {
        Map<String, List<VocabularyTerm>> map = new HashMap<>();
        if (onset != null) {
            map.put(ONSET, Collections.singletonList(onset));
        }
        if (inheritance.length > 0) {
            map.put(INHERITANCE, Arrays.asList(inheritance));
        }
        return map;
    }

    private PatientData<List<VocabularyTerm>> setupMockPatientData(VocabularyTerm onset, VocabularyTerm... inheritance)
    {
        return new DictionaryPatientData<>(this.tested.getName(), setupMockPatientDataMap(onset, inheritance));
    }
}
