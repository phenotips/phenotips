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

import org.phenotips.data.Feature;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.indexing.PatientIndexer;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.DefaultPatientAccess;
import org.phenotips.data.permissions.internal.visibility.PublicVisibility;
import org.phenotips.vocabulary.SolrCoreContainerHandler;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SolrPatientIndexerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientIndexer> mocker =
        new MockitoComponentMockingRule<PatientIndexer>(SolrPatientIndexer.class);

    @Mock
    private Patient patient;

    @Mock
    private SolrClient server;

    private SolrPatientIndexer solrPatientIndexer;

    private Logger logger;

    private QueryManager qm;

    private PatientRepository patientRepository;

    private PermissionsManager permissions;

    private DocumentReference patientDocReference;

    @Before
    public void setUp() throws ComponentLookupException
    {

        MockitoAnnotations.initMocks(this);

        SolrCoreContainerHandler cores = this.mocker.getInstance(SolrCoreContainerHandler.class);

        this.permissions = this.mocker.getInstance(PermissionsManager.class);

        this.qm = this.mocker.getInstance(QueryManager.class);

        this.patientRepository = this.mocker.getInstance(PatientRepository.class);

        this.patientDocReference = new DocumentReference("wiki", "patient", "P0000001");

        doReturn(mock(CoreContainer.class)).when(cores).getContainer();

        this.solrPatientIndexer = (SolrPatientIndexer) this.mocker.getComponentUnderTest();

        this.logger = this.mocker.getMockedLogger();

        ReflectionUtils.setFieldValue(this.solrPatientIndexer, "server", this.server);
    }

    @Before
    public void setupVocabulary() throws ComponentLookupException
    {
        // Setup the vocabulary
        Vocabulary hpo = this.mocker.getInstance(Vocabulary.class, "hpo");

        // Setup mock term
        String[] ancestorIds = { "HP:0011842", "HP:0000924", "HP:0000118", "HP:0000001" };
        Set<VocabularyTerm> ancestors = new HashSet<VocabularyTerm>();
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
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        PatientAccess patientAccess = mock(DefaultPatientAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        CapturingMatcher<SolrInputDocument> capturedArgument = new CapturingMatcher<>();
        when(this.server.add(argThat(capturedArgument))).thenReturn(mock(UpdateResponse.class));

        doReturn(patientDocReference).when(this.patient).getDocument();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("HP:0001367").when(testFeature).getId();

        doReturn(patientAccess).when(this.permissions).getPatientAccess(this.patient);
        doReturn(patientVisibility).when(patientAccess).getVisibility();

        this.solrPatientIndexer.index(this.patient);
        SolrInputDocument inputDoc = capturedArgument.getLastValue();
        verify(this.server).add(inputDoc);
        Assert.assertEquals("public", inputDoc.getFieldValue("visibility"));
        Assert.assertEquals("HP:0001367", inputDoc.getFieldValue("phenotype"));
        Assert.assertEquals(5, inputDoc.getFieldValues("extended_phenotype").size());
    }

    @Test
    public void indexDefaultGeneBehaviourTest() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        PatientAccess patientAccess = mock(DefaultPatientAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        CapturingMatcher<SolrInputDocument> capturedArgument = new CapturingMatcher<>();
        when(this.server.add(argThat(capturedArgument))).thenReturn(mock(UpdateResponse.class));

        doReturn(patientDocReference).when(this.patient).getDocument();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(Collections.EMPTY_SET).when(this.patient).getFeatures();

        List<Map<String, String>> fakeGenes = new ArrayList<Map<String, String>>();
        Map<String, String> fakeGene = new HashMap<String, String>();
        fakeGene.put("gene", "CANDIDATE1");
        fakeGenes.add(fakeGene);
        fakeGene = new HashMap<String, String>();
        fakeGene.put("gene", "CANDIDATE2");
        fakeGene.put("status", "candidate");
        fakeGenes.add(fakeGene);
        fakeGene = new HashMap<String, String>();
        fakeGene.put("gene", "REJECTED1");
        fakeGene.put("status", "rejected");
        fakeGenes.add(fakeGene);
        fakeGene = new HashMap<String, String>();
        fakeGene.put("gene", "SOLVED1");
        fakeGene.put("status", "solved");
        fakeGenes.add(fakeGene);
        fakeGene = new HashMap<String, String>();
        fakeGene.put("gene", "");
        fakeGene.put("status", "candidate");
        fakeGenes.add(fakeGene);

        PatientData<Map<String, String>> fakeGeneData =
            new IndexedPatientData<Map<String, String>>("genes", fakeGenes);
        doReturn(fakeGeneData).when(this.patient).getData("genes");

        doReturn(patientAccess).when(this.permissions).getPatientAccess(this.patient);
        doReturn(patientVisibility).when(patientAccess).getVisibility();

        this.solrPatientIndexer.index(this.patient);
        SolrInputDocument inputDoc = capturedArgument.getLastValue();
        verify(this.server).add(inputDoc);

        Collection<Object> indexedGenes;
        indexedGenes = inputDoc.getFieldValues("candidate_genes");
        Assert.assertEquals(2, indexedGenes.size());
        for (Object s : indexedGenes) {
            Assert.assertTrue(((String) s).startsWith("CANDIDATE"));
        }

        indexedGenes = inputDoc.getFieldValues("solved_genes");
        Assert.assertEquals(1, indexedGenes.size());
        Assert.assertEquals("SOLVED1", indexedGenes.iterator().next());

        indexedGenes = inputDoc.getFieldValues("rejected_genes");
        Assert.assertEquals(1, indexedGenes.size());
        Assert.assertEquals("REJECTED1", indexedGenes.iterator().next());
    }

    @Test
    public void indexThrowsSolrException() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        PatientAccess patientAccess = mock(DefaultPatientAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        doReturn(patientDocReference).when(this.patient).getDocument();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(patientAccess).when(this.permissions).getPatientAccess(this.patient);
        doReturn(patientVisibility).when(patientAccess).getVisibility();
        doThrow(new SolrServerException("Error while adding SolrInputDocument")).when(this.server)
            .add(any(SolrInputDocument.class));

        this.solrPatientIndexer.index(this.patient);

        verify(this.logger).warn("Failed to perform Solr search: {}", "Error while adding SolrInputDocument");
    }

    @Test
    public void indexThrowsIOException() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        PatientAccess patientAccess = mock(DefaultPatientAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        doReturn(patientDocReference).when(this.patient).getDocument();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(patientAccess).when(this.permissions).getPatientAccess(this.patient);
        doReturn(patientVisibility).when(patientAccess).getVisibility();
        doThrow(new IOException("Error while adding SolrInputDocument")).when(this.server)
            .add(any(SolrInputDocument.class));

        this.solrPatientIndexer.index(this.patient);

        verify(this.logger).warn("Error occurred while performing Solr search: {}",
            "Error while adding SolrInputDocument");
    }

    @Test
    public void indexGetReporterIsNull() throws IOException, SolrServerException
    {
        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        PatientAccess patientAccess = mock(DefaultPatientAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        CapturingMatcher<SolrInputDocument> capturedArgument = new CapturingMatcher<>();
        when(this.server.add(argThat(capturedArgument))).thenReturn(mock(UpdateResponse.class));

        doReturn(patientDocReference).when(this.patient).getDocument();
        doReturn(null).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(patientAccess).when(this.permissions).getPatientAccess(this.patient);
        doReturn(patientVisibility).when(patientAccess).getVisibility();

        this.solrPatientIndexer.index(this.patient);
        SolrInputDocument inputDoc = capturedArgument.getLastValue();
        verify(this.server).add(inputDoc);
        Assert.assertEquals(inputDoc.getFieldValue("reporter"), "");
    }

    @Test
    public void deleteDefaultBehaviourTest() throws IOException, SolrServerException
    {
        doReturn(this.patientDocReference).when(this.patient).getDocument();
        this.solrPatientIndexer.delete(this.patient);
        verify(this.server).deleteByQuery("document:"
            + ClientUtils.escapeQueryChars(this.patientDocReference.toString()));
        verify(this.server).commit();
    }

    @Test
    public void deleteThrowsSolrException() throws IOException, SolrServerException
    {
        doReturn(this.patientDocReference).when(this.patient).getDocument();
        doThrow(new SolrServerException("commit failed")).when(this.server).commit();
        this.solrPatientIndexer.delete(this.patient);
        verify(this.logger).warn("Failed to delete from Solr: {}", "commit failed");
    }

    @Test
    public void deleteThrowsIOException() throws IOException, SolrServerException
    {
        doReturn(this.patientDocReference).when(this.patient).getDocument();
        doThrow(new IOException("commit failed")).when(this.server).commit();
        this.solrPatientIndexer.delete(this.patient);
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
        doReturn(this.patient).when(this.patientRepository).getPatientById("P0000001");

        Set<Feature> patientFeatures = new HashSet<>();
        Feature testFeature = mock(Feature.class);
        patientFeatures.add(testFeature);
        DocumentReference reporterReference = new DocumentReference("xwiki", "XWiki", "user");
        PatientAccess patientAccess = mock(DefaultPatientAccess.class);
        Visibility patientVisibility = new PublicVisibility();

        doReturn(patientDocReference).when(this.patient).getDocument();
        doReturn(reporterReference).when(this.patient).getReporter();

        doReturn(patientFeatures).when(this.patient).getFeatures();
        doReturn(true).when(testFeature).isPresent();
        doReturn("phenotype").when(testFeature).getType();
        doReturn("id").when(testFeature).getId();

        doReturn(patientAccess).when(this.permissions).getPatientAccess(this.patient);
        doReturn(patientVisibility).when(patientAccess).getVisibility();

        this.solrPatientIndexer.reindex();

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

        this.solrPatientIndexer.reindex();

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

        this.solrPatientIndexer.reindex();

        verify(this.logger).warn("Error occurred while reindexing patients: {}", "deleteByQuery failed");
    }

    @Test
    public void reindexQueryException() throws QueryException, IOException, SolrServerException
    {
        doThrow(new QueryException("createQuery failed", null, null))
            .when(this.qm).createQuery("from doc.object(PhenoTips.PatientClass) as patient", Query.XWQL);

        this.solrPatientIndexer.reindex();

        verify(this.logger).warn("Failed to search patients for reindexing: {}", "createQuery failed");

    }
}
