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

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
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

//        String ontologyPath = BOQA.class.getClassLoader().getResource("hp.obo.gz").getPath();
//        String annotationPath = BOQA.class.getClassLoader().getResource("new_phenotype.gz").getPath();
        String ontologyPath = "/home/meatcar/dev/boqa/data/hp.obo.gz";
        String annotationPath = "/home/meatcar/dev/boqa/data/new_phenotype.gz";

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
                if (res.getMarginal(o1) < res.getMarginal(o2)) { return 1; }
                if (res.getMarginal(o1) > res.getMarginal(o2)) { return -1; }
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

            // ignore ORPHANET, make sure we still get #limit results
            if ("ORPHANET".equals(ontologyId)) {
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

            results.add(term);

        }

        logger.debug(String.valueOf(results));

        return results;
    }
}
