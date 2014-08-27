/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientDataScriptService} component.
 *
 * @version $Id$
 * @since 1.0M1
 */
public class PatientDataScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientDataScriptService> mocker =
        new MockitoComponentMockingRule<>(PatientDataScriptService.class);

    @Mock
    private Patient patient;

    private DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "jdoe");

    private DocumentReference patientReference = new DocumentReference("xwiki", "data", "P0123456");

    private PatientRepository repo;

    private AuthorizationManager access;

    private DocumentAccessBridge bridge;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.repo = this.mocker.getInstance(PatientRepository.class);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        when(this.patient.getDocument()).thenReturn(this.patientReference);
    }

    @Test
    public void getPatientByIdForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.getPatientById("P0123456")).thenReturn(this.patient);
        when(this.bridge.getCurrentUserReference()).thenReturn(this.currentUser);
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.patientReference)).thenReturn(true);
        Assert.assertSame(this.patient, this.mocker.getComponentUnderTest().getPatientById("P0123456"));
    }

    @Test
    public void getPatientByIdDeniesUnauthorizedAccess() throws ComponentLookupException
    {
        when(this.repo.getPatientById("P0123456")).thenReturn(this.patient);
        when(this.bridge.getCurrentUserReference()).thenReturn(this.currentUser);
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.patientReference)).thenReturn(false);
        Assert.assertNull(this.mocker.getComponentUnderTest().getPatientById("P0123456"));
    }
}
