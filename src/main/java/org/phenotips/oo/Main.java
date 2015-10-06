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
package org.phenotips.oo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.solr.common.SolrInputDocument;

public class Main
{
    private static final String ANNOTATIONS_BASE_URL =
        "http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/";

    private static final String OMIM_SOURCE_URL = "ftp://ftp.omim.org/OMIM/omim.txt.Z";

    private static final String GENE_ANNOTATIONS_URL = "ftp://ftp.omim.org/OMIM/mim2gene.txt";

    private static final String POSITIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "phenotype_annotation.tab";

    private static final String NEGATIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "negative_phenotype_annotation.tab";

    private static final String GENEREVIEWS_MAPPING_URL =
        "ftp://ftp.ncbi.nih.gov/pub/GeneReviews/NBKid_shortname_OMIM.txt";

    private static final String ENCODING = "UTF-8";

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        OmimSourceParser omimDataSource = new OmimSourceParser(OMIM_SOURCE_URL);

        Map<String, SolrInputDocument> omimData = omimDataSource.getData();
        loadGenes(omimData);
        loadSymptoms(omimData);
        loadGeneReviews(omimData);

        for (SolrInputDocument doc : omimData.values()) {
            System.out.println(doc.toString());
        }
    }

    private static void loadSymptoms(Map<String, SolrInputDocument> data)
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(POSITIVE_ANNOTATIONS_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.parse(in)) {
                if ("OMIM".equals(row.get(0))) {
                    SolrInputDocument term = data.get(row.get(1));
                    if (term != null) {
                        term.addField("actual_symptom", row.get(4));
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(NEGATIVE_ANNOTATIONS_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.parse(in)) {
                if ("OMIM".equals(row.get(0))) {
                    SolrInputDocument term = data.get(row.get(1));
                    if (term != null) {
                        term.addField("actual_not_symptom", row.get(4));
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void loadGenes(Map<String, SolrInputDocument> data)
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENE_ANNOTATIONS_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                if (!row.get("Type").contains("gene")) {
                    continue;
                }
                SolrInputDocument term = data.get(row.get(2));
                if (term != null) {
                    String gs = row.get("Approved Gene Symbol");
                    if (!"-".equals(gs)) {
                        term.addField("GENE", gs);
                    }
                    String eid = row.get("Ensembl Gene ID");
                    if (!"-".equals(eid)) {
                        term.addField("GENE", eid);
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void loadGeneReviews(Map<String, SolrInputDocument> data)
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENEREVIEWS_MAPPING_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                SolrInputDocument term = data.get(row.get(2));
                if (term != null) {
                    term.addField("gene_reviews_link", "https://www.ncbi.nlm.nih.gov/books/" + row.get(0));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
