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
package org.phenotips.measurements.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link MeasurementAgeUpdater} component.
 */
public class MeasurementAgeUpdaterTest
{

    @Rule
    public MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(MeasurementAgeUpdater.class);

    @Mock
    private Object data;

    @Mock
    public XWikiDocument source;

    @Mock
    private Event event;

    @Mock
    private BaseObject patientRecordObj;

    @Mock
    private BaseObject measurement;

    private static final String DATE_PROPERTY_NAME = "date";

    private static final String AGE_PROPERTY_NAME = "age";

    private static final String DATE_OF_BIRTH_PROPERTY_NAME = "date_of_birth";

    private List<BaseObject> objects;

    @Before
    public void setUp() throws IllegalAccessException
    {
        MockitoAnnotations.initMocks(this);
        when(this.source.getXObject(any(EntityReference.class))).thenReturn(this.patientRecordObj);

        this.objects = new LinkedList<>();
        this.objects.add(this.measurement);
        when(this.source.getXObjects(any(EntityReference.class))).thenReturn(this.objects);

        when(this.measurement.getStringValue(anyString())).thenReturn(null);
        when(this.measurement.getDateValue(DATE_PROPERTY_NAME)).thenReturn(new Date());
        when(this.patientRecordObj.getDateValue(DATE_OF_BIRTH_PROPERTY_NAME)).thenReturn(new Date());
    }

    @Test
    public void removesAgeWhenBirthDateNull() throws ComponentLookupException
    {
        when(this.patientRecordObj.getDateValue(DATE_OF_BIRTH_PROPERTY_NAME)).thenReturn(null);

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
        verify(this.measurement).removeField(AGE_PROPERTY_NAME);
    }

    @Test
    public void removesAgeWhenMeasurementDateNull() throws ComponentLookupException
    {
        when(this.measurement.getDateValue(DATE_PROPERTY_NAME)).thenReturn(null);

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
        verify(this.measurement).removeField(AGE_PROPERTY_NAME);
    }

    @Test
    public void setsAgeToZeroWhenMeasurementTypeBirth() throws ComponentLookupException
    {
        when(this.measurement.getStringValue("type")).thenReturn("birth");

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);

        verify(this.measurement).setFloatValue(AGE_PROPERTY_NAME, 0);
        verify(this.measurement).removeField(DATE_PROPERTY_NAME);
    }

    @Test
    public void checkAgeIsCorrectlyCalculated() throws ParseException, ComponentLookupException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

        String birthDateString = "01-01-2015 12:00:00";
        Date birthDate = dateFormat.parse(birthDateString);
        when(this.patientRecordObj.getDateValue(DATE_OF_BIRTH_PROPERTY_NAME)).thenReturn(birthDate);

        String measureDateString = "01-01-2016 12:00:00";
        Date measureDate = dateFormat.parse(measureDateString);

        when(this.measurement.getDateValue(DATE_PROPERTY_NAME)).thenReturn(measureDate);
        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
        verify(this.measurement).setFloatValue(eq(AGE_PROPERTY_NAME), eq(365 / 30.4375f));

        measureDateString = "01-01-2035 12:00:00";
        measureDate = dateFormat.parse(measureDateString);

        when(this.measurement.getDateValue(DATE_PROPERTY_NAME)).thenReturn(measureDate);
        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
        verify(this.measurement).setFloatValue(eq(AGE_PROPERTY_NAME), eq((365 * 20 + 5) / 30.4375f));

        measureDateString = "01-03-2016 12:00:00";
        measureDate = dateFormat.parse(measureDateString);

        when(this.measurement.getDateValue(DATE_PROPERTY_NAME)).thenReturn(measureDate);
        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
        verify(this.measurement).setFloatValue(eq(AGE_PROPERTY_NAME), eq((31 + 29 + 365) / 30.4375f));

    }

    @Test
    public void checkMultipleDatesAreUpdated() throws ComponentLookupException
    {
        BaseObject measurement2 = mock(BaseObject.class);
        BaseObject measurement3 = mock(BaseObject.class);
        this.objects.add(measurement2);
        this.objects.add(measurement3);

        when(measurement2.getDateValue(DATE_PROPERTY_NAME)).thenReturn(new Date());
        when(measurement3.getDateValue(DATE_PROPERTY_NAME)).thenReturn(new Date());

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
        verify(this.measurement).setFloatValue(eq(AGE_PROPERTY_NAME), anyFloat());
        verify(measurement2).setFloatValue(eq(AGE_PROPERTY_NAME), anyFloat());
        verify(measurement3).setFloatValue(eq(AGE_PROPERTY_NAME), anyFloat());
    }

    @Test
    public void behavesNormallyWithNullMeasurement() throws ComponentLookupException
    {
        BaseObject measurement2 = mock(BaseObject.class);
        BaseObject measurement3 = null;
        BaseObject measurement4 = mock(BaseObject.class);
        this.objects.add(measurement2);
        this.objects.add(measurement3);
        this.objects.add(measurement4);

        when(measurement2.getDateValue(DATE_PROPERTY_NAME)).thenReturn(new Date());
        when(measurement4.getDateValue(DATE_PROPERTY_NAME)).thenReturn(new Date());

        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
        verify(this.measurement).setFloatValue(eq(AGE_PROPERTY_NAME), anyFloat());
        verify(measurement2).setFloatValue(eq(AGE_PROPERTY_NAME), anyFloat());
        verify(measurement4).setFloatValue(eq(AGE_PROPERTY_NAME), anyFloat());
    }

    @Test
    public void returnsNormallyWithNullPatientRecord() throws ComponentLookupException
    {
        when(this.source.getXObject(any(EntityReference.class))).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
    }

    @Test
    public void returnsNormallyWhenGivenNoMeasurementObjects() throws Exception
    {
        when(this.source.getXObjects(any(EntityReference.class))).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(this.event, this.source, this.data);
    }
}
