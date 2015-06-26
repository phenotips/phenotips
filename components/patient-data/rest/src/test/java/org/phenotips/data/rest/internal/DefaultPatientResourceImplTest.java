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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.PatientResource;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


public class DefaultPatientResourceImplTest {

    @Rule
    public MockitoComponentMockingRule<PatientResource> mocker =
        new MockitoComponentMockingRule<PatientResource>(DefaultPatientResourceImpl.class);

    @Mock
    private Logger logger;

    @Mock
    private PatientRepository repository;

    @Mock
    private AuthorizationManager access;

    @Mock
    private UserManager users;

    @Mock
    private User currentUser;

    @Mock
    private DocumentReference profileDocument;

    @Mock
    private Patient patient;

    private DefaultPatientResourceImpl patientResource;

    private String id = "00000001";


    @Before
    public void setUp() throws ComponentLookupException {
        MockitoAnnotations.initMocks(this);
        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");

        this.patientResource = (DefaultPatientResourceImpl)this.mocker.getComponentUnderTest();

        doReturn(this.currentUser).when(this.users).getCurrentUser();
        doReturn(this.profileDocument).when(this.currentUser).getProfileDocument();

        doReturn(this.patient).when(this.repository).getPatientById(this.id);

        ReflectionUtils.setFieldValue(this.patientResource, "logger", this.logger);
        ReflectionUtils.setFieldValue(this.patientResource, "repository", this.repository);
        ReflectionUtils.setFieldValue(this.patientResource, "access", this.access);
        ReflectionUtils.setFieldValue(this.patientResource, "users", this.users);
    }

    @Test
    public void checkGetPatientBehaviorWhenCantFindPatient() throws XWikiRestException {
        doReturn(null).when(this.repository).getPatientById(anyString());
        Response response = this.patientResource.getPatient(this.id);

        verify(this.logger).debug("No such patient record: [{}]", this.id);
        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void checkDeletePatientBehaviorWhenCantFindPatient() throws XWikiRestException {
        doReturn(null).when(this.repository).getPatientById(anyString());
        Response response = this.patientResource.deletePatient(this.id);

        verify(this.logger).debug("Patient record [{}] didn't exist", this.id);
        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test(expected = WebApplicationException.class)
    public void checkWebApplicationExceptionThrownWhenUpdatePatientCantFindPatient() throws XWikiRestException {
        doReturn(null).when(this.repository).getPatientById(anyString());
        this.patientResource.updatePatient("", this.id);
    }

    @Test
    public void checkLoggerMessageWhenUpdatePatientCantFindPatient() throws XWikiRestException {
        doReturn(null).when(this.repository).getPatientById(anyString());
        try {
            this.patientResource.updatePatient("", this.id);
        } catch (WebApplicationException e){

        }
        verify(this.logger).debug("Patient record [{}] doesn't exist yet. It can be created by POST-ing the JSON to /rest/patients", this.id);
    }

    @Test
    public void checkGetPatientBehaviorWhenUserDoesNotHaveAccess() throws XWikiRestException {
        doReturn(null).when(this.patient).getDocument();
        doReturn(false).when(this.access).hasAccess(Right.VIEW, this.profileDocument, null);

        Response response = this.patientResource.getPatient(this.id);

        verify(this.logger).debug("View access denied to user [{}] on patient record [{}]", this.currentUser, this.id);
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());

    }
}
