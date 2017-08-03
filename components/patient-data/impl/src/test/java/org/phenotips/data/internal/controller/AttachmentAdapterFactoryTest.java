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

import org.phenotips.data.Patient;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.web.Utils;
import net.jcip.annotations.NotThreadSafe;

import static org.mockito.Mockito.when;

/**
 * Test for the {@link AttachmentAdapterFactory} component and the {@link Attachment} objects it creates.
 */
@NotThreadSafe
public class AttachmentAdapterFactoryTest
{
    @ClassRule
    public static TemporaryFolder tf = new TemporaryFolder();

    private static final String JSON_FIELD_FILENAME = "filename";

    private static final String JSON_FIELD_FILESIZE = "filesize";

    private static final String JSON_FIELD_AUTHOR = "author";

    private static final String JSON_FIELD_DATE = "date";

    private static final String JSON_FIELD_CONTENT = "content";

    @Rule
    public MockitoComponentMockingRule<AttachmentAdapterFactory> mocker =
        new MockitoComponentMockingRule<>(AttachmentAdapterFactory.class);

    @Mock
    private XWikiContext context;

    @Mock
    private Patient patient;

    @Mock
    private XWikiAttachment attachment1;

    @Mock
    private XWikiAttachment attachment2;

    private Date date1;

    private Date date2;

    private DocumentReference author1 = new DocumentReference("main", "Users", "padams");

    private DocumentReference author2 = new DocumentReference("genetics", "Users", "hmccoy");

    @BeforeClass
    public static void globalSetUp() throws ComponentLookupException
    {
        // This is needed so that XWikiAttachment#setContent works
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

        Calendar c = new GregorianCalendar(2017, 0, 1, 12, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date1 = c.getTime();
        when(this.attachment1.getFilename()).thenReturn("a1.pdf");
        when(this.attachment1.getFilesize()).thenReturn(4);
        when(this.attachment1.getAuthorReference()).thenReturn(this.author1);
        when(this.attachment1.getDate()).thenReturn(this.date1);
        when(this.attachment1.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("abcd", StandardCharsets.UTF_8));

        c = new GregorianCalendar(2016, 7, 1, 14, 0, 0);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.date2 = c.getTime();
        when(this.attachment2.getFilename()).thenReturn("a2.pdf");
        when(this.attachment2.getFilesize()).thenReturn(3);
        when(this.attachment2.getAuthorReference()).thenReturn(this.author2);
        when(this.attachment2.getDate()).thenReturn(this.date2);
        when(this.attachment2.getContentInputStream(this.context))
            .thenReturn(IOUtils.toInputStream("xyz", StandardCharsets.UTF_8));

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.context);

        EntityReferenceSerializer<String> userSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(userSerializer.serialize(this.author1)).thenReturn("Users.padams");
        when(userSerializer.serialize(this.author2)).thenReturn("genetics:Users.hmccoy");

        DocumentReferenceResolver<String> userResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "user/current");
        when(userResolver.resolve("Users.padams")).thenReturn(this.author1);
        when(userResolver.resolve("genetics:Users.hmccoy")).thenReturn(this.author2);
    }

    @Test
    public void fromXAttachmentIsCorrect() throws ComponentLookupException, IOException
    {
        Attachment result = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment1);
        Assert.assertNotNull(result);
        Assert.assertEquals("a1.pdf", result.getFilename());
        Assert.assertEquals(4, result.getFilesize());
        Assert.assertEquals(this.author1, result.getAuthorReference());
        Assert.assertEquals(this.date1, result.getDate());
        Assert.assertEquals("abcd", IOUtils.toString(result.getContent(), StandardCharsets.US_ASCII));
        Assert.assertTrue(result.getAllAttributes().isEmpty());
    }

    @Test
    public void fromXAttachmentAcceptsGuestAuthor() throws ComponentLookupException
    {
        when(this.attachment1.getAuthorReference()).thenReturn(null);

        Attachment result = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment1);

        Assert.assertNotNull(result);
        Assert.assertNull(result.getAuthorReference());
    }

    @Test
    public void fromXAttachmentReturnsNullForNullArgument() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().fromXWikiAttachment(null));
    }

    @Test
    public void toJSONWritesAllInformation() throws ComponentLookupException
    {
        Attachment a1 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment1);
        a1.addAttribute("strkey", "value");
        a1.addAttribute("boolkey", Boolean.FALSE);
        a1.addAttribute("boolkey", Boolean.TRUE);
        a1.addAttribute("numkey", Integer.valueOf(42));
        JSONObject json1 = a1.toJSON();

        Assert.assertEquals("a1.pdf", json1.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(4L, json1.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("Users.padams", json1.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2017-01-01T12:00:00.000Z", json1.get(JSON_FIELD_DATE));
        Assert.assertEquals("YWJjZA==", json1.get(JSON_FIELD_CONTENT));
        Assert.assertEquals("value", json1.get("strkey"));
        Assert.assertEquals(true, json1.getBoolean("boolkey"));
        Assert.assertEquals(42, json1.getInt("numkey"));

        JSONObject json2 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment2).toJSON();
        Assert.assertEquals("a2.pdf", json2.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(3L, json2.get(JSON_FIELD_FILESIZE));
        Assert.assertEquals("genetics:Users.hmccoy", json2.get(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2016-08-01T14:00:00.000Z", json2.get(JSON_FIELD_DATE));
        Assert.assertEquals("eHl6", json2.get(JSON_FIELD_CONTENT));
    }

    @Test
    public void toJSONWritesNoAuthorForGuestAuthor() throws ComponentLookupException
    {
        when(this.attachment1.getAuthorReference()).thenReturn(null);

        JSONObject json = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment1).toJSON();

        Assert.assertEquals("a1.pdf", json.get(JSON_FIELD_FILENAME));
        Assert.assertEquals(4L, json.get(JSON_FIELD_FILESIZE));
        Assert.assertFalse(json.has(JSON_FIELD_AUTHOR));
        Assert.assertEquals("2017-01-01T12:00:00.000Z", json.get(JSON_FIELD_DATE));
        Assert.assertEquals("YWJjZA==", json.get(JSON_FIELD_CONTENT));
    }

    @Test
    public void fromJSONParsesCorrectly() throws ComponentLookupException
    {
        JSONObject json1 = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"author\":\"Users.padams\","
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\","
            + "\"strkey\":\"value\","
            + "\"boolkey\":true,"
            + "\"numkey\":42"
            + "}");
        JSONObject json2 = new JSONObject("{"
            + "\"filename\":\"a2.pdf\","
            + "\"filesize\":3,"
            + "\"author\":\"genetics:Users.hmccoy\","
            + "\"date\":\"2016-08-01T14:00:00.000Z\","
            + "\"content\":\"eHl6\""
            + "}");

        Attachment expected1 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment1);
        Attachment result1 = this.mocker.getComponentUnderTest().fromJSON(json1);
        Assert.assertEquals(expected1, result1);
        Assert.assertEquals(3, result1.getAllAttributes().size());
        Assert.assertEquals("value", result1.getAttribute("strkey"));
        Assert.assertEquals(true, result1.getAttribute("boolkey"));
        Assert.assertEquals(42, result1.getAttribute("numkey"));

        Attachment expected2 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment2);
        Assert.assertEquals(expected2, this.mocker.getComponentUnderTest().fromJSON(json2));
    }

    @Test
    public void fromJSONAcceptsGuestAuthor() throws ComponentLookupException
    {
        JSONObject json = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "}");

        Assert.assertNull(this.mocker.getComponentUnderTest().fromJSON(json).getAuthorReference());
    }

    @Test
    public void fromJSONReturnsNullForIncompleteJSON() throws ComponentLookupException
    {
        JSONObject json = new JSONObject("{"
            + "\"filesize\":4,"
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "}");
        Assert.assertNull(this.mocker.getComponentUnderTest().fromJSON(json));

        json = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "}");
        Assert.assertNull(this.mocker.getComponentUnderTest().fromJSON(json));

        json = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"content\":\"YWJjZA==\""
            + "}");
        Assert.assertNull(this.mocker.getComponentUnderTest().fromJSON(json));

        json = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"date\":\"2017-01-01T12:00:00.000Z\""
            + "}");
        Assert.assertNull(this.mocker.getComponentUnderTest().fromJSON(json));
    }

    @Test
    public void fromJSONReturnsNullForNullArgument() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().fromJSON(null));
    }

    @Test
    public void checkEquals() throws ComponentLookupException
    {
        JSONObject json = new JSONObject("{"
            + "\"filename\":\"a1.pdf\","
            + "\"filesize\":4,"
            + "\"author\":\"Users.padams\","
            + "\"date\":\"2017-01-01T12:00:00.000Z\","
            + "\"content\":\"YWJjZA==\""
            + "}");
        // Simply initialize the component so that the dependencies are injected
        Attachment parsed = this.mocker.getComponentUnderTest().fromJSON(json);
        Attachment a1 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment1);
        Attachment a2 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment2);
        Assert.assertEquals(a1, parsed);
        Assert.assertFalse(a1.equals(a2));
        Assert.assertFalse(a1.equals(null));
        Assert.assertFalse(a1.equals(json));
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
        Attachment parsed = this.mocker.getComponentUnderTest().fromJSON(json);
        Attachment a1 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment1);
        Attachment a2 = this.mocker.getComponentUnderTest().fromXWikiAttachment(this.attachment2);
        int hashCode1 = a1.hashCode();
        int hashCode2 = a2.hashCode();
        Assert.assertNotEquals(hashCode1, hashCode2);
        Assert.assertEquals(hashCode1, parsed.hashCode());

        // Check that the content is taken into account
        json.put("content", "eHl6");
        parsed = this.mocker.getComponentUnderTest().fromJSON(json);
        Assert.assertNotEquals(hashCode1, parsed.hashCode());
    }
}
