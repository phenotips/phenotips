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
import org.phenotips.data.rest.model.Alternatives;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPatientByLabeledExternalIdentifierResourceImplTest
{
    private static final String ID_LABEL = "a_label";

    private static final String ID_VALUE = "eid";

    private static final String PATIENT_ID = "P0000001";

    private static final String OTHER_PATIENT_ID = "P0000002";

    private static final List<String> MULTIPLE_PATIENTS = Arrays.asList(PATIENT_ID, OTHER_PATIENT_ID);

    private static final String EMPTY_JSON = "{}";

    private static final String KEY_LABELED_EIDS = "labeled_eids";

    private static final String UPDATE = "update";

    @Rule
    public final MockitoComponentMockingRule<PatientByLabeledExternalIdentifierResource> mocker =
        new MockitoComponentMockingRule<>(DefaultPatientByLabeledExternalIdentifierResourceImpl.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

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

    private PatientRepository repository;

    private DomainObjectFactory factory;

    @Mock
    private Alternatives alternatives;

    private QueryManager qm;

    @Mock
    private Query patientsQuery;

    @Mock
    private Query allowOtherQuery;

    @Mock
    private Query labelIsConfiguredQuery;

    private AuthorizationService access;

    private URI uri;

    private Autolinker autolinker;

    private PatientByLabeledExternalIdentifierResource component;

    private DocumentReference patientReference = new DocumentReference("wiki", "data", PATIENT_ID);

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException, QueryException
    {
        MockitoAnnotations.initMocks(this);
        this.uri = new URI("uri");

        // This is needed by the XWikiResource parent class
        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(mock(XWikiContext.class));

        this.repository = this.mocker.getInstance(PatientRepository.class);
        this.factory = this.mocker.getInstance(DomainObjectFactory.class);
        this.qm = this.mocker.getInstance(QueryManager.class);
        this.access = this.mocker.getInstance(AuthorizationService.class);
        this.resolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
        this.component = this.mocker.getComponentUnderTest();

        when(this.qm.createQuery(anyString(), anyString())).thenReturn(this.patientsQuery);
        when(this.patientsQuery.execute()).thenReturn(Collections.emptyList());
        when(this.qm
            .createQuery("select obj.label from Document doc, doc.object(PhenoTips.LabeledIdentifierSettings) obj "
                + "where doc.fullName = 'XWiki.XWikiPreferences' and obj.label = :label",
                Query.XWQL)).thenReturn(this.labelIsConfiguredQuery);
        when(this.labelIsConfiguredQuery.execute()).thenReturn(Collections.singletonList(ID_LABEL));
        when(this.qm.createQuery("select obj.allowOtherEids from Document doc, doc.object("
            + "PhenoTips.LabeledIdentifierGlobalSettings) obj where doc.fullName = 'XWiki.XWikiPreferences'",
            Query.XWQL)).thenReturn(this.allowOtherQuery);
        when(this.allowOtherQuery.execute()).thenReturn(Collections.singletonList(Integer.valueOf(1)));

        ReflectionUtils.setFieldValue(this.component, "uriInfo", this.uriInfo);

        when(this.uriInfo.getBaseUri()).thenReturn(this.uri);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(this.uriBuilder);
        when(this.uriBuilder.path(PatientResource.class)).thenReturn(this.uriBuilder);
        when(this.uriBuilder.build(this.patient.getId())).thenReturn(this.uri);

        when(this.patient.getId()).thenReturn(PATIENT_ID);
        when(this.patient.getDocumentReference()).thenReturn(this.patientReference);
        when(this.repository.get(PATIENT_ID)).thenReturn(this.patient);
        when(this.repository.create()).thenReturn(this.patient);

        final UserManager users = this.mocker.getInstance(UserManager.class);
        when(users.getCurrentUser()).thenReturn(this.currentUser);
        when(this.access.hasAccess(this.currentUser, Right.VIEW, this.patientReference)).thenReturn(true);
        when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(true);
        when(this.access.hasAccess(this.currentUser, Right.DELETE, this.patientReference)).thenReturn(true);
        when(this.resolver.resolve(any(EntityReference.class), any(EntityType.class), any()))
            .thenReturn(mock(EntityReference.class));
        when(this.access.hasAccess(eq(this.currentUser), eq(Right.EDIT), any(EntityReference.class)))
            .thenReturn(true);

        this.autolinker = this.mocker.getInstance(Autolinker.class);
        when(this.autolinker.forResource(any(Class.class), any(UriInfo.class))).thenReturn(this.autolinker);
        when(this.autolinker.withGrantedRight(any(Right.class))).thenReturn(this.autolinker);
        when(this.autolinker.withActionableResources(any(Class.class))).thenReturn(this.autolinker);
        when(this.autolinker.withExtraParameters(any(String.class), any(String.class))).thenReturn(this.autolinker);
        when(this.autolinker.build()).thenReturn(Collections
            .singletonList(new org.phenotips.rest.model.Link().withAllowedMethods(Collections.singletonList("GET"))
                .withHref(this.uri.toString()).withRel("self")));

        when(this.factory.createAlternatives(MULTIPLE_PATIENTS, this.uriInfo)).thenReturn(this.alternatives);
    }

    // ---------------------------- GET Patient Tests ----------------------------

    @Test
    public void getPatientPerformsCorrectly() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        JSONObject jsonObject = new JSONObject();
        when(this.patient.toJSON()).thenReturn(jsonObject);

        Response response = this.component.getPatient(ID_LABEL, ID_VALUE);

        JSONObject links = new JSONObject().accumulate("rel", "self").accumulate("href", "uri")
            .put("allowedMethods", new JSONArray(Collections.singletonList("GET")));
        JSONObject json = new JSONObject().put("links", Collections.singletonList(links));

        verify(this.autolinker).withGrantedRight(Right.EDIT);
        assertTrue(json.similar(response.getEntity()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientReportsViewAccessWhenEditNotAllowed() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(false);
        JSONObject jsonObject = new JSONObject();
        when(this.patient.toJSON()).thenReturn(jsonObject);

        Response response = this.component.getPatient(ID_LABEL, ID_VALUE);

        JSONObject links = new JSONObject().accumulate("rel", "self").accumulate("href", "uri")
            .put("allowedMethods", new JSONArray(Collections.singletonList("GET")));
        JSONObject json = new JSONObject().put("links", Collections.singletonList(links));

        verify(this.autolinker).withGrantedRight(Right.VIEW);
        assertTrue(json.similar(response.getEntity()));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientWithNoAccessReturnsForbidden() throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.FORBIDDEN));
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        when(this.access.hasAccess(this.currentUser, Right.VIEW, this.patientReference)).thenReturn(false);
        this.component.getPatient(ID_LABEL, ID_VALUE);
    }

    @Test
    public void getPatientReturnsNotFoundStatus()
    {
        this.exception.expect(HttpStatus.of(Response.Status.NOT_FOUND));
        this.component.getPatient(ID_LABEL, ID_VALUE);
    }

    @Test
    public void getPatientWithMultipleOptionsReturnsAlternatives() throws QueryException
    {
        when(this.patientsQuery.<String>execute()).thenReturn(MULTIPLE_PATIENTS);
        Response result = this.component.getPatient(ID_LABEL, ID_VALUE);
        Assert.assertEquals(300, result.getStatus());
        Assert.assertSame(this.alternatives, result.getEntity());
    }

    @Test
    public void getPatientCatchesQueryExceptions() throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.NOT_FOUND));
        doThrow(QueryException.class).when(this.patientsQuery).execute();
        this.component.getPatient(ID_LABEL, ID_VALUE);
    }

    // ---------------------------- PUT Patient Tests ----------------------------

    @Test
    public void updatePatientCreatesNewPatient()
    {
        Response result = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());
        verify(this.repository).create();
        verify(this.patient).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.UPDATE));
    }

    @Test
    public void updatePatientUpdatesExistingPatient() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        Response result = this.component.updatePatient("{\"id\":\"P0000001\"}", ID_LABEL, ID_VALUE, UPDATE, true);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), result.getStatus());
        verify(this.repository, never()).create();
        verify(this.patient).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.UPDATE));
    }

    @Test
    public void updatePatientWithMultipleOptionsReturnsAlternatives() throws QueryException
    {
        when(this.patientsQuery.<String>execute()).thenReturn(MULTIPLE_PATIENTS);
        Response result = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        Assert.assertEquals(300, result.getStatus());
        Assert.assertSame(this.alternatives, result.getEntity());
    }

    @Test
    public void updatePatientReturns404WhenPatientDoesNotExistAndCreateIsFalse()
    {
        this.exception.expect(HttpStatus.of(Response.Status.NOT_FOUND));
        this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, false);
    }

    @Test
    public void updatePatientReturns400IfJsonIsInvalid()
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        try {
            this.component.updatePatient("[]", ID_LABEL, ID_VALUE, UPDATE, true);
        } catch (final WebApplicationException ex) {
            verify(this.repository, never()).create();
            throw ex;
        }
    }

    @Test
    public void updatePatientReturns400IfJsonIsNull()
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        this.component.updatePatient(null, ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void updatePatientReturns400IfJsonIsBlank()
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        this.component.updatePatient("", ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void updatePatientNotFoundNewPatientNotCreatedIfUserDoesNotHaveEditRights()
    {
        this.exception.expect(HttpStatus.of(Response.Status.FORBIDDEN));
        when(this.access.hasAccess(eq(this.currentUser), eq(Right.EDIT), any(EntityReference.class)))
            .thenReturn(false);

        try {
            this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        } catch (final WebApplicationException ex) {
            verify(this.repository, never()).create();
            throw ex;
        }
    }

    @Test
    public void createPatientPutsSpecifiedLabeledIdInJson()
    {
        Response response = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.UPDATE));
        assertEquals("[{\"label\":\"a_label\",\"value\":\"eid\"}]", json.getValue().optString(KEY_LABELED_EIDS));
    }

    @Test
    public void createPatientKeepsExistingLabeledIdsInJson()
    {
        Response response = this.component.updatePatient(
            "{\"labeled_eids\":[{\"label\":\"another_label\",\"value\":\"abc\"}]}", ID_LABEL, ID_VALUE, UPDATE, true);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.UPDATE));
        assertTrue(new JSONArray(
            "[{\"label\":\"another_label\",\"value\":\"abc\"}, {\"label\":\"a_label\",\"value\":\"eid\"}]")
                .similar(json.getValue().getJSONArray(KEY_LABELED_EIDS)));
    }

    @Test
    public void updateExistingPatientDoesNotModifyLabeledIds() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        Response response = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, false);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), eq(PatientWritePolicy.UPDATE));
        assertFalse(json.getValue().has(KEY_LABELED_EIDS));
    }

    @Test
    public void createPatientReturns409WithInternalIdConflict()
    {
        this.exception.expect(HttpStatus.of(Response.Status.CONFLICT));
        this.component.updatePatient("{\"id\":\"notid\"}", ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void updatePatientReturns409WithInternalIdConflict() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        this.exception.expect(HttpStatus.of(Response.Status.CONFLICT));
        this.component.updatePatient("{\"id\":\"notid\"}", ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void updatePatientWithNoAccessReturnsForbidden() throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.FORBIDDEN));
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(false);
        this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void updatePatientWithNullPolicyReturnsBadRequest()
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, null, true);
    }

    @Test
    public void updatePatientWithUpdateFromJSONExceptionReturns500()
    {
        this.exception.expect(HttpStatus.of(Response.Status.INTERNAL_SERVER_ERROR));
        doThrow(Exception.class).when(this.patient).updateFromJSON(Matchers.any(JSONObject.class), Matchers.any(
            PatientWritePolicy.class));
        this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void createPatientWithUnkownLabelFailsWhenOtherLabelsAreNotAllowed()
        throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        when(this.labelIsConfiguredQuery.execute()).thenReturn(Collections.emptyList());
        when(this.allowOtherQuery.execute()).thenReturn(Collections.singletonList(Integer.valueOf(0)));
        this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
    }

    @Test
    public void createPatientWithUnkownLabelSucceedsWhenOtherLabelsAreAllowed()
        throws QueryException
    {
        when(this.labelIsConfiguredQuery.execute()).thenReturn(Collections.emptyList());
        Response result = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        Assert.assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void otherLabelsAreAllowedByDefault() throws QueryException
    {
        when(this.labelIsConfiguredQuery.execute())
            .thenThrow(new QueryException("failed", this.labelIsConfiguredQuery, null));
        when(this.allowOtherQuery.execute()).thenReturn(null);
        Response result = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        Assert.assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());

        when(this.allowOtherQuery.execute()).thenReturn(Collections.emptyList());
        result = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        Assert.assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());

        when(this.allowOtherQuery.execute()).thenThrow(new QueryException("failed", this.allowOtherQuery, null));
        result = this.component.updatePatient(EMPTY_JSON, ID_LABEL, ID_VALUE, UPDATE, true);
        Assert.assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    // ---------------------------- PATCH Patient Tests ----------------------------

    @Test
    public void patchPatientCreatesNewPatient()
    {
        Response result = this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());
        verify(this.repository).create();
        // When a new patient is created, the write policy doesn't matter, so UPDATE is used
        verify(this.patient).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.UPDATE));
    }

    @Test
    public void patchPatientUpdatesExistingPatient() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        Response result = this.component.patchPatient("{\"labeled_eids\":[{\"label\":\"a_label\",\"value\":\"eid\"}]}",
            ID_LABEL, ID_VALUE, true);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), result.getStatus());
        verify(this.repository, never()).create();
        verify(this.patient).updateFromJSON(any(JSONObject.class), eq(PatientWritePolicy.MERGE));
    }

    @Test
    public void patchPatientWithMultipleOptionsReturnsAlternatives() throws QueryException
    {
        when(this.patientsQuery.<String>execute()).thenReturn(MULTIPLE_PATIENTS);
        Response result = this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        Assert.assertEquals(300, result.getStatus());
        Assert.assertSame(this.alternatives, result.getEntity());
    }

    @Test
    public void patchPatientReturns404WhenPatientDoesNotExistAndCreateIsFalse()
    {
        this.exception.expect(HttpStatus.of(Response.Status.NOT_FOUND));
        this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, false);
    }

    @Test
    public void patchPatientReturns400IfJsonIsInvalid()
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        try {
            this.component.patchPatient("[]", ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            verify(this.repository, never()).create();
            throw ex;
        }
    }

    @Test
    public void patchPatientReturns400IfJsonIsNull()
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        this.component.patchPatient(null, ID_LABEL, ID_VALUE, true);
    }

    @Test
    public void patchPatientReturns400IfJsonIsBlank()
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        this.component.patchPatient("", ID_LABEL, ID_VALUE, true);
    }

    @Test
    public void patchPatientNotFoundNewPatientNotCreatedIfUserDoesNotHaveEditRights()
    {
        this.exception.expect(HttpStatus.of(Response.Status.FORBIDDEN));
        when(this.access.hasAccess(eq(this.currentUser), eq(Right.EDIT), any(EntityReference.class)))
            .thenReturn(false);

        try {
            this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        } catch (final WebApplicationException ex) {
            verify(this.repository, never()).create();
            throw ex;
        }
    }

    @Test
    public void patchPatientPutsSpecifiedLabeledIdInJson()
    {
        Response response = this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.UPDATE));
        assertEquals("[{\"label\":\"a_label\",\"value\":\"eid\"}]", json.getValue().optString(KEY_LABELED_EIDS));
    }

    @Test
    public void patchPatientKeepsExistingLabeledIdsInJson()
    {
        Response response = this.component.patchPatient(
            "{\"labeled_eids\":[{\"label\":\"another_label\",\"value\":\"abc\"}]}", ID_LABEL, ID_VALUE, true);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), Matchers.eq(PatientWritePolicy.UPDATE));
        assertTrue(new JSONArray(
            "[{\"label\":\"another_label\",\"value\":\"abc\"}, {\"label\":\"a_label\",\"value\":\"eid\"}]")
                .similar(json.getValue().getJSONArray(KEY_LABELED_EIDS)));
    }

    @Test
    public void patchExistingPatientDoesNotModifyLabeledIds() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        Response response = this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, false);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        ArgumentCaptor<JSONObject> json = ArgumentCaptor.forClass(JSONObject.class);
        verify(this.patient).updateFromJSON(json.capture(), eq(PatientWritePolicy.MERGE));
        assertFalse(json.getValue().has(KEY_LABELED_EIDS));
    }

    @Test
    public void patchPatientReturns409WithInternalIdConflict()
    {
        this.exception.expect(HttpStatus.of(Response.Status.CONFLICT));
        this.component.patchPatient("{\"id\":\"notid\"}", ID_LABEL, ID_VALUE, true);
    }

    @Test
    public void patchPatientWithNoAccessReturnsForbidden() throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.FORBIDDEN));
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        when(this.access.hasAccess(this.currentUser, Right.EDIT, this.patientReference)).thenReturn(false);
        this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
    }

    @Test
    public void patchPatientWithUpdateFromJSONExceptionReturns500()
    {
        this.exception.expect(HttpStatus.of(Response.Status.INTERNAL_SERVER_ERROR));
        doThrow(Exception.class).when(this.patient).updateFromJSON(Matchers.any(JSONObject.class), Matchers.any(
            PatientWritePolicy.class));
        this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
    }

    @Test
    public void patchPatientWithUnkownLabelFailsWhenOtherLabelsAreNotAllowed() throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.BAD_REQUEST));
        when(this.labelIsConfiguredQuery.execute()).thenReturn(Collections.emptyList());
        when(this.allowOtherQuery.execute()).thenReturn(Collections.singletonList(Integer.valueOf(0)));
        this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
    }

    @Test
    public void patchPatientWithUnkownLabelSucceedsWhenOtherLabelsAreAllowed() throws QueryException
    {
        when(this.labelIsConfiguredQuery.execute()).thenReturn(Collections.emptyList());
        Response result = this.component.patchPatient(EMPTY_JSON, ID_LABEL, ID_VALUE, true);
        Assert.assertEquals(result.getStatus(), Response.Status.CREATED.getStatusCode());
    }

    // ---------------------------- DELETE Patient Tests ----------------------------

    @Test
    public void deleteInvalidPatientReturns404() throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.NOT_FOUND));
        this.component.deletePatient(ID_LABEL, ID_VALUE);
    }

    @Test
    public void deletePatientWithNoAccessReturns403() throws QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.FORBIDDEN));
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        when(this.access.hasAccess(this.currentUser, Right.DELETE, this.patientReference)).thenReturn(false);
        this.component.deletePatient(ID_LABEL, ID_VALUE);
    }

    @Test
    public void deletePatientCatchesException() throws WebApplicationException, QueryException
    {
        this.exception.expect(HttpStatus.of(Response.Status.INTERNAL_SERVER_ERROR));
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        doThrow(Exception.class).when(this.repository).delete(this.patient);
        this.component.deletePatient(ID_LABEL, ID_VALUE);
    }

    @Test
    public void deletePatientReturnsNoContentResponse() throws QueryException
    {
        when(this.patientsQuery.execute()).thenReturn(Collections.singletonList(PATIENT_ID));
        Response response = this.component.deletePatient(ID_LABEL, ID_VALUE);

        verify(this.repository).delete(this.patient);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientWithMultipleOptionsReturnsAlternatives() throws ComponentLookupException, QueryException
    {
        when(this.patientsQuery.<String>execute()).thenReturn(MULTIPLE_PATIENTS);
        Response result = this.component.deletePatient(ID_LABEL, ID_VALUE);
        Assert.assertEquals(300, result.getStatus());
        Assert.assertSame(this.alternatives, result.getEntity());
    }
}
