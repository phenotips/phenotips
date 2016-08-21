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
import org.phenotips.data.PhenoTipsDate;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
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
 * Migration for PhenoTips issue #1538: new format of phenotips JSON "dates_as_entered" fields
 * ("range" instead of "decade") and same new format for pedigree dates.
 * <ul>
 * <li>For each {@code PatientClass} the existing value of birth_date_as_entered and death_date_as_entered fields
 * is updated.</li>
 * <li>For each {@code PatientClass} birth_date_as_entered and death_date_as_entered fields are filled with data in
 * case they are blank but corresponding birth_date/death_date fields are not (may happen if updated via REST API).</li>
 * <li>All dates in pedigree JSON are also updated.</li>
 * </ul>
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("R71494PhenoTips#1538")
@Singleton
public class R71494PhenoTips1538DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /**
     * Names of "date as entered" PatientClass field names (which store the dates as entered
     * by users in JSON-as-text format), and corresponding Date-valued fields.
     * These are the fields that will be migrated
     */
    private static final Map<String, String> ASENTERED_FIELDNAMES =
        Collections.unmodifiableMap(MapUtils.putAll(new HashMap<String, String>(), new String[][] {
            {"date_of_death_entered", "date_of_death"},
            {"date_of_birth_entered", "date_of_birth"}
        }));

    /**
     * Pedigree XClass that holds pedigree data (image, structure, etc).
     */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
            new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";
    private static final String PEDIGREE_GRAPH_KEY = "GG";
    private static final String PEDIGREE_PROPERTIES_STRING = "prop";
    private static final List<String> PEDIGREE_DATE_FIELDS = Arrays.asList("dob", "dod");

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
        return "Migrate JSON representations of dates";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71494);
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

                // convert dates in "as-entered" fields
                BaseObject patientData = patientXDocument.getXObject(Patient.CLASS_REFERENCE);
                if (patientData == null) {
                    continue;
                }
                this.updatePatientDates(patientData, docName);

                // convert dates in pedigree
                BaseObject pedigreeXObject = patientXDocument.getXObject(PEDIGREE_CLASS_REFERENCE);
                if (pedigreeXObject != null) {
                    this.logger.debug("Updating pedigree for patient {}.", docName);
                    this.updatePedigreeDates(pedigreeXObject, context, docName);
                }

                patientXDocument.setComment(this.getDescription());
                patientXDocument.setMinorEdit(true);
            } catch (Exception e) {
                this.logger.error("Error converting dates for patient {}: [{}]", docName, e.getMessage());
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

    private void updatePatientDates(BaseObject patientData, String documentName)
    {
        for (Map.Entry<String, String> entry : ASENTERED_FIELDNAMES.entrySet()) {
            String asEnteredField = entry.getKey();
            String dateAsEntered = patientData.getStringValue(asEnteredField);
            if (!StringUtils.isEmpty(dateAsEntered)) {
                // if "date as entered" is present, use it and disregard the date field itself.
                try {
                    JSONObject dateAsEnteredJSON = new JSONObject(dateAsEntered);
                    PhenoTipsDate asEnteredDate = new PhenoTipsDate(dateAsEnteredJSON);
                    // PhenoTipsDate can read data in the old format, and can spit out date in the new format
                    patientData.setStringValue(asEnteredField, asEnteredDate.toString());
                } catch (Exception ex) {
                    this.logger.error("Could not process date-as-entered field {} for patient {}: [{}]",
                            asEnteredField, documentName, ex.getMessage());
                }
            } else {
                // otherwise use the date field (if present) to populate the date as entered
                String dateField = entry.getValue();
                Date date = patientData.getDateValue(dateField);
                if (date != null) {
                    this.logger.debug("Using the value from the {} field to populate {} field for patient {}",
                            dateField, asEnteredField, documentName);
                    PhenoTipsDate phenotipsDate = new PhenoTipsDate(date);
                    patientData.setStringValue(asEnteredField, phenotipsDate.toString());
                }
            }
        }
    }

    private void updatePedigreeDates(BaseObject pedigreeXObject, XWikiContext context, String documentName)
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
                    this.convertJSONField(nodeJSON, PEDIGREE_DATE_FIELDS);
                    convertedNodes.put(nodeJSON);
                }
                pedigree.put(PEDIGREE_GRAPH_KEY, convertedNodes);
                String pedigreeData = pedigree.toString();
                pedigreeXObject.set(PEDIGREECLASS_JSONDATA_KEY, pedigreeData, context);
            }
        } catch (Exception e) {
            this.logger.error("Patient pedigree data is not a valid JSON for patient {}: [{}]", documentName, e);
        }
    }

    private void convertJSONField(JSONObject nodeJSON, List<String> dateFieldNames)
    {
        JSONObject properties = nodeJSON.optJSONObject(PEDIGREE_PROPERTIES_STRING);
        if (properties != null) {
            for (String fieldName : dateFieldNames) {
                JSONObject dateObject = properties.optJSONObject(fieldName);
                if (dateObject != null) {
                    PhenoTipsDate date = new PhenoTipsDate(dateObject);
                    properties.put(fieldName, date.toJSON());
                }
            }
            nodeJSON.put(PEDIGREE_PROPERTIES_STRING, properties);
        }
    }
}
