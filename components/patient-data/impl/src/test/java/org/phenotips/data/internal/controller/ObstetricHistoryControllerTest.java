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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ObstetricHistoryControllerTest
{

    private static final Integer AGE_NON_ZERO = 1; // Arbitrary age.

    private static final Integer AGE_ZERO = 0;

    private static final String PREFIX = "pregnancy_history__";

    private static final String GRAVIDA = "gravida";

    private static final String PARA = "para";

    private static final String TERM = "term";

    private static final String PRETERM = "preterm";

    private static final String SAB = "sab";

    private static final String TAB = "tab";

    private static final String LIVE_BIRTHS = "births";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Integer>>(ObstetricHistoryController.class);

    private DocumentAccessBridge documentAccessBridge;

    private ObstetricHistoryController obstetricHistoryController;

    private DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");

    @Mock
    private Patient patient;

    @Mock
    private PatientData<Integer> mockPatientData;

    @Mock
    private Logger logger;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private Provider<XWikiContext> provider;

    private XWikiContext xWikiContext;

    @Mock
    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.obstetricHistoryController =
            (ObstetricHistoryController) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        this.provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xWikiContext = this.provider.get();
        doReturn(this.xwiki).when(this.xWikiContext).getWiki();

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        doReturn(this.patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(this.patientDocument);
    }

    @Test
    public void loadHandlesEmptyPatientTest()
    {
        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));

        PatientData<Integer> testPatientData = this.obstetricHistoryController.load(this.patient);

        Assert.assertNull(testPatientData);
        verify(this.logger).debug("No data for patient [{}]", this.patientDocument);
    }

    @Test
    public void loadDefaultBehaviourTest()
    {
        doReturn(this.data).when(this.doc).getXObject(any(EntityReference.class));
        doReturn(AGE_ZERO).when(this.data).getIntValue(PREFIX + GRAVIDA);
        doReturn(AGE_ZERO).when(this.data).getIntValue(PREFIX + PARA);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + TERM);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + PRETERM);
        doReturn(AGE_ZERO).when(this.data).getIntValue(PREFIX + SAB);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + TAB);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + LIVE_BIRTHS);

        PatientData<Integer> testPatientData = this.obstetricHistoryController.load(this.patient);

        Assert.assertEquals(testPatientData.getName(), "obstetric-history");
        Assert.assertTrue(testPatientData.get(TERM) == 1);
        Assert.assertTrue(testPatientData.get(PRETERM) == 1);
        Assert.assertTrue(testPatientData.get(TAB) == 1);
        Assert.assertTrue(testPatientData.get(LIVE_BIRTHS) == 1);
        Assert.assertEquals(testPatientData.size(), 4);
    }

    @Test
    public void loadIgnoresNoData()
    {
        doReturn(this.data).when(this.doc).getXObject(any(EntityReference.class));
        Assert.assertNull(this.obstetricHistoryController.load(this.patient));
    }

    @Test
    public void loadCatchesUnforeseenExceptions() throws Exception
    {
        Exception testException = new Exception("Test Exception");
        doThrow(testException).when(this.documentAccessBridge).getDocument(this.patientDocument);

        this.obstetricHistoryController.load(this.patient);

        verify(this.logger).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading ", "Test Exception");
    }

    @Test
    public void saveHandlesEmptyPatientTest() throws XWikiException
    {
        doReturn(null).when(this.patient).getData(this.obstetricHistoryController.getName());

        this.obstetricHistoryController.save(this.patient);

        verifyNoMoreInteractions(this.data);
        verify(this.xWikiContext.getWiki(), never()).saveDocument(this.doc,
            "Updated obstetric history from JSON", true, this.xWikiContext);
    }

    @Test
    public void saveDefaultBehaviourTest() throws XWikiException
    {
        doReturn(this.mockPatientData).when(this.patient).getData(this.obstetricHistoryController.getName());
        doReturn(true).when(this.mockPatientData).isNamed();

        doReturn(AGE_NON_ZERO).when(this.mockPatientData).get(GRAVIDA);
        doReturn(AGE_NON_ZERO).when(this.mockPatientData).get(PARA);
        doReturn(AGE_ZERO).when(this.mockPatientData).get(TERM);
        doReturn(AGE_ZERO).when(this.mockPatientData).get(PRETERM);
        doReturn(AGE_NON_ZERO).when(this.mockPatientData).get(SAB);
        doReturn(AGE_NON_ZERO).when(this.mockPatientData).get(TAB);
        doReturn(AGE_ZERO).when(this.mockPatientData).get(LIVE_BIRTHS);

        doReturn(this.data).when(this.doc).getXObject(any(EntityReference.class), eq(true), eq(this.xWikiContext));

        this.obstetricHistoryController.save(this.patient);

        verify(this.xWikiContext.getWiki()).saveDocument(this.doc, "Updated obstetric history from JSON", true,
            this.xWikiContext);
    }

    @Test
    public void saveHandlesExceptionsTest() throws Exception
    {
        Exception testException = new Exception("Test Exception");
        doThrow(testException).when(this.documentAccessBridge).getDocument(this.patientDocument);

        this.obstetricHistoryController.save(this.patient);

        verify(this.logger).error("Failed to save obstetric history: [{}]", "Test Exception");
    }

    @Test
    public void writeJSONWithoutSelectedFieldsTest()
    {
        Map<String, Integer> testData = new LinkedHashMap<String, Integer>();
        testData.put(GRAVIDA, AGE_NON_ZERO);
        testData.put(PARA, AGE_ZERO);
        JSONObject json = new JSONObject();
        PatientData<Integer> testObstetricHistoryData =
            new DictionaryPatientData<Integer>("obstetric-history", testData);
        doReturn(testObstetricHistoryData).when(this.patient).getData(this.obstetricHistoryController.getName());

        this.obstetricHistoryController.writeJSON(this.patient, json);
        Assert.assertNotNull(json);
        Assert.assertTrue(new JSONObject(testData).similar(
            json.getJSONObject("prenatal_perinatal_history").get("obstetric-history")));
    }

    @Test
    public void writeJSONWithoutObstetricHistoryField()
    {
        JSONObject json = new JSONObject();
        Collection<String> fieldList = new ArrayList<>();
        fieldList.add("test field");

        this.obstetricHistoryController.writeJSON(this.patient, json, fieldList);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONIgnoresMissingData()
    {
        JSONObject json = new JSONObject();

        this.obstetricHistoryController.writeJSON(this.patient, json, null);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void readJSONObjectWithNoData()
    {
        JSONObject json = new JSONObject();
        json.put("prenatal_perinatal_history", "obstetric-history");

        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(json);
        Assert.assertNull(patientData);
    }

    @Test
    public void readJSONIgnoresNullParameter()
    {
        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(null);
        Assert.assertNull(patientData);
    }

    @Test
    public void readJSONIgnoresEmptyParameter()
    {
        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(new JSONObject());
        Assert.assertNull(patientData);
    }

    @Test
    public void readJSONDefaultBehaviour()
    {
        JSONObject obstetricData = new JSONObject();
        obstetricData.put(TERM, 0);
        JSONObject json = new JSONObject();
        JSONObject container = json;
        for (String path : StringUtils.split("prenatal_perinatal_history.obstetric-history", '.')) {
            JSONObject parent = container;
            container = parent.optJSONObject(path);
            if (container == null) {
                parent.put(path, new JSONObject());
                container = parent.optJSONObject(path);
            }
        }
        for (String key : obstetricData.keySet()) {
            container.put(key, obstetricData.get(key));
        }

        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(json);
        Assert.assertEquals("obstetric-history", patientData.getName());
        Assert.assertTrue(patientData.get(TERM) == 0);
    }
}
