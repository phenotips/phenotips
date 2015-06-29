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
package org.phenotips.data;

import org.phenotips.data.internal.controller.MedicationController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.MutablePeriod;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link MedicationController} class.
 *
 * @version $Id$
 * @since 1.2RC1
 */
public class MedicationControllerTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientDataController<Medication>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Medication>>(MedicationController.class);

    @Mock
    private Patient patient;

    private DocumentReference docRef = new DocumentReference("xwiki", "data", "P0000001");

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject obj1;

    @Mock
    private BaseObject obj2;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(this.patient.getDocument()).thenReturn(this.docRef);
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getDocument(this.docRef)).thenReturn(this.doc);

        when(this.obj1.getStringValue("name")).thenReturn("n");
        when(this.obj1.getStringValue("genericName")).thenReturn("gn");
        when(this.obj1.getStringValue("dose")).thenReturn("d");
        when(this.obj1.getStringValue("frequency")).thenReturn("f");
        when(this.obj1.getIntValue("durationMonths")).thenReturn(4);
        when(this.obj1.getIntValue("durationYears")).thenReturn(2);
        when(this.obj1.getStringValue("effect")).thenReturn("slightImprovement");
        when(this.obj1.getLargeStringValue("notes")).thenReturn("note");
    }

    @Test
    public void loadReadsObjects() throws ComponentLookupException
    {
        List<BaseObject> objects = new LinkedList<>();
        objects.add(this.obj1);
        objects.add(null);
        objects.add(this.obj2);
        when(this.doc.getXObjects(Medication.CLASS_REFERENCE)).thenReturn(objects);
        PatientData<Medication> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(2, result.size());

        Medication m = result.get(0);
        Assert.assertEquals("n", m.getName());
        Assert.assertEquals("gn", m.getGenericName());
        Assert.assertEquals("d", m.getDose());
        Assert.assertEquals("f", m.getFrequency());
        MutablePeriod p = new MutablePeriod();
        p.setMonths(4);
        p.setYears(2);
        Assert.assertEquals(p, m.getDuration());
        Assert.assertEquals(MedicationEffect.SLIGHT_IMPROVEMENT, m.getEffect());
        Assert.assertEquals("note", m.getNotes());

        m = result.get(1);
        Assert.assertNull(m.getName());
        Assert.assertNull(m.getGenericName());
        Assert.assertNull(m.getDose());
        Assert.assertNull(m.getFrequency());
        Assert.assertEquals(Period.ZERO, m.getDuration());
        Assert.assertNull(m.getEffect());
        Assert.assertNull(m.getNotes());
    }

    @Test
    public void loadWithNoObjectsReturnsNull() throws ComponentLookupException
    {
        when(this.doc.getXObjects(Medication.CLASS_REFERENCE)).thenReturn(Collections.<BaseObject>emptyList());
        Assert.assertNull(this.mocker.getComponentUnderTest().load(this.patient));

        when(this.doc.getXObjects(Medication.CLASS_REFERENCE)).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().load(this.patient));
    }

    @Test
    public void loadWithNullObjectsReturnsNull() throws ComponentLookupException
    {
        List<BaseObject> objects = new LinkedList<>();
        objects.add(null);
        when(this.doc.getXObjects(Medication.CLASS_REFERENCE)).thenReturn(objects);
        Assert.assertNull(this.mocker.getComponentUnderTest().load(this.patient));
    }

    @Test
    public void loadIgnoresUnknownEffect() throws ComponentLookupException
    {
        List<BaseObject> objects = new LinkedList<>();
        objects.add(this.obj1);
        when(this.obj1.getStringValue("effect")).thenReturn("invalid");
        when(this.doc.getXObjects(Medication.CLASS_REFERENCE)).thenReturn(objects);
        PatientData<Medication> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertEquals(1, result.size());

        Medication m = result.get(0);
        Assert.assertEquals("n", m.getName());
        Assert.assertEquals("gn", m.getGenericName());
        Assert.assertEquals("d", m.getDose());
        Assert.assertEquals("f", m.getFrequency());
        MutablePeriod p = new MutablePeriod();
        p.setMonths(4);
        p.setYears(2);
        Assert.assertEquals(p, m.getDuration());
        Assert.assertNull(m.getEffect());
        Assert.assertEquals("note", m.getNotes());
    }

    @Test
    public void loadWithExceptionReturnsNull() throws Exception
    {
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getDocument(this.docRef)).thenThrow(new XWikiException());
        Assert.assertNull(this.mocker.getComponentUnderTest().load(this.patient));
    }
}
