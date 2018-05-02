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
import org.phenotips.vocabularies.rest.VocabularyTermsResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultVocabularyTermsResource} class.
 */
public class DefaultVocabularyTermsResourceTest
{
    private static final String ID_FIELD = "id";

    private static final String TERM_1_ID = "term1";

    private static final String TERM_2_ID = "term2";

    private static final String TERM_3_ID = "term3";

    private static final String HPO_NAME = "hpo";

    private static final String TERM_ID = "term-id";

    private static final String LINKS_FIELD = "links";

    private static final String ROWS_FIELD = "rows";

    @Rule
    public MockitoComponentMockingRule<VocabularyTermsResource> mocker =
        new MockitoComponentMockingRule<>(DefaultVocabularyTermsResource.class);

    @Mock
    private UriInfo uriInfo;

    @Mock
    private Request request;

    @Mock
    private Vocabulary vocabulary;

    @Mock
    private VocabularyTerm term1;

    @Mock
    private VocabularyTerm term2;

    @Mock
    private VocabularyTerm term3;

    private VocabularyTermsResource component;

    private VocabularyManager vm;

    private Logger logger;

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

        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "uriInfo", this.uriInfo);

        this.component = this.mocker.getComponentUnderTest();
        this.vm = this.mocker.getInstance(VocabularyManager.class);
        this.logger = this.mocker.getMockedLogger();

        final Autolinker autolinker = this.mocker.getInstance(Autolinker.class);

        when(autolinker.forSecondaryResource(any(Class.class), eq(this.uriInfo))).thenReturn(autolinker);
        when(autolinker.forResource(any(Class.class), eq(this.uriInfo))).thenReturn(autolinker);
        when(autolinker.withActionableResources(any(Class.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(anyString(), anyString())).thenReturn(autolinker);
        when(autolinker.build()).thenReturn(Collections.emptyList());

        when(this.term1.toJSON()).thenReturn(new JSONObject().put(ID_FIELD, TERM_1_ID));
        when(this.term2.toJSON()).thenReturn(new JSONObject().put(ID_FIELD, TERM_2_ID));
        when(this.term3.toJSON()).thenReturn(new JSONObject().put(ID_FIELD, TERM_3_ID));

        when(this.vm.getVocabulary(HPO_NAME)).thenReturn(this.vocabulary);

        final Set<VocabularyTerm> terms = new HashSet<>();
        terms.add(this.term1);
        terms.add(this.term2);
        terms.add(this.term3);

        when(this.vocabulary.getTerms(Arrays.asList(TERM_1_ID, TERM_2_ID, TERM_3_ID))).thenReturn(terms);
        when(this.vocabulary.getIdentifier()).thenReturn(HPO_NAME);

        final Container container = this.mocker.getInstance(Container.class);

        when(container.getRequest()).thenReturn(this.request);
        when(this.request.getProperties(TERM_ID)).thenReturn(Arrays.asList(TERM_1_ID, TERM_2_ID, TERM_3_ID));
    }

    @Test
    public void getTermsNoRequestedVocabulary()
    {
        when(this.vm.getVocabulary(HPO_NAME)).thenReturn(null);
        final Response response = this.component.getTerms(HPO_NAME);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        verify(this.logger, times(1)).error("The requested vocabulary [{}] was not found", HPO_NAME);
    }

    @Test
    public void getTermsNoTermIdsIncluded()
    {
        when(this.request.getProperties(TERM_ID)).thenReturn(Collections.emptyList());
        final Response response = this.component.getTerms(HPO_NAME);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        verify(this.logger, times(1)).info("No content provided.");
    }

    @Test
    public void getTermsRetrievesCorrectData()
    {
        final Response response = this.component.getTerms(HPO_NAME);
        final JSONObject expected = new JSONObject()
            .put(LINKS_FIELD, new JSONArray())
            .put(ROWS_FIELD, new JSONArray()
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_1_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_2_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_3_ID)));

        final JSONObject actual = (JSONObject) response.getEntity();
        Assert.assertEquals(expected.length(), actual.length());
        Assert.assertTrue(expected.getJSONArray(LINKS_FIELD).similar(actual.getJSONArray(LINKS_FIELD)));
        // Rows are unordered since getTerms returns a set.
        Assert.assertEquals(new HashSet<>(expected.getJSONArray(ROWS_FIELD).toList()),
            new HashSet<>(actual.getJSONArray(ROWS_FIELD).toList()));

        verify(this.logger, never()).info(anyString());
        verify(this.logger, never()).warn(anyString());
        verify(this.logger, never()).error(anyString());
    }

    @Test
    public void getTermsIgnoresNulls()
    {
        final List<Object> termIdList = new ArrayList<>();
        termIdList.add(TERM_1_ID);
        termIdList.add(null);
        termIdList.add(TERM_2_ID);
        termIdList.add(TERM_3_ID);

        when(this.request.getProperties(TERM_ID)).thenReturn(termIdList);
        final Response response = this.component.getTerms(HPO_NAME);
        final JSONObject expected = new JSONObject()
            .put(LINKS_FIELD, new JSONArray())
            .put(ROWS_FIELD, new JSONArray()
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_1_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_2_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_3_ID)));

        final JSONObject actual = (JSONObject) response.getEntity();
        Assert.assertEquals(expected.length(), actual.length());
        Assert.assertTrue(expected.getJSONArray(LINKS_FIELD).similar(actual.getJSONArray(LINKS_FIELD)));
        // Rows are unordered since getTerms returns a set.
        Assert.assertEquals(new HashSet<>(expected.getJSONArray(ROWS_FIELD).toList()),
            new HashSet<>(actual.getJSONArray(ROWS_FIELD).toList()));

        verify(this.logger, never()).info(anyString());
        verify(this.logger, never()).warn(anyString());
        verify(this.logger, never()).error(anyString());
    }
}
