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
import org.phenotips.data.rest.PatientsFetchResource;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPatientsFetchResourceImplTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EID1 = "EID1";

    private static final String EID2 = "EID2";

    private static final String ID_1 = "P0000001";

    private static final String ID_2 = "P0000002";

    private static final String ID_3 = "P0000003";

    private static final String ID_4 = "P0000004";

    @Rule
    public final MockitoComponentMockingRule<PatientsFetchResource> mocker =
        new MockitoComponentMockingRule<PatientsFetchResource>(DefaultPatientsFetchResourceImpl.class);

    @Mock
    private Patient patient1;

    @Mock
    private Patient patient2;

    @Mock
    private Patient patient3;

    @Mock
    private Logger logger;

    private PatientRepository repository;

    private Container container;

    private QueryManager qm;

    private DefaultPatientsFetchResourceImpl component;

    private final DocumentReference patientReference1 = new DocumentReference("wiki", "data", ID_1);

    private final DocumentReference patientReference2 = new DocumentReference("wiki", "data", ID_2);

    private final DocumentReference patientReference3 = new DocumentReference("wiki", "data", ID_1);

    private final JSONObject patient1JSON = new JSONObject().put("id", ID_1);

    private final JSONObject patient2JSON = new JSONObject().put("id", ID_2);

    private final JSONObject patient3JSON = new JSONObject().put("id", ID_3);

    private final Collection<String> uriList = ImmutableList.of("http://uri");

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");

        this.qm = this.mocker.getInstance(QueryManager.class);
        this.container = this.mocker.getInstance(Container.class);
        this.component = (DefaultPatientsFetchResourceImpl) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.repository = this.mocker.getInstance(PatientRepository.class, "secure");

        when(this.patient1.getId()).thenReturn(ID_1);
        when(this.patient1.getDocument()).thenReturn(this.patientReference1);
        when(this.patient2.getId()).thenReturn(ID_2);
        when(this.patient2.getDocument()).thenReturn(this.patientReference2);
        when(this.patient3.getId()).thenReturn(ID_3);
        when(this.patient3.getDocument()).thenReturn(this.patientReference3);

        final Autolinker autolinker = this.mocker.getInstance(Autolinker.class);
        when(autolinker.forSecondaryResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(any(String.class), any(String.class))).thenReturn(autolinker);
        doReturn(this.uriList).when(autolinker).build();
    }

    @Test
    public void getPatientsWithEmptyEidAndId() throws ComponentLookupException
    {
        final Request request = mock(Request.class);
        doReturn(request).when(this.container).getRequest();
        doReturn(Collections.emptyList()).when(request).getProperties("eid");
        doReturn(Collections.emptyList()).when(request).getProperties("id");

        final Response response = this.component.fetchPatients();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("[]", response.getEntity());
    }

    @Test
    public void getPatientsPerformsCorrectlyOnePatientRecordByEidNoneById()
        throws ComponentLookupException, QueryException, IOException
    {
        final List<Object> eidList = ImmutableList.<Object>of(EID1);

        final Request request = mock(Request.class);
        doReturn(request).when(this.container).getRequest();
        doReturn(eidList).when(request).getProperties("eid");
        doReturn(Collections.emptyList()).when(request).getProperties("id");

        final Query query = mock(DefaultQuery.class);
        doReturn(this.patient1JSON).when(this.patient1).toJSON();
        doReturn(query).when(this.qm).createQuery(Matchers.anyString(), Matchers.anyString());
        doReturn(ImmutableList.<Object>of(ID_1)).when(query).execute();

        doReturn(this.patient1).when(this.repository).get(ID_1);
        doReturn(ID_1).when(this.patient1).getId();

        final Response response = this.component.fetchPatients();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final String expectedStr = new JSONArray().put(new JSONObject().put("id", ID_1).put("links", this.uriList))
            .toString();
        @SuppressWarnings("unchecked")
        final List<String> expected = (List<String>) (OBJECT_MAPPER.readValue(expectedStr, List.class));
        @SuppressWarnings("unchecked")
        final List<String> actual = (List<String>) (OBJECT_MAPPER.readValue((String) response.getEntity(), List.class));
        assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlySeveralPatientRecordsAccessToAll() throws ComponentLookupException,
        QueryException, IOException
    {
        final List<Object> eidList = new ArrayList<>();
        eidList.add(EID1);
        eidList.add(EID2);

        final List<String> idList = new ArrayList<>();
        idList.add(ID_3);

        final Request request = mock(Request.class);
        doReturn(request).when(this.container).getRequest();
        doReturn(eidList).when(request).getProperties("eid");
        doReturn(idList).when(request).getProperties("id");

        final Query query = mock(DefaultQuery.class);
        when(this.patient1.toJSON()).thenReturn(this.patient1JSON);
        when(this.patient2.toJSON()).thenReturn(this.patient2JSON);
        when(this.patient3.toJSON()).thenReturn(this.patient3JSON);

        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(ImmutableList.<Object>of(ID_1, ID_2));
        doReturn(this.patient1).when(this.repository).get(ID_1);
        doReturn(this.patient2).when(this.repository).get(ID_2);
        doReturn(this.patient3).when(this.repository).get(ID_3);

        final Response response = this.component.fetchPatients();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final String expectedStr = new JSONArray().put(new JSONObject().put("id", ID_1).put("links", this.uriList))
            .put(new JSONObject().put("id", ID_3).put("links", this.uriList))
            .put(new JSONObject().put("id", ID_2).put("links", this.uriList))
            .toString();
        @SuppressWarnings("unchecked")
        final List<String> expected = (List<String>) (OBJECT_MAPPER.readValue(expectedStr, List.class));
        @SuppressWarnings("unchecked")
        final List<String> actual = (List<String>) (OBJECT_MAPPER.readValue((String) response.getEntity(), List.class));

        assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlySeveralPatientRecordsAccessToSome() throws ComponentLookupException,
        QueryException, IOException
    {
        //TODO: Check logger output
        final List<Object> eidList = new ArrayList<>();
        eidList.add(EID1);
        eidList.add(EID2);

        final List<String> idList = new ArrayList<>();
        idList.add(ID_3);

        final Request request = mock(Request.class);
        doReturn(request).when(this.container).getRequest();
        doReturn(eidList).when(request).getProperties("eid");
        doReturn(idList).when(request).getProperties("id");

        final Query query = mock(DefaultQuery.class);
        when(this.patient1.toJSON()).thenReturn(this.patient1JSON);
        when(this.patient2.toJSON()).thenReturn(this.patient2JSON);
        when(this.patient3.toJSON()).thenReturn(this.patient3JSON);

        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);

        when(query.execute()).thenReturn(ImmutableList.<Object>of(ID_1, ID_2));
        doReturn(this.patient1).when(this.repository).get(ID_1);
        doThrow(SecurityException.class).when(this.repository).get(ID_2);
        doReturn(this.patient3).when(this.repository).get(ID_3);

        final Response response = this.component.fetchPatients();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final String expectedStr = new JSONArray().put(new JSONObject().put("id", ID_1).put("links", this.uriList))
            .put(new JSONObject().put("id", ID_3).put("links", this.uriList))
            .toString();
        @SuppressWarnings("unchecked")
        final List<String> expected = (List<String>) (OBJECT_MAPPER.readValue(expectedStr, List.class));
        @SuppressWarnings("unchecked")
        final List<String> actual = (List<String>) (OBJECT_MAPPER.readValue((String) response.getEntity(), List.class));
        assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlyOnePatientIdDoesNotExist()
        throws ComponentLookupException, QueryException, IOException
    {
        final List<Object> eidList = ImmutableList.<Object>of(EID1, EID2);

        final Request request = mock(Request.class);
        doReturn(request).when(this.container).getRequest();
        doReturn(eidList).when(request).getProperties("eid");
        doReturn(ImmutableList.of(ID_4)).when(request).getProperties("id");

        final Query query = mock(DefaultQuery.class);
        when(this.patient1.toJSON()).thenReturn(this.patient1JSON);
        when(this.patient2.toJSON()).thenReturn(this.patient2JSON);
        when(this.patient3.toJSON()).thenReturn(this.patient3JSON);

        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenReturn(ImmutableList.<Object>of(ID_1, ID_2, ID_3));

        doReturn(this.patient1).when(this.repository).get(ID_1);
        doReturn(this.patient2).when(this.repository).get(ID_2);
        doReturn(this.patient3).when(this.repository).get(ID_3);
        doReturn(null).when(this.repository).get(ID_4);

        final Response response = this.component.fetchPatients();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final String expectedStr = new JSONArray().put(new JSONObject().put("id", ID_1).put("links", this.uriList))
            .put(new JSONObject().put("id", ID_3).put("links", this.uriList))
            .put(new JSONObject().put("id", ID_2).put("links", this.uriList))
            .toString();
        @SuppressWarnings("unchecked")
        final List<String> expected = (List<String>) (OBJECT_MAPPER.readValue(expectedStr, List.class));
        @SuppressWarnings("unchecked")
        final List<String> actual = (List<String>) (OBJECT_MAPPER.readValue((String) response.getEntity(), List.class));
        assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlyIfQueryExceptionIsThrown()
        throws ComponentLookupException, QueryException, IOException
    {
        final List<Object> eidList = new ArrayList<>();
        eidList.add(EID1);

        final Request request = mock(Request.class);
        doReturn(request).when(this.container).getRequest();
        doReturn(eidList).when(request).getProperties("eid");
        doReturn(Collections.emptyList()).when(request).getProperties("id");
        final Query query = mock(DefaultQuery.class);

        when(this.qm.createQuery(Matchers.anyString(), Matchers.anyString())).thenReturn(query);
        when(query.execute()).thenThrow(new QueryException("Exception when executing query", query,
            new XWikiException()));

        final Response response = this.component.fetchPatients();

        verify(this.logger).warn("Failed to retrieve patients with external ids [{}]: {}", eidList,
            "Exception when executing query. Query statement = [null]");

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(null, response.getEntity());
    }
}
