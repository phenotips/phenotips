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
package org.phenotips.services.annotations.ncr.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NCRServletTest
{
    private static final String CONTENT_LABEL = "content";

    private static final String SERVICE_URL = "https://ncr.ccm.sickkids.ca/curr/annotate/";

    private static final String CONTENT = "The paitient was diagnosed with both cardiac disease and renal cancer.";

    private static final String APPLICATION_JSON = "application/json";

    private static final String NCR_RESPONSE = "{\"matches\":[{\"end\":52,\"hp_id\":\"HP:0001627\",\"names\":"
        + "[\"Abnormal heart morphology\",\"Abnormality of cardiac morphology\",\"Abnormality of the heart\",\"Cardiac "
        + "abnormality\",\"Cardiac anomalies\",\"Congenital heart defect\",\"Congenital heart defects\"],\"score\":"
        + "\"0.696756\",\"start\":37},{\"end\":69,\"hp_id\":\"HP:0009726\",\"names\":[\"Renal neoplasm\",\"Kidney "
        + "cancer\",\"Neoplasia of the kidneys\",\"Renal neoplasia\",\"Renal tumors\"],\"score\":\"0.832163\","
        + "\"start\":57}]}";

    private static final String EMPTY_NCR_RESPONSE = "{\"matches\":[]}";

    private static final String FORMATTED_RESPONSE = "[{\"end\":52,\"token\":{\"id\":\"HP:0001627\"},\"start\":37},"
        + "{\"end\":69,\"token\":{\"id\":\"HP:0009726\"},\"start\":57}]";

    @Mock
    private Request postRequest;

    @Mock
    private Response postResponse;

    @Mock
    private Content content;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private PrintWriter writer;

    private NCRServlet component;

    @Before
    public void setUp() throws IOException
    {
        MockitoAnnotations.initMocks(this);

        final NCRServlet ncrServlet = new NCRServlet();
        this.component = spy(ncrServlet);

        doReturn(this.postRequest).when(this.component).getPostRequest(any(URI.class));
        when(this.postRequest.bodyString(anyString(), any())).thenReturn(this.postRequest);
        when(this.postRequest.execute()).thenReturn(this.postResponse);
        when(this.postResponse.returnContent()).thenReturn(this.content);
        when(this.content.asString()).thenReturn(NCR_RESPONSE);

        when(this.request.getParameter(CONTENT_LABEL)).thenReturn(CONTENT);
        when(this.response.getWriter()).thenReturn(this.writer);
    }

    @Test
    public void doPostWhenRequestHasNullContent()
    {
        when(this.request.getParameter(CONTENT_LABEL)).thenReturn(null);
        when(this.content.asString()).thenReturn(EMPTY_NCR_RESPONSE);
        this.component.doPost(this.request, this.response);
        verify(this.postRequest, times(1)).bodyString("{\"text\":\"\"}", ContentType.APPLICATION_JSON);
        verify(this.response, times(1)).setContentType(APPLICATION_JSON);
        verify(this.writer, times(1)).append("[]");
    }

    @Test
    public void doPostWhenRequestHasEmptyContent()
    {
        when(this.request.getParameter(CONTENT_LABEL)).thenReturn(StringUtils.EMPTY);
        when(this.content.asString()).thenReturn(EMPTY_NCR_RESPONSE);
        this.component.doPost(this.request, this.response);
        verify(this.postRequest, times(1)).bodyString("{\"text\":\"\"}", ContentType.APPLICATION_JSON);
        verify(this.response, times(1)).setContentType(APPLICATION_JSON);
        verify(this.writer, times(1)).append("[]");
    }

    @Test
    public void doPostWhenRequestHasBlankContent()
    {
        when(this.request.getParameter(CONTENT_LABEL)).thenReturn(StringUtils.SPACE);
        when(this.content.asString()).thenReturn(EMPTY_NCR_RESPONSE);
        this.component.doPost(this.request, this.response);
        verify(this.postRequest, times(1)).bodyString("{\"text\":\"\"}", ContentType.APPLICATION_JSON);
        verify(this.response, times(1)).setContentType(APPLICATION_JSON);
        verify(this.writer, times(1)).append("[]");
    }

    @Test
    public void doPostWhenRequestHasContent()
    {
        when(this.content.asString()).thenReturn(NCR_RESPONSE);
        this.component.doPost(this.request, this.response);
        verify(this.postRequest, times(1)).bodyString("{\"text\":\"" + CONTENT + "\"}", ContentType.APPLICATION_JSON);
        verify(this.response, times(1)).setContentType(APPLICATION_JSON);
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(this.writer, times(1)).append(captor.capture());
        final String writtenResponse = captor.getValue();
        Assert.assertTrue(new JSONArray(writtenResponse).similar(new JSONArray(FORMATTED_RESPONSE)));
    }

    @Test
    public void doPostWithInvalidURLFormatResultsInErrorCode()
    {
        doReturn("wrong").when(this.component).getServiceUrl();
        this.component.doPost(this.request, this.response);
        verify(this.response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void doPostWithInvalidURIFormatResultsInErrorCode()
    {
        doReturn(SERVICE_URL + "%").when(this.component).getServiceUrl();
        this.component.doPost(this.request, this.response);
        verify(this.response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void doPostIOExceptionResultsInErrorCode() throws IOException
    {
        when(this.postRequest.execute()).thenThrow(new IOException());
        this.component.doPost(this.request, this.response);
        verify(this.response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void doPostExceptionResultsInErrorCode() throws IOException
    {
        when(this.postResponse.returnContent()).thenThrow(new RuntimeException());
        this.component.doPost(this.request, this.response);
        verify(this.response, times(1)).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
