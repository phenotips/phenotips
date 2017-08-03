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
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
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
import net.jcip.annotations.NotThreadSafe;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link AdditionalImagesController} component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
@NotThreadSafe
public class AdditionalImagesControllerTest
{
    @ClassRule
    public static TemporaryFolder tf = new TemporaryFolder();

    private static final String CONTROLLER_NAME = "additionalImages";

    private static final String DATA_NAME = "additional_images";

    private static final String FILE_FIELD_NAME = "file";

    private static final String COMMENTS_FIELD_NAME = "comments";

    private static final String PRINT_FIELD_NAME = "print";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Attachment>> mocker =
        new MockitoComponentMockingRule<>(AdditionalImagesController.class);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject image1;

    @Mock
    private BaseObject image2;

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

    private JSONObject json1 = new JSONObject("{"
        + "\"filename\":\"a1.png\","
        + "\"filesize\":4,"
        + "\"author\":\"Users.padams\","
        + "\"date\":\"2017-01-01T12:00:00.000Z\","
        + "\"content\":\"YWJjZA==\","
        + "\"comments\":\"Comment 1\","
        + "\"print\":1"
        + "}");

    private JSONObject json2 = new JSONObject("{"
        + "\"filename\":\"a2.png\","
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
        when(this.doc.getXObjects(AdditionalImagesController.CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.image1, this.image2));
        when(this.doc.newXObject(AdditionalImagesController.CLASS_REFERENCE, this.context)).thenReturn(this.image1,
            this.image2);
        when(this.image1.getStringValue(FILE_FIELD_NAME)).thenReturn("a1.png");
        when(this.image1.getLargeStringValue(COMMENTS_FIELD_NAME)).thenReturn("Comment 1");
        when(this.image1.getIntValue(PRINT_FIELD_NAME)).thenReturn(1);
        when(this.image2.getStringValue(FILE_FIELD_NAME)).thenReturn("a2.png");

        Calendar c = new GregorianCalendar(2017, 0, 1, 12, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date1 = c.getTime();
        when(this.xattachment1.getFilename()).thenReturn("a1.png");
        when(this.xattachment1.getFilesize()).thenReturn(4);
        when(this.xattachment1.getAuthorReference()).thenReturn(this.author1);
        when(this.xattachment1.getDate()).thenReturn(this.date1);
        when(this.xattachment1.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("abcd", StandardCharsets.UTF_8));
        when(this.doc.getAttachment("a1.png")).thenReturn(this.xattachment1);
        when(this.adapter.fromXWikiAttachment(this.xattachment1)).thenReturn(this.attachment1);
        when(this.adapter.fromJSON(this.json1)).thenReturn(this.attachment1);
        when(this.attachment1.toJSON()).thenReturn(this.json1);
        when(this.attachment1.getFilename()).thenReturn("a1.png");
        when(this.attachment1.getFilesize()).thenReturn(4L);
        when(this.attachment1.getAuthorReference()).thenReturn(this.author1);
        when(this.attachment1.getDate()).thenReturn(this.date1);
        when(this.attachment1.getContent()).thenReturn(IOUtils.toInputStream("abcd", StandardCharsets.UTF_8));
        when(this.attachment1.getAttribute(COMMENTS_FIELD_NAME)).thenReturn("Comment 1");
        when(this.attachment1.getAttribute(PRINT_FIELD_NAME)).thenReturn(true);

        c = new GregorianCalendar(2016, 7, 1, 14, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date2 = c.getTime();
        when(this.xattachment2.getFilename()).thenReturn("a2.png");
        when(this.xattachment2.getFilesize()).thenReturn(3);
        when(this.xattachment2.getAuthorReference()).thenReturn(this.author2);
        when(this.xattachment2.getDate()).thenReturn(this.date2);
        when(this.xattachment2.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("xyz", StandardCharsets.UTF_8));
        when(this.doc.getAttachment("a2.png")).thenReturn(this.xattachment2);
        when(this.adapter.fromXWikiAttachment(this.xattachment2)).thenReturn(this.attachment2);
        when(this.adapter.fromJSON(this.json2)).thenReturn(this.attachment2);
        when(this.attachment2.toJSON()).thenReturn(this.json2);
        when(this.attachment2.getFilename()).thenReturn("a2.png");
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
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.attachment1, this.attachment2)));
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
    public void loadRetursnNullWhenPatientDoesNotHaveImageObjects() throws ComponentLookupException
    {
        when(this.doc.getXObjects(AdditionalImagesController.CLASS_REFERENCE)).thenReturn(null);

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
        when(this.image2.getStringValue(FILE_FIELD_NAME)).thenReturn("a3.png");

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(this.attachment1, result.get(0));
    }

    @Test
    public void loadSkipsNullObjects() throws ComponentLookupException
    {
        when(this.doc.getXObjects(AdditionalImagesController.CLASS_REFERENCE))
            .thenReturn(Arrays.asList(this.image1, null, this.image2, null));

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.attachment1, result.get(0));
        Assert.assertEquals(this.attachment2, result.get(1));
    }

    @Test
    public void loadSkipsAttachmentsNotSelected() throws ComponentLookupException
    {
        when(this.doc.getXObjects(AdditionalImagesController.CLASS_REFERENCE)).thenReturn(Arrays.asList(this.image2));

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(this.attachment2, result.get(0));
    }

    @Test
    public void saveDoesNothingWhenNoDataPresent() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME)).thenReturn(null);
        this.mocker.getComponentUnderTest().save(this.patient);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveClearsRecordsFieldWhenEmptyDataPresent() throws ComponentLookupException
    {
        when(this.patient.getData(CONTROLLER_NAME))
            .thenReturn(new IndexedPatientData<>(DATA_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient);
        verify(this.doc).removeXObjects(AdditionalImagesController.CLASS_REFERENCE);
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
    public void saveUpdatesAttachmentsAndMedicalRecordsField()
        throws ComponentLookupException, IOException, XWikiException
    {
        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalImagesController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalImagesController.CLASS_REFERENCE, this.context);

        verify(this.xattachment1).setFilesize(4);
        verify(this.xattachment1).setDate(this.date1);
        verify(this.xattachment1).setAuthorReference(this.author1);
        verify(this.xattachment1).setContent(this.attachment1.getContent());
        verify(this.image1).setStringValue(FILE_FIELD_NAME, "a1.png");
        verify(this.image1).setLargeStringValue(COMMENTS_FIELD_NAME, "Comment 1");
        verify(this.image1).setIntValue(PRINT_FIELD_NAME, 1);

        verify(this.xattachment2).setFilesize(3);
        verify(this.xattachment2).setDate(this.date2);
        verify(this.xattachment2).setAuthorReference(this.author2);
        verify(this.xattachment2).setContent(this.attachment2.getContent());
        verify(this.image2).setStringValue(FILE_FIELD_NAME, "a2.png");
        verify(this.image2).setLargeStringValue(Matchers.eq(COMMENTS_FIELD_NAME), Matchers.isNull(String.class));
        verify(this.image2, never()).setIntValue(Matchers.eq(PRINT_FIELD_NAME), Matchers.anyInt());
    }

    @Test
    public void saveAddsNewAttachmentsWhenNeeded() throws ComponentLookupException, IOException, XWikiException
    {
        when(this.doc.getAttachment("a1.png")).thenReturn(null);
        CapturingMatcher<XWikiAttachment> newAttachmentCapturer = new CapturingMatcher<>();
        Mockito.doNothing().when(this.doc).addAttachment(Matchers.argThat(newAttachmentCapturer));

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalImagesController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalImagesController.CLASS_REFERENCE, this.context);

        XWikiAttachment newAttachment = newAttachmentCapturer.getLastValue();
        Assert.assertEquals("a1.png", newAttachment.getFilename());
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
        when(this.doc.newXObject(AdditionalImagesController.CLASS_REFERENCE, this.context))
            .thenThrow(new XWikiException());

        this.mocker.getComponentUnderTest().save(this.patient);
    }

    @Test
    public void saveUsesCurrentUserWhenSpecifiedUserDoesNotExist()
        throws ComponentLookupException, IOException, XWikiException
    {
        when(this.xwiki.exists(this.author2, this.context)).thenReturn(false);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalImagesController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalImagesController.CLASS_REFERENCE, this.context);

        verify(this.xattachment1).setAuthorReference(this.author1);
        verify(this.xattachment2).setAuthorReference(this.author1);
    }

    @Test
    public void saveAcceptsGuestAuthor() throws ComponentLookupException, IOException, XWikiException
    {
        when(this.attachment1.getAuthorReference()).thenReturn(null);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.doc).removeXObjects(AdditionalImagesController.CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(AdditionalImagesController.CLASS_REFERENCE, this.context);

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
    public void writeJSONAddsImages() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(2, result.length());
        Assert.assertSame(this.json1, result.getJSONObject(0));
        Assert.assertSame(this.json2, result.getJSONObject(1));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsImages() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singletonList(CONTROLLER_NAME));

        Assert.assertTrue(json.has(DATA_NAME));
        JSONArray result = json.getJSONArray(DATA_NAME);
        Assert.assertEquals(2, result.length());
        Assert.assertSame(this.json1, result.getJSONObject(0));
        Assert.assertSame(this.json2, result.getJSONObject(1));
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
    public void readJSONWithEmptyImagesReturnsEmptyData() throws ComponentLookupException
    {
        JSONObject input = new JSONObject();
        input.put(DATA_NAME, new JSONArray());

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(input);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isIndexed());
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void readJSONReturnsImages() throws ComponentLookupException
    {
        JSONArray a = new JSONArray();
        a.put(this.json1);
        a.put(this.json2);
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, a);

        PatientData<Attachment> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(this.attachment1, result.get(0));
        Assert.assertEquals(this.attachment2, result.get(1));
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
