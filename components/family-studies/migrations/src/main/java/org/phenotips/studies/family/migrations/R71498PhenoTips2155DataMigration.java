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
import org.phenotips.studies.family.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
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
 * Migration for PhenoTips issue #2155: migrating old family studies data.
 *
 * @version $Id$
 * @since 1.3M3
 */
@Component
@Named("R71498PhenoTips#2155")
@Singleton
public class R71498PhenoTips2155DataMigration extends AbstractHibernateDataMigration
{
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

    @Inject
    private PhenotipsFamilyMigrations familyMigrations;

    @Override
    public String getDescription()
    {
        return "Migrating old family studies data.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71498);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigratePedigreeCallback());
    }

    private class MigratePedigreeCallback implements HibernateCallback<Object>
    {
        private static final String RELATIVE_PROPERTY_NAME = "relative_type";

        private static final String RELATIVEOF_PROPERTY_NAME = "relative_of";

        private static final String REFERENCE_PROPERTY_NAME = "reference";

        private final List<String> allowedRelatives = Arrays.asList("parent", "child", "sibling", "twin");

        private R71498PhenoTips2155DataMigration migrator = R71498PhenoTips2155DataMigration.this;

        private EntityReference relativeClassReference = new EntityReference("RelativeClass",
            EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

        private Session session;

        private XWikiContext context;

        @Override
        public Object doInHibernate(Session hSession) throws HibernateException, XWikiException
        {
            this.session = hSession;
            this.context = getXWikiContext();
            XWiki xwiki = this.context.getWiki();

            // Select all patients
            Query q =
                this.session.createQuery("select distinct o.name from BaseObject o, BaseObject ro where"
                    + " o.className = :patclass and o.name <> 'PhenoTips.PatientTemplate'"
                    + " and ro.name = o.name and ro.className = :relclass");
            q.setString("patclass", this.migrator.serializer.serialize(Patient.CLASS_REFERENCE));
            q.setString("relclass", this.migrator.serializer.serialize(this.relativeClassReference));

            @SuppressWarnings("unchecked")
            List<String> documents = q.list();

            this.migrator.logger.debug("Found {} patient documents", documents.size());

            for (String docName : documents) {
                XWikiDocument patientXDocument =
                    xwiki.getDocument(this.migrator.resolver.resolve(docName), this.context);
                if (patientXDocument == null) {
                    continue;
                }

                List<BaseObject> relativeXObjects = patientXDocument.getXObjects(this.relativeClassReference);
                Map<String, XWikiDocument> relativesDocList = getRelativesDocList(relativeXObjects, xwiki);
                if (relativesDocList == null || relativesDocList.isEmpty()) {
                    continue;
                }

                String relativesFamilyRef = getRelativesFamily(relativesDocList);
                String patientFamilyRef = getPatientsFamily(patientXDocument);

                XWikiDocument familyXDocument =
                    processPatientWithRelatives(patientXDocument, patientFamilyRef, relativesFamilyRef,
                        relativesDocList, xwiki);
                if (familyXDocument == null) {
                    this.migrator.logger.debug("Could not create a family. Patient Id: {}.", docName);
                    continue;
                }
                patientXDocument.setComment(this.migrator.getDescription());
                patientXDocument.setMinorEdit(true);
                familyXDocument.setComment(this.migrator.getDescription());
                try {
                    this.session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(patientXDocument, this.context, false);
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(familyXDocument, this.context, false);
                    this.session.flush();
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                } finally {
                    this.context.getWiki().flushCache(this.context);
                    this.migrator.logger.debug("Updated [{}]", docName);
                }
            }

            return null;
        }

        /**
         * Returns either existing patient family or relative family document, or a new one. Patient and relatives are
         * assigned to a new family as members. Returns null if more than one family exists for patient and relatives.
         *
         * @throws XWikiException
         * @throws QueryException
         */
        private XWikiDocument processPatientWithRelatives(XWikiDocument patientXDocument, String patientFamilyRef,
            String relativesFamilyRef, Map<String, XWikiDocument> relativesDocList, XWiki xwiki) throws QueryException,
            XWikiException
        {
            XWikiDocument familyXDocument = null;
            if (relativesFamilyRef == null || patientFamilyRef != null && !"".equals(relativesFamilyRef)) {
                this.migrator.logger.debug("More than one family exists for patient and relatives. Patient Id: {}.",
                    patientXDocument.getId());
            }
            // If patient has a family
            if (patientFamilyRef != null) {
                // set family references for all relatives
                setAllFamilyRefs(patientXDocument, patientFamilyRef, relativesDocList, xwiki);
                familyXDocument = xwiki.getDocument(this.migrator.resolver.resolve(patientFamilyRef), this.context);
                // TODO --- update the family pedigree object only for allowed types of relatives---

                // If one relative has a family
            } else if (!"".equals(relativesFamilyRef)) {
                // set relative family reference to patient doc
                this.migrator.familyMigrations.setFamilyReference(patientXDocument, relativesFamilyRef, this.context);
                // set family references for all relatives docs
                setAllFamilyRefs(patientXDocument, relativesFamilyRef, relativesDocList, xwiki);
                familyXDocument = xwiki.getDocument(this.migrator.resolver.resolve(relativesFamilyRef), this.context);
                // TODO --- update the family pedigree object only for allowed types of relatives---

                // If no one has a family yet
            } else {
                try {
                    familyXDocument = this.createFamilyWithPedigree(patientXDocument, relativesDocList);
                } catch (Exception e) {
                    this.migrator.logger.error("Could not create a new family document: {}", e.getMessage());
                }
                String familyDocumentRef = familyXDocument.getDocumentReference().toString();
                // set new family reference to patient doc
                this.migrator.familyMigrations.setFamilyReference(patientXDocument, familyDocumentRef, this.context);
                // set family references for all relatives docs
                setAllFamilyRefs(patientXDocument, familyDocumentRef, relativesDocList, xwiki);
            }
            return familyXDocument;
        }

        /**
         * A new family document with pedigree object is created for a patient with relatives. A family pedigree is
         * created only for relatives from a list ["parent", "child", "sibling", "twin"].
         *
         * @throws Exception
         * @throws XWikiException
         * @throws QueryException
         */
        private XWikiDocument createFamilyWithPedigree(XWikiDocument patientXDocument,
            Map<String, XWikiDocument> relativesDocList) throws QueryException, XWikiException, Exception
        {
            // TODO *****get new pedigree data and image via API passing over only allowed types of relatives *****
            // if (!allowedRelatives.contains(relativeType))
            JSONObject pedigreeData =
                new JSONObject(
                    "{'GG':[{'id':0,'prop':{'gender':'U','fName':'fm','lName':'fm',"
                        + "'lifeStatus':'alive','externalID':'fm'}"
                        + "}],'ranks':[3],'order':[[],[],[],[0]],'positions':[5]}");
            JSONObject pedigreeImage = new JSONObject("{1:1}");

            // TODO *****Do we need any check here at all?*****
            if (pedigreeData.length() == 0) {
                this.migrator.logger.debug("Can not create pedigree. Patient Id: {}.", patientXDocument.getId());
                return null;
            }

            String patientId = patientXDocument.getDocumentReference().getName();
            JSONObject procesedData = this.migrator.familyMigrations.processPedigree(pedigreeData, patientId);

            this.migrator.logger.debug("Creating new family for patient {}.", patientXDocument.getId());
            XWikiDocument newFamilyXDocument = null;
            newFamilyXDocument =
                this.migrator.familyMigrations.createFamilyDocument(patientXDocument, procesedData,
                    pedigreeImage.toString(), this.context, this.session);

            return newFamilyXDocument;
        }

        /**
         * Set family references for all relatives.
         *
         * @throws XWikiException
         */
        private void setAllFamilyRefs(XWikiDocument patientXDocument, String famReference,
            Map<String, XWikiDocument> relativesDocList, XWiki xwiki) throws XWikiException
        {
            List<String> membersRefsList = new LinkedList<>();
            membersRefsList.add(patientXDocument.getDocumentReference().getName());

            for (String relativeType : relativesDocList.keySet()) {
                // set the family reference to a relative doc
                this.migrator.familyMigrations.setFamilyReference(relativesDocList.get(relativeType), famReference,
                    this.context);
                membersRefsList.add(relativesDocList.get(relativeType).getDocumentReference().getName());
            }
            // add all relatives to family doc as members
            XWikiDocument familyXDocument =
                xwiki.getDocument(this.migrator.resolver.resolve(famReference), this.context);
            BaseObject familyObject = familyXDocument.getXObject(Family.CLASS_REFERENCE);
            if (familyObject == null) {
                familyObject = familyXDocument.newXObject(Family.CLASS_REFERENCE, this.context);
            }
            familyObject.setStringListValue("members", membersRefsList);
        }

        /**
         * Gets the family that patient belongs to, or null if there is no family.
         */
        private String getPatientsFamily(XWikiDocument patientXDocument)
        {
            BaseObject pointer =
                patientXDocument.getXObject(this.migrator.familyMigrations.familyReferenceClassReference);
            if (pointer == null) {
                return null;
            }
            String famReference = pointer.getStringValue(REFERENCE_PROPERTY_NAME);
            if (!StringUtils.isBlank(famReference)) {
                return famReference;
            }
            return null;
        }

        /**
         * Gets a family that relative belongs to, empty string if relatives do not have family, or null if there is
         * more than one family.
         */
        private String getRelativesFamily(Map<String, XWikiDocument> relativesDocList)
        {
            String relativeRef = "";
            int count = 0;
            for (String relativeType : relativesDocList.keySet()) {
                XWikiDocument relativeXDocument = relativesDocList.get(relativeType);
                String famDocReference = getPatientsFamily(relativeXDocument);
                if (!StringUtils.isBlank(famDocReference)) {
                    if (count > 0) {
                        return null;
                    }
                    relativeRef = famDocReference;
                    count++;
                }
            }
            return relativeRef;
        }

        /**
         * Gets the relative XWiki document or null.
         */
        private XWikiDocument getRelativeDoc(String relativeDoc, XWiki xwiki)
            throws XWikiException
        {
            Query rq = this.session.createQuery("select distinct o.name from BaseObject o,"
                + " StringProperty p where o.className = '"
                + this.migrator.serializer.serialize(Patient.CLASS_REFERENCE)
                + "' and p.id.id = o.id and p.id.name = 'external_id' "
                + "and o.name <> 'PhenoTips.PatientTemplate' and p.value = '"
                + relativeDoc + "'");

            @SuppressWarnings("unchecked")
            List<String> relativeDocuments = rq.list();

            if (relativeDocuments.isEmpty()) {
                return null;
            }

            String relativeDocName = relativeDocuments.get(0);
            XWikiDocument relativeXDocument =
                xwiki.getDocument(this.migrator.resolver.resolve(relativeDocName), this.context);
            return relativeXDocument;
        }

        /**
         * Gets the map of patient relative type mapped to relative XWiki document, or null.
         */
        private Map<String, XWikiDocument> getRelativesDocList(List<BaseObject> relativeXObjects, XWiki xwiki)
            throws XWikiException
        {
            if (relativeXObjects == null || relativeXObjects.isEmpty()) {
                return null;
            }
            Map<String, XWikiDocument> relativesDocMap = new HashMap<>();
            for (BaseObject object : relativeXObjects) {
                if (object == null) {
                    continue;
                }

                String relativeType = object.getStringValue(RELATIVE_PROPERTY_NAME);
                String relativeOf = object.getStringValue(RELATIVEOF_PROPERTY_NAME);

                if (StringUtils.isBlank(relativeType) || StringUtils.isBlank(relativeOf)) {
                    continue;
                }
                XWikiDocument relativeXDocument = getRelativeDoc(relativeOf, xwiki);
                if (relativeXDocument == null) {
                    continue;
                }
                relativesDocMap.put(relativeType, relativeXDocument);
            }
            return relativesDocMap;
        }

    }
}
