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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

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
    private static final ContentType REQUEST_CONTENT_TYPE = ContentType.create(
        ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), Consts.UTF_8);

    private static final String ENCODING = "UTF-8";

    private static final double POLIPHEN_THRESHOLD = 0.2;

    private static final double QUALITY_THRESHOLD = 30;

    private static final double THOUSAND_GENOMES_THRESHOLD = 0.01;

    private static final String PROJECT_MANAGER = "ProjectManager";

    private static final String VARIANT_MANAGER = "VariantManager";

    private static final String REQUEST_PARAMETER = "json=";

    private static final List<String> IGNORED_EFFECTS = Arrays.asList("ncRNA_INTRONIC", "UPSTREAM", "DOWNSTREAM",
        "INTERGENIC", "UTR3", "UTR5", "SYNONYMOUS", "INTRONIC");

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** HTTP client used for communicating with the MedSavant server. */
    private final CloseableHttpClient client = HttpClients.createSystem();

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
        HttpPost method = null;
        try {
            PatientData<String> identifiers = patient.getData("identifiers");
            String eid = identifiers.get("external_id");
            String url = getMethodURL(VARIANT_MANAGER, "getVariantCountForDNAIDs");
            method = new HttpPost(url);
            JSONArray parameters = new JSONArray();
            parameters.add(this.projectID);
            parameters.add(0);
            parameters.add(new JSONArray());
            JSONArray ids = new JSONArray();
            ids.add(eid);
            parameters.add(ids);
            for (Integer refID : this.referenceIDs) {
                parameters.set(1, refID);
                String body = REQUEST_PARAMETER + URLEncoder.encode(parameters.toString(), ENCODING);
                method.setEntity(new StringEntity(body, REQUEST_CONTENT_TYPE));
                try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                    String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                    Integer count = Integer.valueOf(response);
                    if (count > 0) {
                        return true;
                    }
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
        HttpPost method = null;
        try {
            MultipartEntityBuilder data = MultipartEntityBuilder.create();
            PatientData<String> identifiers = patient.getData("identifiers");
            String url = getMethodURL("UploadManager", "upload");
            String eid = identifiers.get("external_id");
            XWikiContext context = Utils.getContext();
            XWikiDocument doc = context.getWiki().getDocument(patient.getDocument(), context);
            method = new HttpPost(url);

            boolean hasData = false;
            for (XWikiAttachment attachment : doc.getAttachmentList()) {
                if (StringUtils.endsWithIgnoreCase(attachment.getFilename(), ".vcf")
                    && isCorrectVCF(attachment, eid, context)) {
                    data.addBinaryBody(patient.getId() + ".vcf", attachment.getContentInputStream(context));
                    hasData = true;
                }
            }
            if (hasData) {
                method.setEntity(data.build());
                this.client.execute(method).close();
                return true;
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to upload VCF for patient [{}]: {}", patient.getDocument(), ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return false;
    }

    @Override
    public List<JSONArray> getPatientVariants(Patient patient)
    {
        HttpPost method = null;
        List<JSONArray> result = new LinkedList<JSONArray>();
        try {
            PatientData<String> identifiers = patient.getData("identifiers");
            String eid = identifiers.get("external_id");
            String url = getMethodURL(VARIANT_MANAGER, "getVariants");
            method = new HttpPost(url);
            JSONArray parameters = new JSONArray();
            // 1: Project ID
            parameters.add(this.projectID);
            // 2: Reference ID, will be filled in later, in the loop
            parameters.add(0);

            JSONArray conditions = new JSONArray();
            JSONArray dnaIDConditions = new JSONArray();
            dnaIDConditions.add(makeCondition(0, "BinaryCondition", "equalTo", "dna_id", eid));
            conditions.add(dnaIDConditions);
            // 3: Conditions
            parameters.add(conditions);

            // 4: Start at
            parameters.add(-1);
            // 5: Max number of results -> all
            parameters.add(-1);
            for (Integer refID : this.referenceIDs) {
                parameters.set(1, refID);
                parameters.getJSONArray(2).getJSONArray(0).getJSONObject(0).put("refId", refID);
                String body = REQUEST_PARAMETER + URLEncoder.encode(parameters.toString(), ENCODING);
                method.setEntity(new StringEntity(body, REQUEST_CONTENT_TYPE));
                try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                    String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                    JSONArray results = (JSONArray) JSONSerializer.toJSON(response);
                    result.addAll(results);
                }
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to get variants for patient [{}]: {}", patient.getDocument(), ex.getMessage(), ex);
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
        HttpPost method = null;
        List<JSONArray> result = new LinkedList<JSONArray>();
        try {
            for (Integer refID : this.referenceIDs) {
                PatientData<String> identifiers = patient.getData("identifiers");
                String eid = identifiers.get("external_id");
                String url = getMethodURL(VARIANT_MANAGER, "getVariants");
                method = new HttpPost(url);
                JSONArray parameters = new JSONArray();
                // 1: Project ID
                parameters.add(this.projectID);
                // 2: Reference ID
                parameters.add(refID);
                // 3: Conditions
                parameters.add(getFilteredVariantsConditions(refID, eid));
                // 4: Start at
                parameters.add(-1);
                // 5: Max number of results -> all
                parameters.add(-1);
                String body = REQUEST_PARAMETER + URLEncoder.encode(parameters.toString(), ENCODING);
                method.setEntity(new StringEntity(body, REQUEST_CONTENT_TYPE));
                try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                    String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                    JSONArray results = (JSONArray) JSONSerializer.toJSON(response);
                    result.addAll(results);
                }
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to get filtered variants for patient [{}]: {}", patient.getDocument(),
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
     */
    private String getMethodURL(String service, String method)
    {
        String result = this.configuration.getProperty("phenotips.medsavant.baseUrl",
            "http://localhost:8080/medsavant-json-client/");
        return result + service + "/" + method;
    }

    private Integer getProjectID()
    {
        HttpPost method = null;
        String projectName = this.configuration.getProperty("phenotips.medsavant.projectName", "pc");
        try {
            String url = getMethodURL(PROJECT_MANAGER, "getProjectID");
            method = new HttpPost(url);
            JSONArray parameters = new JSONArray();
            parameters.add(projectName);
            String body = REQUEST_PARAMETER + URLEncoder.encode(parameters.toString(), ENCODING);
            method.setEntity(new StringEntity(body, REQUEST_CONTENT_TYPE));
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                Integer id = Integer.valueOf(response);
                return (id >= 0) ? id : null;
            }
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
        HttpPost method = null;
        try {
            String url = getMethodURL(PROJECT_MANAGER, "getReferenceIDsForProject");
            method = new HttpPost(url);
            JSONArray parameters = new JSONArray();
            parameters.add(this.projectID);
            String body = REQUEST_PARAMETER + URLEncoder.encode(parameters.toString(), ENCODING);
            method.setEntity(new StringEntity(body, REQUEST_CONTENT_TYPE));
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                JSONArray ids = (JSONArray) JSONSerializer.toJSON(response);
                JsonConfig config = new JsonConfig();
                config.setCollectionType(Set.class);
                return JSONArray.toCollection(ids, config);
            }
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
        HttpPost method = null;
        try {
            String url = getMethodURL("AnnotationManager", "getAnnotationFormats");
            method = new HttpPost(url);
            JSONArray parameters = new JSONArray();
            parameters.add(this.projectID);
            parameters.add(refID);
            String body = REQUEST_PARAMETER + URLEncoder.encode(parameters.toString(), ENCODING);
            method.setEntity(new StringEntity(body, REQUEST_CONTENT_TYPE));
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
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

    private JSONArray getFilteredVariantsConditions(Integer refID, String patientId)
    {
        String thousandGColumn = getAnnotationColumnName(refID, "1000g2012apr_all", "Score");
        Collection<JSONObject> thousandGConditions = new LinkedList<JSONObject>();
        thousandGConditions.add(makeCondition(refID, "BinaryCondition", "lessThan", thousandGColumn,
            THOUSAND_GENOMES_THRESHOLD, true));
        thousandGConditions.add(makeCondition(refID, "UnaryCondition", "isNull", thousandGColumn));

        String poliphenColumn = getAnnotationColumnName(refID, "ljb2_pp2hvar", "Score");
        Collection<JSONObject> poliphenConditions = new LinkedList<JSONObject>();
        poliphenConditions.add(makeCondition(refID, "BinaryCondition", "greaterThan",
            poliphenColumn, POLIPHEN_THRESHOLD, true));
        poliphenConditions.add(makeCondition(refID, "UnaryCondition", "isNull", poliphenColumn));

        JSONArray conditions = new JSONArray();
        for (JSONObject thousandGCondition : thousandGConditions) {
            for (JSONObject poliphenCondition : poliphenConditions) {
                JSONArray conditionsRow = new JSONArray();
                conditionsRow.add(makeCondition(refID, "BinaryCondition", "equalTo", "dna_id", patientId));
                conditionsRow.add(makeCondition(refID, "BinaryCondition", "greaterThan", "qual",
                    QUALITY_THRESHOLD, true));
                for (String effect : IGNORED_EFFECTS) {
                    conditionsRow.add(makeCondition(refID, "BinaryCondition", "notEqualTo", "effect", effect));
                }
                conditionsRow.add(thousandGCondition);
                conditionsRow.add(poliphenCondition);
                conditions.add(conditionsRow);
            }
        }

        return conditions;
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

    private boolean isCorrectVCF(XWikiAttachment attachment, String eid, XWikiContext context)
        throws XWikiException, IOException
    {
        BufferedReader in =
            new BufferedReader(new InputStreamReader(attachment.getContentInputStream(context),
                XWiki.DEFAULT_ENCODING));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("##")) {
                // Still in the meta, go on
                continue;
            } else if (!line.startsWith("#CHROM")) {
                // Actual data, we're past the meta but didn't encounter the header, strange...
                // Malformed file, abandon
                break;
            }
            String[] fields = line.split("\t");
            if (fields.length != 10 || !StringUtils.equals(eid, fields[9])) {
                // Wrong sample ID or more than one sample, bail out
                break;
            }
            return true;
        }
        return false;
    }
}
