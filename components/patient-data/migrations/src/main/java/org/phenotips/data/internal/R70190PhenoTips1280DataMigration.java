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
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for PhenoTips issue #1280: Automatically migrate existing {@code genes} (with {@code genes_comments}),
 * {@code rejectedGenes} (with {@code rejectedGenes_comments}) and {@code solved__gene_id} values to the {@code genes}
 * (with {@code genes_comments}) objects of {@code GeneClass} with new additional property of {@code classification}
 * with "candidate", "rejected" and "solved" values respectively. Searches for all documents containing values for the:
 * 1.- {@code genes} (with {@code genes_comments}) property from {@code InvestigationClass} and for each such document
 * set the "candidate" value of {@code classification} of {@code GeneClass}. 2.- {@code rejectedGenes} (with
 * {@code rejectedGenes_comments}) property from {@code RejectedGenesClass} and for each such document migrate property
 * value to the {@code genes} (with {@code genes_comments}) object with "rejected" value of {@code classification} of
 * {@code GeneClass}. 3.- {@code solved__gene_id} property from {@code PatientClass} and for each such document migrate
 * property value to the {@code genes} (with {@code genes_comments}) object with "solved" value of
 * {@code classification} of {@code GeneClass}. The old {@code rejectedGenes}, {@code rejectedGenes_comments},
 * {@code solved__gene_id} properties and {@code RejectedGenesClass} are removed.
 *
 * @version $Id$
 * @since 1.2M1
 */
@Component
@Named("R70190PhenoTips#1280")
@Singleton
public class R70190PhenoTips1280DataMigration extends AbstractHibernateDataMigration implements
    HibernateCallback<Object>
{
    private static final String GENE_NAME = "gene";

    private static final String COMMENTS_NAME = "comments";

    private static final String SOLVED_NAME = "solved__gene_id";

    private static final String CLASSIFICATION_NAME = "classification";

    private static final String BEGINNING = "select distinct o.name from BaseObject o where o.className = '";

    private static final String ENDING = "'";

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
        return new XWikiDBVersion(70190);
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
            R70190PhenoTips1280DataMigration.this.entityResolver.resolve(PATIENT_CLASS);
        DocumentReference investigationClassReference =
            R70190PhenoTips1280DataMigration.this.entityResolver.resolve(INVESTIGATION_CLASS);
        DocumentReference geneClassReference =
            R70190PhenoTips1280DataMigration.this.entityResolver.resolve(GENE_CLASS);
        DocumentReference rejectedGenesClassReference =
            R70190PhenoTips1280DataMigration.this.entityResolver.resolve(REJECTED_CLASS);

        Query q =
            session.createQuery("select distinct o.name from BaseObject o, StringProperty p where o.className = '"
                + this.serializer.serialize(patientClassReference) + "' and p.id.id = o.id and p.id.name = '"
                + SOLVED_NAME + "' and p.value <> ''");
        setSolvedGenes(q, xwiki, patientClassReference, geneClassReference, context, session);

        q = session.createQuery(BEGINNING + this.serializer.serialize(geneClassReference) + ENDING);
        setCandidateGenes(q, xwiki, investigationClassReference, geneClassReference, context, session);

        q = session.createQuery(BEGINNING + this.serializer.serialize(rejectedGenesClassReference) + ENDING);
        setRejectedGenes(q, xwiki, rejectedGenesClassReference, geneClassReference, context, session);

        return null;
    }

    private void setCandidateGenes(Query q, XWiki xwiki, DocumentReference investigationClassReference,
        DocumentReference geneClassReference, XWikiContext context, Session session)
        throws HibernateException, XWikiException
    {
        @SuppressWarnings("unchecked")
        List<String> docs = q.list();
        for (String docName : docs) {
            XWikiDocument doc =
                xwiki.getDocument(R70190PhenoTips1280DataMigration.this.resolver.resolve(docName), context);
            BaseObject gene = doc.getXObject(investigationClassReference);
            StringProperty oldGeneName = (StringProperty) gene.get(GENE_NAME);
            LargeStringProperty oldGeneComments = (LargeStringProperty) gene.get(COMMENTS_NAME);
            BaseObject newgene = doc.newXObject(geneClassReference, context);
            newgene.setStringValue(GENE_NAME, oldGeneName.getValue());
            newgene.setStringValue(COMMENTS_NAME, oldGeneComments.getValue());
            newgene.setStringValue(CLASSIFICATION_NAME, "candidate");
            doc.setComment("Migrate 'candidate' genes to the GeneClass objects");
            doc.setMinorEdit(true);
            doc.removeXObjects(investigationClassReference);
            try {
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                //
            }
        }
    }

    private void setSolvedGenes(Query q, XWiki xwiki, DocumentReference patientClassReference,
        DocumentReference geneClassReference, XWikiContext context, Session session)
        throws HibernateException, XWikiException
    {
        @SuppressWarnings("unchecked")
        List<String> documents = q.list();
        for (String docName : documents) {
            XWikiDocument doc =
                xwiki.getDocument(R70190PhenoTips1280DataMigration.this.resolver.resolve(docName), context);
            BaseObject patient = doc.getXObject(patientClassReference);
            StringProperty oldTarget = (StringProperty) patient.get(SOLVED_NAME);
            patient.removeField(SOLVED_NAME);
            BaseObject gene = doc.newXObject(geneClassReference, context);
            gene.setStringValue(GENE_NAME, oldTarget.getValue());
            gene.setStringValue(CLASSIFICATION_NAME, "solved");
            doc.setComment("Migrate 'solved' genes to the GeneClass objects");
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
    }

    private void setRejectedGenes(Query q, XWiki xwiki, DocumentReference rejectedGenesClassReference,
        DocumentReference geneClassReference, XWikiContext context, Session session)
        throws HibernateException, XWikiException
    {
        @SuppressWarnings("unchecked")
        List<String> docums = q.list();
        for (String docName : docums) {
            XWikiDocument doc =
                xwiki.getDocument(R70190PhenoTips1280DataMigration.this.resolver.resolve(docName), context);
            BaseObject gene = doc.getXObject(rejectedGenesClassReference);
            StringProperty oldGeneName = (StringProperty) gene.get(GENE_NAME);
            LargeStringProperty oldGeneComments = (LargeStringProperty) gene.get(COMMENTS_NAME);
            BaseObject newgene = doc.newXObject(geneClassReference, context);
            newgene.setStringValue(GENE_NAME, oldGeneName.getValue());
            newgene.setStringValue(COMMENTS_NAME, oldGeneComments.getValue());
            newgene.setStringValue(CLASSIFICATION_NAME, "rejected");
            doc.setComment("Migrating 'rejected' genes to the GeneClass objects");
            doc.setMinorEdit(true);
            doc.removeXObjects(rejectedGenesClassReference);
            try {
                session.clear();
                ((XWikiHibernateStore) getStore()).saveXWikiDoc(doc, context, false);
                session.flush();
            } catch (DataMigrationException e) {
                //
            }
        }
    }
}
