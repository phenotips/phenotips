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
import org.phenotips.data.Gene;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.internal.PhenoTipsGene;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
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
public class GeneListController implements PatientDataController<Gene>
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

    private static final String INTERNAL_CANDIDATE_VALUE = "candidate";

    private static final String INTERNAL_REJECTED_VALUE = "rejected";

    private static final String INTERNAL_SOLVED_VALUE = "solved";

    private static final String JSON_GENE_ID = "id";

    private static final String JSON_GENE_SYMBOL = INTERNAL_GENE_KEY;

    private static final String JSON_DEPRECATED_GENE_ID = JSON_GENE_SYMBOL;

    private static final String JSON_SOLVED_KEY = INTERNAL_SOLVED_VALUE;

    private static final String JSON_REJECTEDGENES_KEY = "rejectedGenes";

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
    public PatientData<Gene> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            List<BaseObject> geneXWikiObjects = doc.getXObjects(GENE_CLASS_REFERENCE);
            if (geneXWikiObjects == null || geneXWikiObjects.isEmpty()) {
                return null;
            }

            List<Gene> allGenes = new LinkedList<>();
            for (BaseObject geneObject : geneXWikiObjects) {
                if (geneObject == null || geneObject.getFieldList().isEmpty()) {
                    continue;
                }

                String id = getFieldValue(geneObject, INTERNAL_GENE_KEY);
                String status = getFieldValue(geneObject, INTERNAL_STATUS_KEY);
                Collection<String> strategy = getFieldListValue(geneObject, INTERNAL_STRATEGY_KEY);
                String comment = getFieldValue(geneObject, INTERNAL_COMMENTS_KEY);

                Gene gene = new PhenoTipsGene(id, null, status, strategy, comment);

                allGenes.add(gene);
            }
            if (allGenes.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), allGenes);
            }
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    private Collection<String> getFieldListValue(BaseObject geneObject, String property)
    {
        StringListProperty fields = (StringListProperty) geneObject.getField(property);
        if (fields == null || fields.getList().size() == 0) {
            return null;
        }
        return fields.getList();
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

        PatientData<Gene> data = patient.getData(getName());
        if (data == null || data.size() == 0) {
            if (selectedFieldNames == null || selectedFieldNames.contains(GENES_ENABLING_FIELD_NAME)) {
                json.put(GENES_STRING, new JSONArray());
            }
            return;
        }

        JSONArray geneArray = new JSONArray();

        for (Gene gene : data) {
            geneArray.put(gene.toJSON());
        }

        json.put(GENES_STRING, geneArray);
    }

    @Override
    public PatientData<Gene> readJSON(JSONObject json)
    {
        if (json == null
            || !(json.has(GENES_STRING) || json.has(JSON_SOLVED_KEY) || json.has(JSON_REJECTEDGENES_KEY))) {
            return null;
        }

        try {
            List<Gene> accumulatedGenes = new LinkedList<>();

            parseGenesJson(json, accumulatedGenes);

            // v1.2.x json compatibility
            parseRejectedGenes(json, accumulatedGenes);
            parseSolvedGene(json, accumulatedGenes);

            return new IndexedPatientData<>(getName(), accumulatedGenes);
        } catch (Exception e) {
            this.logger.error("Could not load genes from JSON: [{}]", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Supports both 1.3-m5 and older 1.3-xx format. 1.3-m5 and newer format: {"id": ENSEMBL_Id [[, "gene": HGNC_Symbol]
     * , ...] } 1.3-old format: {"gene": HGNC_Symbol [, ...] }
     */
    private void parseGenesJson(JSONObject json, List<Gene> accumulatedGenes)
    {
        JSONArray genesJson = json.optJSONArray(GENES_STRING);

        Set<String> alreadyCollectedGeneNames = new HashSet<>();

        if (genesJson != null) {
            for (int i = 0; i < genesJson.length(); ++i) {
                JSONObject geneJson = genesJson.getJSONObject(i);

                // gene ID is either the "id" field, or, if missing, the "gene" field
                // if both missing, gene is not created
                if (StringUtils.isBlank(geneJson.optString(JSON_GENE_ID))
                    && StringUtils.isBlank(geneJson.optString(JSON_GENE_SYMBOL))) {
                    continue;
                }

                Gene gene = new PhenoTipsGene(geneJson);
                if (gene == null || alreadyCollectedGeneNames.contains(gene.getId())) {
                    continue;
                }

                accumulatedGenes.add(gene);
                alreadyCollectedGeneNames.add(gene.getId());
            }
        }
    }

    private void parseRejectedGenes(JSONObject json, List<Gene> accumulatedGenes)
    {
        Set<String> rejectedGeneNames = new HashSet<>();

        JSONArray rejectedGenes = json.optJSONArray(JSON_REJECTEDGENES_KEY);
        if (rejectedGenes != null && rejectedGenes.length() > 0) {
            for (int i = 0; i < rejectedGenes.length(); ++i) {
                JSONObject rejectedGeneJson = rejectedGenes.getJSONObject(i);

                // discard it if gene symbol is blank or empty
                if (StringUtils.isBlank(rejectedGeneJson.optString(JSON_DEPRECATED_GENE_ID))) {
                    continue;
                }

                PhenoTipsGene gene = new PhenoTipsGene(rejectedGeneJson);
                if (rejectedGeneNames.contains(gene.getId())) {
                    continue;
                }

                gene.setStatus(INTERNAL_REJECTED_VALUE);

                // overwrite the same gene if it was found to be as candidate
                addOrReplaceGene(accumulatedGenes, gene);

                rejectedGeneNames.add(gene.getId());
            }
        }
    }

    private void parseSolvedGene(JSONObject json, List<Gene> accumulatedGenes)
    {
        JSONObject solvedGene = json.optJSONObject(JSON_SOLVED_KEY);
        if (solvedGene == null) {
            return;
        }

        // discard it if gene symbol is blank or empty
        if (StringUtils.isBlank(solvedGene.optString(JSON_DEPRECATED_GENE_ID))) {
            return;
        }

        PhenoTipsGene gene = new PhenoTipsGene(solvedGene);

        gene.setStatus(INTERNAL_SOLVED_VALUE);

        // overwrite the same gene if it was found to be a candidate or rejected
        addOrReplaceGene(accumulatedGenes, gene);
    }

    private void addOrReplaceGene(List<Gene> allGenes, Gene gene)
    {
        // need index for replacement; performance is not critical since this code is only
        // used for old 1.2.x. patient JSONs
        for (int i = 0; i < allGenes.size(); i++) {
            if (StringUtils.equals(allGenes.get(i).getId(), gene.getId())) {
                allGenes.set(i, gene);
                return;
            }
        }
        allGenes.add(gene);
    }

    @Override
    public void save(Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        try {
            final XWikiDocument docX = patient.getXDocument();
            final PatientData<Gene> genes = patient.getData(getName());
            if (genes == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    docX.removeXObjects(GENE_CLASS_REFERENCE);
                }
            } else if (!genes.isIndexed()) {
                this.logger.info("Wrong data type for gene data");
            } else {
                saveGenes(docX, patient, genes, policy, this.xcontextProvider.get());
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save genes data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves {@code genes} data for {@code patient} according to the provided {@code policy}.
     *
     * @param docX the {@link XWikiDocument} for patient
     * @param patient the {@link Patient} object of interest
     * @param data the newly added gene data
     * @param policy the policy according to which data should be saved
     * @param context the {@link XWikiContext} object
     */
    private void saveGenes(
        @Nonnull final XWikiDocument docX,
        @Nonnull final Patient patient,
        @Nonnull final PatientData<Gene> data,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        docX.removeXObjects(GENE_CLASS_REFERENCE);
        if (PatientWritePolicy.MERGE.equals(policy)) {
            final Map<String, Gene> mergedGenes = getMergedGenes(data, load(patient));
            mergedGenes.forEach((id, gene) -> saveGene(docX, gene, context));
        } else {
            data.forEach(gene -> saveGene(docX, gene, context));
        }
    }

    /**
     * Saves a {@code gene} for the provided {@code patient}.
     *
     * @param docX the {@link XWikiDocument} for patient
     * @param gene a gene to be saved
     * @param context the {@link XWikiContext} object
     */
    private void saveGene(
        @Nonnull final XWikiDocument docX,
        @Nonnull final Gene gene,
        @Nonnull final XWikiContext context)
    {
        try {
            final BaseObject xwikiObject = docX.newXObject(GENE_CLASS_REFERENCE, context);
            setXWikiObjectProperty(INTERNAL_GENE_KEY, gene.getName(), xwikiObject, context);
            String status = gene.getStatus();
            // setting status to default 'candidate' if not defined yet
            setXWikiObjectProperty(INTERNAL_STATUS_KEY,
                StringUtils.isNotBlank(status) ? status : INTERNAL_CANDIDATE_VALUE, xwikiObject, context);
            setXWikiObjectProperty(INTERNAL_STRATEGY_KEY, gene.getStrategy(), xwikiObject, context);
            setXWikiObjectProperty(INTERNAL_COMMENTS_KEY, gene.getComment(), xwikiObject, context);
        } catch (final XWikiException e) {
            this.logger.error("Failed to save a specific gene: [{}]", e.getMessage());
        }
    }

    /**
     * Create a map of gene ID to gene properties that merges existing and updated gene data.
     *
     * @param genes the gene data to add to patient
     * @param storedGenes the gene data already stored in patient
     * @return a list of merged genes
     */
    private Map<String, Gene> getMergedGenes(
        @Nullable final Iterable<Gene> genes,
        @Nullable final Iterable<Gene> storedGenes)
    {
        // If map keys collide, merge genes in favor of the new value
        return Stream.of(storedGenes, genes)
            .filter(Objects::nonNull)
            .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
            .collect(Collectors.toMap(gene -> gene.getId(), Function.identity(), (v1, v2) -> v2, LinkedHashMap::new));
    }

    private void setXWikiObjectProperty(String property, Object value, BaseObject xwikiObject, XWikiContext context)
    {
        if (value instanceof Collection) {
            xwikiObject.set(property, new LinkedList<>((Collection<?>) value), context);
        } else if (value != null) {
            xwikiObject.set(property, value, context);
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }
}
