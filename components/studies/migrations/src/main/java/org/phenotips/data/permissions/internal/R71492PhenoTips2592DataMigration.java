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
package org.phenotips.data.permissions.internal;

import org.xwiki.component.annotation.Component;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-2592: Cannot easily query all studies using a specific field. Update all studies to
 * use DBStringListProperty instead of the simple StringListProperty for storing the list of enabled fields and
 * sections.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("R71492-PT-2592")
@Singleton
public class R71492PhenoTips2592DataMigration extends AbstractHibernateDataMigration
    implements HibernateCallback<Object>
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Update all studies to use DBStringListProperty instead of StringListProperty";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71492);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        Query q =
            session.createQuery("select p from " + StringListProperty.class.getName() + " as p, BaseObject as o"
                + " where o.className='PhenoTips.StudyClass' and p.id=o.id");
        @SuppressWarnings("unchecked")
        List<StringListProperty> wrongProperties = q.list();
        this.logger.debug("Found {} study properties of type StringListProperty", wrongProperties.size());
        for (StringListProperty oldValue : wrongProperties) {
            DBStringListProperty newValue = new DBStringListProperty();
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
