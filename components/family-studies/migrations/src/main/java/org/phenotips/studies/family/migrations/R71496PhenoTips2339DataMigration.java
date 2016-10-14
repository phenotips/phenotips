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
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Migration for PhenoTips issue PT-2339: support for negative phenotypes and phenotype details in pedigree editor. In
 * the past, pedigree editor could have ignored negative phenotypes and phenotype details, since they never propagated
 * back to PhenoTips. Now that they do pedigree should support the same data as PT, so that no data is lost when data is
 * moved form PT to pedigree and then back to PT.
 *
 * @version $Id$
 * @since 1.3M3
 */
@Component
@Named("R71496-PT-2339")
@Singleton
public class R71496PhenoTips2339DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /**
     * Pedigree XClass that holds pedigree data (image, structure, etc).
     */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String PEDIGREE_GRAPH_KEY = "GG";

    private static final String PEDIGREE_PROPERTIES_STRING = "prop";

    private static final String PEDIGREE_OLD_PHENOTYPE_FIELD = "hpoTerms";

    private static final String PEDIGREE_NEW_PHENOTYPE_FIELD = "features";

    private static final String PEDIGREE_NEW_PHENOTYPE_NONSTD_FIELD = "nonstandard_features";

    private static final String PEDIGREE_PHENOTYPE_ID_FIELD = "id";

    private static final String PEDIGREE_PHENOTYPE_LABEL_FIELD = "label";

    private static final String PEDIGREE_PHENOTYPE_OBSERVED_FIELD = "observed";

    // all phenotypes in old versions are assumed to be observed
    private static final String PEDIGREE_PHENOTYPE_OBSERVED_YES_VALUE = "yes";

    private static final String PEDIGREE_PHENOTYPE_TYPE_FIELD = "type";

    private static final String PEDIGREE_PHENOTYPE_TYPE_VALUE = "phenotype";

    private static final Pattern HPO_STANDARD_TERM_PATTERN = Pattern.compile("^HP:(\\d+)$");

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
        return "Migrate pedigree data (support for negative phenotypes and phenotype details)";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71496);
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
                    this.updatePedigreePhenotypes(pedigreeXObject, context, docName);
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

    private void updatePedigreePhenotypes(BaseObject pedigreeXObject, XWikiContext context, String documentName)
    {
        String dataText = pedigreeXObject.getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        if (StringUtils.isEmpty(dataText)) {
            return;
        }

        try {
            JSONObject pedigree = new JSONObject(dataText);

            JSONArray pedigreeNodes = pedigree.optJSONArray(PEDIGREE_GRAPH_KEY);
            JSONArray convertedNodes = new JSONArray();
            if (pedigreeNodes != null) {
                for (Object node : pedigreeNodes) {
                    JSONObject nodeJSON = (JSONObject) node;
                    this.convertPhenotypes(nodeJSON);
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

    private void convertPhenotypes(JSONObject nodeJSON)
    {
        JSONObject properties = nodeJSON.optJSONObject(PEDIGREE_PROPERTIES_STRING);
        if (properties != null) {
            JSONArray phenotypes = properties.optJSONArray(PEDIGREE_OLD_PHENOTYPE_FIELD);
            if (phenotypes != null) {

                // convert from e.g.
                // "hpoTerms":["HP:0004315", "HP:0002045", "custom name"]
                // to
                // "features": [ {"id":"HP:0004315","observed":"yes","type":"phenotype"},
                // {"id":"HP:0002045","observed":"yes","type":"phenotype"} ]
                // "nonstandard_features": [ {"observed":"yes","type":"phenotype","label":"custom name"} ]

                JSONArray newPhenotypes = new JSONArray();
                JSONArray newNonstandardPhenotypes = new JSONArray();

                for (Object phenotype : phenotypes) {

                    // common part for all phenotypes - standard and non-standard
                    // (all assumed to be observed and of type "phenotype")
                    JSONObject convertedPhenotype = new JSONObject();
                    convertedPhenotype.put(PEDIGREE_PHENOTYPE_OBSERVED_FIELD, PEDIGREE_PHENOTYPE_OBSERVED_YES_VALUE);
                    convertedPhenotype.put(PEDIGREE_PHENOTYPE_TYPE_FIELD, PEDIGREE_PHENOTYPE_TYPE_VALUE);

                    String phenotypeID = (String) phenotype;
                    Matcher m = HPO_STANDARD_TERM_PATTERN.matcher(phenotypeID);
                    if (m.find()) {
                        convertedPhenotype.put(PEDIGREE_PHENOTYPE_ID_FIELD, phenotypeID);
                        // "label" field is skipped in this case to simplify conversion,
                        // it will be auto-populated on first load
                        newPhenotypes.put(convertedPhenotype);
                    } else {
                        convertedPhenotype.put(PEDIGREE_PHENOTYPE_LABEL_FIELD, phenotypeID);
                        newNonstandardPhenotypes.put(convertedPhenotype);
                    }
                }
                properties.remove(PEDIGREE_OLD_PHENOTYPE_FIELD);
                properties.put(PEDIGREE_NEW_PHENOTYPE_FIELD, newPhenotypes);
                properties.put(PEDIGREE_NEW_PHENOTYPE_NONSTD_FIELD, newNonstandardPhenotypes);
                nodeJSON.put(PEDIGREE_PROPERTIES_STRING, properties);
            }
        }
    }
}
