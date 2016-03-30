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

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for parsing the special OMIM source, while also gathering annotations from other sources: OMIM-Gene symbols
 * mapping from OMIM, OMIM-Phenotype mapping from HPO, OMIM-GeneReviews mapping from NCBI.
 *
 * @version $Id$
 * @since 1.2RC2
 */
public class OmimSourceParser
{
    /** The location for the official OMIM source. */
    public static final String OMIM_SOURCE_URL = "ftp://ftp.omim.org/OMIM/omim.txt.Z";

    private static final String RECORD_MARKER = "*RECORD*";

    private static final String FIELD_MARKER = "*FIELD* ";

    private static final String FIELD_MIM_NUMBER = "NO";

    private static final String FIELD_TITLE = "TI";

    private static final String FIELD_TEXT = "TX";

    private static final String END_MARKER = "*THEEND*";

    private static final String TITLE_SEPARATOR = ";;";

    private static final String ANNOTATIONS_BASE_URL =
        "http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/";

    private static final String GENE_ANNOTATIONS_URL = "http://omim.org/static/omim/data/mim2gene.txt";

    private static final String POSITIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "phenotype_annotation.tab";

    private static final String NEGATIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "negative_phenotype_annotation.tab";

    private static final String GENEREVIEWS_MAPPING_URL =
        "ftp://ftp.ncbi.nih.gov/pub/GeneReviews/NBKid_shortname_OMIM.txt";

    private static final String ENCODING = "UTF-8";

    private static final String ID_FIELD = "id";

    private static final String NAME_FIELD = "name";

    private static final String SYNONYM_FIELD = "synonym";

    private static final String GENE_FIELD = "GENE";

    private SolrInputDocument crtTerm;

    private Map<String, SolrInputDocument> data = new HashMap<>();

    private Logger logger = LoggerFactory.getLogger(OmimSourceParser.class);

    private Vocabulary hpo;

    /**
     * Constructor which prepares the vocabulary data, parsing OMIM from the official site.
     *
     * @param hpo the HPO vocabulary, needed for computing the ancestors for the MIM-Phenotype mapping
     */
    public OmimSourceParser(Vocabulary hpo)
    {
        this(hpo, OMIM_SOURCE_URL);
    }

    /**
     * Constructor which prepares the vocabulary data, parsing OMIM from the specified source file.
     *
     * @param hpo the HPO vocabulary, needed for computing the ancestors for the MIM-Phenotype mapping
     * @param sourceURL the location from which to fetch the OMIM source, as a zipped file; may be a ftp, http, or local
     *            file URL
     */
    public OmimSourceParser(Vocabulary hpo, String sourceURL)
    {
        this.hpo = hpo;
        try (BufferedReader in =
            new BufferedReader(new InputStreamReader(new CompressorStreamFactory().createCompressorInputStream(
                new URL(sourceURL).openConnection().getInputStream()), ENCODING))) {
            transform(in);
            loadGenes();
            loadSymptoms(true);
            loadSymptoms(false);
            loadGeneReviews();
            loadVersion();
        } catch (NullPointerException | CompressorException | IOException ex) {
            this.logger.error("Failed to prepare the OMIM index: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Return the parsed vocabulary data.
     *
     * @return the vocabulary data, may be an empty collection if parsing the source failed
     */
    public Collection<SolrInputDocument> getData()
    {
        return this.data.values();
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
                fieldValue.append(line.trim()).append(' ');
            }
        }

        return this.data;
    }

    private void storeCrtTerm()
    {
        this.data.put(String.valueOf(this.crtTerm.get(ID_FIELD).getFirstValue()), this.crtTerm);
        this.crtTerm = new SolrInputDocument();
    }

    private void loadField(String name, String value)
    {
        if (StringUtils.isAnyBlank(name, value)) {
            return;
        }
        switch (name) {
            case FIELD_MIM_NUMBER:
                this.crtTerm.setField(ID_FIELD, value);
                break;
            case FIELD_TITLE:
                String title = StringUtils.substringBefore(value, TITLE_SEPARATOR).trim();
                String[] synonyms =
                    StringUtils.split(StringUtils.substringAfter(value, TITLE_SEPARATOR), TITLE_SEPARATOR);
                this.crtTerm.setField(NAME_FIELD, title);
                for (String synonym : synonyms) {
                    this.crtTerm.addField(SYNONYM_FIELD, synonym.trim());
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
        String omimId = "";
        String previousOmimId = null;
        Set<String> ancestors = new HashSet<>();
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(positive ? POSITIVE_ANNOTATIONS_URL : NEGATIVE_ANNOTATIONS_URL)
                .openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.parse(in)) {
                if ("OMIM".equals(row.get(0))) {
                    omimId = row.get(1);
                    addAncestors(previousOmimId, omimId, ancestors, positive);
                    previousOmimId = omimId;
                    SolrInputDocument term = this.data.get(omimId);
                    if (term != null) {
                        term.addField(positive ? "actual_symptom" : "actual_not_symptom", row.get(4));
                    }
                    VocabularyTerm vterm = this.hpo.getTerm(row.get(4));
                    if (vterm != null) {
                        for (VocabularyTerm ancestor : vterm.getAncestorsAndSelf()) {
                            ancestors.add(ancestor.getId());
                        }
                    }
                }
            }
            addAncestors(omimId, null, ancestors, positive);
        } catch (IOException ex) {
            this.logger.error("Failed to load OMIM-HPO links: {}", ex.getMessage(), ex);
        }
    }

    private void addAncestors(String previousOmimId, String newOmimId, Set<String> ancestors, boolean positive)
    {
        if (previousOmimId == null || previousOmimId.equals(newOmimId)) {
            return;
        }
        final String symptomField = "symptom";
        SolrInputDocument term = this.data.get(previousOmimId);
        if (!positive) {
            ancestors.removeAll(term.getFieldValues(symptomField));
            term.addField("not_symptom", new HashSet<String>(ancestors));
        } else {
            term.addField(symptomField, new HashSet<String>(ancestors));
        }
        ancestors.clear();
    }

    private void loadGenes()
    {
        final String missing = "-";
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENE_ANNOTATIONS_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                if (!row.get("Type").contains("gene")) {
                    continue;
                }
                SolrInputDocument term = this.data.get(row.get(2));
                if (term != null) {
                    String gs = row.get("Approved Gene Symbol");
                    if (!missing.equals(gs)) {
                        term.addField(GENE_FIELD, gs);
                    }
                    String eid = row.get("Ensembl Gene ID");
                    if (!missing.equals(eid)) {
                        term.addField(GENE_FIELD, eid);
                    }
                }
            }
        } catch (IOException ex) {
            this.logger.error("Failed to load OMIM-Gene links: {}", ex.getMessage(), ex);
        }
    }

    private void loadGeneReviews()
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENEREVIEWS_MAPPING_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                SolrInputDocument term = this.data.get(row.get(2));
                if (term != null) {
                    term.setField("gene_reviews_link", "https://www.ncbi.nlm.nih.gov/books/" + row.get(0));
                }
            }
        } catch (IOException ex) {
            this.logger.error("Failed to load OMIM-GeneReviews links: {}", ex.getMessage(), ex);
        }
    }

    private void loadVersion()
    {
        SolrInputDocument metaTerm = new SolrInputDocument();
        metaTerm.addField(ID_FIELD, "HEADER_INFO");
        metaTerm.addField("version", ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime()));
        this.data.put("VERSION", metaTerm);
    }
}
