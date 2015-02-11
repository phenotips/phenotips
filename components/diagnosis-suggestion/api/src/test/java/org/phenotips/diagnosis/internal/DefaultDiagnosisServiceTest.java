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
package org.phenotips.diagnosis.internal;

import org.phenotips.diagnosis.DiagnosisService;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.environment.Environment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import sonumina.boqa.calculation.BOQA;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the validity of the results returned by BOQA.
 */
public class DefaultDiagnosisServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<DiagnosisService> mocker =
        new MockitoComponentMockingRule<DiagnosisService>(DefaultDiagnosisService.class);

    @Rule
    public final MockitoComponentMockingRule<Utils> workingUtils =
        new MockitoComponentMockingRule<Utils>(BoqaUtils.class);

    @Test
    public void returnsCorrectDiagnosis() throws ComponentLookupException, IOException, InterruptedException
    {
        String tempDir = System.getProperty("java.io.tmpdir");

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

        OntologyManager ontology = mocker.getInstance(OntologyManager.class);
        Environment env = mocker.getInstance(Environment.class);
        Utils utils = mocker.getInstance(Utils.class);

        File tempMock = mock(File.class);
        doReturn(tempMock).when(env).getTemporaryDirectory();
        doReturn(tempDir).when(tempMock).getPath();

        Environment utilsEnv = workingUtils.getInstance(Environment.class);
        Utils workingUtilsComponent = workingUtils.getComponentUnderTest();
        String annotationPath =
            stream2file(BOQA.class.getClassLoader().getResourceAsStream("new_phenotype.gz")).getPath();
        String ontologyPath = stream2file(BOQA.class.getClassLoader().getResourceAsStream("hp.obo.gz")).getPath();

        File tempSpyObj = new File(tempDir);
        File tempSpy = spy(tempSpyObj);
        doReturn(tempSpy).when(utilsEnv).getTemporaryDirectory();
        workingUtilsComponent.loadDataFiles(ontologyPath, annotationPath);

        doAnswer(new Answer()
        {
            @Override
            public OntologyTerm answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                String id = (String) invocationOnMock.getArguments()[0];
                OntologyTerm term = mock(OntologyTerm.class);
                doReturn(id).when(term).getId();
                doReturn("test").when(term).getName();
                return term;
            }
        }).when(ontology).resolveTerm(anyString());

        doReturn(tempSpy).when(env).getTemporaryDirectory();
        doReturn(workingUtilsComponent.getGraph()).when(utils).getGraph();
        doReturn(workingUtilsComponent.getDataAssociation()).when(utils).getDataAssociation();
        DiagnosisService diagnosisService = mocker.getComponentUnderTest();

        int limit = 3;
        int i = 0;
        for (List<String> phenotypeSet : phenotypes) {
            List<OntologyTerm> diagnoses = diagnosisService.getDiagnosis(phenotypeSet, limit);
            List<String> diagnosisIds = new LinkedList<>();
            for (OntologyTerm diagnosis : diagnoses) {
                diagnosisIds.add(diagnosis.getId());
            }
            assertTrue(diagnosisIds.containsAll(disorderIds.get(i)));
            i++;
        }
        verify(ontology, times(limit * (i - invalidPhenotypes))).resolveTerm(anyString());
    }

    private File stream2file(InputStream in) throws IOException
    {
        final File tempFile = File.createTempFile("phenotips_test", ".tmp");
        tempFile.deleteOnExit();

        FileOutputStream out = new FileOutputStream(tempFile);
        IOUtils.copy(in, out);

        return tempFile;
    }
}
