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
package org.phenotips.panels.internal;

import org.phenotips.panels.GenePanel;
import org.phenotips.panels.PhenotypesForGene;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;
import org.phenotips.vocabulary.internal.GeneNomenclature;
import org.phenotips.vocabulary.internal.solr.HumanPhenotypeOntology;

import org.xwiki.component.manager.ComponentLookupException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DefaultGenePanelImpl}.
 *
 * @version $Id$
 * @since 1.3M5
 */
public class DefaultGenePanelImplTest
{
    private DefaultGenePanelImpl genePanel;

    private Vocabulary hgnc;

    private Vocabulary hpo;

    private VocabularyTerm term1 = mock(VocabularyTerm.class);

    private VocabularyTerm term2 = mock(VocabularyTerm.class);

    private VocabularyTerm term3 = mock(VocabularyTerm.class);

    private VocabularyTerm geneA = mock(VocabularyTerm.class);
    private VocabularyTerm geneB = mock(VocabularyTerm.class);
    private VocabularyTerm geneC = mock(VocabularyTerm.class);
    private VocabularyTerm geneD = mock(VocabularyTerm.class);
    private VocabularyTerm geneE = mock(VocabularyTerm.class);

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        VocabularyManager vocabularyManager = mock(VocabularyManager.class);
        this.hgnc = mock(GeneNomenclature.class);
        this.hpo = mock(HumanPhenotypeOntology.class);

        doReturn(this.hgnc).when(vocabularyManager).getVocabulary("hgnc");
        doReturn(this.hpo).when(vocabularyManager).getVocabulary("hpo");

        this.genePanel = new DefaultGenePanelImpl(Collections.<String>emptyList(), vocabularyManager);
    }

    @Test
    public void getGeneDataFromTermNoAssociatedGenes()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyTerm vocabularyTerm = mock(VocabularyTerm.class);
        doReturn(null).when(vocabularyTerm).get("associated_genes");

        final Method getGeneDataFromTerm = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneDataFromTerm",
            VocabularyTerm.class);
        getGeneDataFromTerm.setAccessible(true);

        final Object responseObject = getGeneDataFromTerm.invoke(this.genePanel, vocabularyTerm);
        assertEquals(Collections.<String>emptyList(), responseObject);
    }

    @Test
    public void getGeneDataFromTermEmptyAssociatedGenesList()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyTerm vocabularyTerm = mock(VocabularyTerm.class);
        doReturn(Collections.<String>emptyList()).when(vocabularyTerm).get("associated_genes");

        final Method getGeneDataFromTerm = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneDataFromTerm",
            VocabularyTerm.class);
        getGeneDataFromTerm.setAccessible(true);

        final Object responseObject = getGeneDataFromTerm.invoke(this.genePanel, vocabularyTerm);
        assertEquals(Collections.<String>emptyList(), responseObject);
    }

    @Test
    public void getGeneDataFromTermGeneDataExists()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final List<String> geneList = ImmutableList.of("gene1", "gene2");
        final VocabularyTerm vocabularyTerm = mock(VocabularyTerm.class);
        doReturn(geneList).when(vocabularyTerm).get("associated_genes");

        final Method getGeneDataFromTerm = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneDataFromTerm",
            VocabularyTerm.class);
        getGeneDataFromTerm.setAccessible(true);

        final Object responseObject = getGeneDataFromTerm.invoke(this.genePanel, vocabularyTerm);
        assertEquals(ImmutableList.of("gene1", "gene2"), responseObject);
    }

    @Test
    public void getGeneIdGeneSymbolEmptyString()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException
    {
        // Empty string should not be passed to to getGeneId
        final Method getGeneId = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneId", String.class);
        getGeneId.setAccessible(true);

        final String geneSymbol = "";

        doReturn(null).when(this.hgnc).getTerm(geneSymbol);

        final Object responseObject = getGeneId.invoke(this.genePanel, geneSymbol);
        assertEquals("", responseObject);
    }

    @Test
    public void getGeneIdGeneSymbolInvalid()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException
    {
        final Method getGeneId = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneId", String.class);
        getGeneId.setAccessible(true);

        final String geneSymbol = "HP:invalid";

        doReturn(null).when(this.hgnc).getTerm(geneSymbol);

        final Object responseObject = getGeneId.invoke(this.genePanel, geneSymbol);
        assertEquals("HP:invalid", responseObject);
    }

    @Test
    public void getGeneIdGeneSymbolValidNoEnsemblIdList()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException
    {
        final VocabularyTerm geneTerm = mock(VocabularyTerm.class);
        final Method getGeneId = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneId", String.class);
        getGeneId.setAccessible(true);

        final String geneSymbol = "HP:valid";

        doReturn(geneTerm).when(this.hgnc).getTerm(geneSymbol);
        doReturn(null).when(geneTerm).get("ensembl_gene_id");

        final Object responseObject = getGeneId.invoke(this.genePanel, geneSymbol);
        assertEquals("HP:valid", responseObject);
    }

    @Test
    public void getGeneIdGeneSymbolValidEnsemblIdListEmpty()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException
    {
        final VocabularyTerm geneTerm = mock(VocabularyTerm.class);
        final Method getGeneId = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneId", String.class);
        getGeneId.setAccessible(true);

        final String geneSymbol = "HP:valid";

        doReturn(geneTerm).when(this.hgnc).getTerm(geneSymbol);
        doReturn(Collections.emptyList()).when(geneTerm).get("ensembl_gene_id");

        final Object responseObject = getGeneId.invoke(this.genePanel, geneSymbol);
        assertEquals("HP:valid", responseObject);
    }

    @Test
    public void getGeneIdGeneSymbolValidEnsemblIdListNotEmpty()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException
    {
        final VocabularyTerm geneTerm = mock(VocabularyTerm.class);
        final Method getGeneId = DefaultGenePanelImpl.class.getDeclaredMethod("getGeneId", String.class);
        getGeneId.setAccessible(true);

        final String geneSymbol = "HP:valid";

        doReturn(geneTerm).when(this.hgnc).getTerm(geneSymbol);
        doReturn(ImmutableList.of("ENSG0001", "ENSG0002", "ENSG0003")).when(geneTerm).get("ensembl_gene_id");

        final Object responseObject = getGeneId.invoke(this.genePanel, geneSymbol);
        assertEquals("ENSG0001", responseObject);
    }

    @Test
    public void createPhenotypesForGeneObjNoEnsemblId()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();

        final Method createPhenotypesForGeneObj =
            DefaultGenePanelImpl.class.getDeclaredMethod("createPhenotypesForGeneObj", Map.class, String.class);
        createPhenotypesForGeneObj.setAccessible(true);

        final String geneSymbol = "gene1";

        doReturn(null).when(this.hgnc).getTerm(geneSymbol);

        final Object responseObj = createPhenotypesForGeneObj.invoke(this.genePanel, phenotypesForGeneMap, geneSymbol);
        assertEquals(new DefaultPhenotypesForGeneImpl(geneSymbol, geneSymbol).toJSON().toString(),
            ((PhenotypesForGene) responseObj).toJSON().toString());
    }

    @Test
    public void createPhenotypesForGeneObjHasEnsemblId()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyTerm vocabularyTerm = mock(VocabularyTerm.class);
        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();

        final Method createPhenotypesForGeneObj =
            DefaultGenePanelImpl.class.getDeclaredMethod("createPhenotypesForGeneObj", Map.class, String.class);
        createPhenotypesForGeneObj.setAccessible(true);

        final String geneSymbol = "gene1";

        doReturn(vocabularyTerm).when(this.hgnc).getTerm(geneSymbol);
        doReturn(ImmutableList.of("ENSG0001", "ENSG0002", "ENSG0003")).when(vocabularyTerm).get("ensembl_gene_id");

        final Object responseObj = createPhenotypesForGeneObj.invoke(this.genePanel, phenotypesForGeneMap, geneSymbol);
        assertEquals(new DefaultPhenotypesForGeneImpl(geneSymbol, "ENSG0001").toJSON().toString(),
            ((PhenotypesForGene) responseObj).toJSON().toString());

        assertTrue(phenotypesForGeneMap.containsKey(geneSymbol)
            && phenotypesForGeneMap.containsValue(responseObj)
            && phenotypesForGeneMap.size() == 1);
    }

    @Test
    public void addPhenotypeForGeneEmptyGeneList()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyTerm vocabularyTerm = mock(VocabularyTerm.class);
        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();
        final Method addPhenotypeForGene = DefaultGenePanelImpl.class.getDeclaredMethod("addPhenotypeForGene",
            VocabularyTerm.class, List.class, Map.class);
        addPhenotypeForGene.setAccessible(true);

        addPhenotypeForGene.invoke(this.genePanel, vocabularyTerm, Collections.emptyList(), phenotypesForGeneMap);
        assertTrue(phenotypesForGeneMap.isEmpty());
    }

    @Test
    public void addPhenotypeForGeneTermNotYetAdded()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyTerm vocabularyTerm = mock(VocabularyTerm.class);
        final VocabularyTerm geneTerm2 = mock(VocabularyTerm.class);
        final VocabularyTerm geneTerm3 = mock(VocabularyTerm.class);

        final JSONObject vocabularyTermJSON = new JSONObject();
        vocabularyTermJSON.put("HP:0001", "data");

        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();
        final String geneSymbol1 = "gene1";
        final String geneSymbol2 = "gene2";
        final String geneSymbol3 = "gene3";

        phenotypesForGeneMap.put(geneSymbol1, new DefaultPhenotypesForGeneImpl(geneSymbol1, "ENSG0001"));

        final Method addPhenotypeForGene = DefaultGenePanelImpl.class.getDeclaredMethod("addPhenotypeForGene",
            VocabularyTerm.class, List.class, Map.class);
        addPhenotypeForGene.setAccessible(true);

        doReturn(vocabularyTermJSON).when(vocabularyTerm).toJSON();
        doReturn(geneTerm2).when(this.hgnc).getTerm(geneSymbol2);
        doReturn(geneTerm3).when(this.hgnc).getTerm(geneSymbol3);

        doReturn(ImmutableList.of("ENSG0002a", "ENSG0002b")).when(geneTerm2).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG0003a")).when(geneTerm3).get("ensembl_gene_id");

        addPhenotypeForGene.invoke(this.genePanel, vocabularyTerm, ImmutableList.of(geneSymbol2, geneSymbol3),
            phenotypesForGeneMap);

        assertTrue(phenotypesForGeneMap.containsKey(geneSymbol1)
            && phenotypesForGeneMap.containsKey(geneSymbol2)
            && phenotypesForGeneMap.containsKey(geneSymbol3)
            && phenotypesForGeneMap.size() == 3);

        assertEquals(phenotypesForGeneMap.get(geneSymbol1).toJSON().toString(),
            new DefaultPhenotypesForGeneImpl(geneSymbol1, "ENSG0001").toJSON().toString());

        final PhenotypesForGene expected1 = new DefaultPhenotypesForGeneImpl(geneSymbol2, "ENSG0002a");
        expected1.addTerm(vocabularyTerm);
        assertEquals(phenotypesForGeneMap.get(geneSymbol2).toJSON().toString(), expected1.toJSON().toString());

        final PhenotypesForGene expected2 = new DefaultPhenotypesForGeneImpl(geneSymbol3, "ENSG0003a");
        expected2.addTerm(vocabularyTerm);
        assertEquals(phenotypesForGeneMap.get(geneSymbol3).toJSON().toString(), expected2.toJSON().toString());

        assertEquals(0, phenotypesForGeneMap.get(geneSymbol1).getCount());
        assertEquals(1, phenotypesForGeneMap.get(geneSymbol2).getCount());
        assertEquals(1, phenotypesForGeneMap.get(geneSymbol3).getCount());
    }

    @Test
    public void addPhenotypeForGeneTermAddedTermsIncrementCorrectly()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyTerm vocabularyTerm1 = mock(VocabularyTerm.class);
        final VocabularyTerm vocabularyTerm2 = mock(VocabularyTerm.class);

        final VocabularyTerm geneTerm1 = mock(VocabularyTerm.class);
        final VocabularyTerm geneTerm2a = mock(VocabularyTerm.class);
        final VocabularyTerm geneTerm2b = mock(VocabularyTerm.class);

        final JSONObject vocabularyTermJSON1 = new JSONObject();
        vocabularyTermJSON1.put("HP:0001", "data");

        final JSONObject vocabularyTermJSON2 = new JSONObject();
        vocabularyTermJSON2.put("HP:0002", "data");

        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();
        final String geneSymbol1 = "gene1";
        final String geneSymbol2 = "gene2";

        final Method addPhenotypeForGene = DefaultGenePanelImpl.class.getDeclaredMethod("addPhenotypeForGene",
            VocabularyTerm.class, List.class, Map.class);
        addPhenotypeForGene.setAccessible(true);

        doReturn(vocabularyTermJSON1).when(vocabularyTerm1).toJSON();
        doReturn(vocabularyTermJSON2).when(vocabularyTerm2).toJSON();

        doReturn(geneTerm1).when(this.hgnc).getTerm(geneSymbol1);
        doReturn(geneTerm2a).when(this.hgnc).getTerm(geneSymbol2);
        doReturn(geneTerm2b).when(this.hgnc).getTerm(geneSymbol2);

        doReturn(ImmutableList.of("ENSG0001a")).when(geneTerm1).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG0002a", "ENSG0002b")).when(geneTerm2a).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG0002a", "ENSG0002b")).when(geneTerm2b).get("ensembl_gene_id");

        addPhenotypeForGene.invoke(this.genePanel, vocabularyTerm1, ImmutableList.of(geneSymbol1, geneSymbol2),
            phenotypesForGeneMap);

        addPhenotypeForGene.invoke(this.genePanel, vocabularyTerm2, ImmutableList.of(geneSymbol2),
            phenotypesForGeneMap);

        assertTrue(phenotypesForGeneMap.containsKey(geneSymbol1)
            && phenotypesForGeneMap.containsKey(geneSymbol2)
            && phenotypesForGeneMap.size() == 2);

        final PhenotypesForGene expected1 = new DefaultPhenotypesForGeneImpl(geneSymbol1, "ENSG0001a");
        expected1.addTerm(vocabularyTerm1);
        assertEquals(phenotypesForGeneMap.get(geneSymbol1).toJSON().toString(), expected1.toJSON().toString());

        final PhenotypesForGene expected2a = new DefaultPhenotypesForGeneImpl(geneSymbol2, "ENSG0002a");
        expected2a.addTerm(vocabularyTerm1);
        expected2a.addTerm(vocabularyTerm2);

        assertEquals(phenotypesForGeneMap.get(geneSymbol2).toJSON().toString(), expected2a.toJSON().toString());

        assertEquals(1, phenotypesForGeneMap.get(geneSymbol1).getCount());
        assertEquals(2, phenotypesForGeneMap.get(geneSymbol2).getCount());
    }

    @Test
    public void buildPresentFeaturesEmptyFeatureList()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final Method buildPresentFeatures = DefaultGenePanelImpl.class.getDeclaredMethod("buildPresentFeatures",
            Collection.class);
        buildPresentFeatures.setAccessible(true);
        final Object responseObj = buildPresentFeatures.invoke(this.genePanel, Collections.emptyList());
        assertEquals(Collections.emptySet(), responseObj);
    }

    @Test
    public void buildPresentFeaturesFeatureListHasNullAndEmptyElements()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final List<String> featureList = new ArrayList<>();
        featureList.add("HP:0001");
        featureList.add("HP:0002");
        featureList.add(null);
        featureList.add(null);
        featureList.add("HP:0001");
        featureList.add("");
        featureList.add(" ");

        final VocabularyTerm vocabularyTerm1 = mock(VocabularyTerm.class);
        final VocabularyTerm vocabularyTerm2 = mock(VocabularyTerm.class);

        doReturn(vocabularyTerm1).when(this.hpo).getTerm("HP:0001");
        doReturn(vocabularyTerm2).when(this.hpo).getTerm("HP:0002");

        final Method buildPresentFeatures = DefaultGenePanelImpl.class.getDeclaredMethod("buildPresentFeatures",
            Collection.class);
        buildPresentFeatures.setAccessible(true);

        final Object responseObj = buildPresentFeatures.invoke(this.genePanel, featureList);
        assertEquals(ImmutableSet.of(vocabularyTerm1, vocabularyTerm2), responseObj);
    }

    @Test
    public void buildPresentFeaturesFeatureListHasNotMappedTerms()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final List<String> featureList = new ArrayList<>();
        featureList.add("HP:0001");
        featureList.add("HP:0002");
        featureList.add(null);
        featureList.add(null);
        featureList.add("HP:0001");
        featureList.add("");
        featureList.add(" ");

        final VocabularyTerm vocabularyTerm2 = mock(VocabularyTerm.class);

        doReturn(null).when(this.hpo).getTerm("HP:0001");
        doReturn(vocabularyTerm2).when(this.hpo).getTerm("HP:0002");

        final Method buildPresentFeatures = DefaultGenePanelImpl.class.getDeclaredMethod("buildPresentFeatures",
            Collection.class);
        buildPresentFeatures.setAccessible(true);

        final Object responseObj = buildPresentFeatures.invoke(this.genePanel, featureList);
        assertEquals(ImmutableSet.of(vocabularyTerm2), responseObj);
    }

    @Test
    public void buildSortedGeneDataFromMapIfEmpty()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();
        final Method buildSortedGeneDataFromMap =
            DefaultGenePanelImpl.class.getDeclaredMethod("buildSortedGeneDataFromMap", Map.class);
        buildSortedGeneDataFromMap.setAccessible(true);
        final Object responseObj = buildSortedGeneDataFromMap.invoke(this.genePanel, phenotypesForGeneMap);
        assertEquals(Collections.emptyList(), responseObj);
    }

    @Test
    public void buildSortedGeneDataFromMapNotEmpty()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyTerm vocabularyTerm1 = mock(VocabularyTerm.class);
        final VocabularyTerm vocabularyTerm2 = mock(VocabularyTerm.class);
        final VocabularyTerm vocabularyTerm3 = mock(VocabularyTerm.class);
        final VocabularyTerm vocabularyTerm4 = mock(VocabularyTerm.class);
        final VocabularyTerm vocabularyTerm5 = mock(VocabularyTerm.class);
        final VocabularyTerm vocabularyTerm6 = mock(VocabularyTerm.class);

        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();

        final PhenotypesForGene phenotypesForGene1 = new DefaultPhenotypesForGeneImpl("gene1", "ENSG1");
        phenotypesForGene1.addTerm(vocabularyTerm3);
        final PhenotypesForGene phenotypesForGene2 = new DefaultPhenotypesForGeneImpl("gene2", "ENSG2");
        phenotypesForGene2.addTerm(vocabularyTerm5);
        phenotypesForGene2.addTerm(vocabularyTerm1);
        phenotypesForGene2.addTerm(vocabularyTerm4);
        final PhenotypesForGene phenotypesForGene3 = new DefaultPhenotypesForGeneImpl("gene3", "ENSG3");
        phenotypesForGene3.addTerm(vocabularyTerm2);
        phenotypesForGene3.addTerm(vocabularyTerm6);

        phenotypesForGeneMap.put("gene2", phenotypesForGene2);
        phenotypesForGeneMap.put("gene1", phenotypesForGene1);
        phenotypesForGeneMap.put("gene3", phenotypesForGene3);

        final Method buildSortedGeneDataFromMap =
            DefaultGenePanelImpl.class.getDeclaredMethod("buildSortedGeneDataFromMap", Map.class);
        buildSortedGeneDataFromMap.setAccessible(true);

        final Object responseObj = buildSortedGeneDataFromMap.invoke(this.genePanel, phenotypesForGeneMap);
        assertEquals(ImmutableList.of(phenotypesForGene2, phenotypesForGene3, phenotypesForGene1), responseObj);
    }

    @Test
    public void toJSONNoGenes() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final VocabularyManager vocabularyManager = mock(VocabularyManager.class);
        final Vocabulary hgnc = mock(GeneNomenclature.class);
        final Vocabulary hpo = mock(HumanPhenotypeOntology.class);

        doReturn(hgnc).when(vocabularyManager).getVocabulary("hgnc");
        doReturn(hpo).when(vocabularyManager).getVocabulary("hpo");

        final GenePanel genePanel = new DefaultGenePanelImpl(Collections.<String>emptyList(), vocabularyManager);
        final JSONObject expected = new JSONObject();
        expected.put("size", 0);
        expected.put("genes", new JSONArray());
        assertEquals(expected.toString(), genePanel.toJSON().toString());
    }

    @Test
    public void toJSONAllSymptomsValid()
    {
        final VocabularyManager vocabularyManager = mock(VocabularyManager.class);
        final Vocabulary hgnc = mock(GeneNomenclature.class);
        final Vocabulary hpo = mock(HumanPhenotypeOntology.class);

        doReturn(hgnc).when(vocabularyManager).getVocabulary("hgnc");
        doReturn(hpo).when(vocabularyManager).getVocabulary("hpo");

        doReturn(this.term1).when(hpo).getTerm("HP:0001");
        doReturn(this.term2).when(hpo).getTerm("HP:0002");
        doReturn(this.term3).when(hpo).getTerm("HP:0003");

        final JSONObject term1JSON = new JSONObject();
        term1JSON.put("HP:0001", "data1");
        final JSONObject term2JSON = new JSONObject();
        term2JSON.put("HP:0002", "data2");
        final JSONObject term3JSON = new JSONObject();
        term3JSON.put("HP:0003", "data3");

        doReturn(term1JSON).when(this.term1).toJSON();
        doReturn(term2JSON).when(this.term2).toJSON();
        doReturn(term3JSON).when(this.term3).toJSON();

        doReturn(this.geneA).when(hgnc).getTerm("geneA");
        doReturn(this.geneB).when(hgnc).getTerm("geneB");
        doReturn(this.geneC).when(hgnc).getTerm("geneC");
        doReturn(this.geneD).when(hgnc).getTerm("geneD");
        doReturn(this.geneE).when(hgnc).getTerm("geneE");

        doReturn(ImmutableList.of("geneA", "geneB", "geneC")).when(this.term1).get("associated_genes");
        doReturn(ImmutableList.of("geneA", "geneB", "geneD")).when(this.term2).get("associated_genes");
        doReturn(ImmutableList.of("geneE", "geneB", "geneC")).when(this.term3).get("associated_genes");

        doReturn(ImmutableList.of("ENSG00A")).when(this.geneA).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG00B", "ENSG00Ba", "ENSG00Bb")).when(this.geneB).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG00C", "ENSG00Ca", "ENSG00Cb")).when(this.geneC).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG00D")).when(this.geneD).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG00E", "ENSG00Ea")).when(this.geneE).get("ensembl_gene_id");

        final GenePanel genePanel = new DefaultGenePanelImpl(ImmutableList.of("HP:0001", "HP:0002", "HP:0003"),
            vocabularyManager);


        final PhenotypesForGene phenotypesForGene1 = new DefaultPhenotypesForGeneImpl("geneA", "ENSG00A");
        phenotypesForGene1.addTerm(this.term1);
        phenotypesForGene1.addTerm(this.term2);

        final PhenotypesForGene phenotypesForGene2 = new DefaultPhenotypesForGeneImpl("geneB", "ENSG00B");
        phenotypesForGene2.addTerm(this.term1);
        phenotypesForGene2.addTerm(this.term2);
        phenotypesForGene2.addTerm(this.term3);

        final PhenotypesForGene phenotypesForGene3 = new DefaultPhenotypesForGeneImpl("geneC", "ENSG00C");
        phenotypesForGene3.addTerm(this.term1);
        phenotypesForGene3.addTerm(this.term3);

        final PhenotypesForGene phenotypesForGene4 = new DefaultPhenotypesForGeneImpl("geneD", "ENSG00D");
        phenotypesForGene4.addTerm(this.term2);

        final PhenotypesForGene phenotypesForGene5 = new DefaultPhenotypesForGeneImpl("geneE", "ENSG00E");
        phenotypesForGene5.addTerm(this.term3);

        final JSONArray genePanelGenes = genePanel.toJSON().getJSONArray("genes");

        assertEquals(5, genePanel.toJSON().getInt("size"));

        assertEquals(phenotypesForGene2.toJSON().toString(), genePanelGenes.getJSONObject(0).toString());

        assertTrue(phenotypesForGene1.toJSON().toString().equals(genePanelGenes.getJSONObject(1).toString())
            || phenotypesForGene1.toJSON().toString().equals(genePanelGenes.getJSONObject(2).toString()));

        assertTrue(phenotypesForGene3.toJSON().toString().equals(genePanelGenes.getJSONObject(1).toString())
            || phenotypesForGene3.toJSON().toString().equals(genePanelGenes.getJSONObject(2).toString()));

        assertTrue(phenotypesForGene4.toJSON().toString().equals(genePanelGenes.getJSONObject(3).toString())
            || phenotypesForGene4.toJSON().toString().equals(genePanelGenes.getJSONObject(4).toString()));

        assertTrue(phenotypesForGene5.toJSON().toString().equals(genePanelGenes.getJSONObject(3).toString())
            || phenotypesForGene5.toJSON().toString().equals(genePanelGenes.getJSONObject(4).toString()));
    }

    @Test
    public void toJSONSomeSymptomsInvalid()
    {
        final VocabularyManager vocabularyManager = mock(VocabularyManager.class);
        final Vocabulary hgnc = mock(GeneNomenclature.class);
        final Vocabulary hpo = mock(HumanPhenotypeOntology.class);

        doReturn(hgnc).when(vocabularyManager).getVocabulary("hgnc");
        doReturn(hpo).when(vocabularyManager).getVocabulary("hpo");

        doReturn(this.term1).when(hpo).getTerm("HP:0001");
        doReturn(this.term2).when(hpo).getTerm("HP:0002");
        doReturn(null).when(hpo).getTerm("HP:0003");

        final JSONObject term1JSON = new JSONObject();
        term1JSON.put("HP:0001", "data1");
        final JSONObject term2JSON = new JSONObject();
        term2JSON.put("HP:0002", "data2");

        doReturn(term1JSON).when(this.term1).toJSON();
        doReturn(term2JSON).when(this.term2).toJSON();

        doReturn(this.geneA).when(hgnc).getTerm("geneA");
        doReturn(this.geneB).when(hgnc).getTerm("geneB");
        doReturn(this.geneC).when(hgnc).getTerm("geneC");

        doReturn(ImmutableList.of("geneA", "geneB", "geneC")).when(this.term1).get("associated_genes");
        doReturn(null).when(this.term2).get("associated_genes");

        doReturn(ImmutableList.of("ENSG00A")).when(this.geneA).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG00B", "ENSG00Ba", "ENSG00Bb")).when(this.geneB).get("ensembl_gene_id");
        doReturn(ImmutableList.of("ENSG00C", "ENSG00Ca", "ENSG00Cb")).when(this.geneC).get("ensembl_gene_id");

        final GenePanel genePanel = new DefaultGenePanelImpl(ImmutableList.of("HP:0001", "HP:0002", "HP:0003"),
            vocabularyManager);

        final PhenotypesForGene phenotypesForGene1 = new DefaultPhenotypesForGeneImpl("geneA", "ENSG00A");
        phenotypesForGene1.addTerm(this.term1);

        final PhenotypesForGene phenotypesForGene2 = new DefaultPhenotypesForGeneImpl("geneB", "ENSG00B");
        phenotypesForGene2.addTerm(this.term1);

        final PhenotypesForGene phenotypesForGene3 = new DefaultPhenotypesForGeneImpl("geneC", "ENSG00C");
        phenotypesForGene3.addTerm(this.term1);

        final JSONArray genePanelGenes = genePanel.toJSON().getJSONArray("genes");

        final JSONArray expected = new JSONArray();
        expected.put(phenotypesForGene3.toJSON());
        expected.put(phenotypesForGene2.toJSON());
        expected.put(phenotypesForGene1.toJSON());

        assertEquals(3, genePanel.toJSON().getInt("size"));

        assertTrue(expected.similar(genePanelGenes));
    }
}
