package org.phenotips.oo;

import org.phenotips.obo2solr.ParameterPreparer;
import org.phenotips.obo2solr.SolrUpdateGenerator;
import org.phenotips.obo2solr.TermData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@SuppressWarnings("deprecation")
public class DisorderDataBuilder
{
    private static final int ID_IDX = 1;

    private static final int NAME_IDX = 2;

    private static final int SYMPTOM_ID_IDX = 4;

    private static final int SYMPTOM_PRELEVANCE_IDX = 8;

    private static final int EXPECTED_COUNT = 10;

    private Map<String, DisorderData> data = new HashMap<String, DisorderData>();

    private Map<String, TermData> oboData;

    private ContentHandler hd;

    private AttributesImpl atts;

    private final static String ROOT_ELEMENT_NAME = "add";

    private final static String DOC_ELEMENT_NAME = "doc";

    private final static String FIELD_ELEMENT_NAME = "field";

    private final static String FIELD_ATTRIBUTE_NAME = "name";

    private final static String FIELD_ATTRIBUTE_BOOST = "boost";

    private final static double DEFAULT_BOOST = 1.0;

    DisorderDataBuilder(Map<String, RecordData> omimData, Map<String, Double> prelevanceData, String symptomsSource,
        String negativePhenotypeSource)
    {
        for (String key : omimData.keySet()) {
            RecordData omimDisorder = omimData.get(key);
            DisorderData d = new DisorderData(omimDisorder.getId(), omimDisorder.getName());
            updateDisorder(key, omimDisorder, prelevanceData.get(key));
        }
        prepareOboData();
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(symptomsSource));
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("OMIM")) {
                    continue;
                }
                String pieces[] = line.split("\t");
                if (pieces.length < EXPECTED_COUNT) {
                    continue;
                }
                updateDisorder(pieces, true);
            }
            in.close();
            in = new BufferedReader(new FileReader(negativePhenotypeSource));
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("OMIM")) {
                    continue;
                }
                String pieces[] = line.split("\t");
                if (pieces.length < EXPECTED_COUNT) {
                    continue;
                }
                updateDisorder(pieces, false);
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareOboData()
    {
        ParameterPreparer paramPrep = new ParameterPreparer();
        SolrUpdateGenerator generator = new SolrUpdateGenerator();
        Map<String, Double> fieldSelection = paramPrep.getFieldSelection("id,is_a");
        this.oboData =
            generator.transform("http://compbio.charite.de/svn/hpo/trunk/src/ontology/human-phenotype-ontology.obo",
                fieldSelection);
    }

    private void updateDisorder(String data[], boolean isSymptom)
    {
        DisorderData d = this.data.get(data[ID_IDX]);
        if (d == null) {
            return;
        }
        if (isSymptom) {
            d.addSymptom(data[SYMPTOM_ID_IDX], data[SYMPTOM_PRELEVANCE_IDX]);
        } else {
            d.addNegativePhenotype(data[SYMPTOM_ID_IDX], data[SYMPTOM_PRELEVANCE_IDX]);
        }
    }

    private void updateDisorder(String dID, RecordData r, Double prelevance)
    {
        DisorderData d = this.data.get(dID);
        if (d == null) {
            d = new DisorderData(r.getId(), r.getName());
            this.data.put(d.getId(), d);
            d.setPrelevance(prelevance != null ? prelevance : 1);
        }
        d.setMeta(r);
    }

    public void generate(File output)
    {
        try {
            FileOutputStream fos = new FileOutputStream(output);
            OutputFormat of = new OutputFormat("XML", "UTF-8", true);
            of.setIndent(2);
            of.setIndenting(true);
            XMLSerializer serializer = new XMLSerializer(fos, of);
            this.hd = serializer.asContentHandler();
            this.hd.startDocument();
            this.atts = new AttributesImpl();
            startElement(ROOT_ELEMENT_NAME);

            for (String id : this.data.keySet()) {
                writeDisorder(id);
            }
            endElement(ROOT_ELEMENT_NAME);
            this.hd.endDocument();
            fos.flush();
            fos.close();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            System.err.println("File does not exist");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        }
    }

    private void writeDisorder(String id) throws SAXException
    {
        writeDisorder(this.data.get(id));
    }

    private void writeDisorder(DisorderData d) throws SAXException
    {
        addAttribute(FIELD_ATTRIBUTE_BOOST, d.getPrelevance());
        startElement(DOC_ELEMENT_NAME);
        writeField("id", d.getId());
        writeField("name", d.getName());
        Map<String, Double> parents = new HashMap<String, Double>();
        Map<String, Double> negativeParents = new HashMap<String, Double>();
        StringBuilder keywords = new StringBuilder();
        StringBuilder negativeKeywords = new StringBuilder();
        keywords.append(d.getName()).append(" ");
        for (Entry<String, Double> s : d.getSymptoms().entrySet()) {
            String hpoId = s.getKey();
            Double boost = s.getValue();
            writeField("symptom", hpoId, boost);
            writeField("actual_symptom", hpoId, boost);
            TermData term = this.oboData.get(hpoId);
            if (term != null) {
                updateKeywords(keywords, term);
                term.expandTermCategories(this.oboData);
                for (String parent : term.get(TermData.TERM_CATEGORY_FIELD_NAME)) {
                    if (!parents.containsKey(parent) || parents.get(parent) < boost) {
                        parents.put(parent, boost);
                        updateKeywords(keywords, parent);
                    }
                }
            }
        }
        for (Entry<String, Double> s : parents.entrySet()) {
            writeField("symptom", s.getKey(), s.getValue());
        }

        for (Entry<String, Double> s : d.getNegativePhenotypes().entrySet()) {
            String hpoId = s.getKey();
            Double boost = s.getValue();
            writeField("not_symptom", hpoId, boost);
            writeField("actual_not_symptom", hpoId, boost);
            TermData term = this.oboData.get(hpoId);
            if (term != null) {
                updateKeywords(negativeKeywords, term);
                term.expandTermCategories(this.oboData);
                for (String parent : term.get(TermData.TERM_CATEGORY_FIELD_NAME)) {
                    if (!parents.containsKey(parent)
                        && (!negativeParents.containsKey(parent) || negativeParents.get(parent) < boost)) {
                        negativeParents.put(parent, boost);
                        updateKeywords(negativeKeywords, parent);
                    }
                }
            }
        }

        for (Entry<String, Double> s : negativeParents.entrySet()) {
            writeField("not_symptom", s.getKey(), s.getValue());
        }
        writeField("keywords", keywords.toString());
        if (negativeKeywords.length() > 0) {
            writeField("not_keywords", negativeKeywords.toString());
        }
        for (String key : d.getMeta().keySet()) {
            for (String value : d.getMeta().get(key)) {
                writeField(key, value);
            }
        }
        endElement(DOC_ELEMENT_NAME);
    }

    private void writeField(String name, String value) throws SAXException
    {
        writeField(name, value, DEFAULT_BOOST);
    }

    private void writeField(String name, String value, Double boost) throws SAXException
    {
        addAttribute(FIELD_ATTRIBUTE_NAME, name);
        // addAttribute(FIELD_ATTRIBUTE_BOOST, boost);
        startElement(FIELD_ELEMENT_NAME);
        characters(value.replaceAll("\"([^\"]+)\".*", "$1"));
        endElement(FIELD_ELEMENT_NAME);
    }

    private void startElement(String qName) throws SAXException
    {
        this.hd.startElement("", "", qName, this.atts);
        this.atts.clear();
    }

    private void endElement(String qName) throws SAXException
    {
        this.hd.endElement("", "", qName);
    }

    private void addAttribute(String qName, Object value) throws SAXException
    {
        this.atts.addAttribute("", "", qName, "", (value + ""));
    }

    private void characters(Object value) throws SAXException
    {
        String text = "";
        if (value != null) {
            text = (value + "");
        }
        this.hd.characters(text.toCharArray(), 0, text.length());
    }

    private void updateKeywords(StringBuilder keywords, String hpoId)
    {
        TermData term = this.oboData.get(hpoId);
        if (term != null) {
            updateKeywords(keywords, term);
        }
    }

    private void updateKeywords(StringBuilder keywords, TermData term)
    {
        for (Collection<String> data : term.values()) {
            if (data != null) {
                for (String entry : data) {
                    keywords.append(
                        entry.replaceAll("^(HP\\:[0-9]{7})\\s*!\\s*(.*)", "$2").replaceAll("^(HP\\:[0-9]{7})$", ""))
                        .append(" ");
                }
            }
        }
    }
}
