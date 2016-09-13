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

import javax.inject.Provider;

import org.joda.time.MutablePeriod;
import org.joda.time.Period;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link MedicationController} class.
 *
 * @version $Id$
 * @since 1.2M5
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
    private XWiki xwiki;

    @Mock
    private XWikiContext xcontext;

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

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
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

    @Test
    public void saveWithNoDataDoesNothing() throws Exception
    {
        when(this.patient.getData(MedicationController.DATA_NAME)).thenReturn(null);
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithNonIndexedDataDoesNothing() throws Exception
    {
        when(this.patient.<Medication>getData(MedicationController.DATA_NAME)).thenReturn(
            new DictionaryPatientData<>(MedicationController.DATA_NAME, Collections.<String, Medication>emptyMap()));
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verifyZeroInteractions(this.doc);
    }

    @Test
    public void saveWithEmptyDataClearsXObjects() throws Exception
    {
        when(this.patient.getData(MedicationController.DATA_NAME))
            .thenReturn(new IndexedPatientData<>(MedicationController.DATA_NAME, Collections.emptyList()));
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verify(this.doc).removeXObjects(Medication.CLASS_REFERENCE);
        Mockito.verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveHandlesExceptions() throws Exception
    {
        setupSampleData();
        when(this.doc.newXObject(eq(Medication.CLASS_REFERENCE), any(XWikiContext.class)))
            .thenThrow(new XWikiException());
        this.mocker.getComponentUnderTest().save(this.patient, this.doc);
        Mockito.verify(this.doc).removeXObjects(Medication.CLASS_REFERENCE);
    }

    @Test
    public void saveUpdatesXObjects() throws Exception
    {
        setupSampleData();
        BaseObject obj1 = mock(BaseObject.class);
        BaseObject obj2 = mock(BaseObject.class);
        when(this.doc.newXObject(eq(Medication.CLASS_REFERENCE), any(XWikiContext.class)))
            .thenReturn(obj1, obj2);

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        verify(this.doc, times(1)).removeXObjects(Medication.CLASS_REFERENCE);

        verify(obj1).setStringValue(Medication.NAME, "n");
        verify(obj1).setStringValue(Medication.GENERIC_NAME, "gn");
        verify(obj1).setStringValue(Medication.DOSE, "d");
        verify(obj1).setStringValue(Medication.FREQUENCY, "f");
        verify(obj1).setIntValue(MedicationController.DURATION_MONTHS, 4);
        verify(obj1).setIntValue(MedicationController.DURATION_YEARS, 2);
        verify(obj1).setStringValue(Medication.EFFECT, "none");
        verify(obj1).setLargeStringValue(Medication.NOTES, "note");

        verify(obj2).setStringValue(Medication.NAME, "n2");
        verify(obj2).setStringValue(Medication.GENERIC_NAME, null);
        verify(obj2).setStringValue(Medication.DOSE, "");
        verify(obj2).setStringValue(Medication.FREQUENCY, null);
        verify(obj2, never()).setIntValue(eq(MedicationController.DURATION_MONTHS), any(Integer.class));
        verify(obj2, never()).setIntValue(eq(MedicationController.DURATION_YEARS), any(Integer.class));
        verify(obj2, never()).setStringValue(Medication.EFFECT, null);
        verify(obj2).setLargeStringValue(Medication.NOTES, "note2");
    }

    @Test
    public void writeRespectsSelectedFields() throws Exception
    {
        setupSampleData();
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singleton("nothing"));
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeWithNoDataDoesNothing() throws Exception
    {
        when(this.patient.getData(MedicationController.DATA_NAME)).thenReturn(null);
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeWithNonIndexedDataDoesNothing() throws Exception
    {
        when(this.patient.<Medication>getData(MedicationController.DATA_NAME)).thenReturn(
            new DictionaryPatientData<>(MedicationController.DATA_NAME, Collections.<String, Medication>emptyMap()));
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeWithEmptyDataDoesNothing() throws Exception
    {
        when(this.patient.getData(MedicationController.DATA_NAME))
            .thenReturn(new IndexedPatientData<>(MedicationController.DATA_NAME, Collections.emptyList()));
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeWithGarbageDataDoesNothing() throws Exception
    {
        when(this.patient.<Medication>getData(MedicationController.DATA_NAME)).thenReturn(
            new IndexedPatientData<>(MedicationController.DATA_NAME, Collections.<Medication>singletonList(null)));
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeFillsInData() throws Exception
    {
        setupSampleData();
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json,
            Collections.singleton(MedicationController.DATA_NAME));
        JSONArray output = json.getJSONArray(MedicationController.DATA_NAME);
        Assert.assertEquals(2, output.length());

        JSONObject o1 = output.getJSONObject(0);
        Assert.assertEquals("n", o1.get(Medication.NAME));
        Assert.assertEquals("gn", o1.get(Medication.GENERIC_NAME));
        Assert.assertEquals("d", o1.get(Medication.DOSE));
        Assert.assertEquals("f", o1.get(Medication.FREQUENCY));
        Assert.assertEquals("2Y4M", o1.get(Medication.DURATION));
        Assert.assertEquals("none", o1.get(Medication.EFFECT));
        Assert.assertEquals("note", o1.get(Medication.NOTES));

        JSONObject o2 = output.getJSONObject(1);
        Assert.assertEquals("n2", o2.get(Medication.NAME));
        Assert.assertFalse(o2.has(Medication.GENERIC_NAME));
        Assert.assertFalse(o2.has(Medication.DOSE));
        Assert.assertFalse(o2.has(Medication.FREQUENCY));
        Assert.assertFalse(o2.has(Medication.DURATION));
        Assert.assertFalse(o2.has(Medication.EFFECT));
        Assert.assertEquals("note2", o2.get(Medication.NOTES));
    }

    @Test
    public void readWithNullDataDoesNothing() throws Exception
    {
        PatientData<Medication> result = this.mocker.getComponentUnderTest().readJSON(null);
        Assert.assertNull(result);
    }

    @Test
    public void readWithNoDataDoesNothing() throws Exception
    {
        JSONObject json = new JSONObject();
        PatientData<Medication> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNull(result);
    }

    @Test
    public void readWithEmptyDataDoesNothing() throws Exception
    {
        JSONObject json = new JSONObject();
        json.put(MedicationController.DATA_NAME, new JSONArray());
        PatientData<Medication> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNull(result);
    }

    @Test
    public void readWithGarbageDataDoesNothing() throws Exception
    {
        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("nothing", "here");
        json.put(MedicationController.DATA_NAME, data);
        PatientData<Medication> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertNull(result);
    }

    @Test
    public void readRecoversData() throws Exception
    {
        JSONObject json = setupSampleJSON();
        PatientData<Medication> result = this.mocker.getComponentUnderTest().readJSON(json);
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
        Assert.assertEquals(MedicationEffect.NONE, m.getEffect());
        Assert.assertEquals("note", m.getNotes());

        m = result.get(1);
        Assert.assertEquals("n2", m.getName());
        Assert.assertNull(m.getGenericName());
        Assert.assertNull(m.getDose());
        Assert.assertNull(m.getFrequency());
        Assert.assertNull(m.getDuration());
        Assert.assertNull(m.getEffect());
        Assert.assertEquals("note2", m.getNotes());
    }

    @Test
    public void getName() throws Exception
    {
        Assert.assertEquals("medication", this.mocker.getComponentUnderTest().getName());
    }

    private void setupSampleData() throws XWikiException
    {
        List<Medication> input = new LinkedList<>();
        MutablePeriod p = new MutablePeriod();
        p.setMonths(4);
        p.setYears(2);
        Medication m = new Medication("n", "gn", "d", "f", p.toPeriod(), "none", "note");
        input.add(m);
        input.add(null);
        m = new Medication("n2", null, "", null, null, null, "note2");
        input.add(m);
        when(this.patient.<Medication>getData(MedicationController.DATA_NAME))
            .thenReturn(new IndexedPatientData<>(MedicationController.DATA_NAME, input));
    }

    private JSONObject setupSampleJSON()
    {
        JSONObject result = new JSONObject();

        JSONObject o1 = new JSONObject();
        o1.put(Medication.NAME, "n");
        o1.put(Medication.GENERIC_NAME, "gn");
        o1.put(Medication.DOSE, "d");
        o1.put(Medication.FREQUENCY, "f");
        o1.put(Medication.DURATION, "2Y4M");
        o1.put(Medication.EFFECT, "none");
        o1.put(Medication.NOTES, "note");
        result.accumulate(MedicationController.DATA_NAME, o1);

        JSONObject o2 = new JSONObject();
        o2.put(Medication.NAME, "n2");
        o2.put(Medication.NOTES, "note2");
        result.accumulate(MedicationController.DATA_NAME, o2);

        return result;
    }
}
