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

import com.xpn.xwiki.XWikiContext;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientByExternalIdResource;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.UserManager;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class DefaultPatientByExternalIdResourceImplTest {
    @Rule
    public final MockitoComponentMockingRule<PatientByExternalIdResource> mocker =
            new MockitoComponentMockingRule<PatientByExternalIdResource>(DefaultPatientByExternalIdResourceImpl.class);

    @Mock
    private Patient patient;

    private EntityReferenceResolver<EntityReference> currentResolver;

    private Logger logger;

    private PatientRepository repository;

    private DomainObjectFactory factory;

    private QueryManager qm;

    private AuthorizationManager access;

    private UserManager users;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

    private URI uri;

    private ParameterizedType entityResolverType = new DefaultParameterizedType(null, EntityReferenceResolver.class,
                                                                                String.class);

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        uri = new URI("uri");
//        this.currentResolver = this.mocker.getInstance(this.entityResolverType, "current");
//        this.logger = this.mocker.getInstance(Logger.class);

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

        doReturn(this.uriBuilder).when(this.uriInfo).getBaseUriBuilder();
    }

    @Test
    public void getPatientWithNoAccessReturnsForbiddenCode() throws ComponentLookupException, XWikiRestException
    {
        when(this.repository.getPatientByExternalId("eid")).thenReturn(this.patient);
        when(this.users.getCurrentUser()).thenReturn(null);
        when(this.patient.getDocument()).thenReturn(null);
        when(this.access.hasAccess(Right.DELETE, null, null)).thenReturn(false);

        Response response = this.mocker.getComponentUnderTest().getPatient("eid");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientPerformsCorrectly() throws ComponentLookupException
    {
        when(this.repository.getPatientByExternalId("eid")).thenReturn(this.patient);
        when(this.users.getCurrentUser()).thenReturn(null);
        when(this.patient.getDocument()).thenReturn(null);
        when(this.access.hasAccess(Right.DELETE, null, null)).thenReturn(false);

        JSONObject jsonObject = new JSONObject();
//        when(this.uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
//        when(uriBuilder.path(PatientResource.class)).thenReturn(uriBuilder);
//        when(uriBuilder.build("id")).thenReturn(uri);

        when(this.patient.toJSON()).thenReturn(jsonObject);

        Response response = this.mocker.getComponentUnderTest().getPatient("id");
    }


}
