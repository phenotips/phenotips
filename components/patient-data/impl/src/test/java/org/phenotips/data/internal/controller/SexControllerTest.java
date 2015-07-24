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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link LifeStatusController} component,
 * implementation of the {@link org.phenotips.data.PatientDataController} interface
 */
public class SexControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController> mocker =
            new MockitoComponentMockingRule<PatientDataController>(SexController.class);

    private Logger logger;

    private DocumentAccessBridge documentAccessBridge;

    private Execution execution;

    private SexController controller;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private static final String DATA_NAME = "sex";

    private static final String INTERNAL_PROPERTY_NAME = "gender";

    private static final String SEX_MALE = "M";

    private static final String SEX_FEMALE = "F";

    private static final String SEX_OTHER = "O";

    private static final String SEX_UNKNOWN = "U";

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.controller = (SexController)this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.execution = this.mocker.getInstance(Execution.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        doReturn(this.executionContext).when(this.execution).getContext();
        doReturn(this.xcontext).when(this.executionContext).getProperty("xwikicontext");
        doReturn(this.xwiki).when(this.xcontext).getWiki();
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<String> result = this.controller.load(this.patient);

        verify(this.logger).error("Failed to load patient gender: [{}]", (String)null);
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.controller.load(this.patient);

        verify(this.logger).error("Failed to load patient gender: [{}]",
            PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsCorrectSex()
    {
        doReturn(SEX_MALE).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        PatientData<String> result = this.controller.load(this.patient);
        Assert.assertEquals(SEX_MALE, result.getValue());

        doReturn(SEX_FEMALE).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.controller.load(this.patient);
        Assert.assertEquals(SEX_FEMALE, result.getValue());

        doReturn(SEX_OTHER).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.controller.load(this.patient);
        Assert.assertEquals(SEX_OTHER, result.getValue());
    }

    @Test
    public void loadReturnsSexUnknown()
    {
        doReturn(SEX_UNKNOWN).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        PatientData<String> result = this.controller.load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());

        doReturn("!!!!!").when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.controller.load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());

        doReturn(null).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.controller.load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());
    }

    @Test
    public void saveCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        this.controller.save(this.patient);

        verify(this.logger).error("Failed to save patient gender: [{}]", (String)null);
    }

    @Test
    public void saveCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        this.controller.save(this.patient);

        verify(this.logger).error("Failed to save patient gender: [{}]",
                PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
    }

    @Test
    public void saveCatchesExceptionFromSaveDocument() throws XWikiException
    {
        XWikiException exception = new XWikiException();
        doThrow(exception).when(this.xwiki).saveDocument(any(XWikiDocument.class),
                anyString(), anyBoolean(), any(XWikiContext.class));
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);

        this.controller.save(this.patient);

        verify(this.xwiki).saveDocument(any(XWikiDocument.class),
                anyString(), anyBoolean(), any(XWikiContext.class));
        verify(this.logger).error("Failed to save patient gender: [{}]",
                exception.getMessage());
    }

    @Test
    public void saveSetsCorrectSex() throws XWikiException
    {
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        this.controller.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_MALE);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        this.controller.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_FEMALE);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_OTHER)).when(this.patient).getData(DATA_NAME);
        this.controller.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_OTHER);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, SEX_UNKNOWN)).when(this.patient).getData(DATA_NAME);
        this.controller.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_UNKNOWN);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, null)).when(this.patient).getData(DATA_NAME);
        this.controller.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, null);
        verify(this.xwiki).saveDocument(this.doc, "Updated gender from JSON", true, this.xcontext);
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGenderNotSelected()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("dates");

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONAddsSex()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);

        Assert.assertEquals(SEX_FEMALE, json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSex()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(INTERNAL_PROPERTY_NAME);

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(SEX_FEMALE, json.get(DATA_NAME));
    }

    @Test
    public void readJSONEmptyJsonReturnsNull()
    {
        Assert.assertNull(this.controller.readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsCorrectSex()
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, SEX_MALE);
        PatientData<String> result = this.controller.readJSON(json);
        Assert.assertEquals(SEX_MALE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, SEX_FEMALE);
        result = this.controller.readJSON(json);
        Assert.assertEquals(SEX_FEMALE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, SEX_OTHER);
        result = this.controller.readJSON(json);
        Assert.assertEquals(SEX_OTHER, result.getValue());
    }

    @Test
    public void readJSONReturnsUnknownSex()
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, SEX_UNKNOWN);

        PatientData<String> result = this.controller.readJSON(json);

        //Change this line to: Assert.assertEquals(SEX_UNKNOWN, result.getValue())
        //if PhenoTips Implementation Changes to Support Unknown Gender
        Assert.assertEquals("", result.getValue());
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.controller.getName());
    }
}
