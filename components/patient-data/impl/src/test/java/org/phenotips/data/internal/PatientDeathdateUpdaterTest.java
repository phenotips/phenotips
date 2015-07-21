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
package org.phenotips.data.internal;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangingEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Date;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link PatientDeathdateUpdater} component
 */
public class PatientDeathdateUpdaterTest
{

    @Rule
    public MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(PatientDeathdateUpdater.class);

    private Container container;

    @Mock
    private Object data;

    @Mock
    public XWikiDocument source;

    @Mock
    private Event event;

    @Mock
    private BaseObject patientRecordObj;

    @Mock
    private Request request;

    private final int patientNumber = 1;

    private final String propertyName = "date_of_death_unknown";

    private final String parameterName =
        Constants.CODE_SPACE + ".PatientClass_" + this.patientNumber + "_" + propertyName;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        doReturn(this.patientRecordObj).when(this.source).getXObject(Patient.CLASS_REFERENCE);
        doReturn(this.patientNumber).when(this.patientRecordObj).getNumber();

        this.container = this.mocker.getInstance(Container.class);
        doReturn(this.request).when(this.container).getRequest();
    }

    @Test
    public void checkConstruction() throws ComponentLookupException
    {
        PatientDeathdateUpdater testInstance = (PatientDeathdateUpdater)this.mocker.getComponentUnderTest();
        Assert.assertEquals("patient-deathdate-updater", testInstance.getName());
        Assert.assertThat(testInstance.getEvents(), hasItem(isA(PatientChangingEvent.class)));
    }

    @Test
    public void returnsNormallyWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.source).getXObject(Patient.CLASS_REFERENCE);
        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
    }

    @Test
    public void dateValuesAreNotSetWhenContainerReturnsNullRequest() throws ComponentLookupException
    {
        doReturn(null).when(this.container).getRequest();

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);

        verify(this.patientRecordObj, never()).setDateValue(anyString(), any(Date.class));
    }

    @Test
    public void dateValuesAreNotSetWhenRequestReturnsNonNumericString() throws ComponentLookupException
    {
        doReturn("STRING").when(this.request).getProperty(this.parameterName);

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);

        verify(this.patientRecordObj, never()).setDateValue(anyString(), any(Date.class));
    }

    @Test
    public void dateValuesAreNotSetWhenRequestReturnsZero() throws ComponentLookupException
    {
        doReturn("0").when(this.request).getProperty(this.parameterName);

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);

        verify(this.patientRecordObj, never()).setDateValue(anyString(), any(Date.class));
    }

    @Test
    public void dateValuesAreSetWhenRequestReturnsOne() throws ComponentLookupException
    {
        doReturn("1").when(this.request).getProperty(this.parameterName);

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);

        verify(this.patientRecordObj).setDateValue("date_of_death", null);
        verify(this.patientRecordObj).setDateValue("date_of_death_entered", null);
    }
}
