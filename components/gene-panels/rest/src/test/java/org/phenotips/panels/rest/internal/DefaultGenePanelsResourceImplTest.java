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
package org.phenotips.panels.rest.internal;

import org.phenotips.panels.GenePanel;
import org.phenotips.panels.rest.GenePanelsResource;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGenePanelsResourceImpl}.
 *
 * @version $Id$
 * @since 1.3M6
 */
public class DefaultGenePanelsResourceImplTest
{
    private static final String START_PAGE_LABEL = "startPage";

    private static final String TOTAL_PAGES_LABEL = "totalPages";

    private static final String REQ_NO = "reqNo";

    private static final String NO_CONTEXT_PROVIDED_MSG = "No content provided.";

    private static final String TERM_1 = "HP:001";

    private static final String TERM_2 = "HP:002";

    private static final String TERM_3 = "HP:003";

    @Rule
    public MockitoComponentMockingRule<GenePanelsResource> mocker =
        new MockitoComponentMockingRule<GenePanelsResource>(DefaultGenePanelsResourceImpl.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private Logger logger;

    private GenePanelsResource component;

    private GenePanelLoader genePanelLoader;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        MockitoAnnotations.initMocks(this);
        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager compManager = this.mocker.getInstance(ComponentManager.class, "context");
        Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = provider.get();
        when(compManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(context).when(executionContext).getProperty("xwikicontext");

        this.component = this.mocker.getComponentUnderTest();
        this.genePanelLoader = this.mocker.getInstance(GenePanelLoader.class);
        this.logger = this.mocker.getMockedLogger();
    }

    @Test
    public void getGeneCountsFromPhenotypesNoPresentNorAbsentTermsProvided()
    {
        final Response response1 = this.component.getGeneCountsFromPhenotypes(null, null, 1, 20, 1);
        verify(this.logger).error("No content provided.");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response1.getStatus());

        final Response response2 = this.component.getGeneCountsFromPhenotypes(null, Collections.<String>emptyList(),
            1, 20, 1);
        verify(this.logger, times(2)).error(NO_CONTEXT_PROVIDED_MSG);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response2.getStatus());

        final Response response3 = this.component.getGeneCountsFromPhenotypes(Collections.<String>emptyList(), null,
            1, 20, 1);
        verify(this.logger, times(3)).error(NO_CONTEXT_PROVIDED_MSG);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response3.getStatus());

        final Response response4 = this.component.getGeneCountsFromPhenotypes(Collections.<String>emptyList(),
            Collections.<String>emptyList(), 1, 20, 1);
        verify(this.logger, times(4)).error(NO_CONTEXT_PROVIDED_MSG);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response4.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesAbsentTermsProvidedPresentTermsGenerateNoContent() throws ExecutionException
    {
        final List<String> presentTerms = Collections.emptyList();
        final List<String> absentTerms = Collections.singletonList(TERM_1);
        when(this.genePanelLoader.get(presentTerms)).thenThrow(new ExecutionException(new Throwable()));
        final Response response = this.component.getGeneCountsFromPhenotypes(presentTerms, absentTerms, 1, 20, 1);
        verify(this.logger).error("No content associated with [present-term: {}, absent-term: {}].", presentTerms,
                absentTerms);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesStartPageNegativeResultsInBadRequest() throws ExecutionException
    {
        final List<String> presentTerms = Collections.singletonList(TERM_2);
        final List<String> absentTerms = Collections.singletonList(TERM_1);
        final GenePanel genePanel = mock(GenePanel.class);

        when(this.genePanelLoader.get(presentTerms)).thenReturn(genePanel);
        final Response response = this.component.getGeneCountsFromPhenotypes(presentTerms, absentTerms, -20, 20, 1);
        verify(this.logger).error("The requested [{}: {}] is out of bounds.", START_PAGE_LABEL, -20);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesStartPageOutOfBounds() throws ExecutionException
    {
        final List<String> presentTerms = Collections.singletonList(TERM_2);
        final List<String> absentTerms = Collections.singletonList(TERM_1);
        final GenePanel genePanel = mock(GenePanel.class);

        when(genePanel.size()).thenReturn(1);
        when(this.genePanelLoader.get(presentTerms)).thenReturn(genePanel);
        final Response response = this.component.getGeneCountsFromPhenotypes(presentTerms, absentTerms, 2, 20, 1);
        verify(this.logger).error("The requested [{}: {}] is out of bounds.", START_PAGE_LABEL, 2);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesReturnEverythingIfNumResultsIsNegative() throws ExecutionException
    {
        final List<String> presentTerms = Arrays.asList(TERM_1, TERM_2);
        final List<String> absentTerms = Collections.singletonList(TERM_3);
        final GenePanel genePanel = mock(GenePanel.class);

        when(genePanel.size()).thenReturn(2);
        when(genePanel.toJSON()).thenReturn(new JSONObject());
        when(this.genePanelLoader.get(presentTerms)).thenReturn(genePanel);
        final Response response = this.component.getGeneCountsFromPhenotypes(presentTerms, absentTerms, 1, -1, 1);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(new JSONObject().put(TOTAL_PAGES_LABEL, 1).put(REQ_NO, 1).similar(response.getEntity()));
    }

    @Test
    public void getGeneCountsFromPhenotypesReturnActualNumberOfResultsIfNumResultsLarger() throws ExecutionException
    {
        final List<String> presentTerms = Collections.singletonList(TERM_1);
        final List<String> absentTerms = Collections.singletonList(TERM_3);
        final GenePanel genePanel = mock(GenePanel.class);

        when(genePanel.size()).thenReturn(1);
        when(genePanel.toJSON()).thenReturn(new JSONObject());
        when(this.genePanelLoader.get(presentTerms)).thenReturn(genePanel);
        final Response response = this.component.getGeneCountsFromPhenotypes(presentTerms, absentTerms, 1, 2, 1);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(new JSONObject().put(TOTAL_PAGES_LABEL, 1).put(REQ_NO, 1).toString(),
            response.getEntity().toString());
    }

    @Test
    public void getGeneCountsFromPhenotypesReturnRequestedNumResults() throws ExecutionException
    {
        final List<String> presentTerms = Arrays.asList(TERM_1, TERM_2);
        final List<String> absentTerms = Collections.singletonList(TERM_3);
        final GenePanel genePanel = mock(GenePanel.class);

        when(genePanel.size()).thenReturn(2);
        when(genePanel.toJSON(0, 1)).thenReturn(new JSONObject());
        when(this.genePanelLoader.get(presentTerms)).thenReturn(genePanel);
        final Response response = this.component.getGeneCountsFromPhenotypes(presentTerms, absentTerms, 1, 1, 1);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(new JSONObject().put(TOTAL_PAGES_LABEL, 2).put(REQ_NO, 1).toString(),
            response.getEntity().toString());
    }

    @Test
    public void getGeneCountsFromPhenotypesDefaultReqNo() throws ExecutionException
    {
        final List<String> presentTerms = Collections.singletonList(TERM_1);
        final List<String> absentTerms = Collections.singletonList(TERM_3);
        final GenePanel genePanel = mock(GenePanel.class);

        when(genePanel.size()).thenReturn(1);
        when(genePanel.toJSON()).thenReturn(new JSONObject());
        when(this.genePanelLoader.get(presentTerms)).thenReturn(genePanel);
        final Response response = this.component.getGeneCountsFromPhenotypes(presentTerms, absentTerms, 1, 1, 0);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(new JSONObject().put(TOTAL_PAGES_LABEL, 1).put(REQ_NO, 0).toString(),
            response.getEntity().toString());
    }
}
