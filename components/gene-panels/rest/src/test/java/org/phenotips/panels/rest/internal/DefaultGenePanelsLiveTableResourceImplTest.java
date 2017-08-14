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
import org.phenotips.panels.rest.GenePanelsLiveTableResource;

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
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGenePanelsLiveTableResourceImpl}.
 *
 * @version $Id$
 * @since 1.4
 */
public class DefaultGenePanelsLiveTableResourceImplTest
{
    private static final String HP1 = "HP:1";

    private static final String HP2 = "HP:2";

    private static final String HP3 = "HP:3";

    private static final String HP4 = "HP:4";

    private static final String HP5 = "HP:5";

    private static final String HP6 = "HP:6";

    private static final String REQ_NO_LABEL = "reqNo";

    private static final String OFFSET_LABEL = "offset";

    private static final String GENE_LABEL = "gene1";

    private static final String GENE_DATA_PLACEHOLDER = "geneData";

    private static final String OFFSET = "1";

    private static final String LIMIT = "10";

    private static final String REQ_NO = "5";

    private static final String RESULTS_LIMIT_LABEL = "limit";

    private static final String PRESENT_TERMS_LABEL = "present-term";

    private static final String ABSENT_TERMS_LABEL = "absent-term";

    private static final String REJECTED_GENES_LABEL = "rejected-gene";

    @Rule
    public MockitoComponentMockingRule<GenePanelsLiveTableResource> mocker =
        new MockitoComponentMockingRule<>(DefaultGenePanelsLiveTableResourceImpl.class);

    @Mock
    private GenePanel panel;

    @Mock
    private Request request;

    private Logger logger;

    private GenePanelsLiveTableResource component;

    private GenePanelLoader panelLoader;

    @Before
    public void setUp() throws Exception
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
        this.panelLoader = this.mocker.getInstance(GenePanelLoader.class);
        this.logger = this.mocker.getMockedLogger();

        Container container = this.mocker.getInstance(Container.class);
        when(container.getRequest()).thenReturn(this.request);

        when(this.panelLoader.get(any(PanelData.class))).thenReturn(this.panel);
        when(this.request.getProperty(OFFSET_LABEL)).thenReturn(OFFSET);
        when(this.request.getProperty(RESULTS_LIMIT_LABEL)).thenReturn(LIMIT);
        when(this.request.getProperty(REQ_NO_LABEL)).thenReturn(REQ_NO);
        when(this.panel.size()).thenReturn(5);
    }

    @Test
    public void getGeneCountsFromPhenotypesPresentAndAbsentTermsEmptyResultsInNoContentResponse()
    {
        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Collections.singletonList(null));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.emptyList());
        when(this.request.getProperties(REJECTED_GENES_LABEL)).thenReturn(Collections.emptyList());
        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).error("No content provided.");
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesOffsetOutOfBoundsResultsInBadRequestResponse()
    {
        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Arrays.asList(null, HP1, HP2, HP4, HP5, HP6));
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(Collections.singletonList(HP3));
        when(this.request.getProperties(REJECTED_GENES_LABEL)).thenReturn(Collections.emptyList());
        when(this.request.getProperty(OFFSET_LABEL)).thenReturn("20");
        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).error("The requested [{}: {}] is out of bounds.", "offset", 20);
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesEmptyGenePanelResultsInNoContentResponse() throws ExecutionException
    {
        when(this.panelLoader.get(any(PanelData.class))).thenThrow(new ExecutionException(new Throwable()));

        final List<Object> absentTermList = Arrays.asList(HP1, HP2, HP3);
        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(Collections.emptyList());
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(absentTermList);
        when(this.request.getProperties(REJECTED_GENES_LABEL)).thenReturn(Collections.emptyList());

        final Set<Object> absentTermsSet = new HashSet<>();
        absentTermsSet.addAll(absentTermList);
        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).warn("No content associated with [present-term: {}, absent-term: {}].",
            Collections.emptySet(), absentTermsSet);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void getGeneCountsFromPhenotypesPanelSmallerThanRequestedNumberOfTermsReturnsCorrectResponse()
    {
        final List<Object> presentTermList = Arrays.asList(HP1, HP3, HP4, HP5, HP6);
        final List<Object> absentTermList = Collections.singletonList(HP2);

        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(presentTermList);
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(absentTermList);
        when(this.request.getProperties(REJECTED_GENES_LABEL)).thenReturn(Collections.emptyList());
        when(this.request.getProperty(OFFSET_LABEL)).thenReturn("3");

        final JSONObject testObj = new JSONObject().put(GENE_LABEL, GENE_DATA_PLACEHOLDER);
        when(this.panel.toJSON(2, 5)).thenReturn(testObj);

        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.panel, times(1)).toJSON(2, 5);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertTrue(new JSONObject().put(GENE_LABEL, GENE_DATA_PLACEHOLDER).put(REQ_NO_LABEL, 5)
            .put(OFFSET_LABEL, 3).similar(response.getEntity()));
    }

    @Test
    public void getGeneCountsFromPhenotypesUnexpectedExceptionResultsInInternalErrorResponse() throws ExecutionException
    {
        when(this.panelLoader.get(any(PanelData.class))).thenThrow(new IllegalStateException());

        final List<Object> presentTermList = Arrays.asList(HP1, HP3, HP4, HP5, HP6);
        final List<Object> absentTermList = Collections.singletonList(HP2);

        when(this.request.getProperties(PRESENT_TERMS_LABEL)).thenReturn(presentTermList);
        when(this.request.getProperties(ABSENT_TERMS_LABEL)).thenReturn(absentTermList);
        when(this.request.getProperties(REJECTED_GENES_LABEL)).thenReturn(Collections.emptyList());
        when(this.request.getProperty(OFFSET_LABEL)).thenReturn("3");

        final Response response = this.component.getGeneCountsFromPhenotypes();
        verify(this.logger).error("Unexpected exception while generating gene panel JSON: {}", (String) null);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
}
