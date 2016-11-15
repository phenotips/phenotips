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

package org.phenotips.studies.family.migrations;

import org.phenotips.Constants;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-2944: translate existing gene data in pedigree from gene name to Ensembl gene IDs.
 *
 * @version $Id$
 * @since 1.3M4
 */
@Component
@Named("R71500-PT-2944")
@Singleton
public class R71500PhenoTips2944DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /**
     * Pedigree XClass that holds pedigree data (image, structure, etc).
     */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String HGNC = "HGNC";

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String PEDIGREE_GRAPH_KEY = "GG";

    private static final String PEDIGREE_SETTINGS_KEY = "settings";

    private static final String PEDIGREE_PROPERTIES_STRING = "prop";

    private static final String PEDIGREE_GENES_FIELD = "genes";

    private static final String PEDIGREE_GENE_FIELD = "gene";

    private static final String PEDIGREE_COLORS_FIELD = "colors";

    private static final String PEDIGREE_SOLVED_GENES_FIELD = "causalGenes";

    private static final String PEDIGREE_CANDIDATE_GENES_FIELD = "candidateGenes";

    private static final String PEDIGREE_REJECTED_GENES_FIELD = "rejectedGenes";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /** Provides access to the all available vocabularies. */
    @Inject
    private VocabularyManager vocabularies;

    private Vocabulary hgnc;

    @Override
    public String getDescription()
    {
        return "Translate existing gene data in pedigree editor from gene name to Ensembl gene ID";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71500);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        this.hgnc = this.vocabularies.getVocabulary(HGNC);

        // Select all families
        Query q = session.createQuery("select distinct o.name from BaseObject o where o.className = '"
            + this.serializer.serialize(PEDIGREE_CLASS_REFERENCE)
            + "' and o.name <> 'PhenoTips.FamilyTemplate' and o.name <> 'PhenoTips.PatientTemplate'");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();

        this.logger.debug("Found {} documents", docs.size());

        for (String docName : docs) {
            XWikiDocument xDocument;

            try {
                xDocument = xwiki.getDocument(this.resolver.resolve(docName), context);
                if (xDocument == null) {
                    continue;
                }

                BaseObject pedigreeXObject = xDocument.getXObject(PEDIGREE_CLASS_REFERENCE);
                if (pedigreeXObject == null) {
                    continue;
                }

                this.logger.debug("Updating pedigree for document {}.", docName);
                if (!this.updatePedigreeGenes(pedigreeXObject, context, docName)) {
                    continue;
                }

                xDocument.setComment(this.getDescription());
                xDocument.setMinorEdit(true);

            } catch (Exception e) {
                this.logger.error("Error converting gene data for document {}: [{}]", docName, e.getMessage());
                continue;
            }

            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(xDocument, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                //
            }

        }
        return null;
    }

    private boolean updatePedigreeGenes(BaseObject pedigreeXObject, XWikiContext context, String documentName)
    {
        try {
            boolean updatedGenes = false;
            boolean updatedColors = false;
            String dataText = pedigreeXObject.getStringValue(PEDIGREECLASS_JSONDATA_KEY);
            if (StringUtils.isEmpty(dataText)) {
                return false;
            }

            JSONObject pedigree = new JSONObject(dataText);
            Map<String, String> hgncToEnsemblMap = new HashMap<String, String>();

            JSONArray pedigreeNodes = pedigree.optJSONArray(PEDIGREE_GRAPH_KEY);
            if (pedigreeNodes != null) {
                for (Object node : pedigreeNodes) {
                    JSONObject nodeJSON = (JSONObject) node;
                    updatedGenes = this.convertGenes(nodeJSON, hgncToEnsemblMap);
                }
            }

            JSONObject pedigreeSettings = pedigree.optJSONObject(PEDIGREE_SETTINGS_KEY);
            if (pedigreeSettings != null) {
                JSONObject pedigreeColors = pedigreeSettings.optJSONObject(PEDIGREE_COLORS_FIELD);
                if (pedigreeColors != null) {
                    updatedColors = this.convertColors(pedigreeColors, hgncToEnsemblMap);
                }
            }

            if (updatedGenes || updatedColors) {
                String pedigreeData = pedigree.toString();
                pedigreeXObject.set(PEDIGREECLASS_JSONDATA_KEY, pedigreeData, context);
            }

            return updatedGenes || updatedColors;
        } catch (Exception e) {
            this.logger.error("Error updating JSON for docuemnt {}: [{}]", documentName, e);
        }
        return false;
    }

    // convert from e.g.
    // "colors":{"causalGenes":{"SLC37A4":"#eac080"},
    // to
    // "colors":{"causalGenes":{"ENSG00000137700":"#eac080"},
    private boolean convertColors(JSONObject colorsJSON, Map<String, String> hgncToEnsemblMap)
    {
        boolean updated = false;
        String[] geneFileds =
        { PEDIGREE_SOLVED_GENES_FIELD, PEDIGREE_CANDIDATE_GENES_FIELD, PEDIGREE_REJECTED_GENES_FIELD };
        for (String field : geneFileds) {
            JSONObject colors = colorsJSON.optJSONObject(field);
            JSONObject updatedColors = new JSONObject();
            if (colors == null || colors.length() == 0) {
                continue;
            }
            Iterator<String> it = colors.keys();
            while (it.hasNext()) {
                String geneSymbol = it.next();
                String color = colors.optString(geneSymbol);
                String enselmbId = hgncToEnsemblMap.containsKey(geneSymbol) ? hgncToEnsemblMap.get(geneSymbol)
                    : this.getEnsemblId(geneSymbol);
                updatedColors.put(enselmbId, color);
            }
            colorsJSON.put(field, updatedColors);
            updated = true;
        }

        return updated;
    }

    // convert from e.g.
    // "genes":[{"gene":"SLC37A4","status":"candidate"}]
    // to
    // "genes":[{"gene":"ENSG00000137700","status":"candidate"}]
    private boolean convertGenes(JSONObject nodeJSON, Map<String, String> hgncToEnsemblMap)
    {
        boolean updated = false;
        JSONObject properties = nodeJSON.optJSONObject(PEDIGREE_PROPERTIES_STRING);
        if (properties == null) {
            return false;
        }
        JSONArray genes = properties.optJSONArray(PEDIGREE_GENES_FIELD);
        if (genes == null || genes.length() == 0) {
            return false;
        }

        for (Object gene : genes) {
            JSONObject geneObj = (JSONObject) gene;
            String geneSymbol = geneObj.optString(PEDIGREE_GENE_FIELD);
            if (geneSymbol == null) {
                continue;
            }
            String geneEnsemblId = this.getEnsemblId(geneSymbol);
            hgncToEnsemblMap.put(geneSymbol, geneEnsemblId);
            geneObj.put(PEDIGREE_GENE_FIELD, geneEnsemblId);
            updated = true;
        }

        return updated;
    }

    /**
     * Gets EnsemblID corresponding to the HGNC symbol.
     *
     * @param geneSymbol the string representation of a gene symbol (e.g. NOD2).
     * @return the string representation of the corresponding Ensembl ID.
     */
    private String getEnsemblId(String geneSymbol)
    {
        final VocabularyTerm term = this.hgnc.getTerm(geneSymbol);
        @SuppressWarnings("unchecked")
        final List<String> ensemblIdList = term != null ? (List<String>) term.get("ensembl_gene_id") : null;
        final String ensemblId = ensemblIdList != null && !ensemblIdList.isEmpty() ? ensemblIdList.get(0) : null;
        // Retain information as is if we can't find Ensembl ID.
        return ensemblId != null ? ensemblId : geneSymbol;
    }
}
