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

import java.util.ArrayList;
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
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #1280: automatically migrate old candidate, rejected and solved genes to the new
 * unified genes data structure.
 * <ul>
 * <li>For each {@code InvestigationClass} object create a new {@code GeneClass} object, copying the {@code gene} and
 * {@code comments}) fields, and set the {@code status} as "candidate".</li>
 * <li>For each {@code RejectedGenesClass} object create a new {@code GeneClass} object, copying the {@code gene} and
 * {@code comments}) fields, and set the {@code status} as "rejected".</li>
 * <li>If the {@code PatientClass} has a non-empty {@code solved__gene_id} property, create a new {@code GeneClass}
 * object, copying the {@code gene} field, and set the {@code status} as "solved".</li>
 * <li>Successfully migrated objects are removed.</li>
 * </ul>
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("R71490PhenoTips#1280")
@Singleton
public class R71490PhenoTips1280DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    private static final String GENE_NAME = "gene";

    private static final String COMMENTS_NAME = "comments";

    private static final String SOLVED_NAME = "solved__gene_id";

    private static final String STATUS_NAME = "status";

    private static final String OR = "' or o.className = '";

    private static final EntityReference PATIENT_CLASS = new EntityReference("PatientClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference INVESTIGATION_CLASS = new EntityReference("InvestigationClass",
        EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference GENE_CLASS = new EntityReference("GeneClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference REJECTED_CLASS = new EntityReference("RejectedGenesClass",
        EntityType.DOCUMENT,
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

    @Override
    public String getDescription()
    {
        return "Migrate all existing gene values to the GeneClass objects";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(71490);
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
        DocumentReference patientClassReference = this.entityResolver.resolve(PATIENT_CLASS);
        DocumentReference investigationClassReference = this.entityResolver.resolve(INVESTIGATION_CLASS);
        DocumentReference geneClassReference = this.entityResolver.resolve(GENE_CLASS);
        DocumentReference rejectedGenesClassReference = this.entityResolver.resolve(REJECTED_CLASS);

        Query q =
            session.createQuery("select distinct o.name from BaseObject o where o.className = '"
                + this.serializer.serialize(investigationClassReference) + OR
                + this.serializer.serialize(rejectedGenesClassReference) + OR
                + this.serializer.serialize(patientClassReference)
                + "' and exists(from StringProperty p where p.id.id = o.id and p.id.name = '"
                + SOLVED_NAME + "' and p.value <> '')");

        @SuppressWarnings("unchecked")
        List<String> docs = q.list();
        for (String docName : docs) {
            XWikiDocument doc = xwiki.getDocument(this.resolver.resolve(docName), context);
            List<String> geneList = new ArrayList<>();
            migrateSolvedGenes(doc, patientClassReference, geneClassReference, context, geneList);
            migrateGenes(doc, rejectedGenesClassReference, geneClassReference, context, geneList, "rejected");
            migrateGenes(doc, investigationClassReference, geneClassReference, context, geneList, "candidate");
            doc.setComment("Migrate old candidate/rejected/solved genes to GeneClass objects");
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

    private void migrateSolvedGenes(XWikiDocument doc, DocumentReference patientClassReference,
        DocumentReference geneClassReference, XWikiContext context, List<String> geneList)
        throws HibernateException, XWikiException
    {
        BaseObject patient = doc.getXObject(patientClassReference);
        StringProperty oldTarget = (StringProperty) patient.get(SOLVED_NAME);
        if (oldTarget == null) {
            return;
        }
        patient.removeField(SOLVED_NAME);
        String geneName = oldTarget.getValue();
        if (!StringUtils.isBlank(geneName)) {
            BaseObject gene = doc.newXObject(geneClassReference, context);
            gene.setStringValue(GENE_NAME, geneName);
            gene.setStringValue(STATUS_NAME, "solved");
            geneList.add(geneName);
        }
    }

    private void migrateGenes(XWikiDocument doc, DocumentReference oldGenesClassReference,
        DocumentReference geneClassReference, XWikiContext context, List<String> geneList, String status)
        throws HibernateException, XWikiException
    {
        List<BaseObject> genes = doc.getXObjects(oldGenesClassReference);
        if (genes == null || genes.isEmpty()) {
            return;
        }
        for (BaseObject gene : genes) {
            if (gene == null) {
                continue;
            }
            StringProperty oldGeneNameProp = (StringProperty) gene.get(GENE_NAME);
            if (oldGeneNameProp == null || StringUtils.isBlank(oldGeneNameProp.getValue())) {
                continue;
            }
            String geneName = oldGeneNameProp.getValue();
            LargeStringProperty oldGeneCommentsProp = (LargeStringProperty) gene.get(COMMENTS_NAME);
            String geneComments = null;
            if (oldGeneCommentsProp != null && !StringUtils.isBlank(oldGeneCommentsProp.getValue())) {
                geneComments = oldGeneCommentsProp.getValue();
            }
            // check if we already have migrated this gene
            if (!geneList.contains(geneName)) {
                BaseObject newgene = doc.newXObject(geneClassReference, context);
                newgene.setStringValue(GENE_NAME, geneName);
                newgene.setStringValue(STATUS_NAME, status);
                if (geneComments != null) {
                    newgene.setLargeStringValue(COMMENTS_NAME, geneComments);
                }
                geneList.add(geneName);
            } else if (geneComments != null) {
                String commentUpend = "\nAutomatic migration: gene was duplicated in the " + status + " gene section.";
                commentUpend += "\nOriginal comment: \n" + geneComments;
                updateComment(geneName, doc, commentUpend, geneClassReference);
            }
        }
        doc.removeXObjects(oldGenesClassReference);
    }

    private void updateComment(String geneName, XWikiDocument doc, String commentUpend,
        DocumentReference geneClassReference) throws HibernateException, XWikiException
    {
        List<BaseObject> genes = doc.getXObjects(geneClassReference);
        for (BaseObject gene : genes) {
            if (gene == null) {
                continue;
            }
            StringProperty geneNameProp = (StringProperty) gene.get(GENE_NAME);
            if (geneNameProp != null && geneNameProp.getValue().equals(geneName)) {
                LargeStringProperty oldGeneCommentsProp = (LargeStringProperty) gene.get(COMMENTS_NAME);
                if (oldGeneCommentsProp == null) {
                    gene.setLargeStringValue(COMMENTS_NAME, commentUpend);
                } else {
                    gene.setLargeStringValue(COMMENTS_NAME, oldGeneCommentsProp.getValue() + commentUpend);
                }
            }
        }
    }
}
