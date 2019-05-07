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
import org.xwiki.model.reference.DocumentReferenceResolver;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #670: Automatically migrate {@code target_property_category} values from
 * {@code StringList} to {@code DBStringList}.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Named("R52091Phenotips#670")
@Singleton
public class R52091PhenoTips670DataMigration extends AbstractHibernateDataMigration
{
    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public String getDescription()
    {
        return "Migrate target_property_category values from StringList to DBStringList";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(52091);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        BaseClass pcc = context.getWiki().getXClass(this.resolver.resolve("PhenoTips.PhenotypeCategoryClass"), context);
        DBListClass tpc = (DBListClass) pcc.getField("target_property_category");
        getStore().executeWrite(getXWikiContext(), new MigrateCustomPhenotypeCategoryCallback(tpc));
    }

    /**
     * Searches for all values for the {@code target_property_category} property stored as {@code StringList}
     * properties, and for each such value converts it to a {@code DBStringList} property.
     */
    private class MigrateCustomPhenotypeCategoryCallback implements HibernateCallback<Object>
    {
        /** The correct property that should be used for storing the custom phenotype category. */
        private DBListClass propertyClass;

        /**
         * Simple constructor passing the {@link #propertyClass expected property class}.
         *
         * @param propertyClass see {@link #propertyClass}
         */
        MigrateCustomPhenotypeCategoryCallback(DBListClass propertyClass)
        {
            this.propertyClass = propertyClass;
        }

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            Query q =
                session.createQuery("select p from " + StringListProperty.class.getName() + " as p, BaseObject as o"
                    + " where o.className=? and p.id=o.id and p.name=?");
            q.setString(0, this.propertyClass.getObject().getName()).setString(1, this.propertyClass.getName());
            @SuppressWarnings("unchecked")
            List<StringListProperty> wrongProperties = q.list();
            for (StringListProperty oldValue : wrongProperties) {
                DBStringListProperty newValue = (DBStringListProperty) this.propertyClass.newProperty();
                newValue.setId(oldValue.getId());
                newValue.setName(oldValue.getName());
                newValue.setValue(oldValue.getList());
                session.delete(oldValue);
                session.save(newValue);
            }

            XWikiContext context = getXWikiContext();
            context.getWiki().flushCache(context);
            return null;
        }
    }
}
