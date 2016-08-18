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
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.model.Alternatives;
import org.phenotips.data.rest.model.PatientSummary;
import org.phenotips.rest.Autolinker;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultDomainObjectFactoryTest
{

    @Rule
    public MockitoComponentMockingRule<DomainObjectFactory> mocker =
        new MockitoComponentMockingRule<DomainObjectFactory>(DefaultDomainObjectFactory.class);

    @Mock
    private Patient patient;

    private DocumentReference patientReference1 = new DocumentReference("wiki", "data", "P0000001");

    private DocumentReference patientReference2 = new DocumentReference("wiki", "data", "P0000002");

    private String eid = "P1";

    private DocumentReference userReference1 = new DocumentReference("wiki", "XWiki", "padams");

    private DocumentReference userReference2 = new DocumentReference("wiki", "XWiki", "hmccoy");

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

    private String requestUri = "http://host/rest/patients/eid/P1";

    private String uri1 = "http://host/rest/patients/P0000001";

    private String uri2 = "http://host/rest/patients/P0000002";

    @Mock
    private User user;

    private ParameterizedType stringResolverType = new DefaultParameterizedType(null, DocumentReferenceResolver.class,
        String.class);

    private AuthorizationManager access;

    private UserManager users;

    private DocumentAccessBridge documentAccessBridge;

    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    @Before
    public void setUp()
        throws ComponentLookupException, IllegalArgumentException, UriBuilderException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.users = this.mocker.getInstance(UserManager.class);
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.stringResolver = this.mocker.getInstance(this.stringResolverType, "current");

        when(this.patient.getDocument()).thenReturn(this.patientReference1);
        when(this.patient.getId()).thenReturn(this.patientReference1.getName());
        when(this.patient.getExternalId()).thenReturn(this.eid);
        when(this.patient.getReporter()).thenReturn(this.userReference1);

        when(this.users.getCurrentUser()).thenReturn(this.user);
        when(this.user.getProfileDocument()).thenReturn(this.userReference1);
        when(this.stringResolver.resolve(this.patientReference1.getName(), Patient.DEFAULT_DATA_SPACE))
            .thenReturn(this.patientReference1);
        when(this.stringResolver.resolve("data.P0000001")).thenReturn(this.patientReference1);
        when(this.stringResolver.resolve(this.patientReference2.getName(), Patient.DEFAULT_DATA_SPACE))
            .thenReturn(this.patientReference2);
        when(this.stringResolver.resolve("data.P0000002")).thenReturn(this.patientReference2);
        when(this.uriInfo.getRequestUri()).thenReturn(new URI(this.requestUri));
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(this.uriBuilder);
        when(this.uriBuilder.path(PatientResource.class)).thenReturn(this.uriBuilder);
        when(this.uriBuilder.build(this.patientReference1.getName())).thenReturn(new URI(this.uri1));
        when(this.uriBuilder.build(this.patientReference2.getName())).thenReturn(new URI(this.uri2));
        when(this.access.hasAccess(Right.VIEW, this.userReference1, this.patientReference1)).thenReturn(true);

        Autolinker autolinker = this.mocker.getInstance(Autolinker.class);
        when(autolinker.forResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withGrantedRight(any(Right.class))).thenReturn(autolinker);
        when(autolinker.withActionableResources(any(Class.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(any(String.class), any(String.class))).thenReturn(autolinker);
        when(autolinker.build()).thenReturn(Collections
            .singletonList(new org.phenotips.rest.model.Link().withAllowedMethods(Collections.singletonList("GET"))
                .withHref(this.uri1).withRel("self")));
    }

    @Test
    public void createPatientWithNullPatientReturnsNull() throws ComponentLookupException
    {
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary((Patient) null, this.uriInfo));
    }

    @Test
    public void cannotCreatePatientWithNoAccess() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.VIEW, this.userReference1, this.patientReference1)).thenReturn(false);
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo));
    }

    @Test
    public void createPatientDocumentExceptionReturnsNull() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(Matchers.any(DocumentReference.class));
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo));
    }

    @Test
    public void createPatientPerformsCorrectly() throws ComponentLookupException, URISyntaxException, Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        Date creationDate = new Date(0);
        DateTime creationDateTime = new DateTime(creationDate).withZone(DateTimeZone.UTC);

        when(this.documentAccessBridge.getDocument(this.patientReference1)).thenReturn(document);

        when(document.getAuthorReference()).thenReturn(this.userReference2);
        when(document.getVersion()).thenReturn("version");
        when(document.getCreationDate()).thenReturn(creationDate);
        when(document.getDate()).thenReturn(creationDate);

        PatientSummary patientSummary =
            this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo);

        assertEquals(this.patientReference1.getName(), patientSummary.getId());
        assertEquals(this.eid, patientSummary.getEid());
        assertEquals("wiki:XWiki.padams", patientSummary.getCreatedBy());
        assertEquals("wiki:XWiki.hmccoy", patientSummary.getLastModifiedBy());
        assertEquals("version", patientSummary.getVersion());
        assertEquals(creationDateTime, patientSummary.getCreatedOn());
        assertEquals(creationDateTime, patientSummary.getLastModifiedOn());
        assertEquals(1, patientSummary.getLinks().size());
        assertEquals(this.uri1, patientSummary.getLinks().get(0).getHref());
    }

    @Test
    public void createPatientWithNoCurrentUserSucceeds() throws Exception
    {
        when(this.users.getCurrentUser()).thenReturn(null);
        when(this.access.hasAccess(Right.VIEW, null, this.patientReference1)).thenReturn(true);

        XWikiDocument document = mock(XWikiDocument.class);
        Date creationDate = new Date(0);

        when(this.documentAccessBridge.getDocument(this.patientReference1)).thenReturn(document);

        when(document.getAuthorReference()).thenReturn(this.userReference2);
        when(document.getVersion()).thenReturn("version");
        when(document.getCreationDate()).thenReturn(creationDate);
        when(document.getDate()).thenReturn(creationDate);

        PatientSummary patientSummary =
            this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo);

        assertEquals(this.patientReference1.getName(), patientSummary.getId());
        assertEquals(this.eid, patientSummary.getEid());
        assertEquals("wiki:XWiki.padams", patientSummary.getCreatedBy());
        assertEquals("wiki:XWiki.hmccoy", patientSummary.getLastModifiedBy());
        assertEquals("version", patientSummary.getVersion());
        assertEquals(1, patientSummary.getLinks().size());
        assertEquals(this.uri1, patientSummary.getLinks().get(0).getHref());
    }

    @Test
    public void createPatientFromSummaryWithWrongLengthReturnsNull() throws Exception
    {
        Object[] summary = { new Object() };
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo));
    }

    @Test
    public void createPatientFromSummaryWithNullSummaryReturnsNull() throws Exception
    {
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary((Object[]) null, this.uriInfo));
    }

    @Test
    public void createPatientFromSummaryWithWrongObjectTypesReturnsNull() throws Exception
    {
        Object[] summary = new Object[7];
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo));
    }

    @Test
    public void createPatientFromSummaryWithNoAccessReturnsNull() throws Exception
    {
        Object[] summary =
            { "data.P0000001", this.eid, "XWiki.padams", new Date(), "version", "XWiki.hmccoy", new Date() };
        when(this.access.hasAccess(Right.VIEW, this.userReference1, this.patientReference1)).thenReturn(false);
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo));
    }

    @Test
    public void createPatientFromSummaryPerformsCorrectly() throws Exception
    {
        Date createdOn = new Date();
        Date modifiedOn = new Date();
        DateTime createdOnDateTime = new DateTime(createdOn).withZone(DateTimeZone.UTC);
        DateTime modifiedOnDateTime = new DateTime(modifiedOn).withZone(DateTimeZone.UTC);

        Object[] summary =
            { "data.P0000001", this.eid, "XWiki.padams", createdOn, "version", "XWiki.hmccoy", modifiedOn };

        PatientSummary patientSummary = this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo);

        assertEquals(this.patientReference1.getName(), patientSummary.getId());
        assertEquals(this.eid, patientSummary.getEid());
        assertEquals("XWiki.padams", patientSummary.getCreatedBy());
        assertEquals("XWiki.hmccoy", patientSummary.getLastModifiedBy());
        assertEquals("version", patientSummary.getVersion());
        assertEquals(createdOnDateTime, patientSummary.getCreatedOn());
        assertEquals(modifiedOnDateTime, patientSummary.getLastModifiedOn());
        assertEquals(1, patientSummary.getLinks().size());
        assertEquals(this.uri1, patientSummary.getLinks().get(0).getHref());
    }

    @Test
    public void createPatientFromSummaryWithNoCurrentUserPerformsCorrectly() throws Exception
    {
        when(this.users.getCurrentUser()).thenReturn(null);
        when(this.access.hasAccess(Right.VIEW, null, this.patientReference1)).thenReturn(true);

        Date createdOn = new Date();
        Date modifiedOn = new Date();
        DateTime createdOnDateTime = new DateTime(createdOn).withZone(DateTimeZone.UTC);
        DateTime modifiedOnDateTime = new DateTime(modifiedOn).withZone(DateTimeZone.UTC);

        Object[] summary =
            { "data.P0000001", this.eid, "XWiki.padams", createdOn, "version", "XWiki.hmccoy", modifiedOn };

        PatientSummary patientSummary = this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo);

        assertEquals(this.patientReference1.getName(), patientSummary.getId());
        assertEquals(this.eid, patientSummary.getEid());
        assertEquals("XWiki.padams", patientSummary.getCreatedBy());
        assertEquals("XWiki.hmccoy", patientSummary.getLastModifiedBy());
        assertEquals("version", patientSummary.getVersion());
        assertEquals(createdOnDateTime, patientSummary.getCreatedOn());
        assertEquals(modifiedOnDateTime, patientSummary.getLastModifiedOn());
        assertEquals(1, patientSummary.getLinks().size());
        assertEquals(this.uri1, patientSummary.getLinks().get(0).getHref());
    }

    @Test
    public void createAlternativesPerformsCorrectly() throws ComponentLookupException, URISyntaxException
    {
        List<String> idList = new ArrayList<>();
        idList.add(this.patientReference1.getName());
        idList.add(this.patientReference2.getName());

        when(this.access.hasAccess(Right.VIEW, this.userReference1, this.patientReference2)).thenReturn(true);

        Alternatives alternatives = this.mocker.getComponentUnderTest().createAlternatives(idList, this.uriInfo);

        assertEquals(2, alternatives.getPatients().size());
        assertEquals(this.patientReference1.getName(), alternatives.getPatients().get(0).getId());
        assertEquals(this.patientReference2.getName(), alternatives.getPatients().get(1).getId());
    }

    @Test
    public void createTwoAlternativesOneNoAccess() throws ComponentLookupException, URISyntaxException
    {
        when(this.users.getCurrentUser()).thenReturn(null);

        List<String> idList = new ArrayList<>();
        idList.add(this.patientReference1.getName());
        idList.add(this.patientReference2.getName());

        when(this.access.hasAccess(Right.VIEW, null, this.patientReference1)).thenReturn(true);
        when(this.access.hasAccess(Right.VIEW, null, this.patientReference2)).thenReturn(false);

        Alternatives alternatives = this.mocker.getComponentUnderTest().createAlternatives(idList, this.uriInfo);

        assertEquals(1, alternatives.getPatients().size());
        assertEquals(this.patientReference1.getName(), alternatives.getPatients().get(0).getId());
    }
}
