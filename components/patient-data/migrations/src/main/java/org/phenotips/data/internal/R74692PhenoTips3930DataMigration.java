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

import java.util.List;
import java.util.stream.Collectors;

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
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #3930: Remove blank Pubmed IDs left by 74690 migrator.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named("74692-PT-3930")
@Singleton
public class R74692PhenoTips3930DataMigration extends AbstractHibernateDataMigration implements
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
        return "Clean up empty pubmed id entries.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(74692);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    /**
     * Searches for all documents containing a solved__pubmed_id property and removes blank pubmed ids if any.
     */
    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        Query q =
            session.createQuery("select distinct p from BaseObject o, " + DBStringListProperty.class.getName()
                + " p where o.className = '" + this.serializer.serialize(Patient.CLASS_REFERENCE)
                + "'and p.id.id = o.id and p.id.name = '" + this.propertyName + "'"
                + " and '' in elements(p.list)");

        @SuppressWarnings("unchecked")
        List<DBStringListProperty> properties = q.list();
        this.logger.debug("Found {} pubmed id properties", properties.size());
        for (DBStringListProperty property : properties) {
            try {
                List<String> values = property.getList();
                List<String> newValues = values.stream()
                    // Filter out any blank pubmed ids
                    .filter(pubmedId -> StringUtils.isNotBlank(pubmedId))
                    .collect(Collectors.toList());
                property.setValue(newValues);
                session.update(property);
            } catch (Exception e) {
                this.logger.warn("Failed to update a pubmed id property: {}", e.getMessage());
            }
        }

        XWikiContext context = getXWikiContext();
        context.getWiki().flushCache(context);
        return null;
    }

}
