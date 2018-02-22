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
package org.phenotips.studies.family.rest.internal;

import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.rest.FamilyResource;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultFamilyResourceImpl} component.
 */
public class DefaultFamilyResourceImplTest
{
    private static final String FAMILY_ID = "FAM00000001";

    @Rule
    public MockitoComponentMockingRule<FamilyResource> mocker =
        new MockitoComponentMockingRule<>(DefaultFamilyResourceImpl.class);

    @Mock
    private User currentUser;

    private FamilyRepository familyRepository;

    @Mock
    private Family family;

    private FamilyTools familyTools;

    private FamilyResource resource;

    @Before
    public void setUp() throws ComponentLookupException, URISyntaxException
    {
        MockitoAnnotations.initMocks(this);

        // This is needed for the XWikiResource initialization
        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");

        this.resource = this.mocker.getComponentUnderTest();

        this.familyRepository = this.mocker.getInstance(FamilyRepository.class);
        this.familyTools = this.mocker.getInstance(FamilyTools.class);

        final UserManager users = this.mocker.getInstance(UserManager.class);
        doReturn(this.currentUser).when(users).getCurrentUser();

        when(this.familyRepository.get(FAMILY_ID)).thenReturn(this.family);
    }

    @Test
    public void deleteFamilyIgnoresMissingFamily()
    {
        when(this.familyRepository.get(FAMILY_ID)).thenReturn(null);

        Response response = this.resource.deleteFamily(FAMILY_ID, false);

        Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteFamilyRejectsRequestWhenUserDoesNotHaveAccess()
    {
        when(this.familyTools.currentUserCanDeleteFamily(FAMILY_ID, false)).thenReturn(false);

        Response response = this.resource.deleteFamily(FAMILY_ID, false);

        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteFamilyReportsNoContentAfterSuccess() throws WebApplicationException
    {
        when(this.familyTools.currentUserCanDeleteFamily(FAMILY_ID, false)).thenReturn(true);
        when(this.familyTools.deleteFamily(FAMILY_ID, false)).thenReturn(true);

        Response response = this.resource.deleteFamily(FAMILY_ID, false);

        verify(this.familyTools).deleteFamily(FAMILY_ID, false);
        Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void deleteFamilyReportsServerErrorAfterFailure() throws WebApplicationException
    {
        when(this.familyTools.currentUserCanDeleteFamily(FAMILY_ID, false)).thenReturn(true);
        when(this.familyTools.deleteFamily(FAMILY_ID, false)).thenReturn(false);

        Response response = this.resource.deleteFamily(FAMILY_ID, false);

        verify(this.familyTools).deleteFamily(FAMILY_ID, false);
        Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
}
