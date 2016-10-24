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
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

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
 * Migration for PhenoTips issue PT-2777: Currently genes are being stored internally as gene symbols, it is better to
 * store them using Ensembl ID, as this is more stable.
 * For each {@code gene} field, switch the value from the HGNC symbol to the Ensembl ID.
 *
 * @version $Id$
 * @since 1.3M4
 */
@Component
@Named("R71499-PT-2777")
@Singleton
public class R71499PhenoTips2777DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    private static final String GENE_NAME = "gene";

    private static final String HGNC = "HGNC";

    private static final EntityReference GENE_CLASS = new EntityReference("GeneClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

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

    /** Provides access to the all available vocabularies. */
    @Inject
    private VocabularyManager vocabularies;

    private Vocabulary hgnc;

    @Override
    public String getDescription()
    {
        return "Migrate all existing gene values from HGNC symbols to Ensembl IDs";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71499);
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
        final DocumentReference geneClassReference = this.entityResolver.resolve(GENE_CLASS);
        this.hgnc = this.vocabularies.getVocabulary(HGNC);

        final Query q =
                session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + this.serializer.serialize(geneClassReference)
                + "' and exists(from StringProperty p where p.id.id = o.id and p.id.name = '"
                + GENE_NAME + "' and p.value <> '')");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();
        for (String docName : docs) {
            final XWikiDocument doc = xwiki.getDocument(this.resolver.resolve(docName), context);
            migrateGeneValues(doc, geneClassReference);
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

    private void migrateGeneValues(final XWikiDocument doc, final DocumentReference geneClassReference)
            throws HibernateException, XWikiException
    {
        final List<BaseObject> genes = doc.getXObjects(geneClassReference);
        if (genes == null) {
            return;
        }
        for (final BaseObject gene : genes) {
            if (gene == null) {
                continue;
            }
            final StringProperty oldGeneNameProp = (StringProperty) gene.get(GENE_NAME);
            if (oldGeneNameProp == null || StringUtils.isBlank(oldGeneNameProp.getValue())) {
                continue;
            }
            final String ensemblId = getEnsemblId(oldGeneNameProp.getValue());
            gene.setStringValue(GENE_NAME, ensemblId);
        }
    }

    /**
     * Gets EnsemblID corresponding to the HGNC symbol.
     * @param geneSymbol the string representation of a gene symbol (e.g. NOD2).
     * @return the string representation of the corresponding Ensembl ID.
     */
    private String getEnsemblId(final String geneSymbol) {
        final VocabularyTerm term = this.hgnc.getTerm(geneSymbol);
        @SuppressWarnings("unchecked")
        final List<String> ensemblIdList = term != null ? (List<String>) term.get("ensembl_gene_id") : null;
        final String ensemblId = ensemblIdList != null && !ensemblIdList.isEmpty() ? ensemblIdList.get(0) : null;
        // Retain information as is if we can't find Ensembl ID.
        return ensemblId != null ? ensemblId : geneSymbol;
    }
}
