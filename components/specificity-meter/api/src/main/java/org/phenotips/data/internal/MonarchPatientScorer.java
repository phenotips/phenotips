/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;

import org.xwiki.component.annotation.Component;

import java.io.IOException;
import java.util.Date;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Patient scorer that uses the remote service offered by the MONARCH initiative.
 *
 * @version $Id$
 * @since 1.0M12
 */
@Component
@Named("monarch")
@Singleton
public class MonarchPatientScorer implements PatientScorer
{
    /** The HTTP client used for contacting the MONARCH server. */
    private CloseableHttpClient client = HttpClients.createDefault();

    @Override
    public PatientSpecificity getSpecificity(Patient patient)
    {
        double score = getScore(patient);
        if (score != -1) {
            return new PatientSpecificity(score, new Date(), "monarchinitiative.org");
        }
        return null;
    }

    @Override
    public double getScore(Patient patient)
    {
        if (patient.getFeatures().isEmpty()) {
            return 0.0;
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
                    features.add(featureObj);
                }
            }
            data.put("features", features);

            HttpGet method =
                new HttpGet(new URIBuilder("http://monarchinitiative.org/score").addParameter("annotation_profile",
                    data.toString()).build());
            response = this.client.execute(method);
            JSONObject score = (JSONObject) JSONSerializer.toJSON(IOUtils.toString(response.getEntity().getContent()));
            if (!score.isNullObject()) {
                return score.getDouble("scaled_score");
            }
        } catch (Exception ex) {
            // Just return failure below
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
}
