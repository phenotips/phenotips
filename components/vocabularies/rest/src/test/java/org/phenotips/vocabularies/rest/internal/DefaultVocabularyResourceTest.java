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
package org.phenotips.vocabularies.rest.internal;

import org.phenotips.rest.Autolinker;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultVocabularyResource} class.
 */
public class DefaultVocabularyResourceTest
{
    private static final String VERSION = "version";

    private static final String HPO_ID = "hpo";

    private static final String HPO_VERSION = "version_1";

    @Rule
    public MockitoComponentMockingRule<VocabularyResource> mocker =
        new MockitoComponentMockingRule<>(DefaultVocabularyResource.class);

    @Mock
    private UriInfo uriInfo;

    @Mock
    private User user;

    @Mock
    private Vocabulary hpoVocab;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary hpoRestVocab;

    private VocabularyManager vm;

    private AuthorizationService authorizationService;

    private VocabularyResource component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(mock(XWikiContext.class));

        this.component = this.mocker.getComponentUnderTest();
        this.vm = this.mocker.getInstance(VocabularyManager.class);

        final Autolinker autolinker = this.mocker.getInstance(Autolinker.class);
        when(autolinker.forResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withGrantedRight(any(Right.class))).thenReturn(autolinker);

        when(autolinker.forSecondaryResource(any(Class.class), eq(this.uriInfo))).thenReturn(autolinker);
        when(autolinker.withActionableResources(any(Class.class))).thenReturn(autolinker);

        when(this.vm.getVocabulary(HPO_ID)).thenReturn(this.hpoVocab);
        when(this.hpoVocab.reindex(null)).thenReturn(0);
        when(this.hpoVocab.getVersion()).thenReturn(HPO_VERSION);

        final DomainObjectFactory objectFactory = this.mocker.getInstance(DomainObjectFactory.class);
        when(objectFactory.createLinkedVocabularyRepresentation(eq(this.hpoVocab), any(), any()))
            .thenReturn(this.hpoRestVocab);

        final UserManager users = this.mocker.getInstance(UserManager.class);
        when(users.getCurrentUser()).thenReturn(this.user);

        this.authorizationService = this.mocker.getInstance(AuthorizationService.class);
        when(this.authorizationService.hasAccess(eq(this.user), eq(Right.ADMIN), any(EntityReference.class)))
            .thenReturn(true);
    }

    @Test(expected = WebApplicationException.class)
    public void getVocabularyNoSuchVocabulary()
    {
        when(this.vm.getVocabulary(HPO_ID)).thenReturn(null);
        this.component.getVocabulary(HPO_ID);
    }

    @Test
    public void getVocabularyValid()
    {
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabulary = this.component.getVocabulary(HPO_ID);
        Assert.assertEquals(this.hpoRestVocab, vocabulary);
    }

    @Test
    public void reindexUrlIsInvalid()
    {
        final Response response = this.component.reindex(HPO_ID, "htps://phenotips.org/");
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void reindexUrlDoesNotExist()
    {
        final Response response = this.component.reindex(HPO_ID, "https://www.url_does_not_exist.ca");
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void reindexUserNotAdmin()
    {
        when(this.authorizationService.hasAccess(eq(this.user), eq(Right.ADMIN), any(EntityReference.class)))
            .thenReturn(false);
        final Response response = this.component.reindex(HPO_ID, null);
        Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void reindexNonexistentVocabulary()
    {
        when(this.vm.getVocabulary(HPO_ID)).thenReturn(null);
        final Response response = this.component.reindex(HPO_ID, null);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void reindexStatusIsOne()
    {
        when(this.hpoVocab.reindex(null)).thenReturn(1);
        final Response response = this.component.reindex(HPO_ID, null);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }

    @Test
    public void reindexStatusIsTwo()
    {
        when(this.hpoVocab.reindex(null)).thenReturn(2);
        final Response response = this.component.reindex(HPO_ID, null);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void reindexIsNotSupported()
    {
        when(this.hpoVocab.reindex(null)).thenThrow(new UnsupportedOperationException());
        final Response response = this.component.reindex(HPO_ID, null);
        Assert.assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }

    @Test
    public void reindexIsSuccessful()
    {
        final Response responseDefault = this.component.reindex(HPO_ID, null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), responseDefault.getStatus());
        Assert.assertTrue(new JSONObject().put(VERSION, HPO_VERSION).similar(responseDefault.getEntity()));

        final Response responseJAR = this.component.reindex(HPO_ID, "jar:abc");
        Assert.assertEquals(Response.Status.OK.getStatusCode(), responseJAR.getStatus());
        Assert.assertTrue(new JSONObject().put(VERSION, HPO_VERSION).similar(responseJAR.getEntity()));
    }
}
