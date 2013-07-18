/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.similarity.internal;

import org.phenotips.data.Phenotype;
import org.phenotips.data.similarity.PhenotypeSimilarityScorer;
import org.phenotips.data.similarity.internal.DefaultPhenotypeSimilarityScorer;
import org.phenotips.data.similarity.internal.mocks.MockOntologyTerm;
import org.phenotips.data.similarity.internal.mocks.MockPhenotype;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PhenotypeSimilarityScorer} {@link DefaultPhenotypeSimilarityScorer implementation}.
 * 
 * @version $Id$
 */
public class DefaultPhenotypeSimilarityScorerTest
{
    @Rule
    public final MockitoComponentMockingRule<PhenotypeSimilarityScorer> mocker =
        new MockitoComponentMockingRule<PhenotypeSimilarityScorer>(
            DefaultPhenotypeSimilarityScorer.class);

    /** Same term should get the maximum score. */
    @Test
    public void testEqualValues() throws ComponentLookupException
    {
        Phenotype match = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Phenotype reference = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Assert.assertEquals(1.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
    }

    /** Different terms should get a zero score. */
    @Test
    public void testDifferentValues() throws ComponentLookupException
    {
        Phenotype match = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Phenotype reference = new MockPhenotype("HP:0001382", "Abnormal joint mobility", "phenotype", true);
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
    }

    /** Related terms should get a high subunitary score. */
    @Test
    public void testCloselyRelatedValues() throws ComponentLookupException
    {
        Phenotype match = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Phenotype reference = new MockPhenotype("HP:0001256", "Mild intelletual disability", "phenotype", true);

        double score = this.mocker.getComponentUnderTest().getScore(match, reference);
        Assert.assertEquals(0.5, score, 0.1);

        double reversedScore = this.mocker.getComponentUnderTest().getScore(reference, match);
        Assert.assertEquals(score, reversedScore, 1.0E-5);
    }

    /** Farther away related terms should get a low subunitary score. */
    @Test
    public void testFarRelatedValues() throws ComponentLookupException
    {
        Phenotype match = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Phenotype reference = new MockPhenotype("HP:0011446", "Abnormal higher mental function", "phenotype", true);
        Assert.assertEquals(0.2, this.mocker.getComponentUnderTest().getScore(match, reference), 0.1);
    }

    /** Too farther away related terms should get a zero score. */
    @Test
    public void testTooFarRelatedValues() throws ComponentLookupException
    {
        Phenotype match = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Phenotype reference = new MockPhenotype("HP:0000707", "Abnormality of the nervous system", "phenotype", true);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
    }

    /** Unknown phenotypes should get a NaN score. */
    @Test
    public void testUnknownValues() throws ComponentLookupException
    {
        Phenotype match = new MockPhenotype("HP:0123456", "Some phenotype", "phenotype", true);
        Phenotype reference = new MockPhenotype("HP:0000707", "Abnormality of the nervous system", "phenotype", true);
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getScore(match, reference)));
    }

    /** Missing reference should get a NaN score. */
    @Test
    public void testMissingReference() throws ComponentLookupException
    {
        Phenotype match = new MockPhenotype("HP:0001249", "Intellectual disability", "phenotype", true);
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getScore(match, null)));
    }

    /** Missing match should get a zero score. */
    @Test
    public void testMissingMatch() throws ComponentLookupException
    {
        Phenotype reference = new MockPhenotype("ONTO:0001", "Low value", "range", true);
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getScore(null, reference)));
    }

    /** Missing both match and reference should get a zero score. */
    @Test
    public void testMissingMatchAndReference() throws ComponentLookupException
    {
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getScore(null, null)));
    }

    @Before
    public void setupComponents() throws ComponentLookupException
    {
        // Setup the ontology manager
        OntologyManager om = this.mocker.getInstance(OntologyManager.class);
        Set<OntologyTerm> ancestors = new HashSet<OntologyTerm>();

        OntologyTerm all =
            new MockOntologyTerm("HP:0000001", Collections.<OntologyTerm> emptySet(),
                Collections.<OntologyTerm> emptySet());
        ancestors.add(all);
        OntologyTerm phenotypes =
            new MockOntologyTerm("HP:0000118", Collections.singleton(all), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(phenotypes);
        OntologyTerm abnormalNS =
            new MockOntologyTerm("HP:0000707", Collections.singleton(phenotypes), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalNS);
        OntologyTerm abnormalCNS =
            new MockOntologyTerm("HP:0002011", Collections.singleton(abnormalNS), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalCNS);
        OntologyTerm abnormalHMF =
            new MockOntologyTerm("HP:0011446", Collections.singleton(abnormalCNS), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalHMF);
        OntologyTerm cognImp =
            new MockOntologyTerm("HP:0100543", Collections.singleton(abnormalHMF), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(cognImp);
        OntologyTerm intDis =
            new MockOntologyTerm("HP:0001249", Collections.singleton(cognImp), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(intDis);
        OntologyTerm mildIntDis =
            new MockOntologyTerm("HP:0001256", Collections.singleton(intDis), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(mildIntDis);
        for (OntologyTerm term : ancestors) {
            when(om.resolveTerm(term.getId())).thenReturn(term);
        }

        ancestors.clear();
        ancestors.add(all);
        ancestors.add(phenotypes);
        OntologyTerm abnormalSkelS =
            new MockOntologyTerm("HP:0000924", Collections.singleton(phenotypes), new HashSet<OntologyTerm>(ancestors));
        ancestors.add(abnormalSkelS);
        OntologyTerm abnormalSkelM =
            new MockOntologyTerm("HP:0011842", Collections.singleton(abnormalSkelS), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(abnormalSkelM);
        OntologyTerm abnormalJointMorph =
            new MockOntologyTerm("HP:0001367", Collections.singleton(abnormalSkelM), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(abnormalJointMorph);
        OntologyTerm abnormalJointMob =
            new MockOntologyTerm("HP:0011729", Collections.singleton(abnormalJointMorph), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(abnormalJointMob);
        OntologyTerm jointHyperm =
            new MockOntologyTerm("HP:0001382", Collections.singleton(abnormalJointMob), new HashSet<OntologyTerm>(
                ancestors));
        ancestors.add(jointHyperm);
        for (OntologyTerm term : ancestors) {
            when(om.resolveTerm(term.getId())).thenReturn(term);
        }
    }
}
