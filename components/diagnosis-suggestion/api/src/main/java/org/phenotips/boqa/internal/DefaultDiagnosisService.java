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
package org.phenotips.boqa.internal;

import org.phenotips.boqa.DiagnosisService;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ontologizer.GlobalPreferences;
import ontologizer.benchmark.Datafiles;
import ontologizer.go.Term;
import ontologizer.types.ByteString;

import sonumina.boqa.calculation.BOQA;
import sonumina.boqa.calculation.Observations;

/**
 * An implementation of Diagnosis Service using BOQA
 * see (http://bioinformatics.oxfordjournals.org/content/28/19/2502.abstract)
 * Created by meatcar on 9/5/14.
 * @version $Id$
 */
@Singleton
@Component
public class DefaultDiagnosisService implements DiagnosisService, Initializable
{
    private Logger logger = LoggerFactory.getLogger(DefaultDiagnosisService.class);
    private BOQA boqa;
    private Map<Integer, ByteString> omimMap;

    @Inject
    private OntologyManager ontology;

    @Inject
    private Environment env;

    @Override
    public void initialize() throws InitializationException {
        //Initialize boqa
        boqa = new BOQA();
        boqa.setConsiderFrequenciesOnly(false);
        boqa.setPrecalculateScoreDistribution(false);
        boqa.setCacheScoreDistribution(false);
        boqa.setPrecalculateItemMaxs(false);
        boqa.setPrecalculateMaxICs(false);
        boqa.setMaxFrequencyTerms(2);
        boqa.setPrecalculateJaccard(false);

        GlobalPreferences.setProxyPort(888);
        GlobalPreferences.setProxyHost("realproxy.charite.de");

        String annotationPath  = null;
        String ontologyPath = null;
        try {
            annotationPath = stream2file(BOQA.class.getClassLoader().getResourceAsStream("new_phenotype.gz")).getPath();
            ontologyPath = stream2file(BOQA.class.getClassLoader().getResourceAsStream("hp.obo.gz")).getPath();
        } catch (IOException e) {
            throw new InitializationException(e.getMessage());
        }

        // Load datafiles
        Datafiles df = null;
        try {
            df = new Datafiles(ontologyPath, annotationPath);
        } catch (InterruptedException e) {
            throw new InitializationException(e.getMessage());
        } catch (IOException e) {
            throw new InitializationException(e.getMessage());
        }

        boqa.setup(df.graph, df.assoc);

        //Set up our index -> OMIM mapping by flipping the OMIM -> Index mapping in boqa
        Set<Map.Entry<ByteString, Integer>> omimtonum = boqa.item2Index.entrySet();
        this.omimMap = new HashMap<Integer, ByteString>(omimtonum.size());

        for (Map.Entry<ByteString, Integer> item : omimtonum) {
            this.omimMap.put(item.getValue(), item.getKey());
        }
    }

    private void addTermAndAncestors(Term t, Observations o) {
        int id = boqa.getTermIndex(t);
        o.observations[id] = true;
        boqa.activateAncestors(id, o.observations);
    }

    /**
     * Get a list of suggest diagnosies given a list of present phenotypes. Each phenotype is represented as a String
     * in the format {@code <ontology prefix>:<term id>}, for example
     *            {@code HP:0002066}
     *
     * @param phenotypes A List of String phenotypes observed in the patient
     * @param limit a number of phenotypes to return
     * @return A list of suggested diagnosies
     */
    public List<OntologyTerm> getDiagnosis(List<String> phenotypes, int limit) {
        Observations o = new Observations();
        o.observations = new boolean[boqa.getOntology().getNumberOfTerms()];

        //Add all hpo terms with ancestors to array of booleans
        for (String hpo : phenotypes) {
            Term t = boqa.getOntology().getTerm(hpo);
            addTermAndAncestors(t, o);
        }

        //Get marginals
        final BOQA.Result res = boqa.assignMarginals(o, false, 1);


        //All of this is sorting diseases by marginals
        Integer[] order = new Integer[res.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }

        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (res.getMarginal(o1) < res.getMarginal(o2)) {
                    return 1;
                }
                if (res.getMarginal(o1) > res.getMarginal(o2)) {
                    return -1;
                }
                return 0;
            }
        });

        //Get top limit results
        List<OntologyTerm> results = new ArrayList<OntologyTerm>();
        for (int id : order) {
            if (results.size() >= limit) {
                break;
            }

            String termId = String.valueOf(this.omimMap.get(id));
            String ontologyId = StringUtils.substringBefore(termId, ":");

            // ignore non-OMIM diseases (BOQA has ORPHANIET and DECIPHER as well)
            if (!"OMIM".equals(ontologyId)) {
                continue;
            }

            // Strip 'O' in "OMIM"
            termId = termId.substring(1);

            OntologyTerm term = ontology.resolveTerm(termId);

            if (term == null) {
                logger.warn(String.format(
                        "Unable to resolve OMIM term '%s' due to outdated OMIM ontology.", termId));
                continue;
            }

            // Do not suggest diseases that start with *, +, and ^
            Pattern pattern = Pattern.compile("[*+^]");
            if (pattern.matcher(term.getName().substring(0, 1)).matches()) {
                continue;
            }

            results.add(term);

        }

        logger.debug(String.valueOf(results));

        return results;
    }

    /**
     * Convert a stream into a file.
     * @param in an inputstream
     * @return a File
     * @throws IOException when we can't open file
     */
    public File stream2file(InputStream in) throws IOException {
        final File tempFile = File.createTempFile(env.getTemporaryDirectory().getPath(), ".tmp");
        tempFile.deleteOnExit();

        FileOutputStream out = new FileOutputStream(tempFile);
        IOUtils.copy(in, out);

        return tempFile;
    }
}
