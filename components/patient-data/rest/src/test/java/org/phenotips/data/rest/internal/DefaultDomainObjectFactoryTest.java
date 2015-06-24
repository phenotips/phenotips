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

import org.junit.Rule;
import org.xwiki.security.authorization.AuthorizationManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.rest.DomainObjectFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.users.UserManager;

import javax.ws.rs.core.UriInfo;

import static org.junit.Assert.assertNull;
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
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.users = this.mocker.getInstance(UserManager.class);
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
    }

    @Test
    public void canCreatePatientSummaryProperly() throws ComponentLookupException
    {
        when(this.patient.getDocument()).thenReturn(null);
        when(this.users.getCurrentUser()).thenReturn(null);
        when(this.access.hasAccess(Right.VIEW, null, null)).thenReturn(false);
        assertNull(this.mocker.getComponentUnderTest().createPatientSummary(this.patient, this.uriInfo));
    }
}