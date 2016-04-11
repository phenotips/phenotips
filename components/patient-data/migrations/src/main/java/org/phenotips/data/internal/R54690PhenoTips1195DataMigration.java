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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #1195: Reference genome listed next to VCF file in patient summary should use GRCh
 * notation.
 *
 * @version $Id$
 * @since 1.1M1
 */
@Component
@Named("R54690PhenoTips#1195")
@Singleton
public class R54690PhenoTips1195DataMigration extends AbstractHibernateDataMigration
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

    @Override
    public String getDescription()
    {
        return "Reference genome listed next to VCF file in patient summary should use GRCh notation."
            + " Previously UCSC notation.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54690);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new UpdateRefGenomeCallback());
    }

    /**
     * Searches for all documents containing vcf files and replaces the reference genome name with the GRCh notation.
     */
    private final class UpdateRefGenomeCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** The names of the properties to fix. */
        private final String propertyName = "reference_genome";

        /** Mapping between non-HPO custom terms that need replacing and their equivalent official HPO terms. */
        private final Map<String, String> translations = new HashMap<>();

        private UpdateRefGenomeCallback()
        {
            this.translations.put("hg38", "GRCh38");
            this.translations.put("hg19", "GRCh37");
            this.translations.put("hg18", "NCBI36");
        }

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {

            XWikiContext context = R54690PhenoTips1195DataMigration.this.getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference classReference =
                new DocumentReference(context.getWikiId(), "PhenoTips", "VCF");
            Query q = session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + R54690PhenoTips1195DataMigration.this.serializer.serialize(classReference) + "'");

            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            R54690PhenoTips1195DataMigration.this.logger.debug("Found {} documents with VCF objects",
                documents.size());
            for (String docName : documents) {
                R54690PhenoTips1195DataMigration.this.logger.debug("Checking [{}]", docName);
                XWikiDocument doc =
                    xwiki.getDocument(R54690PhenoTips1195DataMigration.this.resolver.resolve(docName), context);

                boolean modified = false;
                for (BaseObject object : doc.getXObjects(classReference)) {
                    if (object == null) {
                        continue;
                    }

                    StringProperty property = (StringProperty) object.get(this.propertyName);

                    modified = fixValue(property) || modified;

                }
                if (!modified) {
                    continue;
                }

                doc.setComment(R54690PhenoTips1195DataMigration.this.getDescription());
                doc.setMinorEdit(true);
                try {
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                    R54690PhenoTips1195DataMigration.this.logger.debug("Updated [{}]", docName);
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }

        private boolean fixValue(StringProperty property)
        {
            if (property == null) {
                return false;
            }
            boolean modified = false;
            String value = property.getValue();

            if (this.translations.containsKey(value)) {
                R54690PhenoTips1195DataMigration.this.logger.debug(
                    "Replacing {} with {}", value, this.translations.get(value));
                property.setValue(this.translations.get(value));
                modified = true;
            }

            return modified;
        }
    }
}
