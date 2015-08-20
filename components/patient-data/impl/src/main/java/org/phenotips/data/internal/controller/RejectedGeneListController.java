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
 * Handles the patients rejected genes.
 *
 * @version $Id$
 * @since 1.1M1
 */
@Component(roles = { PatientDataController.class })
@Named("rejectedGenes")
@Singleton
public class RejectedGeneListController extends AbstractComplexController<Map<String, String>>
{
    /** The XClass used for storing gene data. */
    private static final EntityReference GENE_CLASS_REFERENCE = new EntityReference("RejectedGenesClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String REJECTEDGENES_STRING = "rejectedGenes";

    private static final String CONTROLLER_NAME = REJECTEDGENES_STRING;

    private static final String REJECTEDGENES_ENABLING_FIELD_NAME = REJECTEDGENES_STRING;

    private static final String REJECTEDGENES_COMMENTS_ENABLING_FIELD_NAME = "rejectedGenes_comments";

    private static final String GENE_KEY = "gene";

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
        return getName();
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList(GENE_KEY, COMMENTS_KEY);
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
                throw new NullPointerException(GeneListController.ERROR_MESSAGE_NO_GENE_CLASS);
            }

            List<Map<String, String>> allGenes = new LinkedList<Map<String, String>>();
            for (BaseObject geneObject : geneXWikiObjects) {
                Map<String, String> singleGene = new LinkedHashMap<String, String>();
                for (String property : getProperties()) {
                    BaseStringProperty field = (BaseStringProperty) geneObject.getField(property);
                    if (field != null) {
                        singleGene.put(property, field.getValue());
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

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(REJECTEDGENES_ENABLING_FIELD_NAME)) {
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

        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();

            if (!StringUtils.isBlank(item.get(GENE_KEY))) {

                if (StringUtils.isBlank(item.get(COMMENTS_KEY))
                    || (selectedFieldNames != null
                    && !selectedFieldNames.contains(REJECTEDGENES_COMMENTS_ENABLING_FIELD_NAME))) {
                    item.remove(COMMENTS_KEY);
                }

                container.add(item);
            }
        }
    }
}
