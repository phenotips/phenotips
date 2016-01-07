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
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.StringListProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Handles the patients gene variants.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component(roles = { PatientDataController.class })
@Named("variant")
@Singleton
public class VariantListController extends AbstractComplexController<Map<String, String>>
{
    /** The XClass used for storing variant data. */
    private static final EntityReference VARIANT_CLASS_REFERENCE = new EntityReference("GeneVariantClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String VARIANTS_STRING = "variants";

    private static final String CONTROLLER_NAME = VARIANTS_STRING;

    private static final String VARIANTS_ENABLING_FIELD_NAME = VARIANTS_STRING;

    private static final String VARIANTS_GENESYMBOL_ENABLING_FIELD_NAME = "variants_genesymbol";

    private static final String VARIANTS_PROTEIN_ENABLING_FIELD_NAME = "variants_protein";

    private static final String VARIANTS_TRANSCRIPT_ENABLING_FIELD_NAME = "variants_transcript";

    private static final String VARIANTS_DBSNP_ENABLING_FIELD_NAME = "variants_dbsnp";

    private static final String VARIANTS_ZYGOSITY_ENABLING_FIELD_NAME = "variants_zygosity";

    private static final String VARIANTS_EFFECT_ENABLING_FIELD_NAME = "variants_effect";

    private static final String VARIANTS_INTERPRETATION_ENABLING_FIELD_NAME = "variants_interpretation";

    private static final String VARIANTS_INHERITANCE_ENABLING_FIELD_NAME = "variants_inheritance";

    private static final String VARIANTS_EVIDENCE_ENABLING_FIELD_NAME = "variants_evidence";

    private static final String VARIANTS_SEGREGATION_ENABLING_FIELD_NAME = "variants_segregation";

    private static final String VARIANTS_SANGER_ENABLING_FIELD_NAME = "variants_sanger";

    private static final String VARIANT_KEY = "cdna";

    private static final String GENESYMBOL_KEY = "genesymbol";

    private static final String PROTEIN_KEY = "protein";

    private static final String TRANSCRIPT_KEY = "transcript";

    private static final String DBSNP_KEY = "dbsnp";

    private static final String ZYGOSITY_KEY = "zygosity";

    private static final String EFFECT_KEY = "effect";

    private static final String INTERPRETATION_KEY = "interpretation";

    private static final String INHERITANCE_KEY = "inheritance";

    private static final String EVIDENCE_KEY = "evidence";

    private static final String SEGREGATION_KEY = "segregation";

    private static final String SANGER_KEY = "sanger";

    private static final List<String> ZYGOSITY_VALUES = Arrays.asList("heterozygous", "homozygous", "hemizygous");

    private static final List<String> EFFECT_VALUES = Arrays.asList("missense", "nonsense", "insertion_in_frame",
        "insertion_frameshift", "deletion_in_frame", "deletion_frameshift", "indel_in_frame", "indel_frameshift",
        "duplication", "repeat_expansion", "synonymous", "other");

    private static final List<String> INTERPRETATION_VALUES = Arrays.asList("pathogenic", "likely_pathogenic",
        "variant_u_s", "likely_benign", "benign");

    private static final List<String> INHERITANCE_VALUES = Arrays.asList("denovo_germline", "denovo_s_mosaicism",
        "maternal", "paternal", "unknown");

    private static final List<String> EVIDENCE_VALUES = Arrays.asList("rare", "predicted", "reported");

    private static final List<String> SEGREGATION_VALUES = Arrays.asList("segregates", "not_segregates");

    private static final List<String> SANGER_VALUES = Arrays.asList("positive", "negative");

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
        return Arrays.asList(VARIANT_KEY, GENESYMBOL_KEY, PROTEIN_KEY, TRANSCRIPT_KEY, DBSNP_KEY, ZYGOSITY_KEY,
            EFFECT_KEY, INTERPRETATION_KEY, INHERITANCE_KEY, EVIDENCE_KEY, SEGREGATION_KEY, SANGER_KEY);
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
            List<BaseObject> variantXWikiObjects = doc.getXObjects(VARIANT_CLASS_REFERENCE);
            if (variantXWikiObjects == null || variantXWikiObjects.isEmpty()) {
                return null;
            }

            List<Map<String, String>> allVariants = new LinkedList<Map<String, String>>();
            for (BaseObject variantObject : variantXWikiObjects) {
                if (variantObject == null || variantObject.getFieldList().isEmpty()) {
                    continue;
                }
                Map<String, String> singleVariant = new LinkedHashMap<String, String>();
                for (String property : getProperties()) {
                    String value = getFieldValue(variantObject, property);
                    if (value == null) {
                        continue;
                    }
                    singleVariant.put(property, value);
                }
                allVariants.add(singleVariant);
            }
            if (allVariants.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<Map<String, String>>(getName(), allVariants);
            }
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen "
                + "error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    private String getFieldValue(BaseObject variantObject, String property)
    {
        if (EVIDENCE_KEY.equals(property)) {
            StringListProperty fields = (StringListProperty) variantObject.getField(property);
            if (fields == null || fields.getList().size() == 0) {
                return null;
            }
            return fields.getTextValue();

        } else {
            BaseStringProperty field = (BaseStringProperty) variantObject.getField(property);
            if (field == null) {
                return null;
            }
            return field.getValue();
        }
    }

    private void removeKeys(Map<String, String> item, List<String> keys, Map<String, String> enablingProperties,
        Collection<String> selectedFieldNames)
    {
        for (String property : keys) {
            if (StringUtils.isBlank(item.get(property))
                || (selectedFieldNames != null
                && !selectedFieldNames.contains(enablingProperties.get(property)))) {
                item.remove(property);
            }
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(VARIANTS_ENABLING_FIELD_NAME)) {
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
            Arrays.asList(GENESYMBOL_KEY, PROTEIN_KEY, TRANSCRIPT_KEY, DBSNP_KEY, ZYGOSITY_KEY, EFFECT_KEY,
                INTERPRETATION_KEY, INHERITANCE_KEY, EVIDENCE_KEY, SANGER_KEY, SEGREGATION_KEY);
        Map<String, String> enablingPropertiesMap = new HashMap<String, String>();
        enablingPropertiesMap.put(GENESYMBOL_KEY, VARIANTS_GENESYMBOL_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(PROTEIN_KEY, VARIANTS_PROTEIN_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(TRANSCRIPT_KEY, VARIANTS_TRANSCRIPT_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(DBSNP_KEY, VARIANTS_DBSNP_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(ZYGOSITY_KEY, VARIANTS_ZYGOSITY_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(EFFECT_KEY, VARIANTS_EFFECT_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(INTERPRETATION_KEY, VARIANTS_INTERPRETATION_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(INHERITANCE_KEY, VARIANTS_INHERITANCE_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(EVIDENCE_KEY, VARIANTS_EVIDENCE_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(SEGREGATION_KEY, VARIANTS_SEGREGATION_ENABLING_FIELD_NAME);
        enablingPropertiesMap.put(SANGER_KEY, VARIANTS_SANGER_ENABLING_FIELD_NAME);

        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();

            if (!StringUtils.isBlank(item.get(VARIANT_KEY))) {
                removeKeys(item, keys, enablingPropertiesMap, selectedFieldNames);
                container.add(item);
            }
        }
    }

    @Override
    public PatientData<Map<String, String>> readJSON(JSONObject json)
    {
        if (json == null || !json.has(getJsonPropertyName())) {
            return null;
        }

        List<String> enumValueKeys =
            Arrays.asList(ZYGOSITY_KEY, EFFECT_KEY, INTERPRETATION_KEY, INHERITANCE_KEY, SEGREGATION_KEY,
                SANGER_KEY);

        Map<String, List<String>> enumValues = new LinkedHashMap<String, List<String>>();
        enumValues.put(ZYGOSITY_KEY, ZYGOSITY_VALUES);
        enumValues.put(EFFECT_KEY, EFFECT_VALUES);
        enumValues.put(INTERPRETATION_KEY, INTERPRETATION_VALUES);
        enumValues.put(INHERITANCE_KEY, INHERITANCE_VALUES);
        enumValues.put(EVIDENCE_KEY, EVIDENCE_VALUES);
        enumValues.put(SEGREGATION_KEY, SEGREGATION_VALUES);
        enumValues.put(SANGER_KEY, SANGER_VALUES);

        try {
            JSONArray variantsJson = json.getJSONArray(this.getJsonPropertyName());
            List<Map<String, String>> allVariants = new LinkedList<Map<String, String>>();
            List<String> variantSymbols = new ArrayList<String>();
            for (int i = 0; i < variantsJson.size(); ++i) {
                JSONObject variantJson = variantsJson.getJSONObject(i);

                // discard it if variant cDNA is not present in the geneJson, or is whitespace, empty or duplicate
                if (!variantJson.has(VARIANT_KEY) || StringUtils.isBlank(variantJson.getString(VARIANT_KEY))
                    || variantSymbols.contains(variantJson.getString(VARIANT_KEY))) {
                    continue;
                }

                Map<String, String> singleVariant = parseVariantJson(variantJson, enumValues, enumValueKeys);
                if (singleVariant.isEmpty()) {
                    continue;
                }

                allVariants.add(singleVariant);
                variantSymbols.add(variantJson.getString(VARIANT_KEY));
            }

            if (allVariants.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<Map<String, String>>(getName(), allVariants);
            }
        } catch (Exception e) {
            this.logger.error("Could not load variants from JSON", e.getMessage());
        }
        return null;
    }

    private Map<String, String> parseVariantJson(JSONObject variantJson, Map<String, List<String>> enumValues,
        List<String> enumValueKeys)
    {
        Map<String, String> singleVariant = new LinkedHashMap<String, String>();
        for (String property : this.getProperties()) {
            if (variantJson.has(property) && !StringUtils.isBlank(variantJson.getString(property))) {

                String field = variantJson.getString(property);

                if (enumValueKeys.contains(property)) {
                    if (enumValues.get(property).contains(field.toLowerCase())) {
                        singleVariant.put(property, field);
                    }
                } else if (EVIDENCE_KEY.equals(property)) {
                    String evidenceField = "";
                    for (String value : field.split("\\|")) {
                        if (enumValues.get(property).contains(value)) {
                            evidenceField += "|" + value;
                        }
                    }
                    singleVariant.put(property, evidenceField);
                } else {
                    singleVariant.put(property, field);
                }
            }
        }
        return singleVariant;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            PatientData<Map<String, String>> variants = patient.getData(this.getName());
            if (variants == null || !variants.isIndexed()) {
                return;
            }

            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            if (doc == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            XWikiContext context = this.xcontextProvider.get();
            doc.removeXObjects(VARIANT_CLASS_REFERENCE);
            Iterator<Map<String, String>> iterator = variants.iterator();
            while (iterator.hasNext()) {
                try {
                    Map<String, String> variant = iterator.next();
                    BaseObject xwikiObject = doc.newXObject(VARIANT_CLASS_REFERENCE, context);

                    for (String property : this.getProperties()) {
                        String value = variant.get(property);
                        if (value != null) {
                            xwikiObject.set(property, value, context);
                        }
                    }
                } catch (Exception e) {
                    this.logger.error("Failed to save a specific variant: [{}]", e.getMessage());
                }
            }

            context.getWiki().saveDocument(doc, "Updated variants from JSON", true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save variants: [{}]", e.getMessage());
        }
    }
}
