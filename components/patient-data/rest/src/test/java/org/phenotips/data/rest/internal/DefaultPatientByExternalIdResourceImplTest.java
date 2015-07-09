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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import net.sf.json.JSON;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import net.sf.json.JSONObject;
import org.apache.commons.collections.map.MultiValueMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientByExternalIdResource;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.Relations;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.text.StringUtils;
import org.xwiki.users.UserManager;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class DefaultPatientByExternalIdResourceImplTest {
    @Rule
    public final MockitoComponentMockingRule<PatientByExternalIdResource> mocker =
            new MockitoComponentMockingRule<PatientByExternalIdResource>(DefaultPatientByExternalIdResourceImpl.class);

    @Mock
    private Patient patient;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

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
        when(this.users.getCurrentUser()).thenReturn(null);
        when(this.patient.getDocument()).thenReturn(null);
    }

    @Test
    public void getPatientWithNoAccessReturnsForbiddenCode() throws ComponentLookupException, XWikiRestException
    {
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(this.patient);
        when(this.access.hasAccess(Right.DELETE, null, null)).thenReturn(false);

        Response response = this.mocker.getComponentUnderTest().getPatient(this.eid);
        verify(this.logger).debug("Retrieving patient record with external ID [{}] via REST", this.eid);
        verify(this.logger).debug("View access denied to user [{}] on patient record [{}]", null, this.patient.getId());

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientPerformsCorrectly() throws ComponentLookupException, XWikiRestException
    {
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(this.patient);
        when(this.access.hasAccess(Right.VIEW, null, null)).thenReturn(true);

        JSONObject jsonObject = new JSONObject();
        when(this.patient.toJSON()).thenReturn(jsonObject);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(this.uriBuilder);
        when(this.uriBuilder.path(PatientResource.class)).
                thenReturn(uriBuilder);
        when(this.uriBuilder.build(this.patient.getId())).thenReturn(this.uri);

        Response response = this.mocker.getComponentUnderTest().getPatient(this.eid);
        verify(this.logger).debug("Retrieving patient record with external ID [{}] via REST", this.eid);

        JSONObject links = new JSONObject().accumulate("rel", Relations.SELF).accumulate("href", "uri");
        JSONObject json = new JSONObject().accumulate("links", links);

        assertEquals(json, response.getEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException, XWikiRestException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<Object>());

        Response response = this.mocker.getComponentUnderTest().getPatient(this.eid);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", this.eid);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void updatePatientNotFoundChecksForMultipleRecords() throws ComponentLookupException, QueryException, XWikiRestException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<Object>());

        Response response = this.mocker.getComponentUnderTest().updatePatient("json", this.eid);
        verify(this.logger).debug("No patient record with external ID [{}] exists yet", this.eid);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test(expected=WebApplicationException.class)
    public void updatePatientNoAccessReturnsForbiddenCode() throws ComponentLookupException, XWikiRestException
    {
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(this.patient);
        when(this.access.hasAccess(Right.EDIT, null, null)).thenReturn(false);

        Response response = this.mocker.getComponentUnderTest().updatePatient("json", this.eid);
        verify(this.logger).debug("Updating patient record with external ID [{}] via REST", this.eid);
        verify(this.logger).debug("Edit access denied to user [{}] on patient record [{}]", null, this.patient.getId());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }

    @Test
    public void deletePatientCantFindPatient() throws QueryException, XWikiRestException
    {
        Query query = mock(DefaultQuery.class);
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(null);
        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(new ArrayList<Object>());

        Response response = this.component.deletePatient(this.eid);

        verify(this.logger).debug("No patient record with external ID [{}] exists yet", this.eid);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientNoAccessReturnsForbiddenCode() throws XWikiRestException
    {
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(this.patient);
        when(this.access.hasAccess(Right.DELETE, null, null)).thenReturn(false);

        Response response = this.component.deletePatient(this.eid);

        verify(this.logger).debug("Delete access denied to user [{}] on patient record [{}]", null, this.patient.getId());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientCatchesXWikiException() throws XWikiException, XWikiRestException
    {
        XWiki wiki = mock(XWiki.class);
        doReturn(wiki).when(this.context).getWiki();
        doThrow(XWikiException.class).when(wiki).deleteDocument(any(XWikiDocument.class), eq(this.context));
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(this.patient);
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
        verify(this.logger).warn(eq("Failed to delete patient record with external id [{}]: {}"), eq(this.eid), anyString());
    }

    @Test
    public void deletePatientReturnsNoContentResponse() throws XWikiRestException {
        XWiki wiki = mock(XWiki.class);
        doReturn(wiki).when(this.context).getWiki();
        doReturn(true).when(this.access).hasAccess(Right.DELETE, null, null);
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(this.patient);

        Response response = this.component.deletePatient(this.eid);

        verify(this.logger).debug("Deleted patient record with external id [{}]", this.eid);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test(expected=WebApplicationException.class)
    public void updatePatientWrongJSONId() throws ComponentLookupException, XWikiRestException
    {
        when(this.repository.getPatientByExternalId("eid")).thenReturn(this.patient);
        when(this.access.hasAccess(Right.EDIT, null, null)).thenReturn(true);

        this.mocker.getComponentUnderTest().updatePatient("{\"id\":\"notid\"}", "eid");
    }

    @Test(expected=WebApplicationException.class)
    public void updatePatientFromJSONException() throws ComponentLookupException, XWikiRestException
    {
        when(this.repository.getPatientByExternalId("eid")).thenReturn(this.patient);
        when(this.access.hasAccess(Right.EDIT, null, null)).thenReturn(true);
        doThrow(Exception.class).when(this.patient).updateFromJSON(Matchers.any(JSONObject.class));

        this.mocker.getComponentUnderTest().updatePatient("{\"id\":\"id\"}", "eid");
        verify(this.logger).debug("Failed to update patient [{}] from JSON: {}. Source JSON was: {}",
                this.id, "{\"id\":\"id\"}");
    }

    @Test
    public void updatePatientReturnsNoContentResponse() throws ComponentLookupException, XWikiRestException {
        when(this.repository.getPatientByExternalId("eid")).thenReturn(this.patient);
        when(this.access.hasAccess(Right.EDIT, null, null)).thenReturn(true);

        Response response = this.mocker.getComponentUnderTest().updatePatient("{\"id\":\"id\"}", "eid");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientAlwaysSendsLoggerMessageOnRequest() throws XWikiRestException
    {
        XWiki wiki = mock(XWiki.class);
        doReturn(wiki).when(this.context).getWiki();
        doReturn(true).when(this.access).hasAccess(Right.DELETE, null, null);
        when(this.repository.getPatientByExternalId(this.eid)).thenReturn(this.patient);

        this.component.deletePatient(this.eid);

        verify(this.logger).debug("Deleting patient record with external ID [{}] via REST", this.eid);
    }
}
