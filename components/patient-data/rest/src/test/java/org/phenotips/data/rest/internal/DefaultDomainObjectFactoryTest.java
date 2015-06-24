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

import com.xpn.xwiki.doc.XWikiDocument;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.model.PatientSummary;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.UserManager;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultDomainObjectFactoryTest {

    @Rule
    public MockitoComponentMockingRule<DomainObjectFactory> mocker =
            new MockitoComponentMockingRule<DomainObjectFactory>(DefaultDomainObjectFactory.class);

    @Mock
    private Patient patient;

    @Mock
    private UriInfo uriInfo;

    private AuthorizationManager access;

    private UserManager users;

    private DocumentAccessBridge documentAccessBridge;

    @Before
    public void setUp() throws ComponentLookupException {
        MockitoAnnotations.initMocks(this);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.users = this.mocker.getInstance(UserManager.class);
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        when(this.users.getCurrentUser()).thenReturn(null);
    }

    @Test
    public void cannotCreatePatientWithNoAccess() throws ComponentLookupException {
        when(this.patient.getDocument()).thenReturn(null);
        when(this.access.hasAccess(Right.VIEW, null, null)).thenReturn(false);
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo));
    }

    @Test
    public void createPatientDocumentExceptionReturnsNull() throws Exception {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.patient.getDocument()).thenReturn(documentReference);
        when(this.access.hasAccess(Right.VIEW, null, null)).thenReturn(true);
        when(this.documentAccessBridge.getDocument(documentReference)).thenThrow(Exception.class);
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
        Date creationDate = new Date(1);

        when(this.patient.getDocument()).thenReturn(patientDocument);
        when(this.access.hasAccess(Right.VIEW, null, patientDocument)).thenReturn(true);
        when(this.documentAccessBridge.getDocument(patientDocument)).thenReturn(documentReference);

        when(creatorDocument.toString()).thenReturn("creator");

        when(this.patient.getId()).thenReturn("id");
        when(this.patient.getExternalId()).thenReturn("externalid");
        when(this.patient.getReporter()).thenReturn(creatorDocument);

        when(documentReference.getAuthorReference()).thenReturn(creatorDocument);
        when(documentReference.getVersion()).thenReturn("version");
        when(documentReference.getCreationDate()).thenReturn(creationDate);
        when(documentReference.getDate()).thenReturn(creationDate);
        when(uriInfo.getBaseUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(PatientResource.class)).thenReturn(uriBuilder);
        when(uriBuilder.build("id")).thenReturn(uri);

        PatientSummary patientSummary = mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo);

        assertEquals("id", patientSummary.getId());
        assertEquals("externalid", patientSummary.getEid());
        assertEquals("creator", patientSummary.getCreatedBy());
        assertEquals("creator", patientSummary.getLastModifiedBy());
        assertEquals("version", patientSummary.getVersion());
        assertTrue(patientSummary.getCreatedOn() instanceof DateTime);
        assertTrue(patientSummary.getLastModifiedOn() instanceof DateTime);
        assertEquals(1, patientSummary.getLinks().size());
        assertEquals("uri", patientSummary.getLinks().get(0).getHref());
    }
}