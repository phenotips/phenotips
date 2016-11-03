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
package org.phenotips.export.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Feature;
import org.phenotips.tools.PhenotypeMappingService;
import org.phenotips.translation.TranslationManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.script.service.ScriptService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ConversionHelpersTest
{
    @Before
    public void setup() throws Exception
    {
        final ComponentManager cm = mock(ComponentManager.class);
        final TranslationManager tm = mock(TranslationManager.class);
        doReturn(tm).when(cm).getInstance(TranslationManager.class);
        doReturn("No").when(tm).translate("yesno_0");
        doReturn("Yes").when(tm).translate("yesno_1");
        Field cmp = ReflectionUtils.getField(ComponentManagerRegistry.class, "cmProvider");
        cmp.setAccessible(true);
        cmp.set(null, new Provider<ComponentManager>()
        {
            @Override
            public ComponentManager get()
            {
                return cm;
            }
        });
    }

    @Test
    public void featureSetUpNoCategories() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);

        helpersSpy.featureSetUp(true, true, false);
        verify(helpersSpy, atMost(0)).getComponentManager();
    }

    @Test(expected = Exception.class)
    public void featureSetUpCategoriesError() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        ComponentManager manager = mock(ComponentManager.class);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);

        doReturn(manager).when(helpersSpy).getComponentManager();
        doReturn(phenotypeMappingService).when(manager).getInstance(eq(ScriptService.class), eq("phenotypeMapping"));
        doReturn(null).when(phenotypeMappingService).get(anyString());

        helpersSpy.featureSetUp(true, true, true);
    }

    /* A rather poor test. */
    @Test(expected = NullPointerException.class)
    public void featureSetUpCategories() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        ComponentManager manager = mock(ComponentManager.class);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);
        List<Map<String, List<String>>> mapping = new LinkedList<>();
        Map<String, List<String>> categoryEntry = new HashMap<>();

        doReturn(manager).when(helpersSpy).getComponentManager();
        doReturn(phenotypeMappingService).when(manager).getInstance(eq(ScriptService.class), eq("phenotype"));
        doReturn(mapping).when(phenotypeMappingService).get(anyString());
        /* null.toString will fail. */
        categoryEntry.put("title", null);

        helpersSpy.featureSetUp(true, true, true);
    }

    /**
     * If {@link org.phenotips.export.internal.ConversionHelpers#positive} and
     * {@link org.phenotips.export.internal.ConversionHelpers#negative} have not been set up, this should fail.
     */
    @Test(expected = NullPointerException.class)
    public void sortFeaturesSimpleNotSetup()
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature feature = mock(Feature.class);
        Set<Feature> features = new HashSet<>();
        features.add(feature);

        helpersSpy.sortFeaturesSimple(features);
    }

    @Test
    public void sortFeaturesSimple() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature featurePositive = mock(Feature.class);
        Feature featureNegative = mock(Feature.class);
        Set<Feature> features = new HashSet<>();
        features.add(featurePositive);
        features.add(featureNegative);

        doReturn(true).when(featurePositive).isPresent();
        doReturn(false).when(featureNegative).isPresent();

        helpersSpy.featureSetUp(false, true, false);
        List<Feature> sorted = helpersSpy.sortFeaturesSimple(features);

        Assert.assertFalse(sorted.contains(featurePositive));
        Assert.assertTrue(sorted.contains(featureNegative));
    }

    @Test(expected = NullPointerException.class)
    public void sortFeaturesWithSectionsNoSetup() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Set<Feature> features = new HashSet<>();

        helpersSpy.featureSetUp(true, true, false);
        doReturn(null).when(helpersSpy).getCategoryMapping();

        helpersSpy.sortFeaturesWithSections(features);
    }

    @Test
    public void sortFeaturesWithSections() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature featureOne = mock(Feature.class);
        Feature featureTwo = mock(Feature.class);
        Feature featureThree = mock(Feature.class);
        Feature featureFour = mock(Feature.class);
        VocabularyTerm termOne = mock(VocabularyTerm.class);
        VocabularyTerm termTwo = mock(VocabularyTerm.class);
        VocabularyTerm termThree = mock(VocabularyTerm.class);
        VocabularyTerm termFour = mock(VocabularyTerm.class);
        Set<Feature> features = new HashSet<>();
        List<String> mappingIdsOne = new LinkedList<>();
        List<String> mappingIdsTwo = new LinkedList<>();
        Map<String, List<String>> mapping = new HashMap<>();
        features.add(featureOne);
        features.add(featureTwo);
        features.add(featureThree);
        features.add(featureFour);
        mappingIdsOne.add("id1");
        mappingIdsOne.add("id3");
        mappingIdsTwo.add("id2");
        mapping.put("sectionOne", mappingIdsOne);
        mapping.put("sectionTwo", mappingIdsTwo);

        ComponentManager componentManager = mock(ComponentManager.class);
        Vocabulary ontologyService = mock(Vocabulary.class);
        List<Map<String, List<String>>> mappingObj = new LinkedList<>();
        List<Map<String, List<String>>> mappingObjSpy = spy(mappingObj);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);

        doReturn(componentManager).when(helpersSpy).getComponentManager();
        doReturn(ontologyService).when(componentManager).getInstance(eq(Vocabulary.class), eq("hpo"));
        doReturn(phenotypeMappingService).when(componentManager)
            .getInstance(eq(ScriptService.class), eq("phenotypeMapping"));
        doReturn(mappingObjSpy).when(phenotypeMappingService).get(anyString());
        doReturn(mapping).when(helpersSpy).getCategoryMapping();

        doReturn(true).when(featureOne).isPresent();
        doReturn(false).when(featureTwo).isPresent();
        doReturn(false).when(featureThree).isPresent();
        doReturn(true).when(featureFour).isPresent();
        doReturn(termOne).when(ontologyService).getTerm("id1");
        doReturn(termTwo).when(ontologyService).getTerm("id2");
        doReturn(termThree).when(ontologyService).getTerm("id3");
        doReturn(mappingIdsOne).when(termOne).get(anyString());
        doReturn(mappingIdsOne).when(termThree).get(anyString());
        doReturn(mappingIdsTwo).when(termTwo).get(anyString());
        doReturn(new LinkedList<String>()).when(termFour).get(anyString());
        doReturn("id1").when(featureOne).getId();
        doReturn("id2").when(featureTwo).getId();
        doReturn("id3").when(featureThree).getId();
        doReturn("id4").when(featureFour).getId();

        helpersSpy.newPatient();
        helpersSpy.featureSetUp(true, true, true);
        List<Feature> sorted = helpersSpy.sortFeaturesWithSections(features);

        Assert.assertTrue(sorted.contains(featureOne));
        Assert.assertTrue(sorted.contains(featureTwo));
        Assert.assertTrue(sorted.contains(featureThree));
        Assert.assertTrue(sorted.contains(featureFour));
        Assert.assertTrue(helpersSpy.getSectionFeatureTree().containsKey("id4"));
    }

    @Test(expected = NullPointerException.class)
    public void sortFeaturesWithSectionsNewPatientNotCalled() throws Exception
    {
        ConversionHelpers helpers = new ConversionHelpers();
        ConversionHelpers helpersSpy = spy(helpers);
        Feature featureOne = mock(Feature.class);
        Feature featureTwo = mock(Feature.class);
        Feature featureThree = mock(Feature.class);
        VocabularyTerm termOne = mock(VocabularyTerm.class);
        VocabularyTerm termTwo = mock(VocabularyTerm.class);
        VocabularyTerm termThree = mock(VocabularyTerm.class);
        Set<Feature> features = new HashSet<>();
        List<String> mappingIdsOne = new LinkedList<>();
        List<String> mappingIdsTwo = new LinkedList<>();
        Map<String, List<String>> mapping = new HashMap<>();
        features.add(featureOne);
        features.add(featureTwo);
        features.add(featureThree);
        mappingIdsOne.add("id1");
        mappingIdsOne.add("id3");
        mappingIdsTwo.add("id2");
        mapping.put("sectionOne", mappingIdsOne);
        mapping.put("sectionTwo", mappingIdsTwo);

        ComponentManager componentManager = mock(ComponentManager.class);
        Vocabulary ontologyService = mock(Vocabulary.class);
        List<Map<String, List<String>>> mappingObj = new LinkedList<>();
        List<Map<String, List<String>>> mappingObjSpy = spy(mappingObj);
        PhenotypeMappingService phenotypeMappingService = mock(PhenotypeMappingService.class);

        doReturn(componentManager).when(helpersSpy).getComponentManager();
        doReturn(ontologyService).when(componentManager).getInstance(eq(Vocabulary.class), eq("hpo"));
        doReturn(phenotypeMappingService).when(componentManager)
            .getInstance(eq(ScriptService.class), eq("phenotypeMapping"));
        doReturn(mappingObjSpy).when(phenotypeMappingService).get(anyString());
        doReturn(mapping).when(helpersSpy).getCategoryMapping();

        doReturn(true).when(featureOne).isPresent();
        doReturn(false).when(featureTwo).isPresent();
        doReturn(false).when(featureThree).isPresent();
        doReturn(termOne).when(ontologyService).getTerm("id1");
        doReturn(termTwo).when(ontologyService).getTerm("id2");
        doReturn(termThree).when(ontologyService).getTerm("id3");
        doReturn(mappingIdsOne).when(termOne).get(anyString());
        doReturn(mappingIdsOne).when(termThree).get(anyString());
        doReturn(mappingIdsTwo).when(termTwo).get(anyString());
        doReturn("id1").when(featureOne).getId();
        doReturn("id2").when(featureTwo).getId();
        doReturn("id3").when(featureThree).getId();

        helpersSpy.featureSetUp(true, true, true);
        List<Feature> sorted = helpersSpy.sortFeaturesWithSections(features);

        Assert.assertFalse(sorted.contains(featureOne));
        Assert.assertTrue(sorted.contains(featureTwo));
    }
}
