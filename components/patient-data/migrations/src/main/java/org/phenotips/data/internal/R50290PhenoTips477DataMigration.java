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
import org.xwiki.model.reference.DocumentReference;
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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #477: Automatically migrate existing {@code onset} values to the new
 * {@code age_of_onset} field.
 *
 * @version $Id$
 * @since 1.0M7
 */
@Component
@Named("R50290Phenotips#477")
@Singleton
public class R50290PhenoTips477DataMigration extends AbstractHibernateDataMigration
{
    /**
     * Onsets, as defined in HPO. Not all the terms in HPO are used, just the relevant ones.
     */
    private enum HpoOnsets
    {
        /** From birth to 28 days (rounded to a month, since the onset has month granularity). */
        NEONATAL("HP:0003623", 1),
        /** From a month to a year. */
        INFANTILE("HP:0003593", 12),
        /** One to five years. */
        CHILDHOOD("HP:0011463", 60),
        /** Five to 15 years. */
        JUVENILE("HP:0003621", 180),
        /** 15 to 40 years. */
        YOUNG_ADULT("HP:0011462", 480),
        /** 40 to 60 years. */
        MIDDLE_AGE("HP:0003596", 720),
        /** After 60 years. */
        LATE("HP:0003584", Integer.MAX_VALUE);

        /** The identifier of the associated HPO term. */
        public String term;

        /** The upper age limit of this onset. */
        public int upperAgeLimit;

        /**
         * Simple constructor.
         *
         * @param term see {@link #term}
         * @param upperAgeLimit see {@link #upperAgeLimit}
         */
        HpoOnsets(String term, int upperAgeLimit)
        {
            this.term = term;
            this.upperAgeLimit = upperAgeLimit;
        }
    }

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
        return "Migrate existing onset values to the new age_of_onset field";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(50290);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateOnsetCallback());
    }

    /**
     * Searches for all documents containing values for the {@code onset} property, and for each such document and for
     * each such object, updates (or creates) the value for {@code age_of_onset} according to the HPO definitions of
     * possible onset. If the object already has a new age of onset, nothing is updated. If the old onset is {@code -1},
     * which corresponds to the default "congenital onset", the it is not migrated, since this could indicate both an
     * explicit congenital onset, or the fact that the user didn't set an onset and left the default value.
     */
    private class MigrateOnsetCallback implements HibernateCallback<Object>
    {
        /** The name of the old onset property. */
        private static final String OLD_ONSET_NAME = "onset";

        /** The name of the new onset property. */
        private static final String NEW_ONSET_NAME = "age_of_onset";

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            XWikiContext context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            DocumentReference classReference =
                new DocumentReference(context.getWikiId(), Constants.CODE_SPACE, "PatientClass");
            BaseClass cls = xwiki.getXClass(classReference, context);
            Query q =
                session.createQuery("select distinct o.name from BaseObject o, IntegerProperty p where o.className = '"
                    + R50290PhenoTips477DataMigration.this.serializer.serialize(classReference)
                    + "' and p.id.id = o.id and p.id.name = '" + OLD_ONSET_NAME + "' and p.value IS NOT NULL");
            @SuppressWarnings("unchecked")
            List<String> documents = q.list();
            for (String docName : documents) {
                XWikiDocument doc =
                    xwiki.getDocument(R50290PhenoTips477DataMigration.this.resolver.resolve(docName), context);
                BaseObject object = doc.getXObject(classReference);
                IntegerProperty oldOnset = (IntegerProperty) object.get(OLD_ONSET_NAME);
                StringProperty newOnset = (StringProperty) object.get(NEW_ONSET_NAME);
                if (oldOnset == null || (newOnset != null && StringUtils.isNotBlank(newOnset.getValue()))) {
                    continue;
                }
                object.removeField(OLD_ONSET_NAME);
                int value = (Integer) oldOnset.getValue();
                if (value == -1) {
                    // We can't say if this is an actual congenital onset or an unset value... Discard it
                    continue;
                }
                if (newOnset == null) {
                    newOnset = (StringProperty) ((PropertyClass) cls.get(NEW_ONSET_NAME)).newProperty();
                    object.safeput(NEW_ONSET_NAME, newOnset);
                }
                for (HpoOnsets onset : HpoOnsets.values()) {
                    if (value <= onset.upperAgeLimit) {
                        newOnset.setValue(onset.term);
                        break;
                    }
                }
                doc.setComment("Migrated onset to age_of_onset");
                doc.setMinorEdit(true);
                try {
                    // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                    // so we must clear the session cache first.
                    session.clear();
                    ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                    session.flush();
                } catch (DataMigrationException e) {
                    // We're in the middle of a migration, we're not expecting another migration
                }
            }
            return null;
        }
    }
}
