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

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.model.Patient;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.UserManager;

public class DefaultPatientByExternalIdResourceImplTest {

    @Rule
    public final MockitoComponentMockingRule<XWikiRestComponent> mocker =
            new MockitoComponentMockingRule<XWikiRestComponent>(DefaultPatientByExternalIdResourceImpl.class);

    @Mock
    private Patient patiet;

    private EntityReferenceResolver<EntityReference> currentResolver;

    private Logger logger;

    private PatientRepository repository;

    private DomainObjectFactory factory;

    private QueryManager qm;

    private AuthorizationManager access;

    private UserManager users;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.currentResolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        this.logger = this.mocker.getInstance(Logger.class);
        this.repository = this.mocker.getInstance(PatientRepository.class);
        this.factory = this.mocker.getInstance(DomainObjectFactory.class);
        this.qm = this.mocker.getInstance(QueryManager.class);
        this.access = this.mocker.getInstance(AuthorizationManager.class);
        this.users = this.mocker.getInstance(UserManager.class);
    }
}
