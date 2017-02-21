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
package org.phenotips.data.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Gene;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * Holds gene data for patient.
 *
 * @version $Id$
 * @since 1.3M4
 */
public class PhenoTipsGene implements Gene
{
    protected static final String HGNC = "HGNC";

    protected static final String ID = "id";

    protected static final String SYMBOL_KEY = "gene";

    protected static final String STATUS_KEY = "status";

    protected static final String STRATEGY_KEY = "strategy";

    protected static final String COMMENTS_KEY = "comments";

    protected static final String ENSEMBL_ID_PROPERTY_NAME = "ensembl_gene_id";

    protected static final String SYMBOL_PROPERTY_NAME = "symbol";

    protected static List<String> STATUS_VALUES = new LinkedList<String>();

    protected static List<String> STRATEGY_VALUES = new LinkedList<String>();

    /** Logging helper object. */
    protected final Logger logger = LoggerFactory.getLogger(PhenoTipsGene.class);

    /** The gene Ensembl ID. */
    protected String id;

    /** The gene display name as HGNC vocabulary symbol @see #getName(). */
    protected String name;

    /** The gene status, one of possible values: "candidate" (default value), "rejected", "solved" @see #getStatus(). */
    protected String status;

    /**
     * The gene strategy, one or more of possible values: "sequencing", "deletion", "familial_mutation",
     * "common_mutations" @see #getStrategy().
     */
    protected String strategy;

    /** The gene comment, user typed @see #getComment(). */
    protected String comment;

    private Vocabulary hgnc;

    /**
     * Constructor that copies the data from an XProperty value.
     *
     * @param id gene Ensembl ID
     * @param name gene HGNC vocabulary symbol
     * @param status gene status, one of possible values: "candidate" (default value), "rejected", "solved"
     * @param strategy gene strategy
     * @param comment gene user inputed comment
     */
    public PhenoTipsGene(String id, String name, String status, String strategy, String comment)
    {
        if (StringUtils.isBlank(id) && StringUtils.isBlank(name)) {
            throw new IllegalArgumentException();
        }
        if (STATUS_VALUES.size() == 0 || STRATEGY_VALUES.size() == 0) {
            this.getProperties();
        }
        // gene ID is either the "id" field, or, if missing, the "gene" field
        String geneName = StringUtils.isNotBlank(id) ? id : name;
        this.setNames(geneName);
        this.setStatus(status);
        this.setStrategy(strategy);
        this.setComment(comment);
    }

    /**
     * Constructor for initializing from a JSON Object. Supports both 1.3-m5 and older 1.3-xx json formats. 1.3-m5 and
     * newer format: {"id": ENSEMBL_Id [[, "gene": HGNC_Symbol] , ...] } 1.3-old format: {"gene": HGNC_Symbol [, ...] }
     *
     * @param geneJson JSON object that holds gene details info
     */
    public PhenoTipsGene(JSONObject geneJson)
    {
        this(geneJson.optString(ID), geneJson.optString(SYMBOL_KEY), geneJson.optString(STATUS_KEY), null, geneJson
            .optString(COMMENTS_KEY));

        JSONArray strategyArray = geneJson.optJSONArray(STRATEGY_KEY);
        if (strategyArray != null) {
            String internalValue = "";
            for (Object value : strategyArray) {
                if (STRATEGY_VALUES.contains(value)) {
                    internalValue += "|" + value;
                }
            }
            this.setStrategy(internalValue);
        }
    }

    /**
     * Return gene Ensembl ID.
     *
     * @return id
     */
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * Return gene status.
     *
     * @return status
     */
    public String getStatus()
    {
        return this.status;
    }

    /**
     * Return gene strategy.
     *
     * @return strategy
     */
    public String getStrategy()
    {
        return this.strategy;
    }

    /**
     * Return gene comment.
     *
     * @return comment
     */
    public String getComment()
    {
        return this.comment;
    }

    /**
     * Set gene Ensembl ID.
     *
     * @param id gene Ensembl ID
     */
    public void setId(String id)
    {
        if (StringUtils.isNotBlank(id)) {
            this.id = id;
        }
    }

    /**
     * Set gene name.
     *
     * @param name gene HGNC vocabulary symbol
     */
    public void setName(String name)
    {
        if (StringUtils.isNotBlank(name)) {
            this.name = name;
        }
    }

    /**
     * Sets gene Ensembl ID and status via HGNC Vocabulary term.
     *
     * @param geneName either gene Ensembl ID or gene symbol
     */
    @SuppressWarnings("unchecked")
    public void setNames(String geneName)
    {
        VocabularyTerm term = getTerm(geneName);
        if (term != null) {

            // FIXME: refactor HGNC vocabulary to have "ensembl_gene_id" as a single value not a list
            // FIXME: refactor VocabularyTerm to simplify access to a String value using getString() instead of
            // get()? currently VocabularyTerm.get() is only ever used here and in gene migrator

            List<String> ensemblIdList = (List<String>) term.get(ENSEMBL_ID_PROPERTY_NAME);
            String ensemblId = (ensemblIdList != null && !ensemblIdList.isEmpty()) ? ensemblIdList.get(0) : null;
            // retain information as is if we can't find Ensembl ID.
            this.setId(StringUtils.isBlank(ensemblId) ? geneName : ensemblId);

            String geneSymbol = (term != null) ? (String) term.get(SYMBOL_PROPERTY_NAME) : null;
            this.setName(StringUtils.isBlank(geneSymbol) ? geneName : geneSymbol);
        } else {
            this.setId(geneName);
            this.setName(geneName);
        }
    }

    /**
     * Set gene status.
     *
     * @param status gene status
     */
    public void setStatus(String status)
    {
        if (StringUtils.isNotBlank(status) && STATUS_VALUES.contains(status.trim().toLowerCase())) {
            this.status = status;
        }
    }

    /**
     * Set gene strategy.
     *
     * @param strategy gene strategy
     */
    public void setStrategy(String strategy)
    {
        if (StringUtils.isNotBlank(strategy)) {
            this.strategy = strategy.trim().toLowerCase();
        }
    }

    /**
     * Set gene comment.
     *
     * @param comment gene comment
     */
    public void setComment(String comment)
    {
        if (StringUtils.isNotBlank(comment)) {
            this.comment = comment.trim();
        }
    }

    /**
     * Convert gene to JSON.
     *
     * @return gene JSON
     */
    public JSONObject toJSON()
    {
        JSONObject geneJson = new JSONObject();
        geneJson.put(ID, this.id);
        geneJson.put(SYMBOL_KEY, this.getName());
        setStringValueIfNotBlank(geneJson, COMMENTS_KEY, this.comment);
        setStringValueIfNotBlank(geneJson, STATUS_KEY, this.status);
        setArrayValueIfNotBlank(geneJson, STRATEGY_KEY, this.strategy);
        return geneJson;
    }

    private void setStringValueIfNotBlank(JSONObject object, String key, String value)
    {
        if (!StringUtils.isBlank(value)) {
            object.put(key, value);
        }
    }

    private void setArrayValueIfNotBlank(JSONObject object, String key, String listString)
    {
        if (!StringUtils.isBlank(listString)) {
            object.put(key, new JSONArray(listString.split("\\|")));
        }
    }

    private VocabularyTerm getTerm(String gene)
    {
        // lazy-initialize HGNC
        if (this.hgnc == null) {
            this.hgnc = getHGNCVocabulary();
            if (this.hgnc == null) {
                return null;
            }
        }
        return this.hgnc.getTerm(gene);
    }

    private Vocabulary getHGNCVocabulary()
    {
        try {
            VocabularyManager vm =
                ComponentManagerRegistry.getContextComponentManager().getInstance(VocabularyManager.class);
            return vm.getVocabulary(HGNC);
        } catch (ComponentLookupException ex) {
            this.logger.error("Error loading component [{}]", ex.getMessage(), ex);
        }
        return null;
    }

    private void getProperties()
    {
        // lazy initialization of properties from the Gene XClass
        try {
            XWikiContext context = this.getXContext();
            XWikiDocument doc = context.getWiki().getDocument(Gene.GENE_CLASS, context);
            if (doc == null || doc.isNew()) {
                // Inaccessible or deleted document
                return;
            }
            BaseClass gene = doc.getXClass();
            if (gene == null) {
                return;
            }
            StaticListClass statusProp = (StaticListClass) gene.get(STATUS_KEY);
            StaticListClass stategyProp = (StaticListClass) gene.get(STRATEGY_KEY);
            if (statusProp != null) {
                STATUS_VALUES = statusProp.getList(context);
            }
            if (statusProp != null) {
                STRATEGY_VALUES = stategyProp.getList(context);
            }
        } catch (XWikiException ex) {
            // Doesn't matter, the hash is just nice to have
        }
    }

    private XWikiContext getXContext()
    {
        Provider<XWikiContext> xcontextProvider = null;
        try {
            xcontextProvider =
                ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
        } catch (ComponentLookupException ex) {
            // Should not happen
            return null;
        }
        XWikiContext context = xcontextProvider.get();
        return context;
    }

}
