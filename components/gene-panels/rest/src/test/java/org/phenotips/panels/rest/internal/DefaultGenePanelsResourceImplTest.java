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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGenePanelsResourceImpl}.
 *
 * @version $Id$
 * @since 1.3
 */
public class DefaultGenePanelsResourceImplTest
{
    private static final String START_PAGE_LABEL = "startpage";

    private static final String TOTAL_PAGES_LABEL = "totalpages";

    private static final String RESULTS_LIMIT_LABEL = "limit";

    private static final String PRESENT_TERMS_LABEL = "present-term";

    private static final String ABSENT_TERMS_LABEL = "absent-term";

    private static final String REQ_NO = "reqNo";

    private static final String NO_CONTEXT_PROVIDED_MSG = "No content provided.";

    private static final String TERM_1 = "HP:001";

    private static final String TERM_2 = "HP:002";

    private static final String TERM_3 = "HP:003";

    @Rule
    public MockitoComponentMockingRule<GenePanelsResource> mocker =
        new MockitoComponentMockingRule<>(DefaultGenePanelsResourceImpl.class);

    @Mock
    private GenePanel genePanel;

    private Logger logger;

    private GenePanelsResource component;

    private GenePanelLoader genePanelLoader;

    @Mock
    private Request request;

    @Before
    public void setUp() throws ComponentLookupException, ExecutionException
    {
        MockitoAnnotations.initMocks(this);
        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager compManager = this.mocker.getInstance(ComponentManager.class, "context");
        final Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        final XWikiContext context = provider.get();
        when(compManager.getInstance(Execution.class)).thenReturn(execution);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(context);

        this.component = this.mocker.getComponentUnderTest();
        this.genePanelLoader = this.mocker.getInstance(GenePanelLoader.class);
        this.logger = this.mocker.getMockedLogger();

        Container container = this.mocker.getInstance(Container.class);
        when(container.getRequest()).thenReturn(this.request);
        // Some defaults, can be overridden later
        when(this.genePanelLoader.get(any(PanelData.class))).thenReturn(this.genePanel);
        when(this.request.getProperty(START_PAGE_LABEL)).thenReturn("1");
        when(this.request.getProperty(RESULTS_LIMIT_LABEL)).thenReturn("20");
        when(this.request.getProperty(REQ_NO)).thenReturn("1");
    }

    @Test
    public void getGeneCountsFromPhenotypesNoPresentNorAbsentTermsProvided()
    {
        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Collections.emptyList());
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.emptyList());
        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).error(NO_CONTEXT_PROVIDED_MSG);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesAbsentTermsProvidedPresentTermsGenerateNoContent() throws ExecutionException
    {
        final List<Object> presentTerms = Collections.emptyList();
        final List<Object> absentTerms = Collections.singletonList(TERM_1);
        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(presentTerms);
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(absentTerms);
        when(this.genePanelLoader.get(any(PanelData.class)))
            .thenThrow(new ExecutionException(new Throwable()));
        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).warn("No content associated with [present-term: {}, absent-term: {}, rejected-gene: {}].",
            new HashSet<>(presentTerms), new HashSet<>(absentTerms), Collections.emptySet());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesStartPageNegativeResultsInBadRequest()
    {
        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_2));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_1));
        when(this.request.getProperty(START_PAGE_LABEL)).thenReturn("-20");
        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).error("The requested [{}: {}] is out of bounds.", START_PAGE_LABEL, -20);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesStartPageOutOfBounds()
    {
        when(this.genePanel.size()).thenReturn(1);
        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_2));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_1));
        when(this.request.getProperty(START_PAGE_LABEL)).thenReturn("2");

        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).error("The requested [{}: {}] is out of bounds.", START_PAGE_LABEL, 2);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesReturnEverythingIfNumResultsIsNegative()
    {
        when(this.genePanel.size()).thenReturn(2);
        when(this.genePanel.toJSON()).thenReturn(new JSONObject());

        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Arrays.asList(TERM_1, TERM_2));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_3));
        when(this.request.getProperty(RESULTS_LIMIT_LABEL)).thenReturn("-1");

        final Response response = this.component.getGeneCountsFromPhenotypes();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(new JSONObject().put(TOTAL_PAGES_LABEL, 1).put(REQ_NO, 1).similar(response.getEntity()));
    }

    @Test
    public void getGeneCountsFromPhenotypesReturnActualNumberOfResultsIfNumResultsLarger()
    {
        when(this.genePanel.size()).thenReturn(1);
        when(this.genePanel.toJSON()).thenReturn(new JSONObject());

        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_1));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_3));
        when(this.request.getProperty(RESULTS_LIMIT_LABEL)).thenReturn("2");

        final Response response = this.component.getGeneCountsFromPhenotypes();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(new JSONObject().put(TOTAL_PAGES_LABEL, 1).put(REQ_NO, 1).toString(),
            response.getEntity().toString());
    }

    @Test
    public void getGeneCountsFromPhenotypesReturnRequestedNumResults()
    {
        when(this.genePanel.size()).thenReturn(2);
        when(this.genePanel.toJSON(0, 1)).thenReturn(new JSONObject());

        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Arrays.asList(TERM_1, TERM_2));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_3));

        when(this.request.getProperty(RESULTS_LIMIT_LABEL)).thenReturn("1");

        final Response response = this.component.getGeneCountsFromPhenotypes();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(new JSONObject().put(TOTAL_PAGES_LABEL, 2).put(REQ_NO, 1).toString(),
            response.getEntity().toString());
    }

    @Test
    public void getGeneCountsFromPhenotypesDefaultReqNo()
    {
        when(this.genePanel.size()).thenReturn(1);
        when(this.genePanel.toJSON()).thenReturn(new JSONObject());

        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_1));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.singletonList(TERM_3));
        when(this.request.getProperty(REQ_NO)).thenReturn("0");

        final Response response = this.component.getGeneCountsFromPhenotypes();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(new JSONObject().put(TOTAL_PAGES_LABEL, 1).put(REQ_NO, 0).toString(),
            response.getEntity().toString());
    }
}
