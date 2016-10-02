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

import org.phenotips.data.Patient;
import org.phenotips.studies.family.Pedigree;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #2154: migrating existing pedigrees into families with one member.
 *
 * @version $Id$
 * @since 1.3M3
 */
@Component
@Named("R71497PhenoTips#2154")
@Singleton
public class R71497PhenoTips2154DataMigration extends AbstractHibernateDataMigration
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
        return "Creating family for patients with a pedigree and moving pedigree object to the family document.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71497);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigratePedigreeCallback());
    }

    private class MigratePedigreeCallback implements HibernateCallback<Object>
    {
        private R71497PhenoTips2154DataMigration migrator = R71497PhenoTips2154DataMigration.this;

        private Session session;

        private XWikiContext context;

        @Override
        public Object doInHibernate(Session hSession) throws HibernateException, XWikiException
        {
            session = hSession;
            context = getXWikiContext();
            XWiki xwiki = context.getWiki();

            // Select all patients
            Query q =
                session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                    + migrator.serializer.serialize(Patient.CLASS_REFERENCE)
                    + "' and o.name <> 'PhenoTips.PatientTemplate'");

            @SuppressWarnings("unchecked")
            List<String> documents = q.list();

            migrator.logger.debug("Found {} patient documents", documents.size());

            for (String docName : documents) {

                XWikiDocument patientXDocument = xwiki.getDocument(migrator.resolver.resolve(docName), context);
                if (patientXDocument == null) {
                    continue;
                }

                XWikiDocument newFamilyXDocument = this.importPedigreeToFamily(patientXDocument);
                if (newFamilyXDocument == null) {
                    migrator.logger.error("Could not create a family. Patient Id: {}.", docName);
                    continue;
                }

                String familyDocumentRef = newFamilyXDocument.getDocumentReference().toString();

                migrator.familyMigrations.setFamilyReference(patientXDocument, familyDocumentRef, context);
                patientXDocument.removeXObject(patientXDocument.getXObject(Pedigree.CLASS_REFERENCE));
                patientXDocument.setComment(migrator.getDescription());
                patientXDocument.setMinorEdit(true);

                newFamilyXDocument.setComment(migrator.getDescription());

                try {
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(patientXDocument, context, false);
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(newFamilyXDocument, context, false);
                    session.flush();
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                } finally {
                    xwiki.flushCache(context);
                    migrator.logger.debug("Updated [{}]", docName);
                }
            }

            return null;
        }

        /**
         * Creates a new family document with processed pedigree object for a patient with an old version pedigree.
         * Patient is assigned to a new family as a member.
         */
        private XWikiDocument importPedigreeToFamily(XWikiDocument patientXDocument) throws XWikiException
        {
            BaseObject pedigreeXObject = patientXDocument.getXObject(Pedigree.CLASS_REFERENCE);
            if (pedigreeXObject == null) {
                migrator.logger.debug("Patient does not have pedigree. Patient Id: {}.", patientXDocument.getId());
                return null;
            }

            BaseStringProperty data = (BaseStringProperty) pedigreeXObject.get("data");
            BaseStringProperty image = (BaseStringProperty) pedigreeXObject.get("image");

            String dataText = data.toText();
            String imageText = image.toText();

            if (StringUtils.isEmpty(dataText) || StringUtils.isEmpty(imageText)) {
                migrator.logger.debug(
                    "Patient does not have pedigree data or pedigree image properties. Patient Id: {}.",
                    patientXDocument.getId());
                return null;
            }

            String patientId = patientXDocument.getDocumentReference().getName();
            JSONObject procesedData = migrator.familyMigrations.processPedigree(new JSONObject(dataText), patientId);

            migrator.logger.debug("Creating new family for patient {}.", patientXDocument.getId());
            XWikiDocument newFamilyXDocument = null;
            try {
                newFamilyXDocument =
                    migrator.familyMigrations.createFamilyDocument(patientXDocument, procesedData, imageText, context,
                        session);
            } catch (Exception e) {
                migrator.logger.error("Could not create a new family document: {}", e.getMessage());
            }

            return newFamilyXDocument;
        }

    }
}
