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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.io.InputStream;
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
import net.jcip.annotations.NotThreadSafe;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link AdditionalDocumentsController} component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
@NotThreadSafe
public class AdditionalDocumentsControllerTest
{
    @ClassRule
    public static TemporaryFolder tf = new TemporaryFolder();

    private static final String CONTROLLER_NAME = "additionalDocuments";

    private static final String DATA_NAME = "additional_documents";

    private static final String FILE_FIELD_NAME = "file";

    private static final String COMMENTS_FIELD_NAME = "comments";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Attachment>> mocker =
        new MockitoComponentMockingRule<>(AdditionalDocumentsController.class);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject file1;

    @Mock
    private BaseObject file2;

    @Mock
    private XWikiAttachment xattachment1;

    @Mock
    private XWikiAttachment xattachment2;

    @Mock
    private Attachment attachment1;

    @Mock
    private Attachment attachment2;

    private Date date1;

    private Date date2;

    private DocumentReference author1 = new DocumentReference("main", "Users", "padams");

    private DocumentReference author2 = new DocumentReference("genetics", "Users", "hmccoy");

    private AttachmentAdapterFactory adapter;

    private JSONObject json1shallow = new JSONObject("{"
        + "\"filename\":\"a1.pdf\","
        + "\"filesize\":4,"
        + "\"author\":\"Users.padams\","
        + "\"date\":\"2017-01-01T12:00:00.000Z\","
        + "\"link\":\"/download/data/abc.def\","
        + "\"comments\":\"Comment 1\""
        + "}");

    private JSONObject json1full = new JSONObject("{"
        + "\"filename\":\"a1.pdf\","
        + "\"filesize\":4,"
        + "\"author\":\"Users.padams\","
        + "\"date\":\"2017-01-01T12:00:00.000Z\","
        + "\"content\":\"YWJjZA==\","
        + "\"comments\":\"Comment 1\""
        + "}");

    private JSONObject json2shallow = new JSONObject("{"
        + "\"filename\":\"a2.pdf\","
        + "\"filesize\":3,"
        + "\"author\":\"genetics:Users.hmccoy\","
        + "\"date\":\"2016-08-01T14:00:00.000Z\","
        + "\"link\":\"/download/data/xyz.123\","
        + "}");

    private JSONObject json2full = new JSONObject("{"
        + "\"filename\":\"a2.pdf\","
        + "\"filesize\":3,"
        + "\"author\":\"genetics:Users.hmccoy\","
        + "\"date\":\"2016-08-01T14:00:00.000Z\","
        + "\"content\":\"eHl6\""
        + "}");

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
        this.adapter = this.mocker.getInstance(AttachmentAdapterFactory.class);

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObjects(AdditionalDocumentsController.CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.file1, this.file2));
        when(this.doc.newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context)).thenReturn(this.file1,
            this.file2);
        when(this.file1.getStringValue(FILE_FIELD_NAME)).thenReturn("a1.pdf");
        when(this.file1.getLargeStringValue(COMMENTS_FIELD_NAME)).thenReturn("Comment 1");
        when(this.file2.getStringValue(FILE_FIELD_NAME)).thenReturn("a2.pdf");

        Calendar c = new GregorianCalendar(2017, 0, 1, 12, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date1 = c.getTime();
        when(this.xattachment1.getFilename()).thenReturn("a1.pdf");
        when(this.xattachment1.getFilesize()).thenReturn(4);
        when(this.xattachment1.getAuthorReference()).thenReturn(this.author1);
        when(this.xattachment1.getDate()).thenReturn(this.date1);
        when(this.xattachment1.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("abcd", StandardCharsets.UTF_8));
        when(this.doc.getAttachment("a1.pdf")).thenReturn(this.xattachment1);
        when(this.adapter.fromXWikiAttachment(this.xattachment1)).thenReturn(this.attachment1);
        when(this.adapter.fromJSON(this.json1full)).thenReturn(this.attachment1);
        when(this.adapter.fromJSON(this.json1shallow)).thenReturn(null);
        when(this.attachment1.toJSON(true)).thenReturn(this.json1full);
        when(this.attachment1.toJSON(false)).thenReturn(this.json1shallow);
        when(this.attachment1.getFilename()).thenReturn("a1.pdf");
        when(this.attachment1.getFilesize()).thenReturn(4L);
        when(this.attachment1.getAuthorReference()).thenReturn(this.author1);
        when(this.attachment1.getDate()).thenReturn(this.date1);
        when(this.attachment1.getContent()).thenReturn(IOUtils.toInputStream("abcd", StandardCharsets.UTF_8));
        when(this.attachment1.getAttribute(COMMENTS_FIELD_NAME)).thenReturn("Comment 1");

        c = new GregorianCalendar(2016, 7, 1, 14, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date2 = c.getTime();
        when(this.xattachment2.getFilename()).thenReturn("a2.pdf");
        when(this.xattachment2.getFilesize()).thenReturn(3);
        when(this.xattachment2.getAuthorReference()).thenReturn(this.author2);
        when(this.xattachment2.getDate()).thenReturn(this.date2);
        when(this.xattachment2.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("xyz", StandardCharsets.UTF_8));
        when(this.doc.getAttachment("a2.pdf")).thenReturn(this.xattachment2);
        when(this.adapter.fromXWikiAttachment(this.xattachment2)).thenReturn(this.attachment2);
        when(this.adapter.fromJSON(this.json2full)).thenReturn(this.attachment2);
        when(this.adapter.fromJSON(this.json2shallow)).thenReturn(null);
        when(this.attachment2.toJSON(true)).thenReturn(this.json2full);
        when(this.attachment2.toJSON(false)).thenReturn(this.json2shallow);
        when(this.attachment2.getFilename()).thenReturn("a2.pdf");
        when(this.attachment2.getFilesize()).thenReturn(3L);
        when(this.attachment2.getAuthorReference()).thenReturn(this.author2);
        when(this.attachment2.getDate()).thenReturn(this.date2);
        when(this.attachment2.getContent()).thenReturn(IOUtils.toInputStream("xyz", StandardCharsets.UTF_8));

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);

        when(this.context.getUserReference()).thenReturn(this.author1);
        when(this.xwiki.exists(this.author1, this.context)).thenReturn(true);
        when(this.xwiki.exists(this.author2, this.context)).thenReturn(true);

        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Arrays.asList(this.attachment1, this.attachment2)));
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
    public void loadRetursnNullWhenPatientDoesNotHaveFileObjects() throws ComponentLookupException
    {
        when(this.doc.getXObjects(AdditionalDocumentsController.CLASS_REFERENCE)).thenReturn(null);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAttachments() throws ComponentLookupException
    {
        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.attachment1, result.get(0));
        Assert.assertEquals(this.attachment2, result.get(1));
    }

    @Test
    public void loadSkipsNonExistingAttachments() throws ComponentLookupException
    {
        when(this.file2.getStringValue(FILE_FIELD_NAME)).thenReturn("a3.pdf");

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(this.attachment1, result.get(0));
    }

    @Test
    public void loadSkipsNullObjects() throws ComponentLookupException
    {
        when(this.doc.getXObjects(AdditionalDocumentsController.CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.file1, null, this.file2, null));

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.attachment1, result.get(0));
        Assert.assertEquals(this.attachment2, result.get(1));
    }

    @Test
    public void loadSkipsAttachmentsNotSelected() throws ComponentLookupException
    {
        when(this.doc.getXObjects(AdditionalDocumentsController.CLASS_REFERENCE)).thenReturn(
            Collections.singletonList(this.file2));

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(this.attachment2, result.get(0));
    }

    @Test
    public void saveDoesNothingWhenNoDataPresentAndPolicyIsUpdate() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(null);
        this.mocker.getComponentUnderTest().save(this.patient);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveDoesNothingWhenNoDataPresentAndPolicyIsMerge() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(null);
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.MERGE);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveRemovesEverythingWhenNoDataPresentAndPolicyIsReplace() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(null);
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.REPLACE);
        Mockito.verify(this.doc, times(1)).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        Mockito.verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveClearsRecordsFieldWhenEmptyDataPresentAndPolicyIsUpdate() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
    }

    @Test
    public void saveClearsRecordsFieldWhenEmptyDataPresentAndPolicyIsReplace()
        throws ComponentLookupException, XWikiException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, never()).newXObject(any(EntityReference.class), any(XWikiContext.class));
        verify(this.doc, never()).getAttachment(anyString());
    }

    @Test
    public void saveClearsRecordsFieldWhenEmptyDataPresentNoDataSavedAndPolicyIsMerge()
        throws ComponentLookupException, XWikiException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        when(this.doc.getXObjects(AdditionalDocumentsController.CLASS_REFERENCE))
            .thenReturn(Collections.emptyList());
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, never()).newXObject(any(EntityReference.class), any(XWikiContext.class));
        verify(this.doc, never()).getAttachment(anyString());
        verify(this.doc, never()).addAttachment(any(XWikiAttachment.class));
    }

    @Test
    public void saveKeepsRecordsFieldAsIsWhenEmptyDataPresentAndPolicyIsMerge()
        throws ComponentLookupException, XWikiException, IOException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));

        when(this.doc.getXObjects(AdditionalDocumentsController.CLASS_REFERENCE))
            .thenReturn(Collections.singletonList(this.file1));

        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, times(1)).newXObject(any(EntityReference.class), any(XWikiContext.class));
        verify(this.doc, times(2)).getAttachment(anyString());
        verify(this.doc, times(2)).getAttachment("a1.pdf");
        verify(this.doc, never()).addAttachment(any(XWikiAttachment.class));
        verify(this.xattachment1, times(1)).setContent(any(InputStream.class));
    }

    @Test
    public void saveDoesNotRemoveAttachmentsWithUpdatePolicy() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.doc, never()).removeAttachment(any(XWikiAttachment.class));
    }

    @Test
    public void saveDoesNotRemoveAttachmentsWithReplacePolicy() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, never()).removeAttachment(any(XWikiAttachment.class));
    }

    @Test
    public void saveDoesNotRemoveAttachmentsWithMergePolicy() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, never()).removeAttachment(any(XWikiAttachment.class));
    }

    @Test
    public void saveUpdatesAttachmentsAndMedicalRecordsFieldWhenPolicyIsUpdate()
        throws ComponentLookupException, IOException, XWikiException
    {
        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context);

        verify(this.xattachment1).setFilesize(4);
        verify(this.xattachment1).setDate(this.date1);
        verify(this.xattachment1).setAuthorReference(this.author1);
        verify(this.xattachment1).setContent(this.attachment1.getContent());
        verify(this.file1).setStringValue(FILE_FIELD_NAME, "a1.pdf");
        verify(this.file1).setLargeStringValue(COMMENTS_FIELD_NAME, "Comment 1");

        verify(this.xattachment2).setFilesize(3);
        verify(this.xattachment2).setDate(this.date2);
        verify(this.xattachment2).setAuthorReference(this.author2);
        verify(this.xattachment2).setContent(this.attachment2.getContent());
        verify(this.file2).setStringValue(FILE_FIELD_NAME, "a2.pdf");
        verify(this.file2).setLargeStringValue(Matchers.eq(COMMENTS_FIELD_NAME), Matchers.isNull(String.class));
    }

    @Test
    public void saveUpdatesAttachmentsAndMedicalRecordsFieldWhenPolicyIsMerge()
        throws ComponentLookupException, IOException, XWikiException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.singletonList(this.attachment2)));
        when(this.doc.getXObjects(AdditionalDocumentsController.CLASS_REFERENCE))
            .thenReturn(Collections.singletonList(this.file1));
        when(this.doc.newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context)).thenReturn(this.file1,
            this.file2);
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context);
        verify(this.doc, never()).addAttachment(this.xattachment1);
        verify(this.doc, never()).addAttachment(this.xattachment2);

        verify(this.xattachment1).setFilesize(4);
        verify(this.xattachment1).setDate(this.date1);
        verify(this.xattachment1).setAuthorReference(this.author1);
        verify(this.xattachment1).setContent(this.attachment1.getContent());
        verify(this.file1).setStringValue(FILE_FIELD_NAME, "a1.pdf");
        verify(this.file1).setLargeStringValue(COMMENTS_FIELD_NAME, "Comment 1");

        verify(this.xattachment2).setFilesize(3);
        verify(this.xattachment2).setDate(this.date2);
        verify(this.xattachment2).setAuthorReference(this.author2);
        verify(this.xattachment2).setContent(this.attachment2.getContent());
        verify(this.file2).setStringValue(FILE_FIELD_NAME, "a2.pdf");
        verify(this.file2).setLargeStringValue(Matchers.eq(COMMENTS_FIELD_NAME), Matchers.isNull(String.class));
    }

    @Test
    public void saveUpdatesAttachmentsAndMedicalRecordsFieldWhenPolicyIsReplace()
        throws ComponentLookupException, IOException, XWikiException
    {
        this.mocker.getComponentUnderTest().save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context);

        verify(this.xattachment1).setFilesize(4);
        verify(this.xattachment1).setDate(this.date1);
        verify(this.xattachment1).setAuthorReference(this.author1);
        verify(this.xattachment1).setContent(this.attachment1.getContent());
        verify(this.file1).setStringValue(FILE_FIELD_NAME, "a1.pdf");
        verify(this.file1).setLargeStringValue(COMMENTS_FIELD_NAME, "Comment 1");

        verify(this.xattachment2).setFilesize(3);
        verify(this.xattachment2).setDate(this.date2);
        verify(this.xattachment2).setAuthorReference(this.author2);
        verify(this.xattachment2).setContent(this.attachment2.getContent());
        verify(this.file2).setStringValue(FILE_FIELD_NAME, "a2.pdf");
        verify(this.file2).setLargeStringValue(Matchers.eq(COMMENTS_FIELD_NAME), Matchers.isNull(String.class));
    }

    @Test
    public void saveAddsNewAttachmentsWhenNeeded() throws ComponentLookupException, IOException, XWikiException
    {
        when(this.doc.getAttachment("a1.pdf")).thenReturn(null);
        CapturingMatcher<XWikiAttachment> newAttachmentCapturer = new CapturingMatcher<>();
        Mockito.doNothing().when(this.doc).addAttachment(Matchers.argThat(newAttachmentCapturer));

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context);

        XWikiAttachment newAttachment = newAttachmentCapturer.getLastValue();
        Assert.assertEquals("a1.pdf", newAttachment.getFilename());
        Assert.assertEquals(4, newAttachment.getFilesize());
        Assert.assertEquals(this.date1, newAttachment.getDate());
        Assert.assertEquals(this.author1, newAttachment.getAuthorReference());
        Assert.assertArrayEquals("abcd".getBytes(StandardCharsets.ISO_8859_1),
            IOUtils.toByteArray(newAttachment.getContentInputStream(this.context)));

        verify(this.xattachment2).setFilesize(3);
        verify(this.xattachment2).setDate(this.date2);
        verify(this.xattachment2).setAuthorReference(this.author2);
        verify(this.xattachment2).setContent(this.attachment2.getContent());
    }

    @Test
    public void saveCatchesExceptions() throws Exception
    {
        when(this.doc.newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context))
            .thenThrow(new XWikiException());

        this.mocker.getComponentUnderTest().save(this.patient);
    }

    @Test
    public void saveUsesCurrentUserWhenSpecifiedUserDoesNotExist()
        throws ComponentLookupException, IOException, XWikiException
    {
        when(this.xwiki.exists(this.author2, this.context)).thenReturn(false);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context);

        verify(this.xattachment1).setAuthorReference(this.author1);
        verify(this.xattachment2).setAuthorReference(this.author1);
    }

    @Test
    public void saveAcceptsGuestAuthor() throws ComponentLookupException, IOException, XWikiException
    {
        when(this.attachment1.getAuthorReference()).thenReturn(null);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalDocumentsController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalDocumentsController.CLASS_REFERENCE, this.context);

        verify(this.xattachment1).setAuthorReference(null);
    }

    @Test
    public void writeJSONDoesNothingWhenGetDataReturnsNull() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(null);

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithOtherSelectedFieldsDoesNothing() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList("others"));

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsStoresEmptyArrayWhenGetDataReturnsNull() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(null);

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList(CONTROLLER_NAME));

        Assert.assertTrue(json.has(DATA_NAME));
        Assert.assertEquals(0, json.getJSONArray(DATA_NAME).length());
    }

    @Test
    public void writeJSONAddsLinks() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(2, result.length());
        Assert.assertSame(this.json1shallow, result.getJSONObject(0));
        Assert.assertSame(this.json2shallow, result.getJSONObject(1));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsFiles() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList(CONTROLLER_NAME));

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(2, result.length());
        Assert.assertSame(this.json1full, result.getJSONObject(0));
        Assert.assertSame(this.json2full, result.getJSONObject(1));
    }

    @Test
    public void writeJSONWithWrongDataDoesNothing() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new SimpleValuePatientData<>(CONTROLLER_NAME, this.attachment1));

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithEmptyDataDoesNothing() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(CONTROLLER_NAME, Collections.emptyList()));

        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void readJSONWithEmptyJsonReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readJSONWithEmptyFilesReturnsEmptyData() throws ComponentLookupException
    {
        JSONObject input = new JSONObject();
        input.put(DATA_NAME, new JSONArray());

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(input);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isIndexed());
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readJSONReturnsFiles() throws ComponentLookupException
    {
        JSONArray a = new JSONArray();
        a.put(this.json1full);
        a.put(this.json2full);
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, a);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.attachment1, result.get(0));
        Assert.assertEquals(this.attachment2, result.get(1));
    }

    @Test
    public void readJSONSkipsAttachmentsWithNoContent() throws ComponentLookupException
    {
        JSONArray a = new JSONArray();
        a.put(this.json1shallow);
        a.put(this.json2full);
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, a);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(this.attachment2, result.get(0));
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
}
