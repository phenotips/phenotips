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
 * Migration for PhenoTips issue PT-3335: Make Obstetric History exportable and pushable.
 *
 * @version $Id$
 * @since 1.4
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

    private static final String OLD_PREFIX = "pregnancy_history__";

    private static final String GRAVIDA = "gravida";

    private static final String PARA = "para";

    private static final String TERM = "term";

    private static final String PRETERM = "preterm";

    private static final String SAB = "sab";

    private static final String TAB = "tab";

    private static final String BIRTHS = "births";

    private static final String[] PROPERTIES = new String[] { GRAVIDA, PARA, TERM, PRETERM, SAB, TAB, BIRTHS };

    /**
     * Serializes the class reference.
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

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + this.serializer.serialize(PARENTAL_INFORMATION_CLASS) + "'");

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

    private void migrateObstetricHistory(XWikiDocument doc, XWikiContext context)
        throws HibernateException, XWikiException
    {
        BaseObject parentalObject = doc.getXObject(PARENTAL_INFORMATION_CLASS);
        BaseObject obstetricObject = doc.getXObject(OBSTETRIC_HISTORY_CLASS, true, context);

        for (String propName : PROPERTIES) {
            IntegerProperty oldProp = (IntegerProperty) parentalObject.get(OLD_PREFIX + propName);
            if (oldProp != null) {
                Integer propValue = (Integer) oldProp.getValue();
                migrateValue(parentalObject, obstetricObject, propValue, propName);
            }
        }
    }

    private void migrateValue(BaseObject parentalObject, BaseObject obstetricObject, Integer propValue, String propName)
    {
        parentalObject.removeField(OLD_PREFIX + propName);
        if (propValue != null) {
            obstetricObject.setIntValue(propName, propValue);
        }
    }
}
