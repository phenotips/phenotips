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
package org.phenotips.diagnosis.internal;

import org.phenotips.diagnosis.DiagnosisService;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the validity of the results returned by BOQA.
 */
public class DefaultDiagnosisServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<DiagnosisService> mocker =
        new MockitoComponentMockingRule<>(DefaultDiagnosisService.class);

    @Test
    public void returnsCorrectDiagnosis() throws ComponentLookupException, InterruptedException
    {
        /** This test is prone to outdated ontologies. */
        List<List<String>> phenotypes = new LinkedList<>();
        List<List<String>> disorderIds = new LinkedList<>();
        phenotypes.add(Arrays.asList(new String[0]));
        disorderIds.add(Arrays.asList(new String[0]));
        phenotypes.add(
            Arrays.asList("HP:0000028", "HP:0000049", "HP:0000202", "HP:0000204", "HP:0000316", "HP:0001869"));
        disorderIds.add(Arrays.asList("MIM:100050"));
        phenotypes.add(
            Arrays.asList("HP:0000707", "HP:0001939", "HP:0003811"));
        disorderIds.add(Arrays.asList("MIM:306300"));
        phenotypes.add(Arrays.asList("HP:0001417", "HP:0001287"));
        disorderIds.add(Arrays.asList("MIM:308250"));
        /* Harder tests */
        phenotypes.add(Arrays.asList("HP:0001419", "HP:0001939", "HP:0001005"));
        disorderIds.add(Arrays.asList("MIM:308600"));
        phenotypes.add(Arrays.asList("HP:0011495", "HP:0000502", "HP:0001005", "HP:0000534"));
        disorderIds.add(Arrays.asList("MIM:308800"));
        /* An empty/invalid HPO term will fail to find a boqa index and should be handled correctly */
        phenotypes.add(Arrays.asList("HP:"));
        disorderIds.add(Arrays.asList(new String[0]));
        phenotypes.add(
            Arrays.asList("HP:0000028", "HP:0000049", "HP:", "HP:0000202", "HP:0000204", "HP:0000316", "HP:0001869"));
        disorderIds.add(Arrays.asList("MIM:100050"));

        int invalidPhenotypes = 2;

        VocabularyManager vocabulary = this.mocker.getInstance(VocabularyManager.class);

        doAnswer(new Answer<VocabularyTerm>()
        {
            @Override
            public VocabularyTerm answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                String id = (String) invocationOnMock.getArguments()[0];
                VocabularyTerm term = mock(VocabularyTerm.class);
                doReturn(id).when(term).getId();
                doReturn("test").when(term).getName();
                return term;
            }
        }).when(vocabulary).resolveTerm(anyString());

        DiagnosisService diagnosisService = this.mocker.getComponentUnderTest();

        int limit = 3;
        int i = 0;
        List<String> nonstandardPhenotypeSet = new LinkedList<>();
        nonstandardPhenotypeSet.add("Non-standard term");
        for (List<String> phenotypeSet : phenotypes) {
            List<VocabularyTerm> diagnoses =
                diagnosisService.getDiagnosis(phenotypeSet, nonstandardPhenotypeSet, limit);
            List<String> diagnosisIds = new LinkedList<>();
            for (VocabularyTerm diagnosis : diagnoses) {
                diagnosisIds.add(diagnosis.getId());
            }
            assertTrue(diagnosisIds.containsAll(disorderIds.get(i)));
            i++;
        }
        verify(vocabulary, times(limit * (i - invalidPhenotypes))).resolveTerm(anyString());
    }
}
