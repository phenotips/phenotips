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
import org.phenotips.data.rest.PatientByLabeledExternalIdentifierResource;
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

public class DefaultPatientByLabeledExternalIdentifierResourceImplTest
{
    private static final String ID_LABEL = "a_label";

    private static final String ID_VALUE = "eid";

    private static final String PATIENT_ID = "P0000001";

    private static final String EMPTY_JSON = "{}";

    private static final String KEY_LABELED_EIDS = "labeled_eids";

    private static final String UPDATE = "update";

    @Rule
    public final MockitoComponentMockingRule<PatientByLabeledExternalIdentifierResource> mocker =
        new MockitoComponentMockingRule<>(DefaultPatientByLabeledExternalIdentifierResourceImpl.class);

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

    @Mock
    private Query patientsQuery;

    @Mock
    private Query allowOtherQuery;

    private AuthorizationService access;

    private URI uri;

    private PatientByLabeledExternalIdentifierResource component;

    private DocumentReference patientReference = new DocumentReference("wiki", "data", PATIENT_ID);

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException, QueryException
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

        when(this.qm.createQuery(anyString(), anyString())).thenReturn(this.patientsQuery);
        when(this.qm.createQuery("select obj.allowOtherEids from Document doc, doc.object("
            + "PhenoTips.LabeledIdentifierGlobalSettings) obj", Query.XWQL)).thenReturn(this.allowOtherQuery);
        when(this.allowOtherQuery.execute()).thenReturn(Collections.singletonList(Integer.valueOf(1)));

        final UserManager users = this.mocker.getInstance(UserManager.class);
        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.resolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
        ReflectionUtils.setFieldValue(this.component, "uriInfo", this.uriInfo);

        when(this.resolver.resolve(any(EntityReference.class), any(EntityType.class), any()))
            .thenReturn(mock(EntityReference.class));
        doReturn(this.uriBuilder).when(this.uriInfo).getBaseUriBuilder();
        when(this.patient.getId()).thenReturn(PATIENT_ID);
        when(this.patient.getDocumentReference()).thenReturn(this.patientReference);
        when(users.getCurrentUser()).thenReturn(this.currentUser);
        when(this.repository.get(PATIENT_ID)).thenReturn(this.patient);
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

    @Test(expected = WebApplicationException.class)
    public void getPatientWithNoAccessReturnsForbiddenCode() throws ComponentLookupException, QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        when(this.access.hasAccess(this.currentUser, Right.VIEW, this.patientReference)).thenReturn(false);

        try {
            this.component.getPatient(ID_LABEL, ID_VALUE);
        } catch (final WebApplicationException ex) {
            verify(this.logger).debug("View access denied to user [{}] on patient record [{}]", this.currentUser,
                PATIENT_ID);
            Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void getPatientPerformsCorrectly() throws ComponentLookupException, QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        JSONObject jsonObject = new JSONObject();
        when(this.patient.toJSON()).thenReturn(jsonObject);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(this.uriBuilder);
        when(this.uriBuilder.path(PatientResource.class)).thenReturn(this.uriBuilder);
        when(this.uriBuilder.build(this.patient.getId())).thenReturn(this.uri);

        Response response = this.component.getPatient(ID_LABEL, ID_VALUE);
        verify(this.logger).debug("Retrieving patient record with label [{}] and corresponding external ID "
            + "[{}] via REST", ID_LABEL, ID_VALUE);

        JSONObject links = new JSONObject().accumulate("rel", "self").accumulate("href", "uri")
            .put("allowedMethods", new JSONArray(Collections.singletonList("GET")));
        JSONObject json = new JSONObject().put("links", Collections.singletonList(links));

        assertTrue(json.similar(response.getEntity()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void getPatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException
    {
        try {
            when(this.patientsQuery.execute()).thenReturn(new ArrayList<>());
            this.component.getPatient(ID_LABEL, ID_VALUE);
        } catch (final WebApplicationException ex) {
            verify(this.logger).debug("No patient record with label [{}] and corresponding external ID [{}] exists yet",
                ID_LABEL, ID_VALUE);
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void updatePatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        verify(this.logger).debug("No patient record with label [{}] and corresponding external ID [{}] exists yet",
            ID_LABEL, ID_VALUE);
        verify(this.logger).debug("Creating patient record with label [{}] and corresponding external ID [{}]",
            ID_LABEL, ID_VALUE);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientNotFoundNewPatientNotCreatedIfInvalidJson() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(new ArrayList<>());
        try {
            this.component.updatePatient("[]", ID_LABEL, ID_VALUE, UPDATE, true);
        } catch (final WebApplicationException ex) {
            verify(this.repository, never()).create();
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientNotFoundNewPatientNotCreatedIfUserDoesNotHaveEditRights() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(new ArrayList<>());
        when(this.access.hasAccess(eq(this.currentUser), eq(Right.EDIT), any(EntityReference.class)))
            .thenReturn(false);

        try {
            this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        } catch (final WebApplicationException ex) {
            verify(this.logger).debug("No patient record with label [{}] and corresponding external ID [{}] exists yet",
                ID_LABEL, ID_VALUE);
            verify(this.logger).debug("Creating patient record with label [{}] and corresponding external ID [{}]",
                ID_LABEL, ID_VALUE);
            verify(this.logger).error("Edit access denied to user [{}].", this.currentUser);
            verify(this.repository, never()).create();
            Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void updatePatientNotFoundNewPatientSpecifiesIdInJson() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient(
            "{\"labeled_eids\":[{\"label\":\"a_label\",\"value\":\"abc\"}]}", ID_LABEL, ID_VALUE, UPDATE, true);
        verify(this.logger).debug("No patient record with label [{}] and corresponding external ID [{}] exists yet",
            ID_LABEL, ID_VALUE);
        verify(this.logger).debug("Creating patient record with label [{}] and corresponding external ID [{}]",
            ID_LABEL, ID_VALUE);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.UPDATE));
        assertEquals("[{\"label\":\"a_label\",\"value\":\"abc\"}]", json.getValue().optString(KEY_LABELED_EIDS));
        assertEquals(1, json.getValue().length());
    }

    @Test
    public void updatePatientNotFoundNewPatientNoIdInJson() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(new ArrayList<>());

        Response response = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        verify(this.logger).debug("Creating patient record with label [{}] and corresponding external ID [{}]",
            ID_LABEL, ID_VALUE);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.UPDATE));
        assertTrue(new JSONArray("[{\"label\":\"a_label\",\"value\":\"eid\"}]")
            .similar(json.getValue().optJSONArray(KEY_LABELED_EIDS)));
        assertEquals(1, json.getValue().length());
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientNoAccessReturnsForbiddenCode() throws ComponentLookupException
    {
        when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(false);

        Response response = this.component.updatePatient("json", ID_LABEL, ID_VALUE, UPDATE, true);
        verify(this.logger).debug("Edit access denied to user [{}] on patient record [{}]", null, this.patient.getId());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientWrongJSONId() throws ComponentLookupException
    {
        this.component.updatePatient("{\"id\":\"notid\"}", ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test(expected = WebApplicationException.class)
    public void updatePatientFromJSONException() throws ComponentLookupException, QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        doThrow(Exception.class).when(this.patient).updateFromJSON(Matchers.any(JSONObject.class), Matchers.any(
            PatientWritePolicy.class));

        this.component.updatePatient("{\"id\":\"id\"}", ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void updatePatientReturnsNoContentResponse() throws ComponentLookupException
    {
        String json = "{\"id\":\"P0000001\"}";
        Response response = this.component.updatePatient(json, ID_LABEL, ID_VALUE, UPDATE, true);
        verify(this.logger).debug("Updating patient record with label [{}] and corresponding external ID [{}] via REST"
                                  + " with JSON: {}", ID_LABEL, ID_VALUE, json);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void updateNonExistingPatientWithCreatePatientConfigurationFalseThrowsWebApplicationException()
        throws QueryException
    {
        try {
            this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, false);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void updateExistingPatientWithCreatePatientConfigurationFalse() throws QueryException
    {
        List<String> results = new ArrayList<>();
        results.add("P1");
        when(this.patientsQuery.<String>execute()).thenReturn(results);
        when(this.repository.get(results.get(0))).thenReturn(this.patient);

        final Response response = this.component.updatePatient(
            "{\"labeled_eids\":[{\"label\":\"a_label\",\"value\":\"abc\"}]}", ID_LABEL, ID_VALUE, UPDATE, false);
        verify(this.patient, times(1)).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.UPDATE));
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.UPDATE));
        assertEquals("[{\"label\":\"a_label\",\"value\":\"abc\"}]", json.getValue().optString(KEY_LABELED_EIDS));
        assertEquals(1, json.getValue().length());
    }

    @Test(expected = WebApplicationException.class)
    public void deletePatientReturnsNotFoundStatus() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(new ArrayList<>());

        try {
            this.component.deletePatient(ID_LABEL, ID_VALUE);
        } catch (final WebApplicationException ex) {
            verify(this.logger).debug("No patient record with label [{}] and corresponding external ID [{}] exists yet",
                ID_LABEL, ID_VALUE);
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void deletePatientNoAccessReturnsForbiddenCode() throws QueryException
    {
        List<String> results = new ArrayList<>();
        results.add("P1");
        when(this.patientsQuery.<String>execute()).thenReturn(results);
        when(this.repository.get(results.get(0))).thenReturn(this.patient);
        when(this.access.hasAccess(this.currentUser, Right.DELETE, this.patientReference)).thenReturn(false);

        try {
            this.component.deletePatient(ID_LABEL, ID_VALUE);
        } catch (final WebApplicationException ex) {
            verify(this.logger).debug("Deleting patient record with label [{}] and corresponding external ID "
                                      + "[{}] via REST", ID_LABEL, ID_VALUE);
            verify(this.logger).debug("Delete access denied to user [{}] on patient record [{}]", this.currentUser,
                this.patient.getId());
            Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void deletePatientCatchesException() throws WebApplicationException, QueryException
    {
        List<String> results = new ArrayList<>();
        results.add("P1");
        when(this.patientsQuery.<String>execute()).thenReturn(results);
        when(this.repository.get(results.get(0))).thenReturn(this.patient);

        doThrow(Exception.class).when(this.repository).delete(this.patient);
        when(this.access.hasAccess(null, Right.DELETE, null)).thenReturn(true);

        WebApplicationException ex = null;
        try {
            this.component.deletePatient(ID_LABEL, ID_VALUE);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        assertNotNull("deletePatient did not throw a WebApplicationException as expected "
            + "when catching an Exception", ex);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
        verify(this.logger).warn(eq("Failed to delete patient record with label [{}] and corresponding external"
                                    + " id [{}]: {}"), eq(ID_LABEL), eq(ID_VALUE), anyString(), any(Exception.class));
    }

    @Test
    public void deletePatientReturnsNoContentResponse() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        Response response = this.component.deletePatient(ID_LABEL, ID_VALUE);

        verify(this.repository).delete(this.patient);
        verify(this.logger).debug("Deleting patient record with label [{}] and corresponding external ID [{}] via REST",
            ID_LABEL, ID_VALUE);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void checkForMultipleRecordsPerformsCorrectly() throws QueryException
    {
        List<String> results = new ArrayList<>();
        results.add("P1");
        results.add("P2");
        when(this.patientsQuery.<String>execute()).thenReturn(results);

        Response responseGet = this.component.getPatient(ID_LABEL, ID_VALUE);
        Response responseUpdate = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        Response responseDelete = this.component.deletePatient(ID_LABEL, ID_VALUE);

        verify(this.logger, times(3)).debug(
            "Multiple patient records ({}) with label [{}] and corresponding external ID [{}]: {}",
            2, ID_LABEL, ID_VALUE, results);
        verify(this.factory, times(3)).createAlternatives(anyListOf(String.class), eq(this.uriInfo));
        assertEquals(300, responseGet.getStatus());
        assertEquals(300, responseUpdate.getStatus());
        assertEquals(300, responseDelete.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void checkForMultipleRecordsLogsWhenThrowsQueryException() throws QueryException
    {
        doThrow(QueryException.class).when(this.qm)
            .createQuery("select doc.name from Document doc, doc.object(PhenoTips.LabeledIdentifierClass) obj "
                         + "where obj.label = :label and obj.value = :value", Query.XWQL);

        try {
            this.component.getPatient(ID_LABEL, ID_VALUE);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
        }
        try {
            this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
        }
        try {
            this.component.deletePatient(ID_LABEL, ID_VALUE);
        } catch (final WebApplicationException ex) {
            verify(this.logger, times(3))
                .warn(eq("Failed to query patient with label [{}] and corresponding external ID [{}]: {}"),
                    eq(ID_LABEL), eq(ID_VALUE), anyString(), any(Exception.class));
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    // ----------------------------Patch Patient Tests-----------------------------

    @Test(expected = WebApplicationException.class)
    public void patchPatientWithNullJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient(null, ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientWithEmptyJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient(StringUtils.EMPTY, ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientWithBlankJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient(StringUtils.SPACE, ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void patchPatientNoPatientWithSpecifiedEidExistsResultsPatientBeingCreated() throws QueryException
    {
        when(this.repository.get(PATIENT_ID)).thenReturn(null);
        when(this.patientsQuery.execute()).thenReturn(Collections.emptyList());
        final Response response = this.component.patchPatient(EMPTY_JSON, ID_LABEL, PATIENT_ID, true);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(this.repository, times(1)).create();
    }

    @Test
    public void patchPatientManyPatientsWithSpecifiedEidExistResultsInCorrectResponse() throws QueryException
    {
        when(this.repository.get(PATIENT_ID)).thenReturn(null);
        when(this.patientsQuery.execute()).thenReturn(Arrays.asList("one", "two"));
        final Response response = this.component.patchPatient(EMPTY_JSON, ID_LABEL, PATIENT_ID, true);
        Assert.assertEquals(300, response.getStatus());
        verify(this.repository, never()).create();
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientUserHasNoEditAccessThrowsWebApplicationException() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        try {
            when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(false);
            this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientInvalidJsonThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient("[]", ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test(expected = WebApplicationException.class)
    public void patchPatientIdFromJsonAndRetrievedPatientIdConflictThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient("{\"id\":\"wrong\"}", ID_LABEL, ID_VALUE, true);
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
                eq(PatientWritePolicy.UPDATE));
            this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void patchNewPatientCreatedUpdatesPatientSuccessfully()
    {
        final Response response = this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        verify(this.patient, times(1)).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.UPDATE));
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void patchPatientExistUpdatesPatientSuccessfully() throws QueryException
    {
        List<String> results = new ArrayList<>();
        results.add("P1");
        when(this.patientsQuery.<String>execute()).thenReturn(results);
        when(this.repository.get(results.get(0))).thenReturn(this.patient);

        final Response response = this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        verify(this.patient, times(1)).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.MERGE));
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void patchNonExistingPatientWithCreatePatientConfigurationFalseThrowsWebApplicationException()
    {
        try {
            this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, false);
        } catch (final WebApplicationException ex) {
            Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
            throw ex;
        }
    }

    @Test
    public void patchExistingPatientWithCreatePatientConfigurationFalse() throws QueryException
    {
        List<String> results = new ArrayList<>();
        results.add("P1");
        when(this.patientsQuery.<String>execute()).thenReturn(results);
        when(this.repository.get(results.get(0))).thenReturn(this.patient);

        final Response response = this.component.patchPatient(
            "{\"labeled_eids\":[{\"label\":\"a_label\",\"value\":\"abc\"}]}", ID_LABEL, ID_VALUE, false);
        verify(this.patient, times(1)).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.MERGE));
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.MERGE));
        assertEquals("[{\"label\":\"a_label\",\"value\":\"abc\"}]", json.getValue().optString(KEY_LABELED_EIDS));
        assertEquals(1, json.getValue().length());
    }
}
