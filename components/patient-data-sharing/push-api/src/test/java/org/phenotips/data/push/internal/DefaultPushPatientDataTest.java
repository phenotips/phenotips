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

import com.xpn.xwiki.objects.BaseObject;
import org.apache.http.NameValuePair;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.push.PushPatientData;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IllegalFormatCodePointException;
import java.util.List;

import static org.mockito.Mockito.when;

public class DefaultPushPatientDataTest {

    @Rule
    public final MockitoComponentMockingRule<PushPatientData> mocker =
            new MockitoComponentMockingRule<PushPatientData>(DefaultPushPatientData.class);

    @Mock
    private BaseObject serverConfiguration;

    @Test
    public void getBaseUrlTest() throws ComponentLookupException, NoSuchMethodException, IllegalAccessException,
                                        InvocationTargetException
    {
        // set up access for the private method
        Method method = DefaultPushPatientData.class.getDeclaredMethod("getBaseURL", BaseObject.class);
        method.setAccessible(true);
        // test null case
        Assert.assertNull(method.invoke(this.mocker.getComponentUnderTest(), serverConfiguration));

        // test case when serverConfiguration gives http string
        MockitoAnnotations.initMocks(this);
        when(serverConfiguration.getStringValue("url")).thenReturn("http://");
        Assert.assertEquals(method.invoke(this.mocker.getComponentUnderTest(), serverConfiguration),
                "http:/bin/receivePatientData");

        when(serverConfiguration.getStringValue(("url"))).thenReturn("apples");
        Assert.assertEquals(method.invoke(this.mocker.getComponentUnderTest(), serverConfiguration),
                                          "http://apples/bin/receivePatientData");
    }

    @Test
    public void generateRequestTest() throws ComponentLookupException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException
    {
        // set up access for the private method
        Method method = DefaultPushPatientData.class.getDeclaredMethod("generateRequest", String.class,  List.class);
        method.setAccessible(true);


    }

    @Test
    public void generateRequestDataTest()
    {

    }

    @Test
    public void getPushServerConfigurationTest()
    {

    }
    @Test
    public void getRemoteConfigurationTest()
    {

    }

    @Test
    public void sendPatientTest()
    {

    }

    @Test
    public void getPatientURLTest()
    {

    }

}
