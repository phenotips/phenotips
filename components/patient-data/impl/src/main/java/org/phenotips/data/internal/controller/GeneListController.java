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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.StringListProperty;

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
    protected static final EntityReference GENE_CLASS_REFERENCE = new EntityReference("GeneClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String GENES_STRING = "genes";

    private static final String CONTROLLER_NAME = GENES_STRING;

    private static final String GENES_ENABLING_FIELD_NAME = GENES_STRING;

    private static final String INTERNAL_GENE_KEY = "gene";

    private static final String INTERNAL_STATUS_KEY = "status";

    private static final String INTERNAL_STRATEGY_KEY = "strategy";

    private static final String INTERNAL_COMMENTS_KEY = "comments";

    private static final String JSON_GENE_KEY = INTERNAL_GENE_KEY;

    private static final String JSON_STATUS_KEY = INTERNAL_STATUS_KEY;

    private static final String JSON_STRATEGY_KEY = INTERNAL_STRATEGY_KEY;

    private static final String JSON_COMMENTS_KEY = INTERNAL_COMMENTS_KEY;

    private static final List<String> STATUS_VALUES = Arrays.asList("candidate", "rejected", "solved");

    private static final List<String> STRATEGY_VALUES = Arrays.asList("sequencing", "deletion", "familial_mutation",
        "common_mutations");

    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

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
        return Arrays.asList(INTERNAL_GENE_KEY, INTERNAL_STATUS_KEY, INTERNAL_STRATEGY_KEY, INTERNAL_COMMENTS_KEY);
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
                if (geneObject == null || geneObject.getFieldList().isEmpty()) {
                    continue;
                }
                Map<String, String> singleGene = new LinkedHashMap<String, String>();
                for (String property : getProperties()) {
                    String value = getFieldValue(geneObject, property);
                    if (value == null) {
                        continue;
                    }
                    singleGene.put(property, value);
                }
                allGenes.add(singleGene);
            }
            if (allGenes.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<Map<String, String>>(getName(), allGenes);
            }
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen "
                + "error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    private String getFieldValue(BaseObject geneObject, String property)
    {
        if (INTERNAL_STRATEGY_KEY.equals(property)) {
            StringListProperty fields = (StringListProperty) geneObject.getField(property);
            if (fields == null || fields.getList().size() == 0) {
                return null;
            }
            return fields.getTextValue();

        } else {
            BaseStringProperty field = (BaseStringProperty) geneObject.getField(property);
            if (field == null) {
                return null;
            }
            return field.getValue();
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

        Map<String, String> internalToJSONkeys = new HashMap<String, String>();
        internalToJSONkeys.put(JSON_GENE_KEY, INTERNAL_GENE_KEY);
        internalToJSONkeys.put(JSON_STATUS_KEY, INTERNAL_STATUS_KEY);
        internalToJSONkeys.put(JSON_STRATEGY_KEY, INTERNAL_STRATEGY_KEY);
        internalToJSONkeys.put(JSON_COMMENTS_KEY, INTERNAL_COMMENTS_KEY);

        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();
            if (!StringUtils.isBlank(item.get(INTERNAL_GENE_KEY))) {
                JSONObject nextGene = new JSONObject();
                for (String key : internalToJSONkeys.keySet()) {
                    if (!StringUtils.isBlank(item.get(key))) {
                        if (INTERNAL_STRATEGY_KEY.equals(key)) {
                            nextGene.put(key, new JSONArray(item.get(internalToJSONkeys.get(key)).split("\\|")));
                        } else {
                            nextGene.put(key, item.get(internalToJSONkeys.get(key)));
                        }
                    }
                }
                container.put(nextGene);
            }
        }
    }

    @Override
    public PatientData<Map<String, String>> readJSON(JSONObject json)
    {
        if (json == null || !json.has(getJsonPropertyName())) {
            return null;
        }

        Map<String, List<String>> enumValues = new LinkedHashMap<String, List<String>>();
        enumValues.put(INTERNAL_STATUS_KEY, STATUS_VALUES);
        enumValues.put(INTERNAL_STRATEGY_KEY, STRATEGY_VALUES);

        try {
            JSONArray genesJson = json.getJSONArray(this.getJsonPropertyName());
            List<Map<String, String>> allGenes = new LinkedList<Map<String, String>>();
            List<String> geneSymbols = new ArrayList<String>();
            for (int i = 0; i < genesJson.length(); ++i) {
                JSONObject geneJson = genesJson.getJSONObject(i);

                // discard it if gene symbol is not present in the geneJson, or is whitespace, empty or duplicate
                if (!geneJson.has(INTERNAL_GENE_KEY) || StringUtils.isBlank(geneJson.getString(INTERNAL_GENE_KEY))
                    || geneSymbols.contains(geneJson.getString(INTERNAL_GENE_KEY))) {
                    continue;
                }

                Map<String, String> singleGene = parseGeneJson(geneJson, enumValues);
                if (singleGene.isEmpty()) {
                    continue;
                }
                allGenes.add(singleGene);
                geneSymbols.add(geneJson.getString(INTERNAL_GENE_KEY));
            }

            if (allGenes.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<Map<String, String>>(getName(), allGenes);
            }
        } catch (Exception e) {
            this.logger.error("Could not load genes from JSON", e.getMessage());
        }
        return null;
    }

    private Map<String, String> parseGeneJson(JSONObject geneJson, Map<String, List<String>> enumValues)
    {
        Map<String, String> singleGene = new LinkedHashMap<String, String>();
        for (String property : this.getProperties()) {
            if (geneJson.has(property)) {
                parseGeneProperty(property, geneJson, enumValues, singleGene);
            }
        }
        return singleGene;
    }

    private void parseGeneProperty(String property, JSONObject geneJson, Map<String, List<String>> enumValues,
        Map<String, String> singleGene)
    {
        String field = "";
        if (INTERNAL_STRATEGY_KEY.equals(property) && geneJson.getJSONArray(property).length() > 0) {
            JSONArray fieldArray = geneJson.getJSONArray(property);
            for (Object value : fieldArray) {
                if (enumValues.get(property).contains(value)) {
                    field += "|" + value;
                }
            }
            singleGene.put(property, field);
        } else if (INTERNAL_STATUS_KEY.equals(property)
            && !StringUtils.isBlank(geneJson.getString(property))) {
            field = geneJson.getString(property);
            if (enumValues.get(property).contains(field.toLowerCase())) {
                singleGene.put(property, field);
            }
        } else if (!StringUtils.isBlank(geneJson.getString(property))) {
            field = geneJson.getString(property);
            singleGene.put(property, field);
        }
    }

    @Override
    public void save(Patient patient)
    {
        try {
            PatientData<Map<String, String>> genes = patient.getData(this.getName());
            if (genes == null || !genes.isIndexed()) {
                return;
            }

            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            if (doc == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            XWikiContext context = this.xcontextProvider.get();
            doc.removeXObjects(GENE_CLASS_REFERENCE);
            Iterator<Map<String, String>> iterator = genes.iterator();
            while (iterator.hasNext()) {
                try {
                    Map<String, String> gene = iterator.next();
                    BaseObject xwikiObject = doc.newXObject(GENE_CLASS_REFERENCE, context);
                    for (String property : this.getProperties()) {
                        String value = gene.get(property);
                        if (value != null) {
                            xwikiObject.set(property, value, context);
                        }
                    }
                } catch (Exception e) {
                    this.logger.error("Failed to save a specific gene: [{}]", e.getMessage());
                }
            }

            context.getWiki().saveDocument(doc, "Updated genes from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save genes: [{}]", e.getMessage());
        }
    }
}
