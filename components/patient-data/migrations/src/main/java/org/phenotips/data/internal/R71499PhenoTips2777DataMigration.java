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
 * store them using Ensembl ID, as this is more stable. For each {@code gene} field in {@code GeneClass}, switch the
 * value from the HGNC symbol to the Ensembl ID. For each {@code genesymbol} field in {@code GeneVariantClass}, rename
 * the field to {@code gene} and switch the value from HGNC symbol to the Ensembl ID.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Component
@Named("R71499-PT-2777")
@Singleton
public class R71499PhenoTips2777DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    private static final String GENE_NAME = "gene";

    private static final String OLD_GENE_NAME = "genesymbol";

    private static final String HGNC = "HGNC";

    private static final EntityReference GENE_CLASS = new EntityReference("GeneClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

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

    /** Provides access to the all available vocabularies. */
    @Inject
    private VocabularyManager vocabularies;

    private Vocabulary hgnc;

    @Override
    public String getDescription()
    {
        return "Migrate all existing gene values from HGNC symbols to Ensembl IDs; rename all genesymbol values to gene"
            + "and migrate from HGNC symbols to Ensembl IDs";
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
        final DocumentReference geneVariantClassReference = this.entityResolver.resolve(GENE_VARIANT_CLASS);
        this.hgnc = this.vocabularies.getVocabulary(HGNC);

        final Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + this.serializer.serialize(geneClassReference) + "' or o.className = '"
                + this.serializer.serialize(geneVariantClassReference)
                + "' and exists(from StringProperty p where p.id.id = o.id and p.id.name = '"
                + GENE_NAME + "' or p.id.name = '" + OLD_GENE_NAME + "' and p.value <> '')");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();
        for (String docName : docs) {
            final XWikiDocument doc = xwiki.getDocument(this.resolver.resolve(docName), context);
            // Migrate all "gene" fields to Ensembl IDs.
            migrateGenes(doc, geneClassReference);
            // Rename all "genesymbol" fields to "gene", and migrate to Ensembl IDs.
            migrateGeneVariants(doc, geneVariantClassReference);
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

    /**
     * For each {@code gene} field, switch the value from HGNC symbol to Ensembl ID, if provided.
     *
     * @param doc XWiki document
     * @param geneClassReference reference to {@code GeneClass}
     * @throws XWikiException if property value cannot be set
     */
    private void migrateGenes(final XWikiDocument doc, final DocumentReference geneClassReference) throws XWikiException
    {
        final List<BaseObject> genes = doc.getXObjects(geneClassReference);
        if (genes == null) {
            return;
        }
        for (final BaseObject gene : genes) {
            setPropertyValue(gene, GENE_NAME);
        }
    }

    /**
     * For each {@code genesymbol} field, rename it to {@code gene} and swtich the value from HGNC symbol to Ensembl ID,
     * if provided.
     *
     * @param doc XWiki document
     * @param geneVariantClassReference reference to {@code GeneVariantClass}
     * @throws XWikiException if property value cannot be set
     */
    private void migrateGeneVariants(final XWikiDocument doc, final DocumentReference geneVariantClassReference)
        throws XWikiException
    {
        final List<BaseObject> variants = doc.getXObjects(geneVariantClassReference);
        if (variants == null) {
            return;
        }
        for (final BaseObject variant : variants) {
            final StringProperty oldGeneName = setPropertyValue(variant, OLD_GENE_NAME);

            // Rename all "genesymbol" properties to "gene".
            if (oldGeneName != null) {
                variant.removeField(OLD_GENE_NAME);
                final StringProperty newGeneName = (StringProperty) oldGeneName.clone();
                newGeneName.setName(GENE_NAME);
                variant.addField(GENE_NAME, newGeneName);
            }
        }
    }

    /**
     * Sets the value to Ensembl ID for a given property.
     *
     * @param baseObject {@code BaseObject} containing the property name
     * @param propertyName the name of the property, for example {@code gene}
     * @return {@code StringProperty} for the provided property name
     * @throws XWikiException if property value cannot be set
     */
    private StringProperty setPropertyValue(final BaseObject baseObject, final String propertyName)
        throws XWikiException
    {
        if (baseObject == null) {
            return null;
        }

        final StringProperty oldBaseObjProp = (StringProperty) baseObject.get(propertyName);
        if (oldBaseObjProp == null) {
            return null;
        }
        // Want to return oldBaseObjProp in case it is not null, that way it can still be renamed if the
        // genesymbol property, for whatever reason, contains no value.
        final String geneSymbol = oldBaseObjProp.getValue();
        if (StringUtils.isBlank(geneSymbol)) {
            return oldBaseObjProp;
        }

        final String ensemblId = getEnsemblId(geneSymbol);
        baseObject.setStringValue(propertyName, ensemblId);
        return oldBaseObjProp;
    }

    /**
     * Gets EnsemblID corresponding to the HGNC symbol.
     *
     * @param geneSymbol the string representation of a gene symbol (e.g. NOD2).
     * @return the string representation of the corresponding Ensembl ID.
     */
    private String getEnsemblId(final String geneSymbol)
    {
        final VocabularyTerm term = this.hgnc.getTerm(geneSymbol);
        @SuppressWarnings("unchecked")
        final List<String> ensemblIdList = term != null ? (List<String>) term.get("ensembl_gene_id") : null;
        final String ensemblId = ensemblIdList != null && !ensemblIdList.isEmpty() ? ensemblIdList.get(0) : null;
        // Retain information as is if we can't find Ensembl ID.
        return ensemblId != null ? ensemblId : geneSymbol;
    }
}
