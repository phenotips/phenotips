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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link SecurePatientRepository} component.
 *
 * @version $Id$
 * @since 1.3M1
 */
public class SecurePatientRepositoryTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientRepository> mocker =
        new MockitoComponentMockingRule<PatientRepository>(SecurePatientRepository.class);

    @Mock
    private Patient patient;

    private DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "jdoe");

    private DocumentReference patientReference = new DocumentReference("xwiki", "data", "P0123456");

    private AuthorizationManager access;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        PatientRepository internalRepo = this.mocker.getInstance(PatientRepository.class);

        DocumentAccessBridge bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        when(bridge.getCurrentUserReference()).thenReturn(this.currentUser);
        when(this.patient.getDocument()).thenReturn(this.patientReference);

        when(internalRepo.get("P0123456")).thenReturn(this.patient);
        when(internalRepo.getByName("Neuro123")).thenReturn(this.patient);
        when(internalRepo.create()).thenReturn(this.patient);
        when(internalRepo.create(this.currentUser)).thenReturn(this.patient);
        when(internalRepo.load(any(DocumentModelBridge.class))).thenReturn(this.patient);

        EntityReferenceResolver<EntityReference> currentResolver =
            this.mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE, "current");
        when(currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))
            .thenReturn(this.patientReference.getParent());
    }

    @Test
    public void getForwardsCallsWhenAuthorized() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.patientReference)).thenReturn(true);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().get("P0123456"));
    }

    @Test
    public void getForwardsNullResults() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().get("P0123457"));
    }

    @Test(expected = SecurityException.class)
    public void getDeniesUnauthorizedAccess() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.patientReference)).thenReturn(false);
        this.mocker.getComponentUnderTest().get("P0123456");
    }

    @Test
    public void getByNameForwardsCallsWhenAuthorized() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.patientReference)).thenReturn(true);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().getByName("Neuro123"));
    }

    @Test
    public void getByNameForwardsNullResults() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getByName("NotAPatient"));
    }

    @Test(expected = SecurityException.class)
    public void getByNameDeniesUnauthorizedAccess() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.patientReference)).thenReturn(false);
        this.mocker.getComponentUnderTest().getByName("Neuro123");
    }

    @Test
    public void createForwardsCallsWhenAuthorized() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.EDIT, this.currentUser, this.patientReference.getParent())).thenReturn(true);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().create());
    }

    @Test(expected = SecurityException.class)
    public void createDeniesUnauthorizedAccess() throws ComponentLookupException
    {
        when(this.access.hasAccess(Right.EDIT, this.currentUser, this.patientReference.getParent())).thenReturn(false);
        this.mocker.getComponentUnderTest().create();
    }

    @Test
    public void loadForwardsCalls() throws ComponentLookupException
    {
        XWikiDocument doc = new XWikiDocument(this.patientReference);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().load(doc));
    }
}
