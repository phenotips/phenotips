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

import org.phenotips.services.annotations.ncr.internal.utils.NCRMatch;
import org.phenotips.services.annotations.ncr.internal.utils.NCRResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A simple servlet for translating the requests generated by clinical-text-analysis-extension, to the format expected
 * by the Neural Concept Recognizer service, and translating the received responses to the format expected by the
 * clinical-text-analysis-extension.
 *
 * @version $Id$
 * @since 1.4
 */
public class NCRServlet extends HttpServlet
{
    /** The logging object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NCRServlet.class);

    /** The jackson object mapper. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The Neural Concept Recognizer service url. */
    private static final String SERVICE_URL = "https://ncr.ccm.sickkids.ca/curr/annotate/";

    private static final String TEXT_LABEL = "text";

    private static final String CONTENT_LABEL = "content";

    private static final String APPLICATION_JSON = "application/json";

    private static final long serialVersionUID = -8185410465881809448L;

    /**
     * Gets the service URL for the Neural Concept Recognizer.
     *
     * @return the service URL as string
     */
    public String getServiceUrl()
    {
        return SERVICE_URL;
    }

    /**
     * Translates and forwards the post request from the clinical-text-analysis-extension. Writes the response expected
     * by the clinical-text-analysis-extension.
     *
     * @param request the {@link HttpServletRequest request} object
     * @param response the {@link HttpServletResponse response} object
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
    {
        try {
            // Translate and forward the request to the NCR service.
            final String text = forwardPost(request);
            // Set content type for response.
            response.setContentType(APPLICATION_JSON);
            // Get response writer.
            final PrintWriter writer = response.getWriter();
            // Forward the retrieved and translated response.
            writer.append(text);
        } catch (final URISyntaxException e) {
            LOGGER.error("Invalid URI syntax: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (final MalformedURLException e) {
            LOGGER.error("The NCR service url [{}] is invalid: {}", SERVICE_URL, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (final IOException e) {
            LOGGER.error("Error with the request for or response from [{}]: {}", SERVICE_URL, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (final Exception e) {
            LOGGER.error("An unexpected error occurred: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Translates the request parameters to the format expected by the NCR service located at {@link #SERVICE_URL}.
     *
     * @param request the incoming request:
     *     Content-Type: application/x-www-form-urlencoded
     *     Form param: "content=text to annotate"
     * @return a list of name-value pairs, as json string, in format expected by the NCR service, e.g.
     *     {"text":"text to annotate"}
     * @throws JsonProcessingException if there was an error converting request data to json string
     */
    @Nonnull
    private String translateRequest(@Nonnull final HttpServletRequest request) throws JsonProcessingException
    {
        final String text = request.getParameter(CONTENT_LABEL);
        final Map<String, String> params = new HashMap<>();
        params.put(TEXT_LABEL, StringUtils.defaultIfBlank(text, StringUtils.EMPTY));
        return MAPPER.writeValueAsString(params);
    }

    /**
     * Translates the response returned by service to the desired format.
     *
     * @param response the response from the NCR service located at {@link #SERVICE_URL}, as string; e.g.
     *     {
     *       "matches": [
     *         {
     *           "end":52,
     *           "hp_id":"HP:0001627",
     *           "names":["Abnormal heart morphology", ... ,"Congenital heart defects"],
     *           "score":"0.696756",
     *           "start":37
     *         },
     *         {
     *           ...
     *         }
     *       ]
     *     }
     * @return the adapted response, as string; e.g.
     *     [{"end":52,"token":{"id":"HP:0001627"},"start":37}, ...]
     * @throws IOException if {@code response} could not be adapted to the desired format
     */
    @Nonnull
    private String translateResponse(@Nonnull final String response) throws IOException
    {
        final List<NCRMatch> matches = MAPPER.readValue(response, NCRResponse.class).getMatches();
        return MAPPER.writer().writeValueAsString(matches);
    }

    /**
     * Forwards the {@code request} parameters to the {@link #SERVICE_URL}.
     *
     * @param request the {@link HttpServletRequest} containing request parameters
     * @return the response, as string
     * @throws JsonProcessingException if the provided matches cannot be written as string
     * @throws MalformedURLException if the {@link #SERVICE_URL} is malformed
     * @throws URISyntaxException if the uri syntax is invalid
     * @throws IOException if the forwarded post request cannot be executed, or if the json cannot be read into the
     *                     required object structure
     */
    @Nonnull
    private String forwardPost(@Nonnull final HttpServletRequest request)
        throws URISyntaxException, IOException
    {
        final String requestStr = translateRequest(request);
        final URI uri = new URL(getServiceUrl()).toURI();
        final String responseStr = getPostRequest(uri)
            .bodyString(requestStr, ContentType.APPLICATION_JSON)
            .execute()
            .returnContent()
            .asString();
        return translateResponse(responseStr);
    }

    /**
     * Gets the post {@link Request} object. Visible for testing purposes.
     *
     * @param uri the {@link URI} object
     * @return the {@link Request}
     */
    @Nonnull
    Request getPostRequest(@Nonnull final URI uri)
    {
        return Request.Post(uri);
    }
}
