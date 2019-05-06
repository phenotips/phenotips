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
package org.phenotips.data.indexing.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Feature;
import org.phenotips.data.Gene;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.indexing.PatientIndexer;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.DefaultEntityAccess;
import org.phenotips.data.permissions.internal.visibility.PublicVisibility;
import org.phenotips.vocabulary.SolrCoreContainerHandler;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SolrPatientIndexerTest
{
    private static final String STATUS_KEY = "status";

    private static final List<String> STATUS_VALUES = Arrays.asList("candidate", "rejected", "rejected_candidate",
        "solved", "carrier");

    @Rule
    public MockitoComponentMockingRule<PatientIndexer> mocker =
        new MockitoComponentMockingRule<>(SolrPatientIndexer.class);

    @Mock
    private Patient patient;

    @Mock
    private SolrClient server;

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private VocabularyManager vm;

    @Mock
    private Provider<XWikiContext> provider;

    @Mock
    private XWiki xwiki;

    private PatientIndexer patientIndexer;

    private Logger logger;

    private QueryManager qm;

    private PatientRepository patientRepository;

    private EntityPermissionsManager permissions;

    private DocumentReference patientDocReference;

    @Before
    public void setUp() throws Exception
    {

        MockitoAnnotations.initMocks(this);

        SolrCoreContainerHandler cores = this.mocker.getInstance(SolrCoreContainerHandler.class);
        doReturn(mock(CoreContainer.class)).when(cores).getContainer();

        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(VocabularyManager.class)).thenReturn(this.vm);

        this.permissions = this.mocker.getInstance(EntityPermissionsManager.class);

        when(this.cm.getInstance(XWikiContext.TYPE_PROVIDER)).thenReturn(this.provider);
        XWikiContext context = mock(XWikiContext.class);
        when(this.provider.get()).thenReturn(context);
        XWiki x = mock(XWiki.class);
        when(context.getWiki()).thenReturn(x);

        XWikiDocument geneDoc = mock(XWikiDocument.class);
        when(x.getDocument(Gene.GENE_CLASS, context)).thenReturn(geneDoc);
        geneDoc.setNew(false);
        BaseClass c = mock(BaseClass.class);
        when(geneDoc.getXClass()).thenReturn(c);
        StaticListClass lc1 = mock(StaticListClass.class);
        when(c.get(STATUS_KEY)).thenReturn(lc1);
        when(lc1.getList(context)).thenReturn(STATUS_VALUES);

        this.qm = this.mocker.getInstance(QueryManager.class);
        this.patientRepository = this.mocker.getInstance(PatientRepository.class);
        this.patientDocReference = new DocumentReference("wiki", "patient", "P0000001");
        this.patientIndexer = this.mocker.getComponentUnderTest();
        EntityReferenceSerializer<String> referenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(referenceSerializer.serialize(this.patientDocReference)).thenReturn("wiki:patient.P0000001");
        this.logger = this.mocker.getMockedLogger();

        ReflectionUtils.setFieldValue(this.patientIndexer, "server", this.server);
    }

    @Before
    public void setupVocabulary() throws ComponentLookupException
    {
        // Setup the vocabulary
        Vocabulary hpo = this.mocker.getInstance(Vocabulary.class, "hpo");

        // Setup mock term
        String[] ancestorIds = { "HP:0011842", "HP:0000924", "HP:0000118", "HP:0000001" };
        Set<VocabularyTerm> ancestors = new HashSet<>();
        for (String id : ancestorIds) {
            VocabularyTerm ancestor = mock(VocabularyTerm.class);
            when(ancestor.getId()).thenReturn(id);
            ancestors.add(ancestor);
        }
        VocabularyTerm term = mock(VocabularyTerm.class);
        when(term.getId()).thenReturn("HP:0001367");
        ancestors.add(term);

        when(term.getAncestorsAndSelf()).thenReturn(ancestors);
        when(hpo.getTerm(term.getId())).thenReturn(term);
    }

    @Test
    public void indexDefaultPhenotypeBehaviourTest() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("HP:0001367").when(testFeature).getId();
        patientFeatures.add(testFeature);
        Feature negativeTestFeature = mock(Feature.class);
        doReturn(false).when(negativeTestFeature).isPresent();
        doReturn("phenotype").when(negativeTestFeature).getType();
        doReturn("id2").when(negativeTestFeature).getId();
        patientFeatures.add(negativeTestFeature);
        doReturn(patientFeatures).when(this.patient).getFeatures();

        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        EntityAccess entityAccess = mock(DefaultEntityAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        CapturingMatcher<SolrInputDocument> capturedArgument = new CapturingMatcher<>();
        when(this.server.add(argThat(capturedArgument))).thenReturn(mock(UpdateResponse.class));

        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doReturn(reporterReference).when(this.patient).getReporter();
        doReturn(entityAccess).when(this.permissions).getEntityAccess(this.patient);
        doReturn(patientVisibility).when(entityAccess).getVisibility();

        this.patientIndexer.index(this.patient);

        SolrInputDocument inputDoc = capturedArgument.getLastValue();
        verify(this.server).add(inputDoc);
        Assert.assertEquals("public", inputDoc.getFieldValue("visibility"));
        Assert.assertEquals("HP:0001367", inputDoc.getFieldValue("phenotype"));
        Assert.assertEquals("id2", inputDoc.getFieldValue("negative_phenotype"));
        Assert.assertEquals(5, inputDoc.getFieldValues("extended_phenotype").size());
    }

    @Test
    public void indexDefaultGeneBehaviourTest() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        EntityAccess entityAccess = mock(DefaultEntityAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        CapturingMatcher<SolrInputDocument> capturedArgument = new CapturingMatcher<>();
        when(this.server.add(argThat(capturedArgument))).thenReturn(mock(UpdateResponse.class));

        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(Collections.EMPTY_SET).when(this.patient).getFeatures();

        List<Gene> fakeGenes = new LinkedList<>();
        fakeGenes.add(mockGene("CANDIDATE1", "candidate"));
        fakeGenes.add(mockGene("REJECTED1", "rejected"));
        fakeGenes.add(mockGene("REJECTEDC1", "rejected_candidate"));
        fakeGenes.add(mockGene("CARRIER1", "carrier"));
        fakeGenes.add(mockGene("SOLVED1", "solved"));

        PatientData<Gene> fakeGeneData =
            new IndexedPatientData<>("genes", fakeGenes);
        doReturn(fakeGeneData).when(this.patient).getData("genes");

        doReturn(entityAccess).when(this.permissions).getEntityAccess(this.patient);
        doReturn(patientVisibility).when(entityAccess).getVisibility();

        this.patientIndexer.index(this.patient);
        SolrInputDocument inputDoc = capturedArgument.getLastValue();
        verify(this.server).add(inputDoc);

        Collection<Object> indexedGenes;
        indexedGenes = inputDoc.getFieldValues("candidate_genes");
        Assert.assertEquals(1, indexedGenes.size());
        Assert.assertEquals("CANDIDATE1", indexedGenes.iterator().next());

        indexedGenes = inputDoc.getFieldValues("solved_genes");
        Assert.assertEquals(1, indexedGenes.size());
        Assert.assertEquals("SOLVED1", indexedGenes.iterator().next());

        indexedGenes = inputDoc.getFieldValues("rejected_genes");
        Assert.assertEquals(1, indexedGenes.size());
        Assert.assertEquals("REJECTED1", indexedGenes.iterator().next());

        indexedGenes = inputDoc.getFieldValues("rejected_candidate_genes");
        Assert.assertEquals(1, indexedGenes.size());
        Assert.assertEquals("REJECTEDC1", indexedGenes.iterator().next());

        indexedGenes = inputDoc.getFieldValues("carrier_genes");
        Assert.assertEquals(1, indexedGenes.size());
        Assert.assertEquals("CARRIER1", indexedGenes.iterator().next());
    }

    @Test
    public void indexThrowsSolrException() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        EntityAccess entityAccess = mock(DefaultEntityAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(entityAccess).when(this.permissions).getEntityAccess(this.patient);
        doReturn(patientVisibility).when(entityAccess).getVisibility();
        doThrow(new SolrServerException("Error while adding SolrInputDocument")).when(this.server)
            .add(any(SolrInputDocument.class));

        this.patientIndexer.index(this.patient);

        verify(this.logger).warn("Failed to perform Solr search: {}", "Error while adding SolrInputDocument");
    }

    @Test
    public void indexThrowsIOException() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        EntityAccess entityAccess = mock(DefaultEntityAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(entityAccess).when(this.permissions).getEntityAccess(this.patient);
        doReturn(patientVisibility).when(entityAccess).getVisibility();
        doThrow(new IOException("Error while adding SolrInputDocument")).when(this.server)
            .add(any(SolrInputDocument.class));

        this.patientIndexer.index(this.patient);

        verify(this.logger).warn("Error occurred while performing Solr search: {}",
            "Error while adding SolrInputDocument");
    }

    @Test
    public void indexGetReporterIsNull() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        EntityAccess entityAccess = mock(DefaultEntityAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        CapturingMatcher<SolrInputDocument> capturedArgument = new CapturingMatcher<>();
        when(this.server.add(argThat(capturedArgument))).thenReturn(mock(UpdateResponse.class));

        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doReturn(null).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(entityAccess).when(this.permissions).getEntityAccess(this.patient);
        doReturn(patientVisibility).when(entityAccess).getVisibility();

        this.patientIndexer.index(this.patient);
        SolrInputDocument inputDoc = capturedArgument.getLastValue();
        verify(this.server).add(inputDoc);
        Assert.assertEquals(inputDoc.getFieldValue("reporter"), "");
    }

    @Test
    public void deleteDefaultBehaviourTest() throws IOException, SolrServerException
    {
        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        this.patientIndexer.delete(this.patient);
        verify(this.server).deleteByQuery("document:"
            + ClientUtils.escapeQueryChars(this.patientDocReference.toString()));
        verify(this.server).commit();
    }

    @Test
    public void deleteThrowsSolrException() throws IOException, SolrServerException
    {
        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doThrow(new SolrServerException("commit failed")).when(this.server).commit();
        this.patientIndexer.delete(this.patient);
        verify(this.logger).warn("Failed to delete from Solr: {}", "commit failed");
    }

    @Test
    public void deleteThrowsIOException() throws IOException, SolrServerException
    {
        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doThrow(new IOException("commit failed")).when(this.server).commit();
        this.patientIndexer.delete(this.patient);
        verify(this.logger).warn("Error occurred while deleting Solr documents: {}", "commit failed");
    }

    @Test
    public void reindexDefaultBehaviour() throws QueryException, IOException, SolrServerException
    {
        List<String> patientDocs = new ArrayList<>();
        patientDocs.add("P0000001");

        Query testQuery = mock(Query.class);
        doReturn(testQuery).when(this.qm).createQuery("from doc.object(PhenoTips.PatientClass) as patient", Query.XWQL);
        doReturn(patientDocs).when(testQuery).execute();
        doReturn(this.patient).when(this.patientRepository).get("P0000001");

        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        EntityAccess entityAccess = mock(DefaultEntityAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        doReturn(this.patientDocReference).when(this.patient).getDocumentReference();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(entityAccess).when(this.permissions).getEntityAccess(this.patient);
        doReturn(patientVisibility).when(entityAccess).getVisibility();

        this.patientIndexer.reindex();

        verify(this.server).deleteByQuery("*:*");
        verify(this.server).commit();

    }

    @Test
    public void reindexSolrServerException() throws QueryException, IOException, SolrServerException
    {
        List<String> patientDocs = new ArrayList<>();
        patientDocs.add("P0000001");

        Query testQuery = mock(Query.class);
        doReturn(testQuery).when(this.qm).createQuery("from doc.object(PhenoTips.PatientClass) as patient", Query.XWQL);
        doReturn(patientDocs).when(testQuery).execute();

        doThrow(new SolrServerException("deleteByQuery failed")).when(this.server).deleteByQuery("*:*");

        this.patientIndexer.reindex();

        verify(this.logger).warn("Failed to reindex patients: {}", "deleteByQuery failed");
    }

    @Test
    public void reindexIOException() throws QueryException, IOException, SolrServerException
    {
        List<String> patientDocs = new ArrayList<>();
        patientDocs.add("P0000001");

        Query testQuery = mock(Query.class);
        doReturn(testQuery).when(this.qm).createQuery("from doc.object(PhenoTips.PatientClass) as patient", Query.XWQL);
        doReturn(patientDocs).when(testQuery).execute();

        doThrow(new IOException("deleteByQuery failed")).when(this.server).deleteByQuery("*:*");

        this.patientIndexer.reindex();

        verify(this.logger).warn("Error occurred while reindexing patients: {}", "deleteByQuery failed");
    }

    @Test
    public void reindexQueryException() throws QueryException, IOException, SolrServerException
    {
        doThrow(new QueryException("createQuery failed", null, null))
            .when(this.qm).createQuery("from doc.object(PhenoTips.PatientClass) as patient", Query.XWQL);

        this.patientIndexer.reindex();

        verify(this.logger).warn("Failed to search patients for reindexing: {}", "createQuery failed");
    }

    private Gene mockGene(String id, String status)
    {
        Gene result = mock(Gene.class);
        when(result.getId()).thenReturn(id);
        when(result.getStatus()).thenReturn(status);
        return result;
    }
}
