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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.apache.http.NameValuePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.push.PushPatientData;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class DefaultPushPatientDataTest {

    @Rule
    public final MockitoComponentMockingRule<PushPatientData> mocker =
            new MockitoComponentMockingRule<PushPatientData>(DefaultPushPatientData.class);

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
    private ExecutionContext executionContext;

    private Execution execution;

    private DefaultPushPatientData patientData;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.patientData = (DefaultPushPatientData) this.mocker.getComponentUnderTest();
        this.execution = this.mocker.getInstance(Execution.class);
    }

    @Test
    public void getBaseUrlReturnsCorrectString() throws ComponentLookupException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        // set up access for the private method
        Method method = DefaultPushPatientData.class.getDeclaredMethod("getBaseURL", BaseObject.class);
        method.setAccessible(true);
        // test null case
        Assert.assertNull(method.invoke(this.mocker.getComponentUnderTest(), serverConfiguration));

        // test case when serverConfiguration gives http string
        when(serverConfiguration.getStringValue("url")).thenReturn("http://");
        Assert.assertEquals(method.invoke(this.mocker.getComponentUnderTest(), serverConfiguration),
                "http:/bin/receivePatientData");

        when(serverConfiguration.getStringValue(("url"))).thenReturn("apples");
        Assert.assertEquals(method.invoke(this.mocker.getComponentUnderTest(), serverConfiguration),
                "http://apples/bin/receivePatientData");
    }

    @Test
    public void generateRequestDataTest() throws ComponentLookupException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        // set up access for the private method
        Method method = DefaultPushPatientData.class.getDeclaredMethod("generateRequestData",
                String.class, String.class,
                String.class, String.class);
        method.setAccessible(true);

        List<NameValuePair> result = (List<NameValuePair>) method.invoke(this.mocker.getComponentUnderTest(),
                "actionName", "userName",
                "passWord", "userToken");
        Assert.assertEquals("xpage", result.get(0).getName());
        Assert.assertEquals("plain", result.get(0).getValue());
        Assert.assertEquals("push_protocol_version", result.get(1).getName());
        Assert.assertEquals("1", result.get(1).getValue());
        Assert.assertEquals("action", result.get(2).getName());
        Assert.assertEquals("actionName", result.get(2).getValue());
        Assert.assertEquals("username", result.get(3).getName());
        Assert.assertEquals("userName", result.get(3).getValue());
        Assert.assertEquals("user_login_token", result.get(4).getName());
        Assert.assertEquals("userToken", result.get(4).getValue());
    }

    @Test
    public void generateRequestDataNoTokenTest() throws ComponentLookupException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException
    {
        // set up access for the private method
        Method method = DefaultPushPatientData.class.getDeclaredMethod("generateRequestData",
                String.class, String.class,
                String.class, String.class);
        method.setAccessible(true);

        // check that the last
        List<NameValuePair> result = (List<NameValuePair>) method.invoke(this.mocker.getComponentUnderTest(),
                "actionName", "userName",
                "passWord", "");
        Assert.assertEquals("password", result.get(4).getName());
        Assert.assertEquals("passWord", result.get(4).getValue());
    }

    @Test
    public void getRemoteConfigurationPerformsCorrectly() throws ComponentLookupException, XWikiException
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.executionContext.getProperty(Matchers.anyString())).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(Matchers.any(DocumentReference.class), Matchers.any(XWikiContext.class))).thenReturn(this.xWikiDocument);
        this.mocker.getComponentUnderTest().getRemoteConfiguration("id", "name", "pass", "token");
        verify(this.execution).getContext();
    }
}
