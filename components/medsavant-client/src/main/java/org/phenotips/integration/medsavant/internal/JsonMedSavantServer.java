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
package org.phenotips.integration.medsavant.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.integration.medsavant.MedSavantServer;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

/**
 * Communication via HTTP requests with a LIMS server, configured in the wiki preferences via
 * {@code PhenoTips.LimsAuthServer} objects.
 * 
 * @version $Id$
 * @since 1.0M11
 */
@Component
@Singleton
public class JsonMedSavantServer implements MedSavantServer, Initializable
{
    private static final double POLIPHEN_THRESHOLD = 0.2;

    private static final double QUALITY_THRESHOLD = 30;

    private static final double THOUSAND_GENOMES_THRESHOLD = 0.01;

    private static final String PROJECT_MANAGER = "ProjectManager";

    private static final List<String> IGNORED_EFFECTS = Arrays.asList("ncRNA_INTRONIC", "UPSTREAM", "DOWNSTREAM",
        "INTERGENIC", "UTR3", "UTR5", "SYNONYMOUS", "INTRONIC");

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** HTTP client used for communicating with the MedSavant server. */
    private final HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());

    /** Provides access to the configuration, where the location to the MedSavant server and project is specified. */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * The identifier of the project used by this instance of PhenoTips. Configured in
     * {@code phenotips.medsavant.projectName}
     */
    private Integer projectID;

    /** The list of reference genomes available in the MedSavant database. */
    private Collection<Integer> referenceIDs;

    private Map<String, String> annotationColumns = new HashMap<String, String>();

    @Override
    public void initialize() throws InitializationException
    {
        this.projectID = getProjectID();
        if (this.projectID == null) {
            throw new InitializationException("Invalid project configured,"
                + " please make sure MedSavant is properly running and the right project name"
                + " is specified in xwiki.properties under phenotips.medsavant.projectName");
        }

        this.referenceIDs = getReferenceIDs();
        if (this.referenceIDs == null || this.referenceIDs.isEmpty()) {
            throw new InitializationException("Invalid project configured, no reference genomes are loaded");
        }
    }

    @Override
    public boolean hasVCF(Patient patient)
    {
        PostMethod method = null;
        try {
            PatientData<ImmutablePair<String, String>> identifiers = patient.getData("identifiers");
            String eid = identifiers.get(0).getValue();
            String url = getMethodURL("VariantManager", "getVariantCountForDNAIDs");
            method = new PostMethod(url);
            JSONArray parameters = new JSONArray();
            parameters.add(this.projectID);
            parameters.add(0);
            parameters.add(new JSONArray());
            JSONArray ids = new JSONArray();
            ids.add(eid);
            parameters.add(ids);
            for (Integer refID : this.referenceIDs) {
                parameters.set(1, refID);
                String body = "json=" + URLEncoder.encode(parameters.toString(), XWiki.DEFAULT_ENCODING);
                method.setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE,
                    XWiki.DEFAULT_ENCODING));
                this.client.executeMethod(method);
                String response = method.getResponseBodyAsString();
                Integer count = Integer.valueOf(response);
                if (count > 0) {
                    return true;
                }
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to get the number of variants for patient [{}]: {}", patient.getDocument(),
                ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return false;
    }

    @Override
    public boolean uploadVCF(Patient patient)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<JSONArray> getPatientVariants(Patient patient)
    {
        PostMethod method = null;
        List<JSONArray> result = new LinkedList<JSONArray>();
        try {
            PatientData<ImmutablePair<String, String>> identifiers = patient.getData("identifiers");
            String eid = identifiers.get(0).getValue();
            String url = getMethodURL("VariantManager", "getVariants");
            method = new PostMethod(url);
            JSONArray parameters = new JSONArray();
            parameters.add(this.projectID); // Project ID
            parameters.add(0); // Reference ID, will be filled in later, in the loop

            JSONArray conditions = new JSONArray();
            JSONArray dnaIDConditions = new JSONArray();
            dnaIDConditions.add(makeCondition(0, "BinaryCondition", "equalTo", "dna_id", eid));
            conditions.add(dnaIDConditions);
            parameters.add(conditions); // Conditions

            parameters.add(-1); // Start at
            parameters.add(-1); // Max number of results -> all
            for (Integer refID : this.referenceIDs) {
                parameters.set(1, refID);
                parameters.getJSONArray(2).getJSONArray(0).getJSONObject(0).put("refId", refID);
                String body = "json=" + URLEncoder.encode(parameters.toString(), XWiki.DEFAULT_ENCODING);
                method.setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE,
                    XWiki.DEFAULT_ENCODING));
                this.client.executeMethod(method);
                String response = method.getResponseBodyAsString();
                JSONArray results = (JSONArray) JSONSerializer.toJSON(response);
                result.addAll(results);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to get the number of variants for patient [{}]: {}", patient.getDocument(),
                ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return result;
    }

    @Override
    public List<JSONArray> getFilteredVariants(Patient patient)
    {
        PostMethod method = null;
        List<JSONArray> result = new LinkedList<JSONArray>();
        try {
            for (Integer refID : this.referenceIDs) {
                PatientData<ImmutablePair<String, String>> identifiers = patient.getData("identifiers");
                String eid = identifiers.get(0).getValue();
                String url = getMethodURL("VariantManager", "getVariants");
                method = new PostMethod(url);
                JSONArray parameters = new JSONArray();
                parameters.add(this.projectID); // Project ID
                parameters.add(refID); // Reference ID

                Collection<JSONObject> poliphenConditions = new LinkedList<JSONObject>();
                String poliphenColumn = getAnnotationColumnName(refID, "ljb2_pp2hvar", "Score");
                String thousandGColumn = getAnnotationColumnName(refID, "1000g2012apr_all", "Score");
                poliphenConditions.add(makeCondition(refID, "BinaryCondition", "greaterThan",
                    poliphenColumn, POLIPHEN_THRESHOLD, true));
                poliphenConditions.add(makeCondition(refID, "UnaryCondition", "isNull", poliphenColumn));
                JSONArray conditions = new JSONArray();
                for (JSONObject poliphenCondition : poliphenConditions) {
                    JSONArray conditionsRow = new JSONArray();
                    conditionsRow.add(makeCondition(refID, "BinaryCondition", "equalTo", "dna_id", eid));
                    conditionsRow.add(makeCondition(refID, "BinaryCondition", "greaterThan", "qual", QUALITY_THRESHOLD,
                        true));
                    conditionsRow.add(makeCondition(refID, "BinaryCondition", "lessThan", thousandGColumn,
                        THOUSAND_GENOMES_THRESHOLD, true));
                    for (String effect : IGNORED_EFFECTS) {
                        conditionsRow.add(makeCondition(refID, "BinaryCondition", "notEqualTo", "effect", effect));
                    }
                    conditionsRow.add(poliphenCondition);
                    conditions.add(conditionsRow);
                }
                parameters.add(conditions); // Conditions

                parameters.add(-1); // Start at
                parameters.add(-1); // Max number of results -> all
                String body = "json=" + URLEncoder.encode(parameters.toString(), XWiki.DEFAULT_ENCODING);
                this.logger.error(body);
                method.setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE,
                    XWiki.DEFAULT_ENCODING));
                this.client.executeMethod(method);
                String response = method.getResponseBodyAsString();
                JSONArray results = (JSONArray) JSONSerializer.toJSON(response);
                result.addAll(results);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to get the number of variants for patient [{}]: {}", patient.getDocument(),
                ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return result;
    }

    /**
     * Return the base URL of the specified LIMS instance.
     * 
     * @param pn the LIMS instance identifier
     * @param context the current request context
     * @return the configured URL, in the format {@code http://lims.host.name}, or {@code null} if the LIMS instance
     *         isn't registered in the PhenoTips configuration
     * @throws XWikiException if accessing the configuration fails
     */
    private String getMethodURL(String service, String method) throws XWikiException
    {
        String result = this.configuration.getProperty("phenotips.medsavant.baseUrl",
            "http://localhost:8080/medsavant-json-client/");
        return result + service + "/" + method;
    }

    private Integer getProjectID()
    {
        PostMethod method = null;
        String projectName = this.configuration.getProperty("phenotips.medsavant.projectName", "pc");
        try {
            String url = getMethodURL(PROJECT_MANAGER, "getProjectID");
            method = new PostMethod(url);
            JSONArray parameters = new JSONArray();
            parameters.add(projectName);
            String body = "json=" + URLEncoder.encode(parameters.toString(), XWiki.DEFAULT_ENCODING);
            method.setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE,
                XWiki.DEFAULT_ENCODING));
            this.client.executeMethod(method);
            String response = method.getResponseBodyAsString();
            Integer id = Integer.valueOf(response);
            return (id >= 0) ? id : null;
        } catch (Exception ex) {
            this.logger.warn("Failed to get the ID of the project [{}]: {}", projectName, ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Collection<Integer> getReferenceIDs()
    {
        PostMethod method = null;
        try {
            String url = getMethodURL(PROJECT_MANAGER, "getReferenceIDsForProject");
            method = new PostMethod(url);
            JSONArray parameters = new JSONArray();
            parameters.add(this.projectID);
            String body = "json=" + URLEncoder.encode(parameters.toString(), XWiki.DEFAULT_ENCODING);
            method.setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE,
                XWiki.DEFAULT_ENCODING));
            this.client.executeMethod(method);
            String response = method.getResponseBodyAsString();
            JSONArray ids = (JSONArray) JSONSerializer.toJSON(response);
            JsonConfig config = new JsonConfig();
            config.setCollectionType(Set.class);
            return JSONArray.toCollection(ids, config);
        } catch (Exception ex) {
            this.logger.warn("Failed to get the reference IDs: {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return Collections.emptySet();
    }

    private String getAnnotationColumnName(Integer refID, String programName, String subtype)
    {
        String alias = programName + ", " + subtype;
        if (this.annotationColumns.containsKey(alias)) {
            return this.annotationColumns.get(alias);
        }
        PostMethod method = null;
        try {
            String url = getMethodURL("AnnotationManager", "getAnnotationFormats");
            method = new PostMethod(url);
            JSONArray parameters = new JSONArray();
            parameters.add(this.projectID);
            parameters.add(refID);
            String body = "json=" + URLEncoder.encode(parameters.toString(), XWiki.DEFAULT_ENCODING);
            method.setRequestEntity(new StringRequestEntity(body, PostMethod.FORM_URL_ENCODED_CONTENT_TYPE,
                XWiki.DEFAULT_ENCODING));
            this.client.executeMethod(method);
            String response = method.getResponseBodyAsString();
            JSONArray annotations = (JSONArray) JSONSerializer.toJSON(response);
            for (int i = 0; i < annotations.size(); ++i) {
                JSONObject annotation = annotations.getJSONObject(i);
                String program = annotation.getString("program");
                if (!program.startsWith(programName + " ")) {
                    continue;
                }
                JSONArray fields = annotation.getJSONArray("fields");
                for (int j = 0; j < fields.size(); ++j) {
                    String currentAlias = fields.getJSONObject(j).getString("alias");
                    if (StringUtils.equals(currentAlias, alias)) {
                        String name = fields.getJSONObject(j).getString("name");
                        this.annotationColumns.put(alias, name);
                        return name;
                    }
                }
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to get the annotation column for [{}]: {}", programName, ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return "";
    }

    private JSONObject makeCondition(Integer refID, String type, String method, Object... args)
    {
        JSONObject result = new JSONObject();
        result.put("projectId", this.projectID);
        result.put("refId", refID);
        result.put("type", type);
        result.put("method", method);
        JSONArray values = new JSONArray();
        for (Object arg : args) {
            values.add(arg);
        }
        result.put("args", values);
        return result;
    }
}
