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
package org.phenotips.data.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Patient scorer that uses the remote service offered by the MONARCH initiative.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Component
@Named("monarch")
@Singleton
public class MonarchPatientScorer implements PatientScorer, Initializable
{
    private static final String SCORER_NAME = "monarchinitiative.org";

    @Inject
    private Logger logger;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    private String scorerURL;

    /** The HTTP client used for contacting the MONARCH server. */
    private CloseableHttpClient client = HttpClients.createSystem();

    @Inject
    private CacheManager cacheManager;

    private Cache<PatientSpecificity> cache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.scorerURL = this.configuration
                .getProperty("phenotips.patientScoring.monarch.serviceURL", "http://monarchinitiative.org/score");
            CacheConfiguration config = new LRUCacheConfiguration("monarchSpecificityScore", 2048, 3600);
            this.cache = this.cacheManager.createNewCache(config);
        } catch (CacheException ex) {
            throw new InitializationException("Failed to create cache", ex);
        }
    }

    @Override
    public PatientSpecificity getSpecificity(Patient patient)
    {
        String key = getCacheKey(patient);
        PatientSpecificity result = this.cache.get(key);
        if (result == null) {
            double score = getScore(patient);
            if (score != -1.0) {
                // getScore populates the cache
                result = this.cache.get(key);
            }
        }
        return result;
    }

    @Override
    public double getScore(Patient patient)
    {
        String key = getCacheKey(patient);
        PatientSpecificity specificity = this.cache.get(key);
        if (specificity != null) {
            return specificity.getScore();
        }
        if (patient.getFeatures().isEmpty()) {
            this.cache.set(key, new PatientSpecificity(0, now(), SCORER_NAME));
            return 0;
        }
        CloseableHttpResponse response = null;
        try {
            JSONObject data = new JSONObject();
            JSONArray features = new JSONArray();
            for (Feature f : patient.getFeatures()) {
                if (StringUtils.isNotEmpty(f.getId())) {
                    JSONObject featureObj = new JSONObject();
                    featureObj.put("id", f.getId());
                    if (!f.isPresent()) {
                        featureObj.put("isPresent", false);
                    }
                    features.put(featureObj);
                }
            }
            data.put("features", features);

            HttpGet method =
                new HttpGet(new URIBuilder(this.scorerURL).addParameter("annotation_profile",
                    data.toString()).build());
            RequestConfig config = RequestConfig.custom().setSocketTimeout(2000).build();
            method.setConfig(config);
            response = this.client.execute(method);
            JSONObject score = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
            specificity = new PatientSpecificity(score.getDouble("scaled_score"), now(), SCORER_NAME);
            this.cache.set(key, specificity);
            return specificity.getScore();
        } catch (Exception ex) {
            // Just return failure below
            this.logger.error("Failed to compute specificity score for patient [{}] using the monarch server [{}]: {}",
                patient.getDocument(), this.scorerURL, ex.getMessage());
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consumeQuietly(response.getEntity());
                    response.close();
                } catch (IOException ex) {
                    // Not dangerous
                }
            }
        }
        return -1;
    }

    private String getCacheKey(Patient patient)
    {
        StringBuilder result = new StringBuilder();
        for (Feature f : patient.getFeatures()) {
            if (StringUtils.isNotEmpty(f.getId())) {
                if (!f.isPresent()) {
                    result.append('-');
                }
                result.append(f.getId());
            }
        }
        return result.toString();
    }

    private Date now()
    {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime();
    }
}
