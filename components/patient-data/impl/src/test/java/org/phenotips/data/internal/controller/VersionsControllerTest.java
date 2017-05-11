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

import org.phenotips.Constants;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.version.Version;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link VersionsController} Component, only the overridden methods from {@link AbstractSimpleController}
 * are tested here.
 */
public class VersionsControllerTest
{
    private static final EntityReference ONTOLOGY_VERSION_CLASS_REFERENCE =
        new EntityReference("OntologyVersionClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String DATA_NAME = "versions";

    private static final String PHENOTIPS_VERSION_STRING = "PHENOTIPS_VERSION";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(VersionsController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject firstOntologyVersion;

    @Mock
    private BaseObject secondOntologyVersion;

    @Mock
    private Provider<ComponentManager> cmProvider;

    @Mock
    private ComponentManager contextComponentManager;

    @Mock
    private DistributionManager distributionManager;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();

        doReturn("first").when(this.firstOntologyVersion).getStringValue("name");
        doReturn("1.0").when(this.firstOntologyVersion).getStringValue("version");
        doReturn("second").when(this.secondOntologyVersion).getStringValue("name");
        doReturn("2.0").when(this.secondOntologyVersion).getStringValue("version");
        List<BaseObject> ontologyVersionList = Arrays.asList(this.firstOntologyVersion, this.secondOntologyVersion);
        doReturn(ontologyVersionList).when(this.doc).getXObjects(ONTOLOGY_VERSION_CLASS_REFERENCE);

        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.cmProvider);
        doReturn(this.contextComponentManager).when(this.cmProvider).get();
        doReturn(this.distributionManager).when(this.contextComponentManager).getInstance(DistributionManager.class);
        CoreExtension coreExtension = Mockito.mock(CoreExtension.class);
        doReturn(coreExtension).when(this.distributionManager).getDistributionExtension();
        ExtensionId id = Mockito.mock(ExtensionId.class);
        doReturn(id).when(coreExtension).getId();
        Version version = Mockito.mock(Version.class);
        doReturn(version).when(id).getVersion();
        when(version.toString()).thenReturn(PHENOTIPS_VERSION_STRING);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals("meta",
            ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result = ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getProperties();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetEnablingFieldName()
    {
        Assert.assertEquals(DATA_NAME, VersionsController.getEnablingFieldName());
    }

    // --------------------load() is Overridden from AbstractSimpleController--------------------

    @Test
    public void loadCatchesInvalidDocument() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getXDocument();

        this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
    }

    @Test
    public void loadReturnsNormallyWhenPatientDoesNotHaveOntologyVersionsClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObjects(ONTOLOGY_VERSION_CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(PHENOTIPS_VERSION_STRING, result.get("phenotips_version"));
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void loadDoesNotAddBlankVersionInformation() throws ComponentLookupException
    {
        doReturn("").when(this.firstOntologyVersion).getStringValue("name");
        doReturn(null).when(this.secondOntologyVersion).getStringValue("version");

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(PHENOTIPS_VERSION_STRING, result.get("phenotips_version"));
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void loadIgnoresNullOntologyVersions()
        throws ComponentLookupException
    {
        List<BaseObject> ontologyVersionList = Arrays.asList(null, this.firstOntologyVersion);
        doReturn(ontologyVersionList).when(this.doc).getXObjects(ONTOLOGY_VERSION_CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals("1.0", result.get("first_version"));
        Assert.assertEquals(PHENOTIPS_VERSION_STRING, result.get("phenotips_version"));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void loadReturnsNormallyWhenContextComponentManagerThrowsComponentLookupException()
        throws ComponentLookupException
    {
        ComponentLookupException exception = new ComponentLookupException("message");
        doThrow(exception).when(this.contextComponentManager).getInstance(DistributionManager.class);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals("1.0", result.get("first_version"));
        Assert.assertEquals("2.0", result.get("second_version"));
        Assert.assertEquals(2, result.size());
        verify(this.mocker.getMockedLogger()).error("Could not find DistributionManager component");
    }

    // --------------------writeJSON() is Overridden from AbstractSimpleController--------------------

    @Test
    public void writeJSONAddsVersionInformationWhenSelectedFieldsContainsEnablingFieldName()
        throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("first_version", "1.0");
        map.put("second_version", "2.0");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFieldNames = new ArrayList<>();
        selectedFieldNames.add(VersionsController.getEnablingFieldName());

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFieldNames);

        Assert.assertTrue(json.get("meta") instanceof JSONObject);
        JSONObject container = json.getJSONObject("meta");
        Assert.assertEquals("1.0", container.get("first_version"));
        Assert.assertEquals("2.0", container.get("second_version"));

        // if selectedFieldNames is not Null, version information will be added iff selectedFieldNames
        // contains the enablingFieldName
        json = new JSONObject();
        selectedFieldNames.clear();
        selectedFieldNames.add("other_field_name");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFieldNames);

        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONAddsVersionInformationWhenSelectedFieldsIsNull() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("first_version", "1.0");
        map.put("second_version", "2.0");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertTrue(json.get("meta") instanceof JSONObject);
        JSONObject container = json.getJSONObject("meta");
        Assert.assertEquals("1.0", container.get("first_version"));
        Assert.assertEquals("2.0", container.get("second_version"));
    }
}
