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
package org.phenotips.variant.internal;

import org.phenotips.variant.VariantManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Provides access to the available variant tools and their terms to public scripts.
 *
 * @version $Id$
 * @since 1.2M4
 */
@Unstable
@Component
@Named("hgvs")
@Singleton
public class HGVSVariantManager implements VariantManager
{
    private static final String SERVICE_URL = "http://mutalyzer.nl/json/runMutalyzer?variant=";

    /** Performs HTTP requests to the remote REST service. */
    private final CloseableHttpClient client = HttpClients.createSystem();

    @Inject
    private Logger logger;

    /**
     * Validate the HGVS variant description.
     *
     * @param id variant id to validate
     * @return json respond in a form of {"valid": <Boolean>, "messages": []}
     */
    @Override
    public JSONObject validateVariant(String id)
    {
        String safeID;
        try {
            safeID = URLEncoder.encode(id, Consts.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            safeID = id.replaceAll("\\s", "");
            this.logger.warn("Could not find the encoding: {}", Consts.UTF_8.name());
        }
        HttpGet method = new HttpGet(SERVICE_URL + safeID);
        method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
            String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
            return responseJSON;
        } catch (IOException | JSONException ex) {
            this.logger.warn("Failed to validate HGVS variant symbol id: {}", ex.getMessage());
        }
        return new JSONObject(true);
    }

}
