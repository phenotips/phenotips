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
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.StringListProperty;

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

    private static final String VARIANTS_ENABLING_FIELD_NAME = "genes";

    private static final String INTERNAL_VARIANT_KEY = "cdna";

    private static final String INTERNAL_GENESYMBOL_KEY = "genesymbol";

    private static final String INTERNAL_PROTEIN_KEY = "protein";

    private static final String INTERNAL_TRANSCRIPT_KEY = "transcript";

    private static final String INTERNAL_DBSNP_KEY = "dbsnp";

    private static final String INTERNAL_ZYGOSITY_KEY = "zygosity";

    private static final String INTERNAL_EFFECT_KEY = "effect";

    private static final String INTERNAL_INTERPRETATION_KEY = "interpretation";

    private static final String INTERNAL_INHERITANCE_KEY = "inheritance";

    private static final String INTERNAL_EVIDENCE_KEY = "evidence";

    private static final String INTERNAL_SEGREGATION_KEY = "segregation";

    private static final String INTERNAL_SANGER_KEY = "sanger";

    private static final String INTERNAL_CHROMOSOME_KEY = "chromosome";

    private static final String INTERNAL_START_POSITION_KEY = "start_position";

    private static final String INTERNAL_END_POSITION_KEY = "end_position";

    private static final String INTERNAL_REFERENCE_GENOME_KEY = "reference_genome";

    private static final String JSON_VARIANT_KEY = INTERNAL_VARIANT_KEY;

    private static final String JSON_GENESYMBOL_KEY = INTERNAL_GENESYMBOL_KEY;

    private static final String JSON_PROTEIN_KEY = INTERNAL_PROTEIN_KEY;

    private static final String JSON_TRANSCRIPT_KEY = INTERNAL_TRANSCRIPT_KEY;

    private static final String JSON_DBSNP_KEY = INTERNAL_DBSNP_KEY;

    private static final String JSON_ZYGOSITY_KEY = INTERNAL_ZYGOSITY_KEY;

    private static final String JSON_EFFECT_KEY = INTERNAL_EFFECT_KEY;

    private static final String JSON_INTERPRETATION_KEY = INTERNAL_INTERPRETATION_KEY;

    private static final String JSON_INHERITANCE_KEY = INTERNAL_INHERITANCE_KEY;

    private static final String JSON_EVIDENCE_KEY = INTERNAL_EVIDENCE_KEY;

    private static final String JSON_SEGREGATION_KEY = INTERNAL_SEGREGATION_KEY;

    private static final String JSON_SANGER_KEY = INTERNAL_SANGER_KEY;

    private static final String JSON_CHROMOSOME_KEY = INTERNAL_CHROMOSOME_KEY;

    private static final String JSON_START_POSITION_KEY = INTERNAL_START_POSITION_KEY;

    private static final String JSON_END_POSITION_KEY = INTERNAL_END_POSITION_KEY;

    private static final String JSON_REFERENCE_GENOME_KEY = INTERNAL_REFERENCE_GENOME_KEY;

    private static final List<String> ZYGOSITY_VALUES = Arrays.asList("heterozygous", "homozygous", "hemizygous");

    private static final List<String> EFFECT_VALUES = Arrays.asList("missense", "nonsense", "insertion_in_frame",
        "insertion_frameshift", "deletion_in_frame", "deletion_frameshift", "indel_in_frame", "indel_frameshift",
        "duplication", "repeat_expansion", "synonymous", "other");

    private static final List<String> INTERPRETATION_VALUES = Arrays.asList("pathogenic", "likely_pathogenic",
        "variant_u_s", "likely_benign", "benign", "investigation_n");

    private static final List<String> INHERITANCE_VALUES = Arrays.asList("denovo_germline", "denovo_s_mosaicism",
        "maternal", "paternal", "unknown");

    private static final List<String> EVIDENCE_VALUES = Arrays.asList("rare", "predicted", "reported");

    private static final List<String> SEGREGATION_VALUES = Arrays.asList("segregates", "not_segregates");

    private static final List<String> SANGER_VALUES = Arrays.asList("positive", "negative");

    private static final List<String> CHROMOSOME_VALUES = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9",
        "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y");

    private static final List<String> REFERENCE_GENOME_VALUES = Arrays.asList("GRCh37", "GRCh38", "NCBI36");

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
        return Arrays.asList(INTERNAL_VARIANT_KEY, INTERNAL_GENESYMBOL_KEY, INTERNAL_PROTEIN_KEY,
            INTERNAL_TRANSCRIPT_KEY, INTERNAL_DBSNP_KEY, INTERNAL_ZYGOSITY_KEY,
            INTERNAL_EFFECT_KEY, INTERNAL_INTERPRETATION_KEY, INTERNAL_INHERITANCE_KEY, INTERNAL_EVIDENCE_KEY,
            INTERNAL_SEGREGATION_KEY, INTERNAL_SANGER_KEY, INTERNAL_CHROMOSOME_KEY, INTERNAL_START_POSITION_KEY,
            INTERNAL_END_POSITION_KEY, INTERNAL_REFERENCE_GENOME_KEY);
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
            XWikiDocument doc = patient.getXDocument();
            List<BaseObject> variantXWikiObjects = doc.getXObjects(VARIANT_CLASS_REFERENCE);
            if (variantXWikiObjects == null || variantXWikiObjects.isEmpty()) {
                return null;
            }

            List<Map<String, String>> allVariants = new LinkedList<>();
            for (BaseObject variantObject : variantXWikiObjects) {
                if (variantObject == null || variantObject.getFieldList().isEmpty()) {
                    continue;
                }
                Map<String, String> singleVariant = new LinkedHashMap<>();
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
                return new IndexedPatientData<>(getName(), allVariants);
            }
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    private String getFieldValue(BaseObject variantObject, String property)
    {
        if (INTERNAL_EVIDENCE_KEY.equals(property)) {
            StringListProperty fields = (StringListProperty) variantObject.getField(property);
            if (fields == null || fields.getList().size() == 0) {
                return null;
            }
            return fields.getTextValue();

        } else if (INTERNAL_START_POSITION_KEY.equals(property) || INTERNAL_END_POSITION_KEY.equals(property)) {
            int value = variantObject.getIntValue(property, -1);
            if (value == -1) {
                return null;
            }
            return Integer.toString(value);
        } else {
            BaseStringProperty field = (BaseStringProperty) variantObject.getField(property);
            if (field == null) {
                return null;
            }
            return field.getValue();
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(VARIANTS_ENABLING_FIELD_NAME)) {
            return;
        }

        PatientData<Map<String, String>> data = patient.getData(getName());
        if (data == null || data.size() == 0) {
            if (selectedFieldNames != null && selectedFieldNames.contains(VARIANTS_ENABLING_FIELD_NAME)) {
                json.put(getJsonPropertyName(), new JSONArray());
            }
            return;
        }

        Iterator<Map<String, String>> iterator = data.iterator();

        // put() is placed here because we want to create the property iff at least one field is set/enabled
        // (by this point we know there is some data since iterator.hasNext() == true)
        json.put(getJsonPropertyName(), new JSONArray());
        JSONArray container = json.getJSONArray(getJsonPropertyName());

        Map<String, String> internalToJSONkeys = new HashMap<>();
        internalToJSONkeys.put(JSON_VARIANT_KEY, INTERNAL_VARIANT_KEY);
        internalToJSONkeys.put(JSON_GENESYMBOL_KEY, INTERNAL_GENESYMBOL_KEY);
        internalToJSONkeys.put(JSON_PROTEIN_KEY, INTERNAL_PROTEIN_KEY);
        internalToJSONkeys.put(JSON_TRANSCRIPT_KEY, INTERNAL_TRANSCRIPT_KEY);
        internalToJSONkeys.put(JSON_DBSNP_KEY, INTERNAL_DBSNP_KEY);
        internalToJSONkeys.put(JSON_ZYGOSITY_KEY, INTERNAL_ZYGOSITY_KEY);
        internalToJSONkeys.put(JSON_EFFECT_KEY, INTERNAL_EFFECT_KEY);
        internalToJSONkeys.put(JSON_INTERPRETATION_KEY, INTERNAL_INTERPRETATION_KEY);
        internalToJSONkeys.put(JSON_INHERITANCE_KEY, INTERNAL_INHERITANCE_KEY);
        internalToJSONkeys.put(JSON_EVIDENCE_KEY, INTERNAL_EVIDENCE_KEY);
        internalToJSONkeys.put(JSON_SEGREGATION_KEY, INTERNAL_SEGREGATION_KEY);
        internalToJSONkeys.put(JSON_SANGER_KEY, INTERNAL_SANGER_KEY);
        internalToJSONkeys.put(JSON_CHROMOSOME_KEY, INTERNAL_CHROMOSOME_KEY);
        internalToJSONkeys.put(JSON_START_POSITION_KEY, INTERNAL_START_POSITION_KEY);
        internalToJSONkeys.put(JSON_END_POSITION_KEY, INTERNAL_END_POSITION_KEY);
        internalToJSONkeys.put(JSON_REFERENCE_GENOME_KEY, INTERNAL_REFERENCE_GENOME_KEY);

        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();

            if (!StringUtils.isBlank(item.get(INTERNAL_VARIANT_KEY))) {
                JSONObject nextVariant = new JSONObject();
                for (String key : internalToJSONkeys.keySet()) {
                    if (!StringUtils.isBlank(item.get(key))) {
                        if (INTERNAL_EVIDENCE_KEY.equals(key)) {
                            nextVariant.put(key, new JSONArray(item.get(internalToJSONkeys.get(key)).split("\\|")));
                        } else {
                            nextVariant.put(key, item.get(internalToJSONkeys.get(key)));
                        }
                    }
                }
                container.put(nextVariant);
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
            Arrays.asList(INTERNAL_ZYGOSITY_KEY, INTERNAL_EFFECT_KEY, INTERNAL_INTERPRETATION_KEY,
                INTERNAL_INHERITANCE_KEY, INTERNAL_SEGREGATION_KEY,
                INTERNAL_SANGER_KEY);

        Map<String, List<String>> enumValues = new LinkedHashMap<>();
        enumValues.put(INTERNAL_ZYGOSITY_KEY, ZYGOSITY_VALUES);
        enumValues.put(INTERNAL_EFFECT_KEY, EFFECT_VALUES);
        enumValues.put(INTERNAL_INTERPRETATION_KEY, INTERPRETATION_VALUES);
        enumValues.put(INTERNAL_INHERITANCE_KEY, INHERITANCE_VALUES);
        enumValues.put(INTERNAL_EVIDENCE_KEY, EVIDENCE_VALUES);
        enumValues.put(INTERNAL_SEGREGATION_KEY, SEGREGATION_VALUES);
        enumValues.put(INTERNAL_SANGER_KEY, SANGER_VALUES);
        enumValues.put(INTERNAL_CHROMOSOME_KEY, CHROMOSOME_VALUES);
        enumValues.put(INTERNAL_REFERENCE_GENOME_KEY, REFERENCE_GENOME_VALUES);

        try {
            JSONArray variantsJson = json.getJSONArray(this.getJsonPropertyName());
            List<Map<String, String>> allVariants = new LinkedList<>();
            List<String> variantSymbols = new ArrayList<>();
            for (int i = 0; i < variantsJson.length(); ++i) {
                JSONObject variantJson = variantsJson.getJSONObject(i);

                // discard it if variant cDNA is not present in the geneJson, or is whitespace, empty or duplicate
                if (!variantJson.has(INTERNAL_VARIANT_KEY)
                    || StringUtils.isBlank(variantJson.getString(INTERNAL_VARIANT_KEY))
                    || variantSymbols.contains(variantJson.getString(INTERNAL_VARIANT_KEY))) {
                    continue;
                }

                Map<String, String> singleVariant = parseVariantJson(variantJson, enumValues, enumValueKeys);
                if (singleVariant.isEmpty()) {
                    continue;
                }

                allVariants.add(singleVariant);
                variantSymbols.add(variantJson.getString(INTERNAL_VARIANT_KEY));
            }

            if (allVariants.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), allVariants);
            }
        } catch (Exception e) {
            this.logger.error("Could not load variants from JSON", e.getMessage());
        }
        return null;
    }

    private Map<String, String> parseVariantJson(JSONObject variantJson, Map<String, List<String>> enumValues,
        List<String> enumValueKeys)
    {
        Map<String, String> singleVariant = new LinkedHashMap<>();
        for (String property : this.getProperties()) {
            if (variantJson.has(property)) {
                parseVariantProperty(property, variantJson, enumValues, singleVariant, enumValueKeys);
            }
        }
        return singleVariant;
    }

    private void parseVariantProperty(String property, JSONObject variantJson, Map<String, List<String>> enumValues,
        Map<String, String> singleVariant, List<String> enumValueKeys)
    {
        String field = "";
        if (INTERNAL_EVIDENCE_KEY.equals(property) && variantJson.getJSONArray(property).length() > 0) {
            JSONArray fieldArray = variantJson.getJSONArray(property);
            for (Object value : fieldArray) {
                if (enumValues.get(property).contains(value)) {
                    field += "|" + value;
                }
            }
            singleVariant.put(property, field);
        } else if ((INTERNAL_START_POSITION_KEY.equals(property) || INTERNAL_END_POSITION_KEY.equals(property))
            && !StringUtils.isBlank(variantJson.getString(property))) {
            String value = variantJson.optString(property);
            if (NumberUtils.isDigits(value)) {
                singleVariant.put(property, value);
            }
        } else if (enumValueKeys.contains(property) && !StringUtils.isBlank(variantJson.getString(property))) {
            field = variantJson.getString(property);
            if (enumValues.get(property).contains(field.toLowerCase())) {
                singleVariant.put(property, field);
            }
        } else if (!StringUtils.isBlank(variantJson.getString(property))) {
            field = variantJson.getString(property);
            singleVariant.put(property, field);
        }
    }

    @Override
    public void save(Patient patient)
    {
        PatientData<Map<String, String>> variants = patient.getData(this.getName());
        if (variants == null || !variants.isIndexed()) {
            return;
        }

        XWikiContext context = this.xcontextProvider.get();
        patient.getXDocument().removeXObjects(VARIANT_CLASS_REFERENCE);
        Iterator<Map<String, String>> iterator = variants.iterator();
        while (iterator.hasNext()) {
            try {
                Map<String, String> variant = iterator.next();
                BaseObject xwikiObject = patient.getXDocument().newXObject(VARIANT_CLASS_REFERENCE, context);

                for (String property : this.getProperties()) {
                    String value = variant.get(property);
                    if (value != null) {
                        if (INTERNAL_START_POSITION_KEY.equals(property) || INTERNAL_END_POSITION_KEY.equals(property)) {
                            xwikiObject.setIntValue(property, Integer.valueOf(value));
                        } else {
                            xwikiObject.set(property, value, context);
                        }

                    }
                }
            } catch (Exception e) {
                this.logger.error("Failed to save a specific variant: [{}]", e.getMessage());
            }
        }
    }
}
