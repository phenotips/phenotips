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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration for PhenoTips issue #3335: Make Obstetric History exportable and pushable.
 *
 * @version $Id$
 * @since 1.4m2
 */
@Component(roles = { HibernateDataMigration.class })
@Named("71507-PT-3335")
@Singleton
public class R71507PhenoTips3335DataMigration extends AbstractHibernateDataMigration implements
    XWikiHibernateBaseStore.HibernateCallback<Object>
{
    private static final EntityReference OBSTETRIC_HISTORY_CLASS = new EntityReference("ObstetricHistoryClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference PARENTAL_INFORMATION_CLASS = new EntityReference("ParentalInformationClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String BIRTHS = "pregnancy_history__births";

    private static final String GRAVIDA = "pregnancy_history__gravida";

    private static final String PARA = "pregnancy_history__para";

    private static final String PRETERM = "pregnancy_history__preterm";

    private static final String SAB = "pregnancy_history__sab";

    private static final String TAB = "pregnancy_history__tab";

    private static final String TERM = "pregnancy_history__term";

    private DocumentReference parentalInformationClassReference;

    private DocumentReference obstetricHistoryClassReference;
    /**
     * Serializes the rights name.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /**
     * Resolves unprefixed document names to the current wiki.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Resolves class names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        this.parentalInformationClassReference = this.entityResolver.resolve(PARENTAL_INFORMATION_CLASS);
        this.obstetricHistoryClassReference = this.entityResolver.resolve(OBSTETRIC_HISTORY_CLASS);

        Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + this.serializer.serialize(this.parentalInformationClassReference)
                + "' and exists(from StringProperty p where p.id.id = o.id and p.id.name in ('"
                + BIRTHS + "', '" + GRAVIDA + "', '" + PARA + "', '" + PRETERM + "', '" + SAB + "', '" + TAB + "', '"
                + TERM + "') and p.value IS NOT NULL)");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();
        for (String docName : docs) {
            final XWikiDocument doc = xwiki.getDocument(this.resolver.resolve(docName), context);
            // Migrate the Obstetric History data from the ParentalInformationClass to the ObstetricHistoryClass
            migrateObstetricHistory(doc, context);
            doc.setComment(getDescription());
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

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public String getDescription()
    {
        return "Transfers obstetric history data from the ParentalInformationClass to the ObstetricHistoryClass";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71507);
    }

    /***
     * For each Obstetric History field, remove the values from the parentalInformationClass , if provided.
     *
     * @param doc XWiki document
     * @param context XWiki context
     * @throws XWikiException if property value cannot be set
     */
    private void migrateObstetricHistory(XWikiDocument doc, XWikiContext context)
        throws HibernateException, XWikiException
    {
        BaseObject parentalInformation = doc.getXObject(this.parentalInformationClassReference);
        BaseObject obstetricHistory = doc.newXObject(this.obstetricHistoryClassReference, context);

        for (String propName : new String[] { BIRTHS, GRAVIDA, PARA, PRETERM, SAB, TAB, TERM }) {
            IntegerProperty oldProp = (IntegerProperty) parentalInformation.get(propName);
            if (oldProp != null) {
                Integer propValue = (Integer) oldProp.getValue();
                migrator(parentalInformation, obstetricHistory, propValue, propName);
            }
        }
    }

    private void migrator(BaseObject parental, BaseObject obstetric, Integer value, String pregnancyHistory)
        throws XWikiException
    {
        if (value != null) {
            parental.removeField(pregnancyHistory);
            obstetric.setIntValue(pregnancyHistory, value);
        }
    }
}
