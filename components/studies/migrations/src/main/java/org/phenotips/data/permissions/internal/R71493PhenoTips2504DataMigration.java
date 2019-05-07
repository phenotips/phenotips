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
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-2504: After upgrade from 1.2 to 1.3, some studies have form configuration set with
 * gene variants hidden. Update all studies to reference the new "gene-variants" field instead of the old "genes" one.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("R71493-PT-2504")
@Singleton
public class R71493PhenoTips2504DataMigration extends AbstractHibernateDataMigration
    implements HibernateCallback<Object>
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    private XWikiContext context;

    @Override
    public String getDescription()
    {
        return "Update all studies to reference to use DBStringListProperty instead of StringListProperty";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71493);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        this.context = getXWikiContext();
        Query q =
            session.createQuery("select p from " + DBStringListProperty.class.getName() + " as p, BaseObject as o"
                + " where o.className='PhenoTips.StudyClass' and p.id.id=o.id"
                + " and 'org.phenotips.patientSheet.field.genes' in elements(p.list)");
        @SuppressWarnings("unchecked")
        List<DBStringListProperty> properties = q.list();
        this.logger.debug("Found {} studies using the old 'genes' field", properties.size());
        for (DBStringListProperty property : properties) {
            List<String> values = property.getList();
            values.set(values.indexOf("org.phenotips.patientSheet.field.genes"),
                "org.phenotips.patientSheet.field.gene-variants");
            values.remove("org.phenotips.patientSheet.field.rejected_genes");
            session.update(property);
        }
        this.context.getWiki().flushCache(this.context);
        return null;
    }
}
