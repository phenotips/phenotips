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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OncoTree} class.
 */
public class OncoTreeTest
{
    private static final String ONCO_LOWER = "onco";

    private static final String PREFIX = "ONCO:";

    private static final String BLUE_LABEL = "青い";

    private static final String PURPLE_LABEL = "Purple";

    private static final String ESOPHAGUS_STOMACH_LABEL = "Esophagus/Stomach";

    private static final String THYMUS_LABEL = "Thymus";

    private static final String THYMIC_TUMOR_LABEL = "Thymic Tumor";

    private static final String EGC_LABEL = "EGC";

    private static final String STAD_LABEL = "STAD";

    private static final String TET_LABEL = "TET";

    private static final String THYC_LABEL = "THYC";

    private static final String TNET_LABEL = "TNET";

    private static final String DCS_LABEL = "DCS";

    private static final String FDCS_LABEL = "FDCS";

    private static final String HDCN_LABEL = "HDCN";

    private static final String DCS_NAME_LABEL = "Dendritic Cell Sarcoma";

    private static final String FDCS_NAME_LABEL = "Follicular Dendritic Cell Sarcoma";

    private static final String FDCS_ALT_NAME_LABEL = "Follicular Dendritic Cell Sarcoma Alt";

    private static final String HDCN_NAME_LABEL = "Histiocytic and Dendritic Cell Neoplasms";

    private static final String EGC_NAME_LABEL = "Esophagogastric Adenocarcinoma";

    private static final String TNET_NAME_LABEL = "Thymic Neuroendocrine Tumor";

    private static final String ACA_LABEL = "ACA";

    private static final String ADRENAL_GLAND_LABEL = "Adrenal Gland";

    private static final String ESOPHAGOGASTRIC_CANCER_LABEL = "Esophagogastric Cancer";

    private static final String DSTAD_LABEL = "DSTAD";

    private static final String STAD_NAME_LABEL = "Stomach Adenocarcinoma";

    private static final String THYC_NAME_LABEL = "Thymic Carcinoma";

    private static final String ACA_NAME_LABEL = "Adrenocortical Adenoma";

    private static final String ADRENOCORTICAL_CARCINOMA_LABEL = "Adrenocortical Carcinoma";

    private static final String DSTAD_NAME_LABEL = "Diffuse Type Stomach Adenocarcinoma";

    private static final String TET_NAME_LABEL = "Thymic Epithelial Tumor";

    private static final String ONCOTREE_LABEL = "OncoTree";

    private static final String ONCO_UPPER = "ONCO";

    private static final String TISSUE = "tissue";

    private static final String ID = "id";

    private static final String NAME = "name";

    private static final String TERM_GROUP = "metamaintype";

    private static final String COLOUR = "metacolor";

    private static final String NCI_ID = "metanci";

    private static final String UMLS_ID = "metaumls";

    private static final String IS_A = "is_a";

    private static final String TERM_CATEGORY = "term_category";

    private static final String HEADER_INFO_LABEL = "HEADER_INFO";

    private static final String VERSION_FIELD_NAME = "version";

    private static final String VERSION_STR = "2014-01-01";

    private static final String SOFT_TISSUE_LABEL = "Soft Tissue";

    private static final String MYELOID_LABEL = "Myeloid";

    private static final String SYNONYM = "synonym";

    private static final String CANCER = "cancer";

    private static final String DISEASE = "disease";

    @Rule
    public MockitoComponentMockingRule<Vocabulary> mocker = new MockitoComponentMockingRule<>(OncoTree.class);

    @Mock
    private SolrClient server;

    private Vocabulary component;

    private OncoTree oncoTree;

    private Logger logger;

    private URL url;

    @Before
    public void setUp() throws ComponentLookupException, IOException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        this.oncoTree = spy((OncoTree) this.component);

        SolrVocabularyResourceManager externalServicesAccess =
            this.mocker.getInstance(SolrVocabularyResourceManager.class);

        when(externalServicesAccess.getSolrConnection(this.component)).thenReturn(this.server);

        this.logger = this.mocker.getMockedLogger();
        this.url = new URL(this.component.getDefaultSourceLocation());
        final InputStream inputStream = getInputStream("src/test/resources/test.txt");
        doReturn(inputStream).when(this.oncoTree).getInputStream(any(URL.class));
    }

    @Test
    public void getVersionForOncoTree() throws IOException, SolrServerException
    {
        final QueryResponse queryResponse = mock(QueryResponse.class);
        when(this.server.query(any(SolrQuery.class))).thenReturn(queryResponse);
        final SolrDocumentList results = mock(SolrDocumentList.class);
        when(queryResponse.getResults()).thenReturn(results);
        when(results.isEmpty()).thenReturn(false);
        final SolrDocument versionDoc = mock(SolrDocument.class);
        when(results.get(0)).thenReturn(versionDoc);
        when(versionDoc.getFieldValue("version")).thenReturn(VERSION_STR);
        Assert.assertEquals(VERSION_STR, this.component.getVersion());
    }

    @Test
    public void getSolrDocsPerBatchForOncoTree()
    {
        Assert.assertEquals(15000, ((OncoTree) this.component).getSolrDocsPerBatch());
    }

    @Test
    public void loadReturnsEmptyDataWhenExceptionThrownWhenObtainingInputStream() throws IOException
    {
        doThrow(IOException.class).when(this.oncoTree).getInputStream(any(URL.class));
        final Collection<SolrInputDocument> data = this.oncoTree.load(this.url);
        verify(this.logger).error("Failed to load vocabulary source: {}", (Object) null);
        Assert.assertTrue(data.isEmpty());
    }

    @Test
    public void loadReturnsCorrectDataWhenReadingFromSourceThatDoesNotDefineIntermediates() throws IOException
    {
        final InputStream inputStream = getInputStream("src/test/resources/test2.txt");
        doReturn(inputStream).when(this.oncoTree).getInputStream(any(URL.class));
        final Collection<SolrInputDocument> data = this.oncoTree.load(this.url);
        Assert.assertEquals(4, data.size());
        for (final SolrInputDocument datum : data) {
            switch ((String) datum.getFieldValue(ID)) {
                case PREFIX + DCS_LABEL:
                    Assert.assertEquals(3, datum.size());
                    Assert.assertEquals(SOFT_TISSUE_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(DCS_NAME_LABEL, datum.getFieldValue(NAME));
                    break;
                case PREFIX + FDCS_LABEL:
                    Assert.assertEquals(10, datum.size());
                    Assert.assertEquals(2, datum.getFieldValues(TISSUE).size());
                    Assert.assertTrue(datum.getFieldValues(TISSUE).contains(SOFT_TISSUE_LABEL));
                    Assert.assertTrue(datum.getFieldValues(TISSUE).contains(MYELOID_LABEL));
                    Assert.assertEquals(2, datum.getFieldValues(TERM_CATEGORY).size());
                    Assert.assertTrue(datum.getFieldValues(TERM_CATEGORY).contains(PREFIX + HDCN_LABEL));
                    Assert.assertTrue(datum.getFieldValues(TERM_CATEGORY).contains(PREFIX + DCS_LABEL));
                    Assert.assertEquals(2, datum.getFieldValues(IS_A).size());
                    Assert.assertTrue(datum.getFieldValues(IS_A).contains(PREFIX + HDCN_LABEL));
                    Assert.assertTrue(datum.getFieldValues(IS_A).contains(PREFIX + DCS_LABEL));
                    Assert.assertEquals(FDCS_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(1, datum.getFieldValues(TERM_GROUP).size());
                    Assert.assertTrue(datum.getFieldValues(TERM_GROUP).contains("Soft Tissue Sarcoma"));
                    Assert.assertFalse(datum.getFieldValues(COLOUR).isEmpty());
                    Assert.assertEquals(1, datum.getFieldValues(SYNONYM).size());
                    Assert.assertTrue(datum.getFieldValues(SYNONYM).contains(FDCS_ALT_NAME_LABEL));
                    Assert.assertEquals("C9281", datum.getFieldValue(NCI_ID));
                    Assert.assertEquals("C1260325", datum.getFieldValue(UMLS_ID));
                    break;
                case PREFIX + HDCN_LABEL:
                    Assert.assertEquals(3, datum.size());
                    Assert.assertEquals(1, datum.getFieldValues(TISSUE).size());
                    Assert.assertTrue(datum.getFieldValues(TISSUE).contains(MYELOID_LABEL));
                    Assert.assertEquals(HDCN_NAME_LABEL, datum.getFieldValue(NAME));
                    break;
                case HEADER_INFO_LABEL:
                    Assert.assertEquals(2, datum.size());
                    Assert.assertNotNull(datum.getFieldValue(VERSION_FIELD_NAME));
                    break;
                default:
                    Assert.fail();
            }
        }
    }

    @Test
    public void loadReturnsCorrectDataWhenReadingFromSourceWithNonEnglishChars()
    {
        final Collection<SolrInputDocument> data = this.oncoTree.load(this.url);
        Assert.assertEquals(8, data.size());

        for (final SolrInputDocument datum : data) {
            switch ((String) datum.getFieldValue(ID)) {
                case PREFIX + ACA_LABEL:
                    Assert.assertEquals(7, datum.size());
                    Assert.assertEquals(ADRENAL_GLAND_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(ACA_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(ADRENOCORTICAL_CARCINOMA_LABEL, datum.getFieldValue(TERM_GROUP));
                    Assert.assertEquals(PURPLE_LABEL, datum.getFieldValue(COLOUR));
                    Assert.assertEquals("C9003", datum.getFieldValue(NCI_ID));
                    Assert.assertEquals("C0206667", datum.getFieldValue(UMLS_ID));
                    break;
                case PREFIX + EGC_LABEL:
                    Assert.assertEquals(7, datum.size());
                    Assert.assertEquals(ESOPHAGUS_STOMACH_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(EGC_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(ESOPHAGOGASTRIC_CANCER_LABEL, datum.getFieldValue(TERM_GROUP));
                    Assert.assertEquals(BLUE_LABEL, datum.getFieldValue(COLOUR));
                    Assert.assertEquals("C9296", datum.getFieldValue(NCI_ID));
                    Assert.assertEquals("C1332166", datum.getFieldValue(UMLS_ID));
                    break;
                case PREFIX + STAD_LABEL:
                    final Set<String> stadTermCategory = new HashSet<>();
                    stadTermCategory.add(PREFIX + EGC_LABEL);
                    Assert.assertEquals(9, datum.size());
                    Assert.assertEquals(ESOPHAGUS_STOMACH_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(STAD_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(ESOPHAGOGASTRIC_CANCER_LABEL, datum.getFieldValue(TERM_GROUP));
                    Assert.assertEquals(BLUE_LABEL, datum.getFieldValue(COLOUR));
                    Assert.assertEquals("C4004", datum.getFieldValue(NCI_ID));
                    Assert.assertEquals("C0278701", datum.getFieldValue(UMLS_ID));
                    Assert.assertEquals(PREFIX + EGC_LABEL, datum.getFieldValue(IS_A));
                    Assert.assertEquals(stadTermCategory, datum.getFieldValues(TERM_CATEGORY));
                    break;
                case PREFIX + DSTAD_LABEL:
                    final Set<String> dstadTermCategory = new HashSet<>();
                    dstadTermCategory.add(PREFIX + EGC_LABEL);
                    dstadTermCategory.add(PREFIX + STAD_LABEL);
                    Assert.assertEquals(9, datum.size());
                    Assert.assertEquals(ESOPHAGUS_STOMACH_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(DSTAD_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(ESOPHAGOGASTRIC_CANCER_LABEL, datum.getFieldValue(TERM_GROUP));
                    Assert.assertEquals(BLUE_LABEL, datum.getFieldValue(COLOUR));
                    Assert.assertEquals("C9159", datum.getFieldValue(NCI_ID));
                    Assert.assertEquals("C0279635", datum.getFieldValue(UMLS_ID));
                    Assert.assertEquals(PREFIX + STAD_LABEL, datum.getFieldValue(IS_A));
                    Assert.assertEquals(dstadTermCategory, datum.getFieldValues(TERM_CATEGORY));
                    break;
                case PREFIX + TET_LABEL:
                    Assert.assertEquals(7, datum.size());
                    Assert.assertEquals(THYMUS_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(TET_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(THYMIC_TUMOR_LABEL, datum.getFieldValue(TERM_GROUP));
                    Assert.assertEquals(PURPLE_LABEL, datum.getFieldValue(COLOUR));
                    Assert.assertEquals("C6450", datum.getFieldValue(NCI_ID));
                    Assert.assertEquals("C1266101", datum.getFieldValue(UMLS_ID));
                    break;
                case PREFIX + THYC_LABEL:
                    final Set<String> tetTermCategory = new HashSet<>();
                    tetTermCategory.add(PREFIX + TET_LABEL);
                    Assert.assertEquals(9, datum.size());
                    Assert.assertEquals(THYMUS_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(THYC_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(THYMIC_TUMOR_LABEL, datum.getFieldValue(TERM_GROUP));
                    Assert.assertEquals(PURPLE_LABEL, datum.getFieldValue(COLOUR));
                    Assert.assertEquals("C7569", datum.getFieldValue(NCI_ID));
                    Assert.assertEquals("C0205969", datum.getFieldValue(UMLS_ID));
                    Assert.assertEquals(PREFIX + TET_LABEL, datum.getFieldValue(IS_A));
                    Assert.assertEquals(tetTermCategory, datum.getFieldValues(TERM_CATEGORY));
                    break;
                case PREFIX + TNET_LABEL:
                    Assert.assertEquals(6, datum.size());
                    Assert.assertEquals(THYMUS_LABEL, datum.getFieldValue(TISSUE));
                    Assert.assertEquals(TNET_NAME_LABEL, datum.getFieldValue(NAME));
                    Assert.assertEquals(THYMIC_TUMOR_LABEL, datum.getFieldValue(TERM_GROUP));
                    Assert.assertEquals(PURPLE_LABEL, datum.getFieldValue(COLOUR));
                    Assert.assertEquals("CL511204", datum.getFieldValue(UMLS_ID));
                    break;
                case HEADER_INFO_LABEL:
                    Assert.assertEquals(2, datum.size());
                    Assert.assertNotNull(datum.getFieldValue(VERSION_FIELD_NAME));
                    break;
                default:
                    Assert.fail();
            }
        }
    }

    @Test
    public void getIdentifier()
    {
        Assert.assertEquals(ONCO_LOWER, this.component.getIdentifier());
    }

    @Test
    public void getName()
    {
        Assert.assertEquals(ONCOTREE_LABEL, this.component.getName());
    }

    @Test
    public void getAliases()
    {
        final Set<String> aliases = new HashSet<>();
        aliases.add(ONCOTREE_LABEL);
        aliases.add(ONCO_LOWER);
        aliases.add(ONCO_UPPER);
        Assert.assertEquals(aliases, this.component.getAliases());
    }

    @Test
    public void getDefaultSourceLocation()
    {
        Assert.assertEquals("http://oncotree.mskcc.org/oncotree/api/tumor_types.txt?version=oncotree_latest_stable",
            this.component.getDefaultSourceLocation());
    }

    @Test
    public void getWebsite()
    {
        Assert.assertEquals("http://oncotree.mskcc.org/oncotree/", this.component.getWebsite());
    }

    @Test
    public void getCitation()
    {
        Assert.assertEquals("OncoTree: CMO Tumor Type Tree", this.component.getCitation());
    }

    @Test
    public void getSupportedCategoriesReturnsExpectedCategories()
    {
        final Collection<String> categories = this.component.getSupportedCategories();
        Assert.assertEquals(2, categories.size());
        Assert.assertTrue(categories.contains(CANCER));
        Assert.assertTrue(categories.contains(DISEASE));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getSupportedCategoriesReturnsImmutableCollection()
    {
        final Collection<String> categories = this.component.getSupportedCategories();
        categories.add("test");
        categories.remove("aa");
    }

    /**
     * Gets an input stream from a file.
     *
     * @return an {@link InputStream}
     * @throws FileNotFoundException if the file cannot be found
     */
    private InputStream getInputStream(final String path) throws FileNotFoundException
    {
        final File file = new File(path);
        return new FileInputStream(file);
    }
}
