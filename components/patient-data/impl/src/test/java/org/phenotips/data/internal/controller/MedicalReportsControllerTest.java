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
import org.phenotips.data.internal.controller.MedicalReportsController.Attachment;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link MedicalReportsController} component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class MedicalReportsControllerTest
{
    @ClassRule
    public static TemporaryFolder tf = new TemporaryFolder();

    private static final String DATA_NAME = "medical_reports";

    private static final String CONTROLLER_NAME = "medicalReports";

    private static final String FIELD_NAME = "reports_history";

    private static final String JSON_FIELD_FILENAME = "filename";

    private static final String JSON_FIELD_FILESIZE = "filesize";

    private static final String JSON_FIELD_AUTHOR = "author";

    private static final String JSON_FIELD_DATE = "date";

    private static final String JSON_FIELD_CONTENT = "content";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Attachment>> mocker =
        new MockitoComponentMockingRule<>(MedicalReportsController.class);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    @Mock
    private XWikiAttachment attachment1;

    @Mock
    private XWikiAttachment attachment2;

    private Attachment a1;

    private Attachment a2;

    private Date date1;

    private Date date2;

    private DocumentReference author1 = new DocumentReference("main", "Users", "padams");

    private DocumentReference author2 = new DocumentReference("genetics", "Users", "hmccoy");

    @BeforeClass
    public static void globalSetUp() throws ComponentLookupException
    {
        ComponentManager rcm = Mockito.mock(ComponentManager.class);
        Utils.setComponentManager(rcm);
        when(rcm.getInstance(ComponentManager.class, "context")).thenReturn(rcm);
        Environment env = Mockito.mock(Environment.class);
        when(rcm.getInstance(Environment.class, "default")).thenReturn(env);
        when(env.getTemporaryDirectory()).thenReturn(tf.getRoot());
    }

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(this.data);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.context)).thenReturn(this.data);

        Calendar c = new GregorianCalendar(2017, 0, 1, 12, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date1 = c.getTime();
        when(this.attachment1.getFilename()).thenReturn("a1.pdf");
        when(this.attachment1.getFilesize()).thenReturn(4);
        when(this.attachment1.getAuthorReference()).thenReturn(this.author1);
        when(this.attachment1.getDate()).thenReturn(this.date1);
        when(this.attachment1.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("abcd", StandardCharsets.UTF_8));
        when(this.doc.getAttachment("a1.pdf")).thenReturn(this.attachment1);
        this.a1 = new Attachment(this.attachment1);

        c = new GregorianCalendar(2016, 7, 1, 14, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date2 = c.getTime();
        when(this.attachment2.getFilename()).thenReturn("a2.pdf");
        when(this.attachment2.getFilesize()).thenReturn(3);
        when(this.attachment2.getAuthorReference()).thenReturn(this.author2);
        when(this.attachment2.getDate()).thenReturn(this.date2);
        when(this.attachment2.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("xyz", StandardCharsets.UTF_8));
        when(this.doc.getAttachment("a2.pdf")).thenReturn(this.attachment2);
        this.a2 = new Attachment(this.attachment2);

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);

        EntityReferenceSerializer<String> userSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(userSerializer.serialize(this.author1)).thenReturn("Users.padams");
        when(userSerializer.serialize(this.author2)).thenReturn("genetics:Users.hmccoy");

        DocumentReferenceResolver<String> userResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "user/current");
        when(userResolver.resolve("Users.padams")).thenReturn(this.author1);
        when(userResolver.resolve("genetics:Users.hmccoy")).thenReturn(this.author2);

        when(this.context.getUserReference()).thenReturn(this.author1);
        when(this.xwiki.exists(this.author1, this.context)).thenReturn(true);
        when(this.xwiki.exists(this.author2, this.context)).thenReturn(true);
    }

    @Test
    public void loadCatchesInvalidDocument() throws ComponentLookupException
    {
        when(this.patient.getXDocument()).thenReturn(null);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAttachments() throws ComponentLookupException
    {
        when(this.data.getListValue(FIELD_NAME)).thenReturn(Arrays.asList("a1.pdf", "a2.pdf"));

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.a1, result.get(0));
        Assert.assertEquals(this.a2, result.get(1));
    }

    @Test
    public void loadSkipsNonExistingAttachments() throws ComponentLookupException
    {
        when(this.data.getListValue("reports_history"))
            .thenReturn(Arrays.asList("a0.pdf", "a1.pdf", "a2.pdf", "a3.pdf"));

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.a1, result.get(0));
        Assert.assertEquals(this.a2, result.get(1));
    }

    @Test
    public void loadSkipsAttachmentsNotSelected() throws ComponentLookupException
    {
        when(this.data.getListValue("reports_history"))
            .thenReturn(Arrays.asList("a2.pdf", "a3.pdf"));

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(this.a2, result.get(0));
    }

    @Test
    public void loadAcceptsGuestAuthor() throws ComponentLookupException
    {
        when(this.data.getListValue("reports_history")).thenReturn(Arrays.asList("a1.pdf"));
        when(this.attachment1.getAuthorReference()).thenReturn(null);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0).getAuthorReference());
    }

    @Test
    public void loadAcceptsInvalidAuthor() throws ComponentLookupException
    {
        when(this.data.getListValue("reports_history")).thenReturn(Arrays.asList("a1.pdf"));
        when(this.xwiki.exists(this.author1, this.context)).thenReturn(false);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(this.author1, result.get(0).getAuthorReference());
    }

    @Test
    public void saveDoesNothingWhenNoDataPresent() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveClearsRecordsFieldWhenEmptyDataPresent() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.data).setDBStringListValue(FIELD_NAME, Collections.emptyList());
    }

    @Test
    public void saveDoesNotRemoveAttachments() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.doc, Mockito.never()).removeAttachment(any(XWikiAttachment.class));
    }

    @Test
    public void saveUpdatesAttachmentsAndMedicalRecordsField() throws ComponentLookupException, IOException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.a1, this.a2)));
        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data).setDBStringListValue(FIELD_NAME, Arrays.asList("a1.pdf", "a2.pdf"));

        verify(this.attachment1).setFilesize(4);
        verify(this.attachment1).setDate(this.date1);
        verify(this.attachment1).setAuthorReference(this.author1);
        verify(this.attachment1).setContent(this.a1.getContent());

        verify(this.attachment2).setFilesize(3);
        verify(this.attachment2).setDate(this.date2);
        verify(this.attachment2).setAuthorReference(this.author2);
        verify(this.attachment2).setContent(this.a2.getContent());
    }

    @Test
    public void saveAddsNewAttachmentsWhenNeeded() throws ComponentLookupException, IOException, XWikiException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.a1, this.a2)));
        when(this.doc.getAttachment("a1.pdf")).thenReturn(null);
        CapturingMatcher<XWikiAttachment> newAttachmentCapturer = new CapturingMatcher<>();
        Mockito.doNothing().when(this.doc).addAttachment(Matchers.argThat(newAttachmentCapturer));

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data).setDBStringListValue(FIELD_NAME, Arrays.asList("a1.pdf", "a2.pdf"));

        XWikiAttachment newAttachment = newAttachmentCapturer.getLastValue();
        Assert.assertEquals("a1.pdf", newAttachment.getFilename());
        Assert.assertEquals(4, newAttachment.getFilesize());
        Assert.assertEquals(this.date1, newAttachment.getDate());
        Assert.assertEquals(this.author1, newAttachment.getAuthorReference());
        Assert.assertArrayEquals("abcd".getBytes(StandardCharsets.ISO_8859_1),
            IOUtils.toByteArray(newAttachment.getContentInputStream(this.context)));

        verify(this.attachment2).setFilesize(3);
        verify(this.attachment2).setDate(this.date2);
        verify(this.attachment2).setAuthorReference(this.author2);
        verify(this.attachment2).setContent(this.a2.getContent());
    }

    @Test
    public void saveUsesCurrentUserWhenSpecifiedUserDoesNotExist() throws ComponentLookupException, IOException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.a1, this.a2)));
        when(this.xwiki.exists(this.author2, this.context)).thenReturn(false);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data).setDBStringListValue(FIELD_NAME, Arrays.asList("a1.pdf", "a2.pdf"));

        verify(this.attachment1).setAuthorReference(this.author1);
        verify(this.attachment2).setAuthorReference(this.author1);
    }

    @Test
    public void saveAcceptsGuestAuthor() throws ComponentLookupException, IOException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.a1)));
        when(this.a1.getAuthorReference()).thenReturn(null);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data).setDBStringListValue(FIELD_NAME, Arrays.asList("a1.pdf"));

        verify(this.attachment1).setAuthorReference(null);
    }

    @Test
    public void writeJSONDoesNothingWhenGetDataReturnsNull() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithOtherSelectedFieldsDoesNothing() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Arrays.asList(this.a1, this.a2)));

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList("others"));

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsStoresEmptyArrayWhenGetDataReturnsNull() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList(FIELD_NAME));

        Assert.assertTrue(json.has(DATA_NAME));
        Assert.assertEquals(0, json.getJSONArray(DATA_NAME).length());
    }

    @Test
    public void writeJSONAddsReports() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Arrays.asList(this.a1, this.a2)));

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(2, result.length());
        JSONObject json1 = result.getJSONObject(0);
        Assert.assertEquals("a1.pdf", json1.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(4L, json1.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("Users.padams", json1.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2017-01-01T12:00:00.000Z", json1.get(JSON_FIELD_DATE));
        Assert.assertEquals("YWJjZA==", json1.get(JSON_FIELD_CONTENT));

        JSONObject json2 = result.getJSONObject(1);
        Assert.assertEquals("a2.pdf", json2.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(3L, json2.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("genetics:Users.hmccoy", json2.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2016-08-01T14:00:00.000Z", json2.get(JSON_FIELD_DATE));
        Assert.assertEquals("eHl6", json2.get(JSON_FIELD_CONTENT));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsReports() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Arrays.asList(this.a1, this.a2)));

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList(FIELD_NAME));

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(2, result.length());
        JSONObject json1 = result.getJSONObject(0);
        Assert.assertEquals("a1.pdf", json1.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(4L, json1.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("Users.padams", json1.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2017-01-01T12:00:00.000Z", json1.get(JSON_FIELD_DATE));
        Assert.assertEquals("YWJjZA==", json1.get(JSON_FIELD_CONTENT));

        JSONObject json2 = result.getJSONObject(1);
        Assert.assertEquals("a2.pdf", json2.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(3L, json2.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("genetics:Users.hmccoy", json2.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2016-08-01T14:00:00.000Z", json2.get(JSON_FIELD_DATE));
        Assert.assertEquals("eHl6", json2.get(JSON_FIELD_CONTENT));
    }

    @Test
    public void writeJSONWithControllerNameAsSelectedFieldsAddsReports() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Arrays.asList(this.a1, this.a2)));

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList(CONTROLLER_NAME));

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(2, result.length());
        JSONObject json1 = result.getJSONObject(0);
        Assert.assertEquals("a1.pdf", json1.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(4L, json1.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("Users.padams", json1.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2017-01-01T12:00:00.000Z", json1.get(JSON_FIELD_DATE));
        Assert.assertEquals("YWJjZA==", json1.get(JSON_FIELD_CONTENT));

        JSONObject json2 = result.getJSONObject(1);
        Assert.assertEquals("a2.pdf", json2.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(3L, json2.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("genetics:Users.hmccoy", json2.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2016-08-01T14:00:00.000Z", json2.get(JSON_FIELD_DATE));
        Assert.assertEquals("eHl6", json2.get(JSON_FIELD_CONTENT));
    }

    @Test
    public void writeJSONWritesNoAuthorForGuestAuthor() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Arrays.asList(this.a1)));
        when(this.attachment1.getAuthorReference()).thenReturn(null);

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList(FIELD_NAME));

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(1, result.length());
        JSONObject json1 = result.getJSONObject(0);
        Assert.assertEquals("a1.pdf", json1.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(4L, json1.get(JSON_FIELD_FILESIZE));
        Assert.assertFalse(json1.has(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2017-01-01T12:00:00.000Z", json1.get(JSON_FIELD_DATE));
        Assert.assertEquals("YWJjZA==", json1.get(JSON_FIELD_CONTENT));
    }

    @Test
    public void readJSONWithEmptyJsonReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readJSONWithEmptyReportsReturnsEmptyData() throws ComponentLookupException
    {
        JSONObject input = new JSONObject();
        input.put(DATA_NAME, new JSONArray());

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(input);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isIndexed());
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readJSONReturnsReports() throws ComponentLookupException
    {
        JSONObject json = new JSONObject("{\"medical_reports\":["
            + "{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"author\":\"Users.padams\","
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "},{"
            + "\"filename\":\"a2.pdf\","
            + "\"filesize\":3,"
            + "\"author\":\"genetics:Users.hmccoy\","
            + "\"date\":\"2016-08-01T14:00:00.000Z\","
            + "\"content\":\"eHl6\""
            + "}]}");

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.a1, result.get(0));
        Assert.assertEquals(this.a2, result.get(1));
    }

    @Test
    public void readJSONAcceptsGuestAuthor() throws ComponentLookupException
    {
        JSONObject json = new JSONObject("{\"medical_reports\":["
            + "{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "}]}");

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0).getAuthorReference());
        when(this.attachment1.getAuthorReference()).thenReturn(null);
        Assert.assertEquals(this.a1, result.get(0));
    }

    @Test
    public void readJSONDoesNotReturnUnexpectedValue() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, "!!!!!");
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
        json.put(DATA_NAME, 12);
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
        json.put(DATA_NAME, true);
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
        json.put(DATA_NAME, JSONObject.NULL);
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkEqualsAndHashCode() throws ComponentLookupException
    {
        JSONObject json = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"author\":\"Users.padams\","
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "}");
        // Simply initialize the component so that the dependencies are injected
        this.mocker.getComponentUnderTest();
        Attachment parsed = new Attachment(json);
        Assert.assertEquals(this.a1, parsed);
        Assert.assertFalse(this.a1.equals(this.a2));
        Assert.assertFalse(this.a1.equals(null));
        Assert.assertFalse(this.a1.equals(json));
    }

    @Test
    public void checkHashCode() throws ComponentLookupException
    {
        JSONObject json = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"author\":\"Users.padams\","
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "}");
        // Simply initialize the component so that the dependencies are injected
        this.mocker.getComponentUnderTest();
        Attachment parsed = new Attachment(json);
        int hashCode1 = this.a1.hashCode();
        int hashCode2 = this.a2.hashCode();
        Assert.assertNotEquals(hashCode1, hashCode2);
        Assert.assertEquals(hashCode1, parsed.hashCode());

        // Check that the content is taken into account
        json.put("content", "eHl6");
        parsed = new Attachment(json);
        Assert.assertNotEquals(hashCode1, parsed.hashCode());
    }
}
