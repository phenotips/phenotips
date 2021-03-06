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
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientByExternalIdResource;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.Autolinker;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPatientByExternalIdResourceImplTest
{
    private static final String EID = "eid";

    private static final String ID = "id";

    private static final String PATIENT_ID = "P0000001";

    private static final String EMPTY_JSON = "{}";

    private static final String EXTERNAL_ID = "external_id";

    private static final String UPDATE = "update";

    @Rule
    public final MockitoComponentMockingRule<PatientByExternalIdResource> mocker =
        new MockitoComponentMockingRule<>(DefaultPatientByExternalIdResourceImpl.class);

    @Mock
    private Patient patient;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

    @Mock
    private User currentUser;

    @Mock
    private EntityReferenceResolver<EntityReference> resolver;

    private Logger logger;

    private PatientRepository repository;

    private DomainObjectFactory factory;

    private QueryManager qm;

    private AuthorizationService access;

    private URI uri;

    private DefaultPatientByExternalIdResourceImpl component;

    private DocumentReference patientReference = new DocumentReference("wiki", "data", PATIENT_ID);

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
        this.access = this.mocker.getInstance(AuthorizationService.class);

        final UserManager users = this.mocker.getInstance(UserManager.class);
        this.component = (DefaultPatientByExternalIdResourceImpl) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.resolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
        ReflectionUtils.setFieldValue(this.component, "uriInfo", this.uriInfo);

        when(this.resolver.resolve(any(EntityReference.class), any(EntityType.class), any()))
            .thenReturn(mock(EntityReference.class));
        doReturn(this.uriBuilder).when(this.uriInfo).getBaseUriBuilder();
        when(this.patient.getId()).thenReturn(ID);
        when(this.patient.getDocumentReference()).thenReturn(this.patientReference);
        when(users.getCurrentUser()).thenReturn(this.currentUser);
        when(this.repository.getByName(EID)).thenReturn(this.patient);
        when(this.repository.create()).thenReturn(this.patient);
        when(this.access.hasAccess(this.currentUser, Right.VIEW, this.patientReference)).thenReturn(true);
        when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(true);
        when(this.access.hasAccess(this.currentUser, Right.DELETE, this.patientReference)).thenReturn(true);
        when(this.access.hasAccess(eq(this.currentUser), eq(Right.EDIT), any(EntityReference.class)))
            .thenReturn(true);

        Autolinker autolinker = this.mocker.getInstance(Autolinker.class);
        when(autolinker.forResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withGrantedRight(any(Right.class))).thenReturn(autolinker);
        when(autolinker.withActionableResources(any(Class.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(any(String.class), any(String.class))).thenReturn(autolinker);
        when(autolinker.build()).thenReturn(Collections
            .singletonList(new org.phenotips.rest.model.Link().withAllowedMethods(Collections.singletonList("GET"))
                .withHref(this.uri.toString()).withRel("self")));
    }

    @Test
    public void getPatientWithNoAccessReturnsForbiddenCode() throws ComponentLookupException
    {
        when(this.access.hasAccess(this.currentUser, Right.VIEW, this.patientReference)).thenReturn(false);

        Response response = this.component.getPatient(EID);
        verify(this.logger).debug("View access denied to user [{}] on patient record [{}]", this.currentUser,
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

        Response response = this.component.getPatient(EID);
        verify(this.logger).debug("Retrieving patient record with external ID [{}] via REST", EID);

        JSONObject links = new JSONObject().accumulate("rel", "self").accumulate("href", "uri")
            .put("allowedMethods", new JSONArray(Collections.singletonList("GET")));
        JSONObject json = new JSONObject().put("links", Collections.singletonList(links));

        assertTrue(json.similar(response.getEntity()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(EID)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.getPatient(EID);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", EID);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void updatePatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(EID)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient(EMPTY_JSON, EID, UPDATE);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", EID);
        verify(this.logger).debug("Creating patient record with external ID [{}]", EID);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void updatePatientNotFoundNewPatientNotCreatedIfInvalidJson() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(EID)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient("[]", EID, UPDATE);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", EID);
        verify(this.logger).debug("Creating patient record with external ID [{}]", EID);
        verify(this.repository, never()).create();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void updatePatientNotFoundNewPatientNotCreatedIfUserDoesNotHaveEditRights() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(EID)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());
        when(this.access.hasAccess(eq(this.currentUser), eq(Right.EDIT), any(EntityReference.class)))
            .thenReturn(false);

        Response response = this.component.updatePatient(EMPTY_JSON, EID, UPDATE);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", EID);
        verify(this.logger).debug("Creating patient record with external ID [{}]", EID);
        verify(this.logger).error("Edit access denied to user [{}].", this.currentUser);
        verify(this.repository, never()).create();
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void updatePatientNotFoundNewPatientSpecifiesIdInJson() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(EID)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient("{\"external_id\":\"abc\"}", EID, UPDATE);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", EID);
        verify(this.logger).debug("Creating patient record with external ID [{}]", EID);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture());
        assertEquals("abc", json.getValue().optString(EXTERNAL_ID));
        assertEquals(1, json.getValue().length());
    }

    @Test
    public void updatePatientNotFoundNewPatientNoIdInJson() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(EID)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient(EMPTY_JSON, EID, UPDATE);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", EID);
        verify(this.logger).debug("Creating patient record with external ID [{}]", EID);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture());
        assertEquals(EID, json.getValue().optString(EXTERNAL_ID));
        assertEquals(1, json.getValue().length());
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientNoAccessReturnsForbiddenCode() throws ComponentLookupException
    {
        when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(false);

        Response response = this.component.updatePatient("json", EID, UPDATE);
        verify(this.logger).debug("Edit access denied to user [{}] on patient record [{}]", null, this.patient.getId());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientWrongJSONId() throws ComponentLookupException
    {
        this.component.updatePatient("{\"id\":\"notid\"}", EID, UPDATE);
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientFromJSONException() throws ComponentLookupException
    {
        doThrow(Exception.class).when(this.patient).updateFromJSON(Matchers.any(JSONObject.class), Matchers.any(
            PatientWritePolicy.class));

        this.component.updatePatient("{\"id\":\"id\"}", EID, UPDATE);
        verify(this.logger).debug("Failed to update patient [{}] from JSON: {}. Source JSON was: {}",
            ID, "{\"id\":\"id\"}");
    }

    @Test
    public void updatePatientReturnsNoContentResponse() throws ComponentLookupException
    {
        String json = "{\"id\":\"id\"}";
        Response response = this.component.updatePatient(json, EID, UPDATE);
        verify(this.logger).debug("Updating patient record with external ID [{}] via REST with JSON: {}", EID,
            json);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientReturnsNotFoundStatus() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getByName(EID)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.deletePatient(EID);

        verify(this.logger).debug("No patient record with external ID [{}] exists yet", EID);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientNoAccessReturnsForbiddenCode()
    {
        when(this.access.hasAccess(this.currentUser, Right.DELETE, this.patientReference)).thenReturn(false);

        Response response = this.component.deletePatient(EID);

        verify(this.logger).debug("Delete access denied to user [{}] on patient record [{}]", this.currentUser,
            this.patient.getId());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientCatchesException() throws WebApplicationException
    {
        doThrow(Exception.class).when(this.repository).delete(this.patient);
        when(this.repository.getByName(EID)).thenReturn(this.patient);
        when(this.access.hasAccess(null, Right.DELETE, null)).thenReturn(true);

        WebApplicationException ex = null;
        try {
            this.component.deletePatient(EID);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        assertNotNull("deletePatient did not throw a WebApplicationException as expected "
            + "when catching an Exception", ex);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
        verify(this.logger).warn(eq("Failed to delete patient record with external id [{}]: {}"), eq(EID),
            anyString());
    }

    @Test
    public void deletePatientReturnsNoContentResponse()
    {
        Response response = this.component.deletePatient(EID);

        verify(this.repository).delete(this.patient);
        verify(this.logger).debug("Deleting patient record with external ID [{}] via REST", EID);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void checkForMultipleRecordsPerformsCorrectly() throws QueryException
    {
        Query q = mock(DefaultQuery.class);
        doReturn(q).when(this.qm)
            .createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);

        List<String> results = new ArrayList<>();
        results.add(EID);
        results.add(EID);
        doReturn(results).when(q).execute();

        when(this.repository.getByName(EID)).thenReturn(null);

        Response responseGet = this.component.getPatient(EID);
        Response responseUpdate = this.component.updatePatient(EID, EID, UPDATE);
        Response responseDelete = this.component.deletePatient(EID);

        verify(this.logger, times(3)).debug("Multiple patient records ({}) with external ID [{}]: {}",
            2, EID, results);
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

        when(this.repository.getByName(EID)).thenReturn(null);

        Response responseGet = this.component.getPatient(EID);
        Response responseUpdate = this.component.updatePatient(EMPTY_JSON, EID, UPDATE);
        Response responseDelete = this.component.deletePatient(EID);

        verify(this.logger, times(3)).warn("Failed to retrieve patient with external id [{}]: {}", EID, null);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseGet.getStatus());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), responseUpdate.getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseDelete.getStatus());
    }

    // ----------------------------Patch Patient Tests-----------------------------

    @Test(expected = WebApplicationException.class)
    public void patchPatientWithNullJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient(null, EID);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientWithEmptyJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient(StringUtils.EMPTY, EID);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientWithBlankJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient(StringUtils.SPACE, EID);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void patchPatientNoPatientWithSpecifiedEidExistsResultsPatientBeingCreated() throws QueryException
    {
        when(this.repository.get(PATIENT_ID)).thenReturn(null);
        final Query query = mock(Query.class);
        when(this.qm.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);
        when(query.execute()).thenReturn(Collections.emptyList());
        final Response response = this.component.patchPatient(EMPTY_JSON, PATIENT_ID);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(this.repository, times(1)).create();
    }

    @Test
    public void patchPatientManyPatientsWithSpecifiedEidExistResultsInCorrectResponse() throws QueryException
    {
        when(this.repository.get(PATIENT_ID)).thenReturn(null);
        final Query query = mock(Query.class);
        when(this.qm.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("one", "two"));
        final Response response = this.component.patchPatient(EMPTY_JSON, PATIENT_ID);
        Assert.assertEquals(300, response.getStatus());
        verify(this.repository, never()).create();
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientUserHasNoEditAccessThrowsWebApplicationException()
    {
        try {
            when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(false);
            this.component.patchPatient(EMPTY_JSON, EID);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientInvalidJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient("[]", EID);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientIdFromJsonAndRetrievedPatientIdConflictThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient("{\"id\":\"wrong\"}", EID);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.CONFLICT.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientUpdatingPatientFromJsonFailsResultsInWebApplicationException()
    {
        try {
            doThrow(new RuntimeException()).when(this.patient).updateFromJSON(any(JSONObject.class),
                eq(PatientWritePolicy.MERGE));
            this.component.patchPatient(EMPTY_JSON, EID);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void patchPatientUpdatesPatientSuccessfully()
    {
        final Response response = this.component.patchPatient(EMPTY_JSON, EID);
        verify(this.patient, times(1)).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.MERGE));
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
}
