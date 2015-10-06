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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;

public class OmimSourceParser
{
    private static final String OMIM_SOURCE_URL = "ftp://ftp.omim.org/OMIM/omim.txt.Z";

    private static final String RECORD_MARKER = "*RECORD*";

    private static final String FIELD_MARKER = "*FIELD* ";

    private static final String FIELD_MIM_NUMBER = "NO";

    private static final String FIELD_TITLE = "TI";

    private static final String FIELD_TEXT = "TX";

    private static final String END_MARKER = "*THEEND*";

    private static final String ANNOTATIONS_BASE_URL =
        "http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/";

    private static final String GENE_ANNOTATIONS_URL = "ftp://ftp.omim.org/OMIM/mim2gene.txt";

    private static final String POSITIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "phenotype_annotation.tab";

    private static final String NEGATIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "negative_phenotype_annotation.tab";

    private static final String GENEREVIEWS_MAPPING_URL =
        "ftp://ftp.ncbi.nih.gov/pub/GeneReviews/NBKid_shortname_OMIM.txt";

    private static final String ENCODING = "UTF-8";

    private SolrInputDocument crtTerm;

    private Map<String, SolrInputDocument> data = new HashMap<>();

    public OmimSourceParser()
    {
        try (BufferedReader in =
            new BufferedReader(new InputStreamReader(new CompressorStreamFactory().createCompressorInputStream(
                new URL(OMIM_SOURCE_URL).openConnection().getInputStream()), "UTF-8"))) {
            transform(in);
            loadGenes();
            loadSymptoms(true);
            loadSymptoms(false);
            loadGeneReviews();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (CompressorException ex) {
            ex.printStackTrace();
        }
    }

    public Map<String, SolrInputDocument> getData()
    {
        return this.data;
    }

    private Map<String, SolrInputDocument> transform(BufferedReader in) throws IOException
    {
        String line;
        StringBuilder fieldValue = new StringBuilder();
        String fieldName = null;
        while ((line = in.readLine()) != null) {
            if (RECORD_MARKER.equalsIgnoreCase(line) || END_MARKER.equalsIgnoreCase(line)) {
                if (this.crtTerm != null) {
                    loadField(fieldName, fieldValue.toString().trim());
                    storeCrtTerm();
                } else {
                    this.crtTerm = new SolrInputDocument();
                }
            } else if (line.startsWith(FIELD_MARKER)) {
                loadField(fieldName, fieldValue.toString().trim());
                fieldValue.setLength(0);
                fieldName = line.substring(FIELD_MARKER.length());
            } else {
                fieldValue.append(line.trim()).append(" ");
            }
        }

        return this.data;
    }

    private void storeCrtTerm()
    {
        this.data.put(String.valueOf(this.crtTerm.get("id").getFirstValue()), this.crtTerm);
        this.crtTerm = new SolrInputDocument();
    }

    private void loadField(String name, String value)
    {
        if (StringUtils.isAnyBlank(name, value)) {
            return;
        }
        switch (name) {
            case FIELD_MIM_NUMBER:
                this.crtTerm.addField("id", value);
                break;
            case FIELD_TITLE:
                String title = StringUtils.substringBefore(value, ";;").trim();
                String[] synonyms = StringUtils.split(StringUtils.substringAfter(value, ";;"), ";;");
                this.crtTerm.addField("name", title);
                for (String synonym : synonyms) {
                    this.crtTerm.addField("synonym", synonym.trim());
                }
                break;
            case FIELD_TEXT:
                this.crtTerm.addField("def", value);
                break;
            default:
                return;
        }
    }

    private void loadSymptoms(boolean positive)
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(positive ? POSITIVE_ANNOTATIONS_URL : NEGATIVE_ANNOTATIONS_URL)
                .openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.parse(in)) {
                if ("OMIM".equals(row.get(0))) {
                    SolrInputDocument term = this.data.get(row.get(1));
                    if (term != null) {
                        term.addField(positive ? "actual_symptom" : "actual_not_symptom", row.get(4));
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadGenes()
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENE_ANNOTATIONS_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                if (!row.get("Type").contains("gene")) {
                    continue;
                }
                SolrInputDocument term = this.data.get(row.get(2));
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

    private void loadGeneReviews()
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENEREVIEWS_MAPPING_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                SolrInputDocument term = this.data.get(row.get(2));
                if (term != null) {
                    term.addField("gene_reviews_link", "https://www.ncbi.nlm.nih.gov/books/" + row.get(0));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
