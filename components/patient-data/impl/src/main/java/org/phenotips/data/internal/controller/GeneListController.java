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
package org.phenotips.data.internal.controller;

import org.phenotips.Constants;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Handles the patients genes.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { PatientDataController.class })
@Named("gene")
@Singleton
public class GeneListController extends AbstractComplexController<Map<String, String>>
{
    /** The XClass used for storing gene data. */
    private static final EntityReference GENE_CLASS_REFERENCE = new EntityReference("GeneClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String GENES_STRING = "genes";

    private static final String CONTROLLER_NAME = GENES_STRING;

    private static final String GENES_ENABLING_FIELD_NAME = GENES_STRING;

    private static final String GENES_STATUS_ENABLING_FIELD_NAME = "genes_status";

    private static final String GENES_EVIDENCE_ENABLING_FIELD_NAME = "genes_evidence";

    private static final String GENES_COMMENTS_ENABLING_FIELD_NAME = "genes_comments";

    private static final String GENE_KEY = "gene";

    private static final String STATUS_KEY = "status";

    private static final String EVIDENCE_KEY = "evidence";

    private static final String COMMENTS_KEY = "comments";

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList(GENE_KEY, STATUS_KEY, EVIDENCE_KEY, COMMENTS_KEY);
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }

    @Override
    public PatientData<Map<String, String>> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            List<BaseObject> geneXWikiObjects = doc.getXObjects(GENE_CLASS_REFERENCE);
            if (geneXWikiObjects == null || geneXWikiObjects.isEmpty()) {
                return null;
            }

            List<Map<String, String>> allGenes = new LinkedList<Map<String, String>>();
            for (BaseObject geneObject : geneXWikiObjects) {
                Map<String, String> singleGene = new LinkedHashMap<String, String>();
                for (String property : getProperties()) {
                    BaseStringProperty field = (BaseStringProperty) geneObject.getField(property);
                    if (field != null) {
                        String value = "";
                        switch (property) {
                            case STATUS_KEY:
                                value = parseStatus(field.getValue());
                                break;
                            case EVIDENCE_KEY:
                                value = parseEvidence(field.getValue());
                                break;
                            default:
                                value = field.getValue();
                                break;
                        }
                        singleGene.put(property, value);
                    }
                }
                allGenes.add(singleGene);
            }
            return new IndexedPatientData<Map<String, String>>(getName(), allGenes);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen "
                + "error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    private String parseEvidence(String value)
    {
        String evidence = "";
        switch (value) {
            case "biological_relevance":
                evidence = "Implicated in relevant biological process";
                break;
            case "significant_variant":
                evidence = "Contains variant of functional significance";
                break;
            default:
                evidence = "Verified association with relevant phenotype/disease";
                break;
        }
        return evidence;
    }

    private String parseStatus(String value)
    {
        String status = "";
        switch (value) {
            case "solved":
                status = "Confirmed causal";
                break;
            case "rejected":
                status = "Excluded by testing";
                break;
            default:
                status = "Candidate";
                break;
        }
        return status;
    }

    private void removeKeys(Map<String, String> item, List<String> keys, List<String> enablingProperties,
        Collection<String> selectedFieldNames)
    {
        int count = 0;
        for (String property : keys) {
            if (StringUtils.isBlank(item.get(property))
                || (selectedFieldNames != null
                && !selectedFieldNames.contains(enablingProperties.get(count)))) {
                item.remove(property);
            }
            count++;
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(GENES_ENABLING_FIELD_NAME)) {
            return;
        }

        PatientData<Map<String, String>> data = patient.getData(getName());
        if (data == null) {
            return;
        }
        Iterator<Map<String, String>> iterator = data.iterator();
        if (!iterator.hasNext()) {
            return;
        }

        // put() is placed here because we want to create the property iff at least one field is set/enabled
        // (by this point we know there is some data since iterator.hasNext() == true)
        json.put(getJsonPropertyName(), new JSONArray());
        JSONArray container = json.getJSONArray(getJsonPropertyName());

        List<String> keys =
            Arrays.asList(GENE_KEY, EVIDENCE_KEY, COMMENTS_KEY);

        List<String> enablingProperties =
            Arrays.asList(GENES_STATUS_ENABLING_FIELD_NAME, GENES_EVIDENCE_ENABLING_FIELD_NAME,
                GENES_COMMENTS_ENABLING_FIELD_NAME);

        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();
            if (!StringUtils.isBlank(item.get(GENE_KEY))) {
                removeKeys(item, keys, enablingProperties, selectedFieldNames);
                container.add(item);
            }
        }
    }
}
