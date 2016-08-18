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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientByExternalIdResource;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPatientByExternalIdResourceImplTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientByExternalIdResource> mocker =
        new MockitoComponentMockingRule<PatientByExternalIdResource>(DefaultPatientByExternalIdResourceImpl.class);

    @Mock
    private Patient patient;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

    @Mock
    private User user;

    private Logger logger;

    private PatientRepository repository;

    private DomainObjectFactory factory;

    private QueryManager qm;

    private AuthorizationManager access;

    private UserManager users;

    private URI uri;

    private XWikiContext context;

    private DefaultPatientByExternalIdResourceImpl component;

    private final String eid = "eid";

    private final String id = "id";

    private DocumentReference userReference = new DocumentReference("wiki", "XWiki", "padams");

    private DocumentReference patientReference = new DocumentReference("wiki", "data", "P0000001");

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        this.uri = new URI("uri");

        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");

        this.repository = this.mocker.getInstance(PatientRepository.class);
        this.factory = this.mocker.getInstance(DomainObjectFactory.class);
        this.qm = this.mocker.getInstance(QueryManager.class);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.users = this.mocker.getInstance(UserManager.class);
        this.component = (DefaultPatientByExternalIdResourceImpl) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.context = provider.get();
        ReflectionUtils.setFieldValue(this.component, "uriInfo", this.uriInfo);

        doReturn(this.uriBuilder).when(this.uriInfo).getBaseUriBuilder();
        when(this.patient.getId()).thenReturn(this.id);
        when(this.patient.getDocument()).thenReturn(this.patientReference);
        when(this.users.getCurrentUser()).thenReturn(this.user);
        when(this.user.getProfileDocument()).thenReturn(this.userReference);
        when(this.repository.getByName(this.eid)).thenReturn(this.patient);
        when(this.access.hasAccess(Right.VIEW, this.userReference, this.patientReference)).thenReturn(true);
        when(this.access.hasAccess(Right.EDIT, this.userReference, this.patientReference)).thenReturn(true);
        when(this.access.hasAccess(Right.DELETE, this.userReference, this.patientReference)).thenReturn(true);

        Autolinker autolinker = this.mocker.getInstance(Autolinker.class);
        when(autolinker.forResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withActionableResources(any(Class.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(any(String.class), any(String.class))).thenReturn(autolinker);
        when(autolinker.build()).thenReturn(Collections
            .singletonList(new org.phenotips.rest.model.Link()
                .withHref(this.uri.toString()).withRel("self")));
    }

    @Test
    public void getPatientWithNoAccessReturnsForbiddenCode() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.VIEW, this.userReference, this.patientReference)).thenReturn(false);

        Response response = this.component.getPatient(this.eid);
        verify(this.logger).debug("View access denied to user [{}] on patient record [{}]", this.user,
            this.patient.getId());

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientPerformsCorrectly() throws ComponentLookupException
    {
        JSONObject jsonObject = new JSONObject();
        when(this.patient.toJSON()).thenReturn(jsonObject);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(this.uriBuilder);
        when(this.uriBuilder.path(PatientResource.class)).thenReturn(this.uriBuilder);
        when(this.uriBuilder.build(this.patient.getId())).thenReturn(this.uri);

        Response response = this.component.getPatient(this.eid);
        verify(this.logger).debug("Retrieving patient record with external ID [{}] via REST", this.eid);

        JSONObject links = new JSONObject().accumulate("rel", "self").accumulate("href", "uri");
        JSONObject json = new JSONObject().put("links", Collections.singletonList(links));

        assertTrue(json.similar(response.getEntity()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(this.eid)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.getPatient(this.eid);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", this.eid);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void updatePatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(this.eid)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient("json", this.eid);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", this.eid);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientNoAccessReturnsForbiddenCode() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.EDIT, this.userReference, this.patientReference)).thenReturn(false);

        Response response = this.component.updatePatient("json", this.eid);
        verify(this.logger).debug("Edit access denied to user [{}] on patient record [{}]", null, this.patient.getId());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientWrongJSONId() throws ComponentLookupException
    {
        this.component.updatePatient("{\"id\":\"notid\"}", "eid");
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientFromJSONException() throws ComponentLookupException
    {
        doThrow(Exception.class).when(this.patient).updateFromJSON(Matchers.any(JSONObject.class));

        this.component.updatePatient("{\"id\":\"id\"}", "eid");
        verify(this.logger).debug("Failed to update patient [{}] from JSON: {}. Source JSON was: {}",
            this.id, "{\"id\":\"id\"}");
    }

    @Test
    public void updatePatientReturnsNoContentResponse() throws ComponentLookupException
    {
        String json = "{\"id\":\"id\"}";
        Response response = this.component.updatePatient(json, this.eid);
        verify(this.logger).debug("Updating patient record with external ID [{}] via REST with JSON: {}", this.eid,
            json);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientReturnsNotFoundStatus() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(this.eid)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.deletePatient(this.eid);

        verify(this.logger).debug("No patient record with external ID [{}] exists yet", this.eid);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientNoAccessReturnsForbiddenCode()
    {
        when(this.access.hasAccess(Right.DELETE, this.userReference, this.patientReference)).thenReturn(false);

        Response response = this.component.deletePatient(this.eid);

        verify(this.logger).debug("Delete access denied to user [{}] on patient record [{}]", this.user,
            this.patient.getId());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientCatchesXWikiException() throws XWikiException
    {
        XWiki wiki = mock(XWiki.class);
        doReturn(wiki).when(this.context).getWiki();
        doThrow(XWikiException.class).when(wiki).deleteDocument(any(XWikiDocument.class), eq(this.context));
        when(this.repository.getByName(this.eid)).thenReturn(this.patient);
        when(this.access.hasAccess(Right.DELETE, null, null)).thenReturn(true);

        WebApplicationException ex = null;
        try {
            this.component.deletePatient(this.eid);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        assertNotNull("deletePatient did not throw a WebApplicationException as expect "
            + "when catching an XWikiException", ex);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
        verify(this.logger).warn(eq("Failed to delete patient record with external id [{}]: {}"), eq(this.eid),
            anyString());
    }

    @Test
    public void deletePatientReturnsNoContentResponse()
    {
        XWiki wiki = mock(XWiki.class);
        doReturn(wiki).when(this.context).getWiki();

        Response response = this.component.deletePatient(this.eid);

        verify(this.logger).debug("Deleting patient record with external ID [{}] via REST", this.eid);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void checkForMultipleRecordsPerformsCorrectly() throws QueryException
    {
        Query q = mock(DefaultQuery.class);
        doReturn(q).when(this.qm)
            .createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);

        List<String> results = new ArrayList<>();
        results.add(this.eid);
        results.add(this.eid);
        doReturn(results).when(q).execute();

        when(this.repository.getByName(this.eid)).thenReturn(null);

        Response responseGet = this.component.getPatient(this.eid);
        Response responseUpdate = this.component.updatePatient(this.eid, this.eid);
        Response responseDelete = this.component.deletePatient(this.eid);

        verify(this.logger, times(3)).debug("Multiple patient records ({}) with external ID [{}]: {}",
            2, this.eid, results);
        verify(this.factory, times(3)).createAlternatives(anyListOf(String.class), eq(this.uriInfo));
        assertEquals(300, responseGet.getStatus());
        assertEquals(300, responseUpdate.getStatus());
        assertEquals(300, responseDelete.getStatus());
    }

    @Test
    public void checkForMultipleRecordsLogsWhenThrowsQueryException() throws QueryException
    {
        doThrow(QueryException.class).when(this.qm)
            .createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);

        when(this.repository.getByName(this.eid)).thenReturn(null);

        Response responseGet = this.component.getPatient(this.eid);
        Response responseUpdate = this.component.updatePatient(this.eid, this.eid);
        Response responseDelete = this.component.deletePatient(this.eid);

        verify(this.logger, times(3)).warn("Failed to retrieve patient with external id [{}]: {}", this.eid, null);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseGet.getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseUpdate.getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseDelete.getStatus());
    }
}
