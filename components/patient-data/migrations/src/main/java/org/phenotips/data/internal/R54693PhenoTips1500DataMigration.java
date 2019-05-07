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
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.ArrayList;
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
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #1500: Automatically migrate {@code global_mode_of _inheritance} from
 * {@code StringProperty} to {@code DBStringListProperty} to allow for the selection of multiple modes of inheritance.
 *
 * @version $Id$
 * @since 1.2M1
 */
@Component
@Named("R54693PhenoTips#1500")
@Singleton
public class R54693PhenoTips1500DataMigration extends AbstractHibernateDataMigration
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Multiple values should be selectable for global mode of inheritance.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54693);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new UpdateModeOfInheritanceCallback());
    }

    /**
     * Searches for all documents containing a global_mode_of_inheritance property and changes that property type from
     * {@code StringProperty} to {@code DBStringListProperty}.
     */
    private final class UpdateModeOfInheritanceCallback implements XWikiHibernateBaseStore.HibernateCallback<Object>
    {
        /** The names of the properties to fix. */
        private final String propertyName = "global_mode_of_inheritance";

        @Override
        public Object doInHibernate(Session session) throws HibernateException, XWikiException
        {
            Query q =
                session.createQuery("select distinct p from BaseObject o, StringProperty p where o.className = '"
                    + R54693PhenoTips1500DataMigration.this.serializer.serialize(Patient.CLASS_REFERENCE)
                    + "'and p.id.id = o.id and p.id.name = '" + this.propertyName + "'");

            @SuppressWarnings("unchecked")
            List<StringProperty> properties = q.list();
            R54693PhenoTips1500DataMigration.this.logger.debug("Found {} global mode of inheritance properties",
                properties.size());
            for (StringProperty oldProperty : properties) {
                try {
                    DBStringListProperty newProperty = new DBStringListProperty();
                    newProperty.setName(oldProperty.getName());
                    newProperty.setId(oldProperty.getId());

                    List<String> newValue = new ArrayList<String>();
                    if (oldProperty.getValue() != null) {
                        newValue.add(oldProperty.getValue());
                        newProperty.setValue(newValue);
                    }
                    session.delete(oldProperty);
                    session.save(newProperty);
                } catch (Exception e) {
                    R54693PhenoTips1500DataMigration.this.logger
                        .warn("Failed to update a global mode of inheritance property: {}", e.getMessage());
                }
            }

            XWikiContext context = getXWikiContext();
            context.getWiki().flushCache(context);
            return null;
        }
    }
}
