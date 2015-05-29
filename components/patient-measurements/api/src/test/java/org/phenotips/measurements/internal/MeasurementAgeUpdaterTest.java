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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.observation.event.Event;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;


/**
 * Tests for the {@link MeasurementAgeUpdater} component.
 *
 */
public class MeasurementAgeUpdaterTest
{

    @Rule
    public MockitoComponentMockingRule<MeasurementAgeUpdater> mocker =
            new MockitoComponentMockingRule<>(MeasurementAgeUpdater.class);

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

    private String DATE_PROPERTY_NAME;

    private String AGE_PROPERTY_NAME;

    private List<BaseObject> objects;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException
    {
        MockitoAnnotations.initMocks(this);
        when(this.source.getXObject((EntityReference) Matchers.any())).thenReturn(this.patientRecordObj);

        Field age = ReflectionUtils.getField(MeasurementAgeUpdater.class, "AGE_PROPERTY_NAME");
        Field date = ReflectionUtils.getField(MeasurementAgeUpdater.class, "DATE_PROPERTY_NAME");

        age.setAccessible(true);
        date.setAccessible(true);

        this.DATE_PROPERTY_NAME = (String)date.get(null);
        this.AGE_PROPERTY_NAME = (String)age.get(null);

        objects = new LinkedList<>();
        objects.add(measurement);
        when(this.source.getXObjects((EntityReference)Matchers.any())).thenReturn(objects);

        when(this.measurement.getStringValue(Matchers.anyString())).thenReturn(null);
    }

    @Test
    public void testBirthDateNull() throws ComponentLookupException
    {
        when(this.measurement.getDateValue(Matchers.anyString())).thenReturn(new Date());
        when(this.patientRecordObj.getDateValue(Matchers.matches("date_of_birth"))).thenReturn(null);

        this.mocker.getComponentUnderTest().onEvent(event, source, data);
        verify(this.measurement).removeField(AGE_PROPERTY_NAME);
    }

    @Test
    public void testMeasurementDateNull() throws ComponentLookupException
    {
        when(this.patientRecordObj.getDateValue(Matchers.matches("date_of_birth"))).thenReturn(new Date());

        this.mocker.getComponentUnderTest().onEvent(event, source, data);
        verify(this.measurement).removeField(AGE_PROPERTY_NAME);
    }

    @Test
    public void testMeasurementTypeBirth() throws ComponentLookupException
    {
        when(this.measurement.getStringValue("type")).thenReturn("birth");

        this.mocker.getComponentUnderTest().onEvent(event, source, data);

        verify(this.measurement).setFloatValue(AGE_PROPERTY_NAME, 0);
        verify(this.measurement).removeField(DATE_PROPERTY_NAME);
    }

    @Test
    public void testDateDifference() throws ParseException, ComponentLookupException
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        String birthDateString = "01-01-15 12:00:00";
        String measureDateString = "01-01-16 12:00:00";
        Date birthDate = dateFormat.parse(birthDateString);
        Date measureDate = dateFormat.parse(measureDateString);

        when(this.patientRecordObj.getDateValue(Matchers.matches("date_of_birth"))).thenReturn(birthDate);
        when(this.measurement.getDateValue(DATE_PROPERTY_NAME)).thenReturn(measureDate);

        this.mocker.getComponentUnderTest().onEvent(event, source, data);

        verify(this.measurement).setFloatValue(eq(AGE_PROPERTY_NAME), eq(365/30.4375f));
    }
}
