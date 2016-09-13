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

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientSpecificity;
import org.phenotips.data.PatientSpecificityService;

import org.xwiki.cache.CacheException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class SpecificityControllerTest
{
    @Mock
    private Patient patient;

    @Mock
    private PatientSpecificity spec;

    private PatientSpecificityService service;

    private Date date;

    private String dateStr;

    private DateFormat isoDateFormat;

    @Rule
    public final MockitoComponentMockingRule<PatientDataController<Object>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Object>>(SpecificityController.class);

    @Before
    public void setup() throws CacheException, ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
        this.isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.service = this.mocker.getInstance(PatientSpecificityService.class);
        when(this.spec.getComputingMethod()).thenReturn("monarchinitiative.org");
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+02:00"), Locale.ROOT);
        c.set(2010, 2, 20, 14, 20, 0);
        c.set(Calendar.MILLISECOND, 12);
        this.date = c.getTime();
        this.dateStr = this.isoDateFormat.format(this.date);
        when(this.spec.getComputationDate()).thenReturn(this.date);
        when(this.spec.getScore()).thenReturn(0.25);

        when(this.service.getSpecificity(this.patient)).thenReturn(this.spec);
    }

    @Test
    public void load() throws ComponentLookupException
    {
        PatientData<Object> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertTrue(result.isNamed());
        Assert.assertEquals("specificity", result.getName());
        Assert.assertEquals(0.25, (double) result.get("score"), 0.0);
        Assert.assertEquals(this.dateStr, result.get("date"));
        Assert.assertEquals("monarchinitiative.org", result.get("server"));
    }

    @Test
    public void loadWithNoSpecificityDoesNothing() throws ComponentLookupException
    {
        when(this.service.getSpecificity(this.patient)).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().load(this.patient));
    }

    @Test
    public void writeJSON() throws ComponentLookupException
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", this.dateStr);
        map.put("server", "monarchinitiative.org");
        map.put("score", 0.25d);
        PatientData<Object> data = new DictionaryPatientData<>("specificity", map);
        when(this.patient.getData("specificity")).thenReturn(data);
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertTrue(json.has("specificity"));
        json = json.getJSONObject("specificity");
        Assert.assertEquals("monarchinitiative.org", json.getString("server"));
        Assert.assertEquals("2010-03-20T12:20:00.012Z", json.getString("date"));
        Assert.assertEquals(0.25d, json.getDouble("score"), 0.0);
    }

    @Test
    public void writeJSONUpdatesJSON() throws ComponentLookupException
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", this.dateStr);
        map.put("server", "monarchinitiative.org");
        map.put("score", 0.25d);
        PatientData<Object> data = new DictionaryPatientData<>("specificity", map);
        when(this.patient.getData("specificity")).thenReturn(data);
        JSONObject json = new JSONObject();
        JSONObject specIn = new JSONObject();
        specIn.put("manualEvaluation", Double.valueOf(0.75));
        json.put("specificity", specIn);
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertTrue(json.has("specificity"));
        json = json.getJSONObject("specificity");
        Assert.assertEquals("monarchinitiative.org", json.getString("server"));
        Assert.assertEquals("2010-03-20T12:20:00.012Z", json.getString("date"));
        Assert.assertEquals(0.25d, json.getDouble("score"), 0.0);
        Assert.assertEquals("Existing 'specificity' object was discarded", 0.75, json.getDouble("manualEvaluation"),
            0.0);
    }

    @Test
    public void writeJSONCanBeSkipped() throws ComponentLookupException
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", this.date);
        map.put("server", "monarchinitiative.org");
        map.put("score", 0.25d);
        PatientData<Object> data = new DictionaryPatientData<>("specificity", map);
        when(this.patient.getData("specificity")).thenReturn(data);
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, Collections.singleton("phenotype"));
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithNoSpecificityDoesNothing() throws ComponentLookupException
    {
        when(this.patient.getData("specificity")).thenReturn(null);
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithNonNamedSpecificityDoesNothing() throws ComponentLookupException
    {
        when(this.patient.getData("specificity")).thenReturn(
            new IndexedPatientData<Object>("specificity", Collections.<Object>singletonList(0.25d)));
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void readJSONDoesNothing() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(null));
    }

    @Test
    public void saveDoesNothing() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient, null);
        this.mocker.getComponentUnderTest().save(null, null);
        Mockito.verifyZeroInteractions(this.service);
    }

    @Test
    public void getName() throws ComponentLookupException
    {
        Assert.assertEquals("specificity", this.mocker.getComponentUnderTest().getName());
    }
}
