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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #2991: populates {@code life_status} from {@code date_of_death_unknown} or
 * {@code date_of_death} properties. If date of death is present (either as "unknown" or as actual date) then life
 * status is "deceased" otherwise life status is "alive". The migrator also removes the {@code date_of_death_unknown}
 * property
 *
 * @version $Id$
 * @since 1.3M6
 */
@Component
@Named("R71501PhenoTips#2991")
@Singleton
public class R71501PhenoTips2991DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    private static final String DATE_OF_DEATH = "date_of_death";

    private static final String DATE_OF_DEATH_UNKNOWN = "date_of_death_unknown";

    private static final String DATE_OF_DEATH_ENTERED = "date_of_death_entered";

    private static final String DATA_NAME = "life_status";

    private static final String ALIVE = "alive";

    private static final String DECEASED = "deceased";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public String getDescription()
    {
        return "A migrator to populate life status.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71501);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    /**
     * Searches for all documents containing a date_of_death_unknown or date_of_death property and populates
     * life_status.
     */
    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className= '"
                + this.serializer.serialize(Patient.CLASS_REFERENCE)
                + "' and o.name <> 'PhenoTips.PatientTemplate'");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();

        this.logger.debug("Found {} patient documents", docs.size());

        for (String docName : docs) {
            XWikiDocument patientXDocument = null;

            try {
                patientXDocument = xwiki.getDocument(this.resolver.resolve(docName), context);
                if (patientXDocument == null) {
                    continue;
                }

                BaseObject data = patientXDocument.getXObject(Patient.CLASS_REFERENCE);
                if (data == null) {
                    continue;
                }

                StringProperty newProperty = new StringProperty();
                newProperty.setName(DATA_NAME);
                newProperty.setValue(getLifeStatus(data));
                data.addField(DATA_NAME, newProperty);

                data.removeField(DATE_OF_DEATH_UNKNOWN);

                patientXDocument.setComment(this.getDescription());
                patientXDocument.setMinorEdit(true);
            } catch (Exception e) {
                this.logger.warn("Failed to update a global mode of inheritance property: {}", e.getMessage());
            }

            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(patientXDocument, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                //
            }
        }
        return null;
    }

    private String getLifeStatus(BaseObject data)
    {
        String lifeStatus = ALIVE;
        Date date = data.getDateValue(DATE_OF_DEATH);
        String dodEntered = data.getStringValue(DATE_OF_DEATH_ENTERED);
        if (date != null || (StringUtils.isNotBlank(dodEntered) && !"{}".equals(dodEntered))) {
            lifeStatus = DECEASED;
        } else {
            // check if "unknown death date" checkbox is checked
            Integer deathDateUnknown = data.getIntValue(DATE_OF_DEATH_UNKNOWN);
            if (deathDateUnknown == 1) {
                lifeStatus = DECEASED;
            }
        }
        return lifeStatus;
    }

}
