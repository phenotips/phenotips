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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
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
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-3509: copy previously-entered cancer data from pedigree to patient sheet.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("R71510-PT-3509")
@Singleton
public class R71510PhenoTips3509DataMigration extends AbstractHibernateDataMigration
    implements XWikiHibernateBaseStore.HibernateCallback<Object>
{
    /** The version number. */
    private static final int VERSION = 71510;

    private static final String PEDIGREE_CLASS_DATA_KEY = "data";

    private static final String MEMBERS_KEY = "members";

    private static final String PROPERTIES_KEY = "properties";

    private static final String ID_KEY = "id";

    private static final String CANCERS_KEY = "cancers";

    /** Pedigree XClass that holds pedigree data (image, structure, etc). */
    private static final EntityReference PEDIGREE_CLASS_REFERENCE =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /** Resolves un-prefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public Object doInHibernate(@Nonnull final Session session) throws HibernateException, XWikiException
    {
        // Select all families.
        final Query query = session.createQuery("select distinct o.name from BaseObject o where o.className= '"
            + this.serializer.serialize(PEDIGREE_CLASS_REFERENCE) + "' and o.name <> 'PhenoTips.FamilyTemplate'");

        @SuppressWarnings("unchecked")
        List<String> docs = query.list();

        this.logger.debug("Found {} documents", docs.size());

        final XWikiContext context = getXWikiContext();
        final XWiki xwiki = context.getWiki();
        // Migrate each family.
        docs.forEach(docName -> migrateFamilyData(docName, context, xwiki, session));
        return null;
    }

    private void migrateFamilyData(
        @Nonnull final String docName,
        @Nonnull final XWikiContext context,
        @Nonnull final XWiki xwiki,
        @Nonnull final Session session)
    {
        try {
            final XWikiDocument familyDocument = xwiki.getDocument(this.resolver.resolve(docName), context);
            if (familyDocument != null) {
                this.logger.debug("Accessing pedigree data for {}", docName);
                final BaseObject pedigreeXObject = familyDocument.getXObject(PEDIGREE_CLASS_REFERENCE);
                if (pedigreeXObject != null) {
                    updateIndividuals(pedigreeXObject, context, xwiki, session, docName);
                }
            }
        } catch (final XWikiException e) {
            this.logger.error("Error obtaining family document {}: [{}]", docName, e.getMessage());
        } catch (final Exception e) {
            this.logger.error("An unexpected exception occurred when migrating family {}", docName, e.getMessage());
        }
    }

    private void updateIndividuals(
        @Nonnull final BaseObject pedigreeXObject,
        @Nonnull final XWikiContext context,
        @Nonnull final XWiki xwiki,
        @Nonnull final Session session,
        @Nonnull final String familyId)
    {
        // Try to retrieve stored pedigree data.
        final String storedData = pedigreeXObject.getStringValue(PEDIGREE_CLASS_DATA_KEY);
        if (StringUtils.isNotBlank(storedData)) {
            // Try to construct a JSONObject from the storedData string.
            final JSONObject pedigree = new JSONObject(storedData);
            // Try to get the family members.
            final JSONArray members = pedigree.optJSONArray(MEMBERS_KEY);
            if (members != null && members.length() > 0) {
                // For each family member, get the properties since it contains ID and cancer data.
                IntStream.range(0, members.length())
                    .mapToObj(members::optJSONObject)
                    .filter(Objects::nonNull)
                    .map(member -> member.optJSONObject(PROPERTIES_KEY))
                    .filter(Objects::nonNull)
                    .forEach(memberProperties -> updateIndividual(memberProperties, context, xwiki, session));
            } else {
                this.logger.debug("Family {} has no members", familyId);
            }
        }

    }

    private void updateIndividual(
        @Nonnull final JSONObject properties,
        @Nonnull final XWikiContext context,
        @Nonnull final XWiki xwiki,
        @Nonnull final Session session)
    {
        // We're looking at pedigree data of an individual.
        final String patientId = properties.optString(ID_KEY);
        try {
            if (StringUtils.isNotBlank(patientId)) {
                // Try to get the patient document.
                final XWikiDocument doc = xwiki.getDocument(this.resolver.resolve(patientId), context);
                migrateCancers(properties, patientId, doc, context, session);
            } else {
                this.logger.debug("Data is not associated with any patient");
            }
        } catch (final XWikiException e) {
            this.logger.error("Error obtaining patient document {}: [{}]", patientId, e.getMessage());
        } catch (final DataMigrationException e) {
            this.logger.error("Could not save cancer data for patient with ID {}: [{}]", patientId, e.getMessage());
        } catch (final Exception e) {
            this.logger.error("An unexpected exception occurred when migrating patient {}", patientId, e.getMessage());
        }
    }

    private void migrateCancers(
        @Nonnull final JSONObject properties,
        @Nonnull final String patientId,
        @Nonnull final XWikiDocument doc,
        @Nonnull final XWikiContext context,
        @Nonnull final Session session) throws DataMigrationException, XWikiException
    {
        final JSONArray cancers = properties.optJSONArray(CANCERS_KEY);
        if (cancers != null && cancers.length() > 0) {
            // For each cancer, wrap it in a Cancer object, and write the data to the patient doc.
            IntStream.range(0, cancers.length())
                .mapToObj(cancers::optJSONObject)
                .filter(Objects::nonNull)
                .map(PhenoTipsCancer::new)
                .forEach(cancer -> cancer.write(doc, context));
            saveData(doc, session, context);
        } else {
            // Patient has no cancer data. Do nothing.
            this.logger.debug("Patient {} has no associated cancer data", patientId);
        }
    }

    /**
     * Saves the updated data in {@code xDocument}.
     *
     * @param xDocument the XWikiDocument containing updated cancer data
     * @param session the {@link Session}
     * @param context the {@link XWikiContext}
     * @throws DataMigrationException if the {@link XWikiHibernateStore} cannot be obtained
     * @throws XWikiException if the {@link XWikiDocument} cannot be saved
     */
    private void saveData(
        @Nonnull final XWikiDocument xDocument,
        @Nonnull final Session session,
        @Nonnull final XWikiContext context)
        throws DataMigrationException, XWikiException
    {
        xDocument.setComment(this.getDescription());
        xDocument.setMinorEdit(true);
        // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
        // so we must clear the session cache first.
        session.clear();
        ((XWikiHibernateStore) getStore()).saveXWikiDoc(xDocument, context, false);
        session.flush();
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public String getDescription()
    {
        return "Copy pedigree cancer data to patient sheet";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(VERSION);
    }
}
