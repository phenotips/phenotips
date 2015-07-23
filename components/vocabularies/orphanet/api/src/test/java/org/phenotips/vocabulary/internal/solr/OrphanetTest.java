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

//import org.phenotips.vocabulary.SolrVocabularyResourceManager;
//import org.phenotips.vocabulary.Vocabulary;
//import org.phenotips.vocabulary.VocabularyTerm;
//
//import org.xwiki.cache.Cache;
//import org.xwiki.component.manager.ComponentLookupException;
//import org.xwiki.component.util.ReflectionUtils;
//import org.xwiki.test.mockito.MockitoComponentMockingRule;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//
//import org.apache.solr.client.solrj.SolrClient;
//import org.apache.solr.common.SolrInputDocument;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.mockito.Matchers;
//import org.mockito.MockitoAnnotations;
//
//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.ImmutableSet;
//import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
//import com.hp.hpl.jena.ontology.OntClass;
//import com.hp.hpl.jena.ontology.OntModel;
//import com.hp.hpl.jena.ontology.OntModelSpec;
//import com.hp.hpl.jena.ontology.OntProperty;
//import com.hp.hpl.jena.ontology.Restriction;
//import com.hp.hpl.jena.rdf.model.ModelFactory;
//import com.hp.hpl.jena.util.FileManager;
//
//import static com.hp.hpl.jena.vocabulary.OWL2.NS;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
///**
// * Tests for the {@link Orphanet} component.
// *
// * @version $Id $
// */
//public class OrphanetTest
//{
//    /** Root class identifiers */
//    private static final List<String> ROOT_CLASS_LABELS = ImmutableList.of("Orphanet_377789", "Orphanet_377794",
//        "Orphanet_377791", "Orphanet_377793", "Orphanet_377795", "Orphanet_377797", "Orphanet_377788",
//        "Orphanet_377790", "Orphanet_377792", "Orphanet_377796", "Orphanet_410297", "Orphanet_410298", "Orphanet_410299"
//    );
//
//    private static final String TERM_CATEGORY_LABEL = "term_category";
//
//    private static final String IS_A_LABEL = "is_a";
//
//    private static final String PRESENT_IN_LABEL = "present_in";
//
//    private static final String PART_OF_LABEL = "part_of";
//
//    private static final String POINT_PREV_RANGE_LABEL = "has_point_prevalence_range";
//
//    private static final String HAS_CASES_LABEL = "has_cases/families_average_value";
//
//    private static final String NAME_LABEL = "label";
//
//    private static final String MESH_ID_LABEL = "mesh_id";
//
//    private static final String OMIM_ID_LABEL = "omim_id";
//
//    private static final String ICD_10_ID_LABEL = "icd-10_id";
//
//    private static final String UMLS_ID_LABEL = "umls_id";
//
//    private static final String DEF_LABEL = "definition";
//
//    private static final String DEF_CITATION_LABEL = "definition_citation";
//
//    private static final String HEADER_INFO_LABEL = "HEADER_INFO";
//
//    private static final String ID_LABEL = "id";
//
//    private static final String VERSION_LABEL = "version";
//
//    @Rule
//    public MockitoComponentMockingRule<Vocabulary> mocker = new MockitoComponentMockingRule<Vocabulary>(Orphanet.class);
//
//    private Vocabulary orphanet;
//
//    private OntModel ontModel;
//
//    private SolrInputDocument doc;
//
//    private Method extractRestrictionData;
//
//    private Method extractIntersectionData;
//
//    private Method extractNamedClassData;
//
//    private Method parseSolrDocumentFromOntParentClasses;
//
//    private Method extractProperties;
//
//    @SuppressWarnings("unchecked")
//    @Before
//    public void setUp()
//        throws ComponentLookupException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException
//    {
//        MockitoAnnotations.initMocks(this);
//
//        final Cache<VocabularyTerm> cache = mock(Cache.class);
//        final SolrClient server = mock(SolrClient.class);
//
//        final SolrVocabularyResourceManager externalServicesAccess =
//            this.mocker.getInstance(SolrVocabularyResourceManager.class);
//
//        when(externalServicesAccess.getTermCache()).thenReturn(cache);
//        when(externalServicesAccess.getSolrConnection()).thenReturn(server);
//
//        this.doc = spy(new SolrInputDocument());
//        this.orphanet = this.mocker.getComponentUnderTest();
//
//        this.ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
//        this.ontModel.read(FileManager.get().open("orphanet-test.owl"), NS);
//
//        this.extractRestrictionData = this.orphanet.getClass().getDeclaredMethod("extractRestrictionData",
//            SolrInputDocument.class, OntClass.class);
//        this.extractRestrictionData.setAccessible(true);
//
//        this.extractIntersectionData = this.orphanet.getClass().getDeclaredMethod("extractIntersectionData",
//            SolrInputDocument.class, OntClass.class, OntClass.class);
//        this.extractIntersectionData.setAccessible(true);
//
//        this.extractNamedClassData = this.orphanet.getClass().getDeclaredMethod("extractNamedClassData",
//            SolrInputDocument.class, OntClass.class, OntClass.class);
//        this.extractNamedClassData.setAccessible(true);
//
//        this.parseSolrDocumentFromOntParentClasses = this.orphanet.getClass().getSuperclass()
//            .getDeclaredMethod("parseSolrDocumentFromOntParentClasses", SolrInputDocument.class, OntClass.class);
//        this.parseSolrDocumentFromOntParentClasses.setAccessible(true);
//
//        this.extractProperties = this.orphanet.getClass().getSuperclass().getDeclaredMethod("extractProperties",
//            SolrInputDocument.class, OntClass.class);
//        this.extractProperties.setAccessible(true);
//
//        final Field field = ReflectionUtils.getField(this.orphanet.getClass(), "hierarchyRoots");
//        field.setAccessible(true);
//        final Set<OntClass> hierarchyRoots = ImmutableSet.<OntClass>builder()
//            .add(this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_C001"))
//            .add(this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_C010"))
//            .build();
//        field.set(this.orphanet, hierarchyRoots);
//    }
//
//    @Test
//    public void getRootClassesTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
//    {
//        final Method getRoots = this.orphanet.getClass().getDeclaredMethod("getRootClasses", OntModel.class);
//        getRoots.setAccessible(true);
//        @SuppressWarnings("unchecked")
//        final Collection<OntClass> roots = (Collection<OntClass>) getRoots.invoke(this.orphanet, this.ontModel);
//
//        // Check we get all the relevant root classes.
//        assertTrue(ROOT_CLASS_LABELS.size() == roots.size());
//        for (final OntClass root : roots) {
//            assertTrue(ROOT_CLASS_LABELS.contains(root.getLocalName()));
//        }
//    }
//    @Test
//    public void extractRestrictionDataUnspecifiedRestrictionsShouldBeIgnored()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_102003");
//        final OntProperty testProperty = this.ontModel.createOntProperty("http://www.orpha.net/ORDO#TestProperty");
//        final Restriction restriction = this.ontModel.createRestriction(testProperty);
//        final AllValuesFromRestriction avfRestr = restriction.convertToAllValuesFromRestriction(ontClass);
//
//        this.extractRestrictionData.invoke(this.orphanet, this.doc, avfRestr);
//
//        verify(this.doc, never()).addField(Matchers.anyString(), Matchers.any());
//        assertEquals(Collections.emptySet(), this.doc.keySet());
//    }
//
//    @Test
//    public void extractRestrictionDataSomeValuesFromRestrictionsAreAdded()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_xxxxxx");
//
//        final OntClass parent = ontClass.listSuperClasses(true).next();
//        this.extractRestrictionData.invoke(this.orphanet, this.doc, parent);
//
//        verify(this.doc, times(1)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("90642", doc.getFieldValue(PART_OF_LABEL).toString());
//        assertEquals(ImmutableSet.of(PART_OF_LABEL), this.doc.keySet());
//    }
//
//    @Test
//    public void extractRestrictionDataHasValueRestrictionsAreAdded()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_yyyyyy");
//
//        final OntClass parent = ontClass.listSuperClasses(true).next();
//        this.extractRestrictionData.invoke(this.orphanet, this.doc, parent);
//
//        verify(this.doc, times(1)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("6.0", doc.getFieldValue(HAS_CASES_LABEL).toString());
//        assertEquals(ImmutableSet.of(HAS_CASES_LABEL), this.doc.keySet());
//    }
//
//    @Test
//    public void extractIntersectionDataAllRestrictionsAreAdded() throws InvocationTargetException,
//        IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_zzzzzz");
//
//        final OntClass parent = ontClass.listSuperClasses(true).next();
//        this.extractIntersectionData.invoke(this.orphanet, this.doc, ontClass, parent);
//
//        verify(this.doc, times(2)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("7.0", doc.getFieldValue(HAS_CASES_LABEL).toString());
//        assertEquals("Worldwide", doc.getFieldValue(PRESENT_IN_LABEL).toString());
//        assertEquals(ImmutableSet.of(PRESENT_IN_LABEL, HAS_CASES_LABEL), this.doc.keySet());
//    }
//
//    @Test
//    public void extractNamedClassDataParentIsHierarchyRoot() throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_zzzzzz");
//        final OntClass parent = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_C001");
//        ontClass.setSuperClass(parent);
//        this.extractNamedClassData.invoke(this.orphanet, this.doc, ontClass, parent);
//        verify(this.doc, never()).addField(Matchers.anyString(), Matchers.any());
//        assertEquals(Collections.emptySet(), this.doc.keySet());
//    }
//
//    @Test
//    public void extractNamedClassDataParentIsOneOfPhenomeSubcategories()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_1000");
//        final OntClass parent = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_377788");
//        this.extractNamedClassData.invoke(this.orphanet, this.doc, ontClass, parent);
//        verify(this.doc, never()).addField(Matchers.anyString(), Matchers.any());
//        assertEquals(Collections.emptySet(), this.doc.keySet());
//    }
//
//    @Test
//    public void extractNamedClassDataParentIsDirectSuperclass() throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_68363");
//        final OntClass parent = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_102003");
//        this.extractNamedClassData.invoke(this.orphanet, this.doc, ontClass, parent);
//        verify(this.doc, times(2)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("102003", this.doc.getFieldValue(TERM_CATEGORY_LABEL).toString());
//        assertEquals("102003", this.doc.getFieldValue(IS_A_LABEL).toString());
//        assertEquals(ImmutableSet.of(TERM_CATEGORY_LABEL, IS_A_LABEL), this.doc.keySet());
//    }
//
//    @Test
//    public void extractNamedClassDataParentIsIndirectSuperclass()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_156159");
//        final OntClass parent = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_102003");
//        this.extractNamedClassData.invoke(this.orphanet, this.doc, ontClass, parent);
//        verify(this.doc, times(1)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("102003", this.doc.getFieldValue(TERM_CATEGORY_LABEL).toString());
//        assertEquals(null, this.doc.get(IS_A_LABEL));
//        assertEquals(ImmutableSet.of(TERM_CATEGORY_LABEL), this.doc.keySet());
//    }
//
//    @Test
//    public void parseSolrDocumentFromOntParentClassesParentsAreCategoryRoots()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_90642");
//        this.parseSolrDocumentFromOntParentClasses.invoke(this.orphanet, this.doc, ontClass);
//        verify(this.doc, never()).addField(Matchers.anyString(), Matchers.any());
//        assertEquals(Collections.emptySet(), this.doc.keySet());
//    }
//
//    @Test
//    public void parseSolrDocumentFromOntParentClassesParentsAreCategoryRootsHasAnonymousParents()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_1000");
//        this.parseSolrDocumentFromOntParentClasses.invoke(this.orphanet, this.doc, ontClass);
//        verify(this.doc, times(4)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("Worldwide", this.doc.getFieldValue(PRESENT_IN_LABEL).toString());
//        assertEquals("<1 / 1 000 000", this.doc.getFieldValue(POINT_PREV_RANGE_LABEL)
//            .toString());
//        assertEquals("7.0", this.doc.getFieldValue(HAS_CASES_LABEL).toString());
//        assertEquals("90642", this.doc.getFieldValue(PART_OF_LABEL).toString());
//        assertEquals(ImmutableSet.of(PRESENT_IN_LABEL, POINT_PREV_RANGE_LABEL, HAS_CASES_LABEL, PART_OF_LABEL),
//            this.doc.keySet());
//    }
//
//    @Test
//    public void parseSolrDocumentFromOntParentClassesMultipleParentLevels()
//        throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_376724");
//        this.parseSolrDocumentFromOntParentClasses.invoke(this.orphanet, this.doc, ontClass);
//
//        final Set<String> expected = ImmutableSet.of("156159", "183521", "71859", "391799", "68363", "102003", "98053",
//            "98006");
//
//        verify(this.doc, times(9)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("156159", this.doc.getFieldValue(IS_A_LABEL).toString());
//        assertEquals(expected, ImmutableSet.builder().addAll(this.doc.getFieldValues(TERM_CATEGORY_LABEL)).build());
//        assertEquals(ImmutableSet.of(TERM_CATEGORY_LABEL, IS_A_LABEL), this.doc.keySet());
//    }
//
//    @Test
//    public void extractPropertiesHasSeveralProperties() throws InvocationTargetException, IllegalAccessException
//    {
//        final OntClass ontClass = this.ontModel.getOntClass("http://www.orpha.net/ORDO/Orphanet_1000");
//        this.extractProperties.invoke(this.orphanet, this.doc, ontClass);
//        verify(this.doc, times(7)).addField(Matchers.anyString(), Matchers.any());
//        assertEquals("Ocular albinism with late-onset sensorineural deafness",
//            this.doc.getFieldValue(NAME_LABEL));
//        assertEquals("orphanet", this.doc.getFieldValue(DEF_CITATION_LABEL));
//        assertEquals("Ocular albinism with late-onset sensorineural deafness (OASD), is a rare, X-linked "
//                + "inherited type of ocular albinism (see this term) described in one African kindred (7 males over "
//                + "3 generations) to date, characterized by severe visual impairment, translucent pale-blue iridies, "
//                + "a reduction in the retinal pigment and moderately severe deafness by middle age (fourth to fifth "
//                + "decade of life). It is unclear whether it is allelic to X-linked recessive ocular albinism (see "
//                + "this term) or a contiguous gene syndrome.", this.doc.getFieldValue(DEF_LABEL));
//        assertEquals("300650", this.doc.getFieldValue(OMIM_ID_LABEL));
//        assertEquals("C537043", this.doc.getFieldValue(MESH_ID_LABEL));
//        assertEquals("E70.3", this.doc.getFieldValue(ICD_10_ID_LABEL));
//        assertEquals("C1845069", this.doc.getFieldValue(UMLS_ID_LABEL));
//        assertEquals(ImmutableSet.of(NAME_LABEL, DEF_CITATION_LABEL, DEF_LABEL, OMIM_ID_LABEL, MESH_ID_LABEL,
//            ICD_10_ID_LABEL, UMLS_ID_LABEL), this.doc.keySet());
//    }
//
//    @Test
//    public void setVersionTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
//    {
//        final Method setVersion = this.orphanet.getClass().getSuperclass().getDeclaredMethod("setVersion",
//            SolrInputDocument.class, OntModel.class);
//        setVersion.setAccessible(true);
//        setVersion.invoke(this.orphanet, this.doc, this.ontModel);
//        verify(this.doc, times(1)).addField(ID_LABEL, HEADER_INFO_LABEL);
//        verify(this.doc, times(1)).addField(VERSION_LABEL, "2.3");
//    }
//}
