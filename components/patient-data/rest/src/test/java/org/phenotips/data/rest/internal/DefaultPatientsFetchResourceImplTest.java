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
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

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
    private static final String EID1 = "EID1";

    private static final String EID2 = "EID2";

    private static final String ID_1 = "P0000001";

    private static final String ID_2 = "P0000002";

    private static final String ID_3 = "P0000003";

    private static final String ID_4 = "P0000004";

    private static final String EID_LABEL = "eid";

    private static final String ID_LABEL = "id";

    private static final String LINKS_LABEL = "links";

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

    private DefaultPatientsFetchResourceImpl component;

    private Request request;

    private Query query;

    private final JSONObject patient1JSON = new JSONObject().put(ID_LABEL, ID_1);

    private final JSONObject patient2JSON = new JSONObject().put(ID_LABEL, ID_2);

    private final JSONObject patient3JSON = new JSONObject().put(ID_LABEL, ID_3);

    private final Collection<String> uriList = ImmutableList.of("http://uri");

    @Before
    public void setUp() throws ComponentLookupException, QueryException
    {
        MockitoAnnotations.initMocks(this);

        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");

        this.component = (DefaultPatientsFetchResourceImpl) this.mocker.getComponentUnderTest();

        this.logger = this.mocker.getMockedLogger();
        this.repository = this.mocker.getInstance(PatientRepository.class, "secure");

        when(this.patient1.getId()).thenReturn(ID_1);
        when(this.patient2.getId()).thenReturn(ID_2);
        when(this.patient3.getId()).thenReturn(ID_3);

        final Autolinker autolinker = this.mocker.getInstance(Autolinker.class);
        when(autolinker.forSecondaryResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(any(String.class), any(String.class))).thenReturn(autolinker);
        doReturn(this.uriList).when(autolinker).build();

        final Container container = this.mocker.getInstance(Container.class);
        this.request = mock(Request.class);
        doReturn(this.request).when(container).getRequest();

        this.query = mock(DefaultQuery.class);
        final QueryManager qm = this.mocker.getInstance(QueryManager.class);
        doReturn(this.query).when(qm).createQuery(Matchers.anyString(), Matchers.anyString());

        when(this.patient1.toJSON()).thenReturn(this.patient1JSON);
        when(this.patient2.toJSON()).thenReturn(this.patient2JSON);
        when(this.patient3.toJSON()).thenReturn(this.patient3JSON);

        when(this.repository.get(ID_1)).thenReturn(this.patient1);
        when(this.repository.get(ID_2)).thenReturn(this.patient2);
        when(this.repository.get(ID_3)).thenReturn(this.patient3);
        when(this.repository.get(ID_4)).thenReturn(null);
    }

    @Test
    public void getPatientsWithEmptyEidAndId()
    {
        doReturn(Collections.emptyList()).when(this.request).getProperties(EID_LABEL);
        doReturn(Collections.emptyList()).when(this.request).getProperties(ID_LABEL);

        final Response response = this.component.fetchPatients();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("[]", response.getEntity());
    }

    @Test
    public void getPatientsPerformsCorrectlyOnePatientRecordByEidNoneById() throws QueryException
    {
        final List<Object> eidList = ImmutableList.<Object>of(EID1);

        doReturn(eidList).when(this.request).getProperties(EID_LABEL);
        doReturn(Collections.emptyList()).when(this.request).getProperties(ID_LABEL);

        doReturn(ImmutableList.<Object>of(ID_1)).when(this.query).execute();

        final Response response = this.component.fetchPatients();

        final JSONArray expected = new JSONArray().put(new JSONObject().put(ID_LABEL, ID_1).put(LINKS_LABEL, this.uriList));

        final JSONArray actual = new JSONArray(response.getEntity().toString());
        assertTrue(expected.similar(actual));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlySeveralPatientRecordsAccessToAll() throws QueryException
    {
        final List<Object> eidList = ImmutableList.<Object>of(EID1, EID2);

        final List<String> idList = ImmutableList.of(ID_3);

        doReturn(eidList).when(this.request).getProperties(EID_LABEL);
        doReturn(idList).when(this.request).getProperties(ID_LABEL);

        when(this.query.execute()).thenReturn(ImmutableList.<Object>of(ID_1, ID_2));

        final Response response = this.component.fetchPatients();

        final JSONArray expected = new JSONArray()
            .put(new JSONObject().put(ID_LABEL, ID_1).put(LINKS_LABEL, this.uriList))
            .put(new JSONObject().put(ID_LABEL, ID_2).put(LINKS_LABEL, this.uriList))
            .put(new JSONObject().put(ID_LABEL, ID_3).put(LINKS_LABEL, this.uriList));
        final JSONArray actual = new JSONArray(response.getEntity().toString());

        assertTrue(expected.similar(actual));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlySeveralPatientRecordsAccessToSome() throws QueryException
    {
        final List<Object> eidList = ImmutableList.<Object>of(EID1, EID2);

        final List<String> idList = ImmutableList.of(ID_3);

        doReturn(eidList).when(this.request).getProperties(EID_LABEL);
        doReturn(idList).when(this.request).getProperties(ID_LABEL);

        when(this.query.execute()).thenReturn(ImmutableList.<Object>of(ID_1, ID_2));
        doThrow(SecurityException.class).when(this.repository).get(ID_2);

        final Response response = this.component.fetchPatients();

        final JSONArray expected = new JSONArray().put(new JSONObject().put(ID_LABEL, ID_1).put(LINKS_LABEL, this.uriList))
            .put(new JSONObject().put(ID_LABEL, ID_3).put(LINKS_LABEL, this.uriList));
        final JSONArray actual = new JSONArray(response.getEntity().toString());
        verify(this.logger).warn("Failed to retrieve patient with ID [{}]: {}", ID_2, null);
        assertTrue(expected.similar(actual));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlyOnePatientIdDoesNotExist() throws QueryException
    {
        final List<Object> eidList = ImmutableList.<Object>of(EID1, EID2);

        doReturn(eidList).when(this.request).getProperties(EID_LABEL);
        doReturn(ImmutableList.of(ID_4)).when(this.request).getProperties(ID_LABEL);

        when(this.query.execute()).thenReturn(ImmutableList.<Object>of(ID_1, ID_2, ID_3));

        final Response response = this.component.fetchPatients();

        final JSONArray expected = new JSONArray().put(new JSONObject().put(ID_LABEL, ID_1).put(LINKS_LABEL, this.uriList))
            .put(new JSONObject().put(ID_LABEL, ID_2).put(LINKS_LABEL, this.uriList))
            .put(new JSONObject().put(ID_LABEL, ID_3).put(LINKS_LABEL, this.uriList));
        final JSONArray actual = new JSONArray(response.getEntity().toString());
        assertTrue(expected.similar(actual));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientsPerformsCorrectlyIfQueryExceptionIsThrown() throws QueryException
    {
        final List<Object> eidList = new ArrayList<>();
        eidList.add(EID1);

        doReturn(eidList).when(this.request).getProperties(EID_LABEL);
        doReturn(Collections.emptyList()).when(this.request).getProperties(ID_LABEL);

        when(this.query.execute()).thenThrow(new QueryException("Exception when executing query", this.query,
            new XWikiException()));

        final Response response = this.component.fetchPatients();

        verify(this.logger).error("Failed to retrieve patients with external ids [{}]: {}", eidList,
            "Exception when executing query. Query statement = [null]");

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(null, response.getEntity());
    }
}
