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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.UserManager;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
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

    @Mock
    private UriInfo uriInfo;

    private ParameterizedType stringResolverType = new DefaultParameterizedType(null, DocumentReferenceResolver.class,
        String.class);

    private AuthorizationManager access;

    private UserManager users;

    private DocumentAccessBridge documentAccessBridge;

    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.users = this.mocker.getInstance(UserManager.class);
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.stringResolver = this.mocker.getInstance(this.stringResolverType, "current");

        when(this.users.getCurrentUser()).thenReturn(null);
    }

    @Test
    public void cannotCreatePatientWithNoAccess() throws ComponentLookupException
    {
        when(this.patient.getDocument()).thenReturn(null);
        when(this.access.hasAccess(Right.VIEW, null, null)).thenReturn(false);
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo));
    }

    @Test
    public void createPatientDocumentExceptionReturnsNull() throws Exception
    {
        when(this.patient.getDocument()).thenReturn(null);
        when(this.access.hasAccess(Right.VIEW, null, null)).thenReturn(true);

        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(Matchers.any(DocumentReference.class));
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo));
    }

    @Test
    public void createPatientPerformsCorrectly() throws ComponentLookupException, URISyntaxException, Exception
    {
        XWikiDocument documentReference = mock(XWikiDocument.class);
        DocumentReference patientDocument = mock(DocumentReference.class);
        DocumentReference creatorDocument = mock(DocumentReference.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("uri");
        Date creationDate = new Date(0);
        DateTime creationDateTime = new DateTime(creationDate).withZone(DateTimeZone.UTC);

        when(this.patient.getDocument()).thenReturn(patientDocument);
        when(this.access.hasAccess(Right.VIEW, null, patientDocument)).thenReturn(true);
        when(this.documentAccessBridge.getDocument(patientDocument)).thenReturn(documentReference);
        when(this.patient.getId()).thenReturn("id");
        when(this.patient.getExternalId()).thenReturn("externalid");
        when(this.patient.getReporter()).thenReturn(creatorDocument);

        when(creatorDocument.toString()).thenReturn("creator");
        when(documentReference.getAuthorReference()).thenReturn(creatorDocument);
        when(documentReference.getVersion()).thenReturn("version");
        when(documentReference.getCreationDate()).thenReturn(creationDate);
        when(documentReference.getDate()).thenReturn(creationDate);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(PatientResource.class)).thenReturn(uriBuilder);
        when(uriBuilder.build("id")).thenReturn(uri);

        PatientSummary patientSummary =
            this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo);

        assertEquals("id", patientSummary.getId());
        assertEquals("externalid", patientSummary.getEid());
        assertEquals("creator", patientSummary.getCreatedBy());
        assertEquals("creator", patientSummary.getLastModifiedBy());
        assertEquals("version", patientSummary.getVersion());
        assertEquals(creationDateTime, patientSummary.getCreatedOn());
        assertEquals(creationDateTime, patientSummary.getLastModifiedOn());
        assertEquals(1, patientSummary.getLinks().size());
        assertEquals("uri", patientSummary.getLinks().get(0).getHref());
    }

    @Test
    public void createPatientFromSummaryWrongLength() throws Exception
    {
        Object[] summary = { new Object() };
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo));
    }

    @Test
    public void createPatientFromSummaryWrongObjectTypes() throws Exception
    {
        Object[] summary = new Object[7];
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo));
    }

    @Test
    public void createPatientFromSummaryNoAccess() throws Exception
    {
        Object[] summary = { "", "", "", new Date(), "", "", new Date() };
        when(this.stringResolver.resolve(Matchers.anyString())).thenReturn(null);
        when(this.access.hasAccess(Right.VIEW, null, null)).thenReturn(false);
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo));
    }

    @Test
    public void createPatientFromSummaryPerformsCorrectly() throws Exception
    {
        Date createdOn = new Date();
        Date modifiedOn = new Date();
        DateTime createdOnDateTime = new DateTime(createdOn).withZone(DateTimeZone.UTC);
        DateTime modifiedOnDateTime = new DateTime(modifiedOn).withZone(DateTimeZone.UTC);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("uri");
        DocumentReference documentReference = new DocumentReference("wikiname", "spacename", "pagename");

        Object[] summary = { "doc", "externalid", "creator", createdOn, "version", "creator", modifiedOn };

        when(this.stringResolver.resolve("doc")).thenReturn(documentReference);
        when(this.access.hasAccess(Right.VIEW, null, documentReference)).thenReturn(true);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);

        when(uriBuilder.path(PatientResource.class)).thenReturn(uriBuilder);
        when(uriBuilder.build("pagename")).thenReturn(uri);

        PatientSummary patientSummary = this.mocker.getComponentUnderTest().createPatientSummary(summary, this.uriInfo);

        assertEquals("pagename", patientSummary.getId());
        assertEquals("externalid", patientSummary.getEid());
        assertEquals("creator", patientSummary.getCreatedBy());
        assertEquals("creator", patientSummary.getLastModifiedBy());
        assertEquals("version", patientSummary.getVersion());
        assertEquals(createdOnDateTime, patientSummary.getCreatedOn());
        assertEquals(modifiedOnDateTime, patientSummary.getLastModifiedOn());
        assertEquals(1, patientSummary.getLinks().size());
        assertEquals("uri", patientSummary.getLinks().get(0).getHref());
    }

    @Test
    public void createAlternativesPerformsCorrectly() throws ComponentLookupException, URISyntaxException
    {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("uri");
        List<String> idList = new ArrayList<>();
        idList.add("page1");
        idList.add("page2");
        DocumentReference doc1 = new DocumentReference("wikiname", "spacename", "page1");
        DocumentReference doc2 = new DocumentReference("wikiname", "spacename", "page2");

        when(this.uriInfo.getRequestUri()).thenReturn(uri);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(this.stringResolver.resolve("page1", Patient.DEFAULT_DATA_SPACE)).thenReturn(doc1);
        when(this.stringResolver.resolve("page2", Patient.DEFAULT_DATA_SPACE)).thenReturn(doc2);
        when(this.access.hasAccess(Right.VIEW, null, doc1)).thenReturn(true);
        when(this.access.hasAccess(Right.VIEW, null, doc2)).thenReturn(true);

        when(uriBuilder.path(PatientResource.class)).thenReturn(uriBuilder);
        when(uriBuilder.build("page1")).thenReturn(uri);
        when(uriBuilder.build("page2")).thenReturn(uri);

        Alternatives alternatives = this.mocker.getComponentUnderTest().createAlternatives(idList, this.uriInfo);

        assertEquals(2, alternatives.getPatients().size());
        assertEquals("page1", alternatives.getPatients().get(0).getId());
        assertEquals("page2", alternatives.getPatients().get(1).getId());
    }

    @Test
    public void createTwoAlternativesOneNoAccess() throws ComponentLookupException, URISyntaxException
    {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("uri");
        List<String> idList = new ArrayList<>();
        idList.add("page1");
        idList.add("page2");
        DocumentReference doc1 = new DocumentReference("wikiname", "spacename", "page1");
        DocumentReference doc2 = new DocumentReference("wikiname", "spacename", "page2");

        when(this.uriInfo.getRequestUri()).thenReturn(uri);
        when(this.uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(this.stringResolver.resolve("page1", Patient.DEFAULT_DATA_SPACE)).thenReturn(doc1);
        when(this.stringResolver.resolve("page2", Patient.DEFAULT_DATA_SPACE)).thenReturn(doc2);
        when(this.access.hasAccess(Right.VIEW, null, doc1)).thenReturn(true);
        when(this.access.hasAccess(Right.VIEW, null, doc2)).thenReturn(false);

        when(uriBuilder.path(PatientResource.class)).thenReturn(uriBuilder);
        when(uriBuilder.build("page1")).thenReturn(uri);

        Alternatives alternatives = this.mocker.getComponentUnderTest().createAlternatives(idList, this.uriInfo);

        assertEquals(1, alternatives.getPatients().size());
        assertEquals("page1", alternatives.getPatients().get(0).getId());
    }
}
