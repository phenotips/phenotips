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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

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
 * Migration for PhenoTips issue #481: Automatically migrate existing {@code relative} values to the new
 * {@code RelativeClass} objects. Searches for all documents containing values for the {@code relative_of} property, and
 * for each such document creates a new {@code PhenoTips.RelativeClass}. The old {@code relative} and
 * {@code relative_of} properties are removed.
 *
 * @version $Id$
 * @since 1.0M7
 */
@Component
@Named("R50291Phenotips#481")
@Singleton
public class R50291PhenoTips481DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    /** The name of the old relative type property. */
    private static final String OLD_TYPE_NAME = "relative";

    /** The name of the new relative type property. */
    private static final String NEW_TYPE_NAME = "relative_type";

    /** The name of the property identifying the target patient. */
    private static final String TARGET_RELATIVE_NAME = "relative_of";

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Migrate existing relative values to the new RelativeClass objects";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(50291);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(Session session) throws HibernateException, XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        DocumentReference patientClassReference =
            new DocumentReference(context.getDatabase(), Constants.CODE_SPACE, "PatientClass");
        DocumentReference relativeClassReference =
            new DocumentReference("RelativeClass", patientClassReference.getLastSpaceReference());
        Query q =
            session.createQuery("select distinct o.name from BaseObject o, StringProperty p where o.className = '"
                + this.serializer.serialize(patientClassReference) + "' and p.id.id = o.id and p.id.name = '"
                + TARGET_RELATIVE_NAME + "' and p.value <> ''");
        @SuppressWarnings("unchecked")
        List<String> documents = q.list();
        for (String docName : documents) {
            XWikiDocument doc = xwiki.getDocument(this.resolver.resolve(docName), context);
            BaseObject patient = doc.getXObject(patientClassReference);
            StringProperty oldType = (StringProperty) patient.get(OLD_TYPE_NAME);
            if (StringUtils.isBlank(oldType.getValue())) {
                continue;
            }
            StringProperty oldTarget = (StringProperty) patient.get(TARGET_RELATIVE_NAME);
            patient.removeField(OLD_TYPE_NAME);
            patient.removeField(TARGET_RELATIVE_NAME);
            BaseObject relative = doc.newXObject(relativeClassReference, context);
            if ("Mother".equals(oldType.getValue()) || "Father".equals(oldType.getValue())) {
                relative.set(NEW_TYPE_NAME, "parent", context);
            } else {
                relative.set(NEW_TYPE_NAME, oldType.getValue().toLowerCase(Locale.ROOT), context);
            }
            relative.set(TARGET_RELATIVE_NAME, oldTarget.getValue(), context);

            doc.setComment("Migrate existing relative values to the new RelativeClass objects.");
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
}
