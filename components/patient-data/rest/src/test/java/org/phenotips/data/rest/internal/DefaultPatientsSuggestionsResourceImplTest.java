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
package org.phenotips.data.rest.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.PatientsSuggestionsResource;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultPatientsSuggestionsResourceImplTest
{
    private static final String ID = "id";

    private static final String EID = "eid";

    @Rule
    public final MockitoComponentMockingRule<PatientsSuggestionsResource> mocker =
        new MockitoComponentMockingRule<>(DefaultPatientsSuggestionsResourceImpl.class);

    private PatientsSuggestionsResource component;

    private PatientRepository repository;

    private String readablePatient1Name = "readablePatient1";

    private DocumentReference readablePatient1Reference =
        new DocumentReference("wiki", "data", this.readablePatient1Name);

    @Mock
    private Patient readablePatient1;

    private String readablePatient2Name = "readablePatient2";

    private DocumentReference readablePatient2Reference =
        new DocumentReference("wiki", "data", this.readablePatient2Name);

    @Mock
    private Patient readablePatient2;

    private String writablePatient1Name = "writablePatient1";

    private DocumentReference writablePatient1Reference =
        new DocumentReference("wiki", "data", this.writablePatient1Name);

    @Mock
    private Patient writablePatient1;

    private String writablePatient2Name = "writablePatient2";

    private DocumentReference writablePatient2Reference =
        new DocumentReference("wiki", "data", this.writablePatient2Name);

    @Mock
    private Patient writablePatient2;

    private String inaccessiblePatient1Name = "inaccessiblePatient1";

    private DocumentReference inaccessiblePatient1Reference =
        new DocumentReference("wiki", "data", this.inaccessiblePatient1Name);

    @Mock
    private Patient inaccessiblePatient1;

    private String inaccessiblePatient2Name = "inaccessiblePatient2";

    private DocumentReference inaccessiblePatient2Reference =
        new DocumentReference("wiki", "data", this.inaccessiblePatient2Name);

    @Mock
    private Patient inaccessiblePatient2;

    private AuthorizationService auth;

    @Mock
    private Logger logger;

    private QueryManager queryManager;

    @Mock
    private Query query;

    @Mock
    private RecordConfiguration configuration;

    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private User user;

    @Before
    public void setUp() throws ComponentLookupException, QueryException
    {
        MockitoAnnotations.initMocks(this);
        Provider<XWikiContext> xcp = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xcontext = xcp.get();
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(this.xcontext).when(executionContext).getProperty("xwikicontext");

        this.component = this.mocker.getComponentUnderTest();

        this.logger = this.mocker.getMockedLogger();

        UserManager userManager = this.mocker.getInstance(UserManager.class);
        when(userManager.getCurrentUser()).thenReturn(this.user);

        this.repository = this.mocker.getInstance(PatientRepository.class);
        this.auth = this.mocker.getInstance(AuthorizationService.class);
        setupPatient(this.readablePatient1, this.readablePatient1Name, null, this.readablePatient1Reference, true,
            false,
            "John", "Doe");
        setupPatient(this.readablePatient2, this.readablePatient2Name, "", this.readablePatient2Reference, true, false,
            "Timmy", null);
        setupPatient(this.writablePatient1, this.writablePatient1Name, "Beast", this.writablePatient1Reference, true,
            true, null,
            "McCoy");
        setupPatient(this.writablePatient2, this.writablePatient2Name, null, this.writablePatient2Reference, true, true,
            null,
            null);
        setupPatient(this.inaccessiblePatient1, this.inaccessiblePatient1Name, "X", this.inaccessiblePatient1Reference,
            false, false, "Charles", "Xavier");
        setupPatient(this.inaccessiblePatient2, this.inaccessiblePatient2Name, null, this.inaccessiblePatient2Reference,
            false, false, null, null);

        RecordConfigurationManager rcm = this.mocker.getInstance(RecordConfigurationManager.class);
        when(rcm.getConfiguration("patient")).thenReturn(this.configuration);
        when(this.configuration.getEnabledFieldNames())
            .thenReturn(Arrays.asList("first_name", "last_name", "date_of_birth", "phenotypes"));

        this.queryManager = this.mocker.getInstance(QueryManager.class);
        when(this.queryManager.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(this.query);
        when(this.query.execute()).thenReturn(Arrays.asList("readablePatient1", "inaccessiblePatient",
            "writablePatient1", "readablePatient2", "writablePatient2"));
    }

    @Test(expected = WebApplicationException.class)
    public void suggestAsJSONWithNullInputThrowsException()
    {
        this.component.suggestAsJSON(null, 5, "view", ID, "asc");
    }

    @Test(expected = WebApplicationException.class)
    public void suggestAsXMLWithNullInputThrowsException()
    {
        this.component.suggestAsXML(null, 5, "view", ID, "asc");
    }

    @Test(expected = WebApplicationException.class)
    public void suggestAsJSONWithEmptyInputThrowsException()
    {
        this.component.suggestAsJSON("", 5, "view", ID, "asc");
    }

    @Test(expected = WebApplicationException.class)
    public void suggestAsXMLWithEmptyInputThrowsException()
    {
        this.component.suggestAsXML("", 5, "view", ID, "asc");
    }

    @Test
    public void suggestAsJSONWithViewAccessAndSortById() throws QueryException
    {
        JSONObject response = new JSONObject(this.component.suggestAsJSON("dOe", 2, "view", ID, "asc"));
        JSONArray suggestions = response.getJSONArray("matchedPatients");
        Mockito.verify(this.queryManager).createQuery(
            "select doc.name from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                + " where doc.name <> :t"
                + " and lower(doc.name) like :input or lower(patient.external_id) like :input"
                + " or lower(patient.first_name) like :input or lower(patient.last_name) like :input"
                + " order by doc.name asc",
            Query.XWQL);
        Mockito.verify(this.query).bindValue("t", "PatientTemplate");
        Mockito.verify(this.query).bindValue("input", "%doe%");
        Assert.assertEquals(2, suggestions.length());
        Assert.assertEquals("readablePatient1", suggestions.getJSONObject(0).getString(ID));
        Assert.assertEquals("writablePatient1", suggestions.getJSONObject(1).getString(ID));
    }

    @Test
    public void suggestAsXMLWithViewAccessAndSortById() throws Exception
    {
        Document response = parseXML(this.component.suggestAsXML("dOe", 2, "view", ID, "asc"));
        NodeList suggestions = response.getElementsByTagName("rs");
        Mockito.verify(this.queryManager).createQuery(
            "select doc.name from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                + " where doc.name <> :t"
                + " and lower(doc.name) like :input or lower(patient.external_id) like :input"
                + " or lower(patient.first_name) like :input or lower(patient.last_name) like :input"
                + " order by doc.name asc",
            Query.XWQL);
        Mockito.verify(this.query).bindValue("t", "PatientTemplate");
        Mockito.verify(this.query).bindValue("input", "%doe%");
        Assert.assertEquals(2, suggestions.getLength());
        Assert.assertEquals("/view/readablePatient1", ((Element) suggestions.item(0)).getAttribute(ID));
        Assert.assertEquals("wiki:data.readablePatient1", ((Element) suggestions.item(0)).getAttribute("info"));
        Assert.assertEquals("readablePatient1, name: John Doe", ((Element) suggestions.item(0)).getTextContent());
        Assert.assertEquals("/view/writablePatient1", ((Element) suggestions.item(1)).getAttribute(ID));
        Assert.assertEquals("wiki:data.writablePatient1", ((Element) suggestions.item(1)).getAttribute("info"));
        Assert.assertEquals("writablePatient1, name: McCoy, identifier: Beast",
            ((Element) suggestions.item(1)).getTextContent());
    }

    @Test
    public void suggestAsJSONWithEditAccessAndSortByEid() throws QueryException
    {
        JSONObject response = new JSONObject(this.component.suggestAsJSON("dOe", 4, "edit", EID, "desc"));
        JSONArray suggestions = response.getJSONArray("matchedPatients");
        Mockito.verify(this.queryManager).createQuery(
            "select doc.name from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                + " where doc.name <> :t"
                + " and lower(doc.name) like :input or lower(patient.external_id) like :input"
                + " or lower(patient.first_name) like :input or lower(patient.last_name) like :input"
                + " order by patient.external_id desc",
            Query.XWQL);
        Mockito.verify(this.query).bindValue("t", "PatientTemplate");
        Mockito.verify(this.query).bindValue("input", "%doe%");
        Assert.assertEquals(2, suggestions.length());
        Assert.assertEquals("writablePatient1", suggestions.getJSONObject(0).getString(ID));
        Assert.assertEquals("Beast", suggestions.getJSONObject(0).getString("identifier"));
        Assert.assertEquals("writablePatient1, name: McCoy, identifier: Beast",
            suggestions.getJSONObject(0).getString("textSummary"));
        Assert.assertEquals("writablePatient2", suggestions.getJSONObject(1).getString(ID));
        Assert.assertFalse(suggestions.getJSONObject(1).has("identifier"));
        Assert.assertEquals("writablePatient2", suggestions.getJSONObject(1).getString("textSummary"));
    }

    @Test
    public void suggestAsXMLWithEditAccessAndSortByEid() throws Exception
    {
        Document response = parseXML(this.component.suggestAsXML("dOe", 4, "edit", EID, "desc"));
        NodeList suggestions = response.getElementsByTagName("rs");
        Mockito.verify(this.queryManager).createQuery(
            "select doc.name from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                + " where doc.name <> :t"
                + " and lower(doc.name) like :input or lower(patient.external_id) like :input"
                + " or lower(patient.first_name) like :input or lower(patient.last_name) like :input"
                + " order by patient.external_id desc",
            Query.XWQL);
        Mockito.verify(this.query).bindValue("t", "PatientTemplate");
        Mockito.verify(this.query).bindValue("input", "%doe%");
        Assert.assertEquals(2, suggestions.getLength());
        Assert.assertEquals("/view/writablePatient1", ((Element) suggestions.item(0)).getAttribute(ID));
        Assert.assertEquals("wiki:data.writablePatient1", ((Element) suggestions.item(0)).getAttribute("info"));
        Assert.assertEquals("writablePatient1, name: McCoy, identifier: Beast",
            ((Element) suggestions.item(0)).getTextContent());
        Assert.assertEquals("/view/writablePatient2", ((Element) suggestions.item(1)).getAttribute(ID));
        Assert.assertEquals("wiki:data.writablePatient2", ((Element) suggestions.item(1)).getAttribute("info"));
        Assert.assertEquals("writablePatient2", ((Element) suggestions.item(1)).getTextContent());
    }

    @Test
    public void suggestAsJSONWithNamesDisabled() throws QueryException
    {
        when(this.configuration.getEnabledFieldNames()).thenReturn(Arrays.asList("phenotypes"));
        JSONObject response = new JSONObject(this.component.suggestAsJSON("dOe", 2, "view", ID, "asc"));
        JSONArray suggestions = response.getJSONArray("matchedPatients");
        Mockito.verify(this.queryManager).createQuery(
            "select doc.name from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                + " where doc.name <> :t"
                + " and lower(doc.name) like :input or lower(patient.external_id) like :input"
                + " order by doc.name asc",
            Query.XWQL);
        Mockito.verify(this.query).bindValue("t", "PatientTemplate");
        Mockito.verify(this.query).bindValue("input", "%doe%");
        Assert.assertEquals(2, suggestions.length());
        Assert.assertEquals("readablePatient1", suggestions.getJSONObject(0).getString(ID));
        Assert.assertEquals("writablePatient1", suggestions.getJSONObject(1).getString(ID));
    }

    @Test
    public void suggestAsJSONWithMorePatientsRequested() throws QueryException
    {
        JSONObject response = new JSONObject(this.component.suggestAsJSON("dOe", 200, "view", ID, "asc"));
        JSONArray suggestions = response.getJSONArray("matchedPatients");
        Mockito.verify(this.queryManager).createQuery(
            "select doc.name from Document doc, doc.object(PhenoTips.PatientClass) as patient"
                + " where doc.name <> :t"
                + " and lower(doc.name) like :input or lower(patient.external_id) like :input"
                + " or lower(patient.first_name) like :input or lower(patient.last_name) like :input"
                + " order by doc.name asc",
            Query.XWQL);
        Mockito.verify(this.query).bindValue("t", "PatientTemplate");
        Mockito.verify(this.query).bindValue("input", "%doe%");
        Assert.assertEquals(4, suggestions.length());
        Assert.assertEquals("readablePatient1", suggestions.getJSONObject(0).getString(ID));
        Assert.assertEquals("writablePatient1", suggestions.getJSONObject(1).getString(ID));
        Assert.assertEquals("readablePatient2", suggestions.getJSONObject(2).getString(ID));
        Assert.assertEquals("writablePatient2", suggestions.getJSONObject(3).getString(ID));
    }

    @Test
    public void suggestAsJSONReturnsEmptyListOnExceptions() throws QueryException
    {
        when(this.query.execute()).thenThrow(new QueryException("", this.query, null));
        JSONObject response = new JSONObject(this.component.suggestAsJSON("dOe", 200, "view", ID, "asc"));
        JSONArray suggestions = response.getJSONArray("matchedPatients");
        Assert.assertEquals(0, suggestions.length());
    }

    @SuppressWarnings("ParameterNumber")
    private void setupPatient(Patient patient, String patientId, String patientExternalId,
        DocumentReference patientReference, boolean canView, boolean canEdit, String firstName, String lastName)
    {
        when(this.repository.get(patientId)).thenReturn(patient);
        when(patient.getExternalId()).thenReturn(patientExternalId);
        when(patient.getId()).thenReturn(patientId);
        when(patient.getDocumentReference()).thenReturn(patientReference);
        when(this.auth.hasAccess(this.user, Right.VIEW, patientReference)).thenReturn(canView);
        when(this.auth.hasAccess(this.user, Right.EDIT, patientReference)).thenReturn(canEdit);
        when(this.xwiki.getURL(patientReference, "view", this.xcontext)).thenReturn("/view/" + patientId);
        Map<String, String> nameData = new HashMap<>();
        if (StringUtils.isNotEmpty(firstName)) {
            nameData.put("first_name", firstName);
        }
        if (StringUtils.isNotEmpty(lastName)) {
            nameData.put("last_name", lastName);
        }
        if (!nameData.isEmpty()) {
            when(patient.<String>getData("patientName"))
                .thenReturn(new DictionaryPatientData<>("patientName", nameData));
        }
    }

    private Document parseXML(String input) throws Exception
    {
        DOMImplementationLS implementation =
            (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        LSInput in = implementation.createLSInput();
        in.setStringData(input);
        LSParser parser = implementation.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
        Document doc = parser.parse(in);
        return doc;
    }
}
