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
package org.phenotips.data.push.internal;

import org.phenotips.Constants;
import org.phenotips.data.push.PushPatientData;
import org.phenotips.data.shareprotocol.ShareProtocol;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.List;

import javax.inject.Provider;

import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.when;

public class DefaultPushPatientDataTest
{
    @Rule
    public final MockitoComponentMockingRule<PushPatientData> mocker =
        new MockitoComponentMockingRule<>(DefaultPushPatientData.class);

    @Mock
    private BaseObject serverConfiguration;

    @Mock
    private List<NameValuePair> data;

    @Mock
    private XWiki xWiki;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private BaseObject configObject;

    @Mock
    private CloseableHttpClient client;

    @Captor
    private ArgumentCaptor<HttpPost> post;

    @Mock
    private CloseableHttpResponse response;

    @Before
    public void setUp() throws ComponentLookupException, XWikiException, ClientProtocolException, IOException
    {
        MockitoAnnotations.initMocks(this);
        Provider<XWikiContext> xcp = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcp.get()).thenReturn(this.xWikiContext);

        when(this.xWikiContext.getWiki()).thenReturn(this.xWiki);
        when(this.xWikiContext.getWikiId()).thenReturn("xwiki");
        when(this.xWiki.getDocument(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"), this.xWikiContext))
            .thenReturn(this.xWikiDocument);
        when(this.xWikiDocument.getXObject(new DocumentReference("xwiki", Constants.CODE_SPACE,
            "PushPatientServer"), "name", "RemoteServer1")).thenReturn(this.configObject);
        when(this.configObject.getStringValue("url")).thenReturn("http://localhost:8080/");

        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "client", this.client);
        when(this.client.execute(this.post.capture())).thenReturn(this.response);
    }

    @Test
    public void getRemoteConfigurationSendsTokenInsteadOfPass()
        throws ComponentLookupException, ClientProtocolException, IOException
    {
        when(this.response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(this.response.getEntity()).thenReturn(new ByteArrayEntity("{}".getBytes()));
        Assert.assertNotNull(
            this.mocker.getComponentUnderTest().getRemoteConfiguration("RemoteServer1", "name", "pass", "token"));
        Mockito.verify(this.client).execute(Matchers.any(HttpPost.class));
        HttpPost request = this.post.getValue();
        Assert.assertEquals("http://localhost:8080/bin/receivePatientData", request.getURI().toString());
        List<NameValuePair> requestData = URLEncodedUtils.parse(request.getEntity());
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_ACTION,
                ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_INFO)));
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_USERNAME, "name")));
        Assert.assertFalse(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_PASSWORD, "pass")));
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_USER_TOKEN, "token")));
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_PROTOCOLVER,
                ShareProtocol.CURRENT_PUSH_PROTOCOL_VERSION)));
    }

    @Test
    public void getRemoteConfigurationFixesPartialBaseURL()
        throws ComponentLookupException, ClientProtocolException, IOException
    {
        when(this.configObject.getStringValue("url")).thenReturn("localhost:8080");
        when(this.response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(this.response.getEntity())
            .thenReturn(new ByteArrayEntity("{\"response_protocol_version\":\"1.2\"}".getBytes()));
        Assert.assertNotNull(
            this.mocker.getComponentUnderTest().getRemoteConfiguration("RemoteServer1", "name", "pass", "token"));
        Mockito.verify(this.client).execute(Matchers.any(HttpPost.class));
        HttpPost request = this.post.getValue();
        Assert.assertEquals("http://localhost:8080/bin/receivePatientData", request.getURI().toString());
    }

    @Test
    public void getRemoteConfigurationSendsPassWhenTokenUnavailable()
        throws ComponentLookupException, ClientProtocolException, IOException
    {
        when(this.response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"));
        when(this.response.getEntity()).thenReturn(new ByteArrayEntity("{}".getBytes()));
        Assert.assertNotNull(
            this.mocker.getComponentUnderTest().getRemoteConfiguration("RemoteServer1", "name", "pass", ""));
        Mockito.verify(this.client).execute(Matchers.any(HttpPost.class));
        HttpPost request = this.post.getValue();
        List<NameValuePair> requestData = URLEncodedUtils.parse(request.getEntity());
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_ACTION,
                ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_INFO)));
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_USERNAME, "name")));
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_PASSWORD, "pass")));
        Assert.assertFalse(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_USER_TOKEN, "")));
        Assert.assertTrue(
            requestData.contains(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_PROTOCOLVER,
                ShareProtocol.CURRENT_PUSH_PROTOCOL_VERSION)));
    }

    @Test
    public void getRemoteConfigurationReturnsNullWhenServerNotConfigured()
        throws ComponentLookupException, ClientProtocolException, IOException
    {
        Assert.assertNull(
            this.mocker.getComponentUnderTest().getRemoteConfiguration("UnknownRemoteServer", "name", "pass", ""));
        Mockito.verify(this.client, Mockito.never()).execute(Matchers.any(HttpPost.class));
    }

    @Test
    public void getRemoteConfigurationReturnsNullWhenServerConfiguredWithBlankBaseURL()
        throws ComponentLookupException, ClientProtocolException, IOException
    {
        when(this.configObject.getStringValue("url")).thenReturn("");
        Assert.assertNull(
            this.mocker.getComponentUnderTest().getRemoteConfiguration("RemoteServer1", "name", "pass", ""));
        Mockito.verify(this.client, Mockito.never()).execute(Matchers.any(HttpPost.class));
    }
}
