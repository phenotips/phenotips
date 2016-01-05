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
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the obstetric history of the mother.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component(roles = { PatientDataController.class })
@Named("obstetric-history")
@Singleton
public class ObstetricHistoryController implements PatientDataController<Integer>
{
    /** The XClass used for storing parental information. */
    public static final EntityReference CLASS_REFERENCE =
        new EntityReference("ParentalInformationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String PREFIX = "pregnancy_history__";

    private static final String GRAVIDA = "gravida";

    private static final String PARA = "para";

    private static final String TERM = "term";

    private static final String PRETERM = "preterm";

    private static final String SAB = "sab";

    private static final String TAB = "tab";

    private static final String LIVE_BIRTHS = "births";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The current request context, needed when working with the XWiki old core. */
    @Inject
    private Provider<XWikiContext> xcontext;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public String getName()
    {
        return "obstetric-history";
    }

    @Override
    public PatientData<Integer> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(getXClassReference());
            if (data == null) {
                this.logger.debug("No data for patient [{}]", patient.getDocument());
                return null;
            }
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String property : getProperties()) {
                int age = data.getIntValue(PREFIX + property);
                if (age != 0) {
                    result.put(property, age);
                }
            }
            if (!result.isEmpty()) {
                return new DictionaryPatientData<Integer>(getName(), result);
            }
        } catch (Exception ex) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", ex.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());

            PatientData<Integer> data = patient.getData(getName());
            if (data == null || !data.isNamed()) {
                return;
            }
            XWikiContext context = this.xcontext.get();
            BaseObject o = doc.getXObject(getXClassReference(), true, context);
            for (String property : getProperties()) {
                o.set(PREFIX + property, data.get(property), context);
            }

            context.getWiki().saveDocument(doc, "Updated obstetric history from JSON", true, context);
        } catch (Exception ex) {
            this.logger.error("Failed to save obstetric history: [{}]", ex.getMessage());
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(getName())) {
            return;
        }
        PatientData<Integer> data = patient.getData(getName());
        if (data == null || !data.isNamed() || data.size() <= 0) {
            return;
        }

        JSONObject result = new JSONObject();

        for (Iterator<Map.Entry<String, Integer>> entries = data.dictionaryIterator(); entries.hasNext();) {
            Map.Entry<String, Integer> entry = entries.next();
            result.put(entry.getKey(), entry.getValue());
        }
        if (result.length() > 0) {
            JSONObject container = findContainer(json);
            for (String key : result.keySet()) {
                container.put(key, result.get(key));
            }
        }
    }

    @Override
    public PatientData<Integer> readJSON(JSONObject json)
    {
        if (json == null || json.length() == 0) {
            return null;
        }
        JSONObject data = json;
        for (String path : StringUtils.split(getJsonPropertyName(), '.')) {
            data = data.optJSONObject(path);
            if (data == null || data.length() == 0) {
                return null;
            }
        }
        Map<String, Integer> result = new LinkedHashMap<>();

        for (String property : getProperties()) {
            if (data.has(property)) {
                int age = data.getInt(property);
                result.put(property, age);
            }
        }
        return new DictionaryPatientData<>(getName(), result);
    }

    protected EntityReference getXClassReference()
    {
        return CLASS_REFERENCE;
    }

    protected List<String> getProperties()
    {
        return Arrays.asList(GRAVIDA, PARA, TERM, PRETERM, SAB, TAB, LIVE_BIRTHS);
    }

    private String getJsonPropertyName()
    {
        return "prenatal_perinatal_history." + getName();
    }

    private JSONObject findContainer(JSONObject json)
    {
        JSONObject container = json;
        for (String path : StringUtils.split(getJsonPropertyName(), '.')) {
            JSONObject parent = container;
            container = parent.optJSONObject(path);
            if (container == null) {
                parent.put(path, new JSONObject());
                container = parent.optJSONObject(path);
            }
        }
        return container;
    }
}
