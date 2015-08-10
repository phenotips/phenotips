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
 * Handles the patients gene variants.
 *
 * @version $Id$
 * @since 1.2M1
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

    private static final String VARIANTS_RESOLUTION_ENABLING_FIELD_NAME = "variants_resolution";

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

    private static final String RESOLUTION_KEY = "resolution";

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
        return Arrays.asList(VARIANT_KEY, GENESYMBOL_KEY, PROTEIN_KEY, TRANSCRIPT_KEY, DBSNP_KEY, ZYGOSITY_KEY,
            EFFECT_KEY,
            INTERPRETATION_KEY, INHERITANCE_KEY, EVIDENCE_KEY, SEGREGATION_KEY, SANGER_KEY, RESOLUTION_KEY);
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

    private String parseInterpretation(String value)
    {
        String interpretation = "";
        switch (value) {
            case "pathogenic":
                interpretation = "Pathogenic";
                break;
            case "likely_pathogenic":
                interpretation = "Likely Pathogenic";
                break;
            case "likely_benign":
                interpretation = "Likely Benign";
                break;
            case "benign":
                interpretation = "Benign";
                break;
            default:
                interpretation = "Variant of Unknown Significance";
                break;
        }
        return interpretation;
    }

    private String parseSegregation(String value)
    {
        String classification = "";
        switch (value) {
            case "complete":
                classification = "Completel";
                break;
            case "in_progress":
                classification = "In progress";
                break;
            default:
                classification = "Not done";
                break;
        }
        return classification;
    }

    private String parseEvidence(String value)
    {
        String classification = "";
        switch (value) {
            case "rare":
                classification = "Rare (MAF<0.01)";
                break;
            case "predicted":
                classification = "Predicted damaging by in silico models";
                break;
            case "reported":
                classification = "Reported in other individuals";
                break;
            default:
                classification = "Segregates with phenotype within family";
                break;
        }
        return classification;
    }

    private String parseResolution(String value)
    {
        String classification = "";
        switch (value) {
            case "solved":
                classification = "Confirmed causal";
                break;
            case "rejected":
                classification = "Rejected";
                break;
            default:
                classification = "Unknown";
                break;
        }
        return classification;
    }

    private String parseInheritance(String value)
    {
        String classification = "";
        switch (value) {
            case "denovo":
                classification = "de novo";
                break;
            case "denovo_s_mosaicism":
                classification = "de novo somatic mosaicism";
                break;
            default:
                classification = value;
                break;
        }
        return classification;
    }

    private String parseEffect(String value)
    {
        String classification = "";
        switch (value) {
            case "insertion_in_frame":
                classification = "insertion - in frame";
                break;
            case "insertion_frameshift":
                classification = "insertion - frameshift";
                break;
            case "deletion_in_frame":
                classification = "deletion - in frame";
                break;
            case "deletion_frameshift":
                classification = "deletion - frameshift";
                break;
            case "indel_in_frame":
                classification = "indel - in frame";
                break;
            case "indel_frameshift":
                classification = "indel - frameshift";
                break;
            case "repeat_expansion":
                classification = "repeat expansion";
                break;
            default:
                classification = value;
                break;
        }
        return classification;
    }

    private Map<String, String> parseFields(BaseObject variantObject)
    {
        Map<String, String> singleVariant = new LinkedHashMap<String, String>();
        for (String property : getProperties()) {
            BaseStringProperty field = (BaseStringProperty) variantObject.getField(property);
            if (field != null) {
                String value = "";
                switch (property) {
                    case INTERPRETATION_KEY:
                        value = parseInterpretation(field.getValue());
                        break;
                    case RESOLUTION_KEY:
                        value = parseResolution(field.getValue());
                        break;
                    case SEGREGATION_KEY:
                        value = parseSegregation(field.getValue());
                        break;
                    case SANGER_KEY:
                        value = parseSegregation(field.getValue());
                        break;
                    case EVIDENCE_KEY:
                        value = parseEvidence(field.getValue());
                        break;
                    case EFFECT_KEY:
                        value = parseEffect(field.getValue());
                        break;
                    case INHERITANCE_KEY:
                        value = parseInheritance(field.getValue());
                        break;
                    default:
                        value = field.getValue();
                        break;
                }
                singleVariant.put(property, value);
            }
        }
        return singleVariant;
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
                Map<String, String> singleVariant = parseFields(variantObject);
                allVariants.add(singleVariant);
            }
            return new IndexedPatientData<Map<String, String>>(getName(), allVariants);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen "
                + "error has occurred during controller loading ", e.getMessage());
        }
        return null;
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
            Arrays.asList(GENESYMBOL_KEY, PROTEIN_KEY, TRANSCRIPT_KEY, DBSNP_KEY, EFFECT_KEY, INTERPRETATION_KEY,
                INHERITANCE_KEY, EVIDENCE_KEY, SANGER_KEY,
                RESOLUTION_KEY);
        List<String> enablingProperties =
            Arrays.asList(VARIANTS_GENESYMBOL_ENABLING_FIELD_NAME, VARIANTS_PROTEIN_ENABLING_FIELD_NAME,
                VARIANTS_TRANSCRIPT_ENABLING_FIELD_NAME,
                VARIANTS_DBSNP_ENABLING_FIELD_NAME, VARIANTS_ZYGOSITY_ENABLING_FIELD_NAME,
                VARIANTS_EFFECT_ENABLING_FIELD_NAME, VARIANTS_INTERPRETATION_ENABLING_FIELD_NAME,
                VARIANTS_INHERITANCE_ENABLING_FIELD_NAME,
                VARIANTS_EVIDENCE_ENABLING_FIELD_NAME, VARIANTS_SEGREGATION_ENABLING_FIELD_NAME,
                VARIANTS_SANGER_ENABLING_FIELD_NAME,
                VARIANTS_RESOLUTION_ENABLING_FIELD_NAME);

        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();

            if (!StringUtils.isBlank(item.get(VARIANT_KEY))) {
                removeKeys(item, keys, enablingProperties, selectedFieldNames);
                container.add(item);
            }
        }
    }
}
