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
package org.phenotips.hpoa.main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.phenotips.hpoa.PhenotypeMappingScriptService;
import org.phenotips.hpoa.annotation.AnnotationTerm;
import org.phenotips.hpoa.annotation.GeneHPOAnnotations;
import org.phenotips.hpoa.annotation.OmimHPOAnnotations;
import org.phenotips.hpoa.annotation.PrettyPrint;
import org.phenotips.hpoa.annotation.SearchResult;
import org.phenotips.hpoa.ontology.HPO;
import org.phenotips.hpoa.ontology.Ontology;
import org.phenotips.hpoa.prediction.ICPredictor;

public class Main
{
    private static PhenotypeMappingScriptService hpoa;

    public static void main(String[] args)
    {
        Ontology hpo = HPO.getInstance();
        // PrettyPrint.printList(hpo.getNodes(), System.out);
        GeneHPOAnnotations gann = new GeneHPOAnnotations(hpo);
        gann.load(hpoa.getInputFileHandler(
            "http://compbio.charite.de/svn/hpo/trunk/src/annotation/phenotype_to_genes.txt", false));

        OmimHPOAnnotations ann = new OmimHPOAnnotations(hpo);
        ann.load(hpoa.getInputFileHandler(
            "http://compbio.charite.de/svn/hpo/trunk/src/annotation/phenotype_annotation.tab", false));
        // // PrettyPrint.printList(ann.getOMIMNodes(), System.out);
        // // PrettyPrint.printList(ann.getHPONodes(), System.out);
        // //
        AnnotationTerm ot = ann.getOMIMNode("OMIM:163950");
        // IDAGNode t = hpo.getTerm("HP:0006936");

        // System.out.println(ann.getMICA("HP:0006936", "HP:0001344"));
        // System.out.println(ann.getMICA("HP:0006936", "HP:0000457"));
        // //System.out.println(ann.getMICA("HP:0200051", "HP:0005011"));
        // System.out
        // .println(ann.getIC(ann.getMICAId("HP:0200051", "HP:0005011")));

        // PrettyPrint.printMap(ann.getPhenotypes(ot.getId()), System.out);
        // ann.propagateHPOAnnotations();
        // PrettyPrint.printMap(ann.getPhenotypes(ot.getId()), System.out);
        //
        // // System.out.println(hpo.getRoot());
        //
        Set<String> phenotypes = new HashSet<String>()
        {
            {
                // add("HP:0000006");
                // add("HP:0000028");
                // add("HP:0000126");
                // add("HP:0000148");
                // add("HP:0000175");
                // add("HP:0000189");
                // add("HP:0000193");
                // add("HP:0000238");
                // add("HP:0000244");
                // add("HP:0000270");
                // add("HP:0000272");
                // add("HP:0000303");
                // add("HP:0000316");
                // add("HP:0000354");
                // add("HP:0000365");
                // add("HP:0000389");
                // add("HP:0000416");
                // add("HP:0000425");
                // add("HP:0000452");
                // add("HP:0000486");
                // add("HP:0000494");
                // add("HP:0000586");
                // add("HP:0000684");
                // add("HP:0000689");
                // add("HP:0001061");
                // add("HP:0001162");
                // add("HP:0001177");
                // add("HP:0001249");
                // add("HP:0001274");
                // add("HP:0001331");
                // add("HP:0001355");
                // add("HP:0001507");
                // add("HP:0001629");
                // add("HP:0002021");
                // add("HP:0002032");
                // add("HP:0002119");
                // add("HP:0002623");
                // add("HP:0003041");
                // add("HP:0004397");
                // add("HP:0004440");
                // add("HP:0004468");
                // add("HP:0004473");
                // add("HP:0004487");
                // add("HP:0004635");
                // add("HP:0005048");
                // add("HP:0006153");
                // add("HP:0007099");
                // add("HP:0007291");
                // add("HP:0007343");
                // add("HP:0008111");
                // add("HP:0009836");
                // add("HP:0010554");

                // add("HP:0001878");
                // add("HP:0000123");
                // add("HP:0000992");
                // add("tre");
                add("HP:0007565");
                add("HP:0000997");
                add("HP:0000256");
                // add("HP:0001369");
            }
        };

        for (String ph : phenotypes) {
            System.out.println(ph + "\t" + hpo.getName(ph));
        }
        System.out.println();
        ICPredictor predictor = new ICPredictor();
        predictor.setAnnotation(ann);
        final int LIMIT = 5;
        List<SearchResult> results = predictor.getMatches(phenotypes);
        PrettyPrint.printList(results, LIMIT, System.out);
        for (int i = 0; i < LIMIT; ++i) {
            System.out.println(ann.getAnnotationNode(results.get(i).getId()));
        }

        double matchScore =
            predictor
                .asymmetricPhenotypeSimilarity(phenotypes, ann.getPhenotypesWithAnnotation("OMIM:152700").keySet());
        System.out.println(matchScore);
        System.out.println(ann.getAnnotationNode("OMIM:152700"));

        List<SearchResult> presults = predictor.getDifferentialPhenotypes(phenotypes);
        PrettyPrint.printList(presults, 20, System.out);
    }
}
