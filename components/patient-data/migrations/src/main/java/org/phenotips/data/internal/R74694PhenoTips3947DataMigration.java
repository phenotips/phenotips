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
import org.xwiki.model.reference.DocumentReference;
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
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue PT-3947: migrator to clean up leading and trailing spaces and to ensure the prefix "c."
 * is lowercase in variants' cDNA field.
 *
 * @version $Id$
 * @since 1.4.5
 */
@Component
@Named("R74694-PT-3947")
@Singleton
public class R74694PhenoTips3947DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    private static final String CDNA_NAME = "cdna";

    private static final EntityReference GENE_VARIANT_CLASS = new EntityReference("GeneVariantClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    /** Serializes the class name without the wiki prefix, to be used in the database query. */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    /** Resolves class names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Override
    public String getDescription()
    {
        return "Migrator to clean up leading and trailing spaces and to ensure the prefix 'c.' is lowercase in"
            + "variants' cDNA field.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(74694);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), this);
    }

    @Override
    public Object doInHibernate(final Session session) throws HibernateException, XWikiException
    {
        final XWikiContext context = getXWikiContext();
        final XWiki xwiki = context.getWiki();
        final DocumentReference geneVariantClassReference = this.entityResolver.resolve(GENE_VARIANT_CLASS);

        final Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + this.serializer.serialize(geneVariantClassReference)
                + "' and exists(from StringProperty p where p.id.id = o.id and p.id.name = '"
                + CDNA_NAME + "' and p.value <> '')");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();
        for (String docName : docs) {
            final XWikiDocument doc = xwiki.getDocument(this.resolver.resolve(docName), context);

            List<BaseObject> variants = doc.getXObjects(geneVariantClassReference);
            if (variants == null) {
                continue;
            }

            for (BaseObject variant : variants) {
                if (variant == null) {
                    continue;
                }
                StringProperty oldBaseObjProp = (StringProperty) variant.get(CDNA_NAME);

                if (oldBaseObjProp == null) {
                    continue;
                }

                String cdna = oldBaseObjProp.getValue();
                oldBaseObjProp.setValue(cdna.trim().replaceFirst("^C\\.", "c."));
            }

            doc.setComment(getDescription());
            doc.setMinorEdit(true);
            try {
                // There's a bug in XWiki which prevents saving an object in the same session that it was loaded,
                // so we must clear the session cache first.
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                //
            }
        }
        return null;
    }
}
