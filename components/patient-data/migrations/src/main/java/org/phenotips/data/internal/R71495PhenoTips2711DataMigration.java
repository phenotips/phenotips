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

import org.phenotips.Constants;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;

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
 * Migration for PhenoTips issue PT-2711: support for new genes in pedigree editor. Genes in pedigree are now
 * stored in the same format they are stored in PhenoTips patient JSON, which requires conversion of existing
 * pedigree data. Also pedigree legend data has to be updated to have separate data for causal and candidate genes.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("R71495-PT-2711")
@Singleton
public class R71495PhenoTips2711DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /**
     * Pedigree XClass that holds pedigree data (image, structure, etc).
     */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
            new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String STRING_GENES = "genes";
    private static final String STRING_CANDIDATE_GENES = "candidateGenes";

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String PEDIGREE_SETTINGS_KEY = "settings";
    private static final String PEDIGREE_SETTINGS_COLORS_KEY = "colors";
    private static final String PEDIGREE_OLD_COLORS_GENE_KEY = STRING_GENES;
    private static final String PEDIGREE_NEW_COLORS_GENE_KEY = STRING_CANDIDATE_GENES;

    private static final String PEDIGREE_GRAPH_KEY = "GG";
    private static final String PEDIGREE_PROPERTIES_STRING = "prop";
    private static final String PEDIGREE_OLD_GENE_FIELD = STRING_CANDIDATE_GENES;
    private static final String PEDIGREE_NEW_GENE_FIELD = STRING_GENES;

    private static final String PEDIGREE_GENE_NAME_FIELD = "gene";
    private static final String PEDIGREE_GENE_STATUS_FIELD = "status";
    // all genes in old versions are assumed to be candidate genes
    private static final String PEDIGREE_GENE_STATUS_CANDIDATE = "candidate";

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

    @Override
    public String getDescription()
    {
        return "Migrate pedigree data (support for new genes)";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71495);
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

        // Select all patients
        Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + this.serializer.serialize(Patient.CLASS_REFERENCE)
                + "' and o.name <> 'PhenoTips.PatientTemplate'");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();

        this.logger.debug("Found {} patient documents", docs.size());

        for (String docName : docs) {
            XWikiDocument patientXDocument;

            try {
                patientXDocument = xwiki.getDocument(this.resolver.resolve(docName), context);
                if (patientXDocument == null) {
                    continue;
                }

                BaseObject pedigreeXObject = patientXDocument.getXObject(PEDIGREE_CLASS_REFERENCE);
                if (pedigreeXObject != null) {
                    this.logger.debug("Updating pedigree for patient {}.", docName);
                    this.updatePedigreeGenes(pedigreeXObject, context, docName);
                }

                patientXDocument.setComment(this.getDescription());
                patientXDocument.setMinorEdit(true);
            } catch (Exception e) {
                this.logger.error("Error converting gene data for patient {}: [{}]", docName, e.getMessage());
                continue;
            }

            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(patientXDocument, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                //
            }
        }
        return null;
    }

    private void updatePedigreeGenes(BaseObject pedigreeXObject, XWikiContext context, String documentName)
    {
        String dataText = pedigreeXObject.getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        if (StringUtils.isEmpty(dataText)) {
            return;
        }

        try {
            JSONObject pedigree = new JSONObject(dataText);

            JSONObject pedigreeSettings = pedigree.optJSONObject(PEDIGREE_SETTINGS_KEY);
            if (pedigreeSettings != null) {
                JSONObject settingsColors = pedigreeSettings.optJSONObject(PEDIGREE_SETTINGS_COLORS_KEY);
                if (settingsColors != null) {
                    JSONObject colorsGenes = settingsColors.optJSONObject(PEDIGREE_OLD_COLORS_GENE_KEY);
                    if (colorsGenes != null) {
                        settingsColors.remove(PEDIGREE_OLD_COLORS_GENE_KEY);
                        settingsColors.put(PEDIGREE_NEW_COLORS_GENE_KEY, colorsGenes);
                        pedigreeSettings.put(PEDIGREE_SETTINGS_KEY, settingsColors);
                        pedigree.put(PEDIGREE_SETTINGS_KEY, pedigreeSettings);
                    }
                }
            }

            JSONArray pedigreeNodes = pedigree.optJSONArray(PEDIGREE_GRAPH_KEY);
            JSONArray convertedNodes = new JSONArray();
            if (pedigreeNodes != null) {
                for (Object node : pedigreeNodes) {
                    JSONObject nodeJSON = (JSONObject) node;
                    this.convertGenes(nodeJSON);
                    convertedNodes.put(nodeJSON);
                }
                pedigree.put(PEDIGREE_GRAPH_KEY, convertedNodes);
            }
            String pedigreeData = pedigree.toString();
            pedigreeXObject.set(PEDIGREECLASS_JSONDATA_KEY, pedigreeData, context);
        } catch (Exception e) {
            this.logger.error("Patient pedigree data is not a valid JSON for patient {}: [{}]", documentName, e);
        }
    }

    private void convertGenes(JSONObject nodeJSON)
    {
        JSONObject properties = nodeJSON.optJSONObject(PEDIGREE_PROPERTIES_STRING);
        if (properties != null) {
            JSONArray genes = properties.optJSONArray(PEDIGREE_OLD_GENE_FIELD);
            if (genes != null) {
                // convert from e.g.
                //      "candidateGenes":["LINC01074","ABCD"]
                // to
                //      "genes":[ {"gene":"LINC01074","status":"candidate"}, {"gene":"ABCD","status":"candidate"} ]
                JSONArray newGenes = new JSONArray();
                for (Object gene : genes) {
                    String geneName = (String) gene;
                    JSONObject convertedGene = new JSONObject();
                    convertedGene.put(PEDIGREE_GENE_NAME_FIELD, geneName);
                    convertedGene.put(PEDIGREE_GENE_STATUS_FIELD, PEDIGREE_GENE_STATUS_CANDIDATE);
                    newGenes.put(convertedGene);
                }
                properties.remove(PEDIGREE_OLD_GENE_FIELD);
                properties.put(PEDIGREE_NEW_GENE_FIELD, newGenes);
                nodeJSON.put(PEDIGREE_PROPERTIES_STRING, properties);
            }
        }
    }
}
