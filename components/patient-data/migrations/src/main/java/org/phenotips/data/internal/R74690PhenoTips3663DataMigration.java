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

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #3663: Automatically migrate {@code solved__pubmed_id} from {@code StringProperty} to
 * {@code DBStringListProperty} to allow storing multiple Pubmed IDs.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("76490-PT-3663")
@Singleton
public class R74690PhenoTips3663DataMigration extends AbstractHibernateDataMigration implements
    XWikiHibernateBaseStore.HibernateCallback<Object>
{
    private final String propertyName = "solved__pubmed_id";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
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
    public String getDescription()
    {
        return "Make Pubmed ID to be a multiple value field.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(74690);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    /**
     * Searches for all documents containing a solved__pubmed_id property and changes that property type from
     * {@code StringProperty} to {@code DBStringListProperty}.
     */
    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        Query q =
            session.createQuery("select distinct p from BaseObject o, StringProperty p where o.className = '"
                + this.serializer.serialize(Patient.CLASS_REFERENCE)
                + "'and p.id.id = o.id and p.id.name = '" + this.propertyName + "'");

        @SuppressWarnings("unchecked")
        List<StringProperty> properties = q.list();
        this.logger.debug("Found {} pubmed id properties", properties.size());
        for (StringProperty oldProperty : properties) {
            try {
                DBStringListProperty newProperty = new DBStringListProperty();
                newProperty.setName(oldProperty.getName());
                newProperty.setId(oldProperty.getId());

                List<String> newValue = new ArrayList<String>();
                if (StringUtils.isNotBlank(oldProperty.getValue())) {
                    newValue.add(oldProperty.getValue());
                    newProperty.setValue(newValue);
                }
                session.delete(oldProperty);
                session.save(newProperty);
            } catch (Exception e) {
                this.logger.warn("Failed to update a pubmed id property: {}", e.getMessage());
            }
        }

        XWikiContext context = getXWikiContext();
        context.getWiki().flushCache(context);
        return null;
    }

}
