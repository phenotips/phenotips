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

package org.phenotips.measurements.internal;

import org.xwiki.component.annotation.Component;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #1428: Measurement age will now be stored as a float not an int.
 *
 * @version $Id$
 * @since 1.1RC1
 */
@Component
@Named("R54691Phenotips#1428")
@Singleton
public class R54691PhenoTips1428DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Changing the measurement age property from an int to a float";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54691);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new MigrateAgePropertyCallback());
    }

    /**
     * Searches for all measurement objects and updates their age property to a float.
     */
    private class MigrateAgePropertyCallback implements HibernateCallback<Object>
    {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            String measurementsClassReference = "Phenotips.MeasurementsClass";
            Query q = session.createQuery("select p from " + IntegerProperty.class.getName() + " as p, BaseObject as o"
                + " where o.className='" + measurementsClassReference + "' and p.id.id=o.id and p.id.name='age'");
            @SuppressWarnings("unchecked")
            List<IntegerProperty> oldAgeProperties = q.list();
            for (IntegerProperty oldAgeProperty : oldAgeProperties) {
                FloatProperty newAgeProperty = new FloatProperty();
                newAgeProperty.setId(oldAgeProperty.getId());
                newAgeProperty.setName(oldAgeProperty.getName());
                Integer oldValue = (Integer) oldAgeProperty.getValue();
                if (oldValue != null) {
                    newAgeProperty.setValue((float) oldValue);
                }
                session.delete(oldAgeProperty);
                session.save(newAgeProperty);
            }

            XWikiContext context = getXWikiContext();
            context.getWiki().flushCache(context);
            return null;
        }
    }
}
