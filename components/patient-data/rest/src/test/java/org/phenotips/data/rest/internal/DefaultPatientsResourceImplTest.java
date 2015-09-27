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
import org.phenotips.data.rest.PatientsResource;
import org.phenotips.data.rest.model.PatientSummary;
import org.phenotips.data.rest.model.Patients;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
import java.util.List;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

import net.sf.json.JSONObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPatientsResourceImplTest
{
    @Rule
    public MockitoComponentMockingRule<PatientsResource> mocker =
        new MockitoComponentMockingRule<PatientsResource>(DefaultPatientsResourceImpl.class);

    @Mock
    private User currentUser;

    @Mock
    private Patient patient;

    @Mock
    private Logger logger;

    @Mock
    private UriInfo uriInfo;

    private DomainObjectFactory factory;

    private PatientRepository repository;

    private QueryManager queries;

    private AuthorizationManager access;

    private UserManager users;

    private DocumentReference userProfileDocument;

    private URI uri;

    private DefaultPatientsResourceImpl patientsResource;

    private XWikiContext context;

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager compManager = this.mocker.getInstance(ComponentManager.class, "context");
        Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.context = provider.get();
        when(compManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(this.context).when(executionContext).getProperty("xwikicontext");

        this.repository = this.mocker.getInstance(PatientRepository.class);
        this.users = this.mocker.getInstance(UserManager.class);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.patientsResource = (DefaultPatientsResourceImpl) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.queries = this.mocker.getInstance(QueryManager.class);
        this.uri = new URI("http://uri");
        this.userProfileDocument = new DocumentReference("wiki", "user", "00000001");
        this.factory = this.mocker.getInstance(DomainObjectFactory.class);

        doReturn(this.uri).when(this.uriInfo).getBaseUri();
        doReturn(this.uri).when(this.uriInfo).getRequestUri();
        ReflectionUtils.setFieldValue(this.patientsResource, "uriInfo", this.uriInfo);

        doReturn("P00000001").when(this.patient).getId();
        doReturn(this.currentUser).when(this.users).getCurrentUser();
        doReturn(this.userProfileDocument).when(this.currentUser).getProfileDocument();
    }

    @Test
    public void addPatientUserDoesNotHaveAccess()
    {
        WebApplicationException exception = null;
        doReturn(false).when(this.access).hasAccess(eq(Right.EDIT), any(DocumentReference.class),
            any(EntityReference.class));
        try {
            Response response = this.patientsResource.addPatient("");
        } catch (WebApplicationException ex) {
            exception = ex;
        }
        Assert.assertNotNull(exception);
        Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    public void addEmptyPatient()
    {
        doReturn(true).when(this.access).hasAccess(eq(Right.EDIT), any(DocumentReference.class),
            any(EntityReference.class));
        doReturn(this.patient).when(this.repository).createNewPatient();
        Response response = this.patientsResource.addPatient(null);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(this.logger).debug("Importing new patient from JSON via REST: {}", (String) null);
    }

    @Test
    public void creatingPatientFails()
    {
        JSONObject json = new JSONObject();
        Exception exception = new NullPointerException();
        doReturn(true).when(this.access).hasAccess(eq(Right.EDIT), any(DocumentReference.class),
            any(EntityReference.class));
        doThrow(exception).when(this.repository).createNewPatient();
        Response response = this.patientsResource.addPatient(json.toString());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        verify(this.logger).error("Could not process patient creation request: {}", exception.getMessage(), exception);
    }

    @Test
    public void addPatientAsJSON()
    {
        doReturn(true).when(this.access).hasAccess(eq(Right.EDIT), any(DocumentReference.class),
            any(EntityReference.class));
        doReturn(this.patient).when(this.repository).createNewPatient();
        JSONObject jsonPatient = new JSONObject();
        Response response = this.patientsResource.addPatient(jsonPatient.toString());
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(this.logger).debug("Importing new patient from JSON via REST: {}", jsonPatient.toString());
    }

    @Test
    public void listPatientsNullOrderField() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doReturn(new ArrayList<Object[]>()).when(query).execute();
        Patients result = this.patientsResource.listPatients(0, 30, null, "asc");
        verify(this.queries).createQuery(
            "select doc.fullName, p.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                + " from Document doc, doc.object(PhenoTips.PatientClass) p where doc.name <> :t order by "
                + "doc.name" + " asc",
            "xwql");
    }

    @Test
    public void listPatientsNullOrder() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doReturn(new ArrayList<Object[]>()).when(query).execute();
        Patients result = this.patientsResource.listPatients(0, 30, "id", null);
        verify(this.queries).createQuery(
            "select doc.fullName, p.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                + " from Document doc, doc.object(PhenoTips.PatientClass) p where doc.name <> :t order by "
                + "doc.name" + " asc",
            "xwql");
    }

    @Test
    public void listPatientsNonDefaultBehaviour() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doReturn(new ArrayList<Object[]>()).when(query).execute();
        Patients result = this.patientsResource.listPatients(0, 30, "eid", "desc");
        verify(this.queries).createQuery(
            "select doc.fullName, p.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                + " from Document doc, doc.object(PhenoTips.PatientClass) p where doc.name <> :t order by "
                + "p.external_id" + " desc",
            "xwql");
    }

    @Test
    public void listPatientsNoUserAccess() throws QueryException
    {
        Object[] patientSummaryData = new Object[7];
        List<Object[]> patientList = new ArrayList<Object[]>();
        patientList.add(patientSummaryData);
        Query query = mock(DefaultQuery.class);
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doReturn(patientList).when(query).execute();
        doReturn(false).when(this.access).hasAccess(eq(Right.VIEW), any(DocumentReference.class),
            any(EntityReference.class));
        doReturn(null).when(this.factory).createPatientSummary(patientSummaryData, this.uriInfo);
        Patients result = this.patientsResource.listPatients(0, 30, "id", "asc");
        verify(this.queries).createQuery(
            "select doc.fullName, p.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                + " from Document doc, doc.object(PhenoTips.PatientClass) p where doc.name <> :t order by "
                + "doc.name" + " asc",
            "xwql");
        Assert.assertTrue(result.getPatientSummaries().isEmpty());
    }

    @Test
    public void listPatientsUserHasAccess() throws QueryException
    {
        Object[] patientSummaryData = new Object[7];
        List<Object[]> patientList = new ArrayList<Object[]>();
        patientList.add(patientSummaryData);
        Query query = mock(DefaultQuery.class);
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doReturn(patientList).when(query).execute();
        doReturn(true).when(this.access).hasAccess(eq(Right.VIEW), any(DocumentReference.class),
            any(EntityReference.class));
        doReturn(new PatientSummary()).when(this.factory).createPatientSummary(any(Object[].class), eq(this.uriInfo));
        Patients result = this.patientsResource.listPatients(0, 30, "id", "asc");
        verify(this.queries).createQuery(
            "select doc.fullName, p.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                + " from Document doc, doc.object(PhenoTips.PatientClass) p where doc.name <> :t order by "
                + "doc.name" + " asc",
            "xwql");
        Assert.assertFalse(result.getPatientSummaries().isEmpty());
    }

    @Test
    public void listPatientsSpecificNumberOfRecords() throws QueryException
    {
        List<Object[]> patientList = new ArrayList<Object[]>();
        for (int i = 0; i < 30; i++) {
            Object[] patientSummaryData = new Object[7];
            patientList.add(patientSummaryData);
        }
        Query query = mock(DefaultQuery.class);
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doReturn(patientList).when(query).execute();
        doReturn(true).when(this.access).hasAccess(eq(Right.VIEW), any(DocumentReference.class),
            any(EntityReference.class));
        doReturn(new PatientSummary()).when(this.factory).createPatientSummary(any(Object[].class), eq(this.uriInfo));

        Patients allPatients = this.patientsResource.listPatients(0, 30, "id", "asc");
        Assert.assertEquals(30, allPatients.getPatientSummaries().size());

        Patients selectedNumberOfPatients = this.patientsResource.listPatients(15, 15, "id", "asc");
        Assert.assertEquals(15, selectedNumberOfPatients.getPatientSummaries().size());

        Patients onePatient = this.patientsResource.listPatients(15, 1, "id", "asc");
        Assert.assertEquals(1, onePatient.getPatientSummaries().size());

        Patients incorrectLookup = this.patientsResource.listPatients(31, 5, "id", "asc");
        Assert.assertEquals(0, incorrectLookup.getPatientSummaries().size());
    }

    @Test
    public void listPatientsGetMoreRecordsThanAdded() throws QueryException
    {
        List<Object[]> patientList = new ArrayList<Object[]>();
        for (int i = 0; i < 15; i++) {
            Object[] patientSummaryData = new Object[7];
            patientList.add(patientSummaryData);
        }
        Query query = mock(DefaultQuery.class);
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doReturn(patientList).when(query).execute();
        doReturn(true).when(this.access).hasAccess(eq(Right.VIEW), any(DocumentReference.class),
            any(EntityReference.class));
        doReturn(new PatientSummary()).when(this.factory).createPatientSummary(any(Object[].class), eq(this.uriInfo));
        Patients result = this.patientsResource.listPatients(0, 30, "id", "asc");
        Assert.assertEquals(15, result.getPatientSummaries().size());
    }

    @Test
    public void listPatientFailureHandling() throws QueryException
    {
        Query query = mock(DefaultQuery.class);
        WebApplicationException exception = null;
        QueryException queryException = new QueryException("query.execute() failed", query, new Exception());
        doReturn(query).when(this.queries).createQuery(anyString(), anyString());
        doReturn(query).when(query).bindValue(anyString(), anyString());
        doThrow(queryException).when(query).execute();
        try {
            Patients result = this.patientsResource.listPatients(0, 30, "id", "asc");
        } catch (WebApplicationException ex) {
            exception = ex;
        }
        Assert.assertNotNull(exception);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        verify(this.logger).error("Failed to list patients: {}", queryException.getMessage(), queryException);
    }
}
