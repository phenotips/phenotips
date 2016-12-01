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
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultPatientResourceImpl} component.
 */
public class DefaultPatientResourceImplTest
{
    @Rule
    public MockitoComponentMockingRule<PatientResource> mocker =
        new MockitoComponentMockingRule<PatientResource>(DefaultPatientResourceImpl.class);

    @Mock
    private User currentUser;

    @Mock
    private Patient patient;

    @Mock
    private UriInfo uriInfo;

    private Logger logger;

    private PatientRepository repository;

    private AuthorizationManager access;

    private UserManager users;

    private String uriString = "http://self/uri";

    private String id = "00000001";

    private DocumentReference patientDocument;

    private DocumentReference userProfileDocument;

    private XWikiContext context;

    private DefaultPatientResourceImpl patientResource;

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);

        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");
        this.patientResource = (DefaultPatientResourceImpl) this.mocker.getComponentUnderTest();

        this.logger = this.mocker.getMockedLogger();
        this.repository = this.mocker.getInstance(PatientRepository.class);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.users = this.mocker.getInstance(UserManager.class);

        this.userProfileDocument = new DocumentReference("wiki", "user", "00000001");
        doReturn(this.currentUser).when(this.users).getCurrentUser();
        doReturn(this.userProfileDocument).when(this.currentUser).getProfileDocument();

        this.patientDocument = new DocumentReference("wiki", "data", "P0000001");
        doReturn(this.patient).when(this.repository).get(this.id);
        doReturn(this.patientDocument).when(this.patient).getDocumentReference();

        doReturn(new URI(this.uriString)).when(this.uriInfo).getRequestUri();
        ReflectionUtils.setFieldValue(this.patientResource, "uriInfo", this.uriInfo);

        Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.context = provider.get();

        Autolinker autolinker = this.mocker.getInstance(Autolinker.class);
        when(autolinker.forResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withGrantedRight(any(Right.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(any(String.class), any(String.class))).thenReturn(autolinker);
        when(autolinker.build()).thenReturn(Collections
            .singletonList(new org.phenotips.rest.model.Link().withAllowedMethods(Collections.singletonList("GET"))
                .withHref(this.uriString).withRel("self")));
    }

    // ----------------------------Get Patient Tests----------------------------

    @Test
    public void getPatientIgnoresMissingPatient()
    {
        doReturn(null).when(this.repository).get(anyString());

        Response response = this.patientResource.getPatient(this.id);

        verify(this.logger).debug("No such patient record: [{}]", this.id);
        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientRejectsRequestWhenUserDoesNotHaveAccess()
    {
        doReturn(false).when(this.access).hasAccess(Right.VIEW, this.userProfileDocument, this.patientDocument);

        Response response = this.patientResource.getPatient(this.id);

        verify(this.logger).debug("View access denied to user [{}] on patient record [{}]", this.currentUser, this.id);
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientNormalBehaviour()
    {
        doReturn(true).when(this.access).hasAccess(Right.VIEW, this.userProfileDocument, this.patientDocument);
        doReturn(new JSONObject()).when(this.patient).toJSON();

        Response response = this.patientResource.getPatient(this.id);

        Assert.assertTrue(response.getEntity() instanceof JSONObject);
        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has("links"));
        JSONArray links = json.getJSONArray("links");
        JSONObject selfLink = null;
        for (int i = 0; i < links.length(); ++i) {
            JSONObject link = links.getJSONObject(i);
            if ("self".equals(link.getString("rel"))) {
                selfLink = link;
            }
        }
        Assert.assertNotNull(selfLink);
        Assert.assertEquals(this.uriString, selfLink.getString("href"));
        Map<String, List<Object>> actualMap = response.getMetadata();
        Assert.assertThat(actualMap, hasValue(hasItem(MediaType.APPLICATION_JSON_TYPE)));
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    // ----------------------------Update Patient Tests----------------------------

    @Test
    public void updatePatientIgnoresMissingPatient()
    {
        doReturn(null).when(this.repository).get(anyString());

        WebApplicationException ex = null;
        try {
            this.patientResource.updatePatient("", this.id);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        Assert.assertNotNull("updatePatient did not throw a WebApplicationException as expected "
            + "when the patient could not be found", ex);
        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getResponse().getStatus());
        verify(this.logger).debug("Patient record [{}] doesn't exist yet. It can be created by POST-ing the"
            + " JSON to /rest/patients", this.id);
    }

    @Test
    public void updatePatientRejectsRequestWhenUserDoesNotHaveAccess()
    {
        doReturn(false).when(this.access).hasAccess(Right.EDIT, this.userProfileDocument, this.patientDocument);

        WebApplicationException ex = null;
        try {
            this.patientResource.updatePatient("", this.id);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        Assert.assertNotNull("updatePatient did not throw a WebApplicationException as expected "
            + "when the User did not have edit rights", ex);
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
        verify(this.logger).debug("Edit access denied to user [{}] on patient record [{}]", this.currentUser, this.id);
    }

    @Test
    public void updatePatientThrowsExceptionWhenSentWrongIdInJSON()
    {
        doReturn(true).when(this.access).hasAccess(Right.EDIT, this.userProfileDocument, this.patientDocument);
        JSONObject json = new JSONObject();
        json.put("id", "!!!!!");
        doReturn(this.id).when(this.patient).getId();

        WebApplicationException ex = null;
        try {
            this.patientResource.updatePatient(json.toString(), this.id);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        Assert.assertNotNull("updatePatient did not throw a WebApplicationException as expected "
            + "when json id did not match patient id", ex);
        Assert.assertEquals(Status.CONFLICT.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    public void updatePatientCatchesExceptions()
    {
        doReturn(true).when(this.access).hasAccess(Right.EDIT, this.userProfileDocument, this.patientDocument);
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        doReturn(this.id).when(this.patient).getId();
        doThrow(Exception.class).when(this.patient).updateFromJSON(any(JSONObject.class));

        WebApplicationException ex = null;
        try {
            this.patientResource.updatePatient(json.toString(), this.id);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        Assert.assertNotNull("updatePatient did not throw a WebApplicationException as expected "
            + "when catching an Exception from Patient.updateFromJSON", ex);
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
        verify(this.logger).warn("Failed to update patient [{}] from JSON: {}. Source JSON was: {}",
            this.patient.getId(),
            ex.getMessage(), json.toString());
    }

    @Test
    public void updatePatientNormalBehaviour()
    {
        doReturn(true).when(this.access).hasAccess(Right.EDIT, this.userProfileDocument, this.patientDocument);
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        doReturn(this.id).when(this.patient).getId();

        Response response = this.patientResource.updatePatient(json.toString(), this.id);

        verify(this.patient).updateFromJSON(any(JSONObject.class));
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    // ----------------------------Delete Patient Tests----------------------------

    @Test
    public void deletePatientIgnoresMissingPatient()
    {
        doReturn(null).when(this.repository).get(anyString());

        Response response = this.patientResource.deletePatient(this.id);

        verify(this.logger).debug("Patient record [{}] didn't exist", this.id);
        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientRejectsRequestWhenUserDoesNotHaveAccess()
    {
        doReturn(false).when(this.access).hasAccess(Right.DELETE, this.userProfileDocument, this.patientDocument);

        Response response = this.patientResource.deletePatient(this.id);

        verify(this.logger).debug("Delete access denied to user [{}] on patient record [{}]", this.currentUser,
            this.id);
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void deletePatientCatchesXWikiException() throws XWikiException
    {
        XWiki wiki = mock(XWiki.class);
        doReturn(wiki).when(this.context).getWiki();
        doReturn(true).when(this.access).hasAccess(Right.DELETE, this.userProfileDocument, this.patientDocument);
        doThrow(XWikiException.class).when(wiki).deleteDocument(any(XWikiDocument.class), eq(this.context));

        WebApplicationException ex = null;
        try {
            this.patientResource.deletePatient(this.id);
        } catch (WebApplicationException temp) {
            ex = temp;
        }

        Assert.assertNotNull("deletePatient did not throw a WebApplicationException as expected "
            + "when catching an XWikiException", ex);
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
        verify(this.logger).warn(eq("Failed to delete patient record [{}]: {}"), eq(this.id), anyString());
    }

    @Test
    public void deletePatientNormalBehaviour() throws XWikiException
    {
        XWiki wiki = mock(XWiki.class);
        XWikiDocument patientXWikiDoc = mock(XWikiDocument.class);
        doReturn(wiki).when(this.context).getWiki();
        doReturn(patientXWikiDoc).when(wiki).getDocument(this.patientDocument, this.context);
        doReturn(true).when(this.access).hasAccess(Right.DELETE, this.userProfileDocument, this.patientDocument);

        Response response = this.patientResource.deletePatient(this.id);

        verify(wiki).getDocument(this.patientDocument, this.context);
        verify(wiki).deleteDocument(patientXWikiDoc, this.context);
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
}
