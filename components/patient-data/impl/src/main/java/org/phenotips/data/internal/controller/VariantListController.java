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
 * @since 1.0RC1
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

    private static final String VARIANTS_INTERPRETATION_ENABLING_FIELD_NAME = "variants_interpretation";

    private static final String VARIANTS_INHERITANCE_ENABLING_FIELD_NAME = "variants_inheritance";

    private static final String VARIANTS_VALIDATED_ENABLING_FIELD_NAME = "variants_validated";

    private static final String VARIANT_KEY = "hgvs_id";

    private static final String GENESYMBOL_KEY = "genesymbol";

    private static final String INTERPRETATION_KEY = "interpretation";

    private static final String INHERITANCE_KEY = "inheritance";

    private static final String VALIDATED_KEY = "validated";

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
        return Arrays.asList(VARIANT_KEY, GENESYMBOL_KEY, INTERPRETATION_KEY, INHERITANCE_KEY, VALIDATED_KEY);
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
                Map<String, String> singleVariant = new LinkedHashMap<String, String>();
                for (String property : getProperties()) {
                    BaseStringProperty field = (BaseStringProperty) variantObject.getField(property);
                    if (field != null) {
                        String value = "";
                        switch (property) {
                            case INTERPRETATION_KEY:
                                value = parseInterpretation(field.getValue());
                                break;
                            default:
                                value = field.getValue();
                                break;
                        }
                        singleVariant.put(property, value);
                    }
                }
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
            Arrays.asList(GENESYMBOL_KEY, INTERPRETATION_KEY, INHERITANCE_KEY, VALIDATED_KEY);
        List<String> enablingProperties =
            Arrays.asList(VARIANTS_GENESYMBOL_ENABLING_FIELD_NAME, VARIANTS_INTERPRETATION_ENABLING_FIELD_NAME,
                VARIANTS_INHERITANCE_ENABLING_FIELD_NAME, VARIANTS_VALIDATED_ENABLING_FIELD_NAME);

        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();

            if (!StringUtils.isBlank(item.get(VARIANT_KEY))) {
                removeKeys(item, keys, enablingProperties, selectedFieldNames);
                container.add(item);
            }
        }
    }
}
