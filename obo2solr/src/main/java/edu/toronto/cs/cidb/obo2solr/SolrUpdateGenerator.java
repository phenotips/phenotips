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
package edu.toronto.cs.cidb.obo2solr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SolrUpdateGenerator
{
    private final static String TERM_MARKER = "[Term]";

    private final static String ROOT_ELEMENT_NAME = "add";

    private final static String TERM_ELEMENT_NAME = "doc";

    private final static String FIELD_ELEMENT_NAME = "field";

    private final static String FIELD_ATTRIBUTE_NAME = "name";

    private final static String FIELD_ATTRIBUTE_BOOST = "boost";

    private final static String FIELD_NAME_VALUE_SEPARATOR = "\\s*:\\s+";

    private AttributesImpl atts;

    private ContentHandler hd;

    private int counter = 0;

    private TermData crtTerm = new TermData();

    private Map<String, TermData> data = new LinkedHashMap<String, TermData>();

    private Map<String, Double> fieldSelection;

    public void transform(File input, File output, Map<String, Double> fieldSelection)
    {
        this.fieldSelection = fieldSelection;
        try {
            BufferedReader in = new BufferedReader(new FileReader(input));
            FileOutputStream fos = new FileOutputStream(output);
            OutputFormat of = new OutputFormat("XML", "UTF-8", true);
            of.setIndent(2);
            of.setIndenting(true);
            XMLSerializer serializer = new XMLSerializer(fos, of);
            this.hd = serializer.asContentHandler();
            this.hd.startDocument();
            this.atts = new AttributesImpl();
            startElement(ROOT_ELEMENT_NAME);

            String line;
            this.counter = 0;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(TERM_MARKER)) {
                    if (this.counter > 0) {
                        storeCrtTerm();
                    }
                    ++this.counter;
                    continue;
                }
                String pieces[] = line.split(FIELD_NAME_VALUE_SEPARATOR, 2);
                if (pieces.length != 2) {
                    continue;
                }
                String name = pieces[0], value = pieces[1];
                loadField(name, value);
            }
            if (this.counter > 0) {
                storeCrtTerm();
            }
            if (isFieldSelected(TermData.TERM_CATEGORY_FIELD_NAME)) {
                propagateAncestors();

                for (String id : this.data.keySet()) {
                    writeTerm(id);
                }
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
            System.err.println("Could not locate source file: " + input.getAbsolutePath());
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } finally {
            this.fieldSelection = null;
        }
    }

    public Map<String, TermData> transform(String ontologyUrl, Map<String, Double> fieldSelection)
    {
        URL url;
        try {
            url = new URL(ontologyUrl);
        } catch (MalformedURLException ex) {
            return null;
        }
        return transform(url, fieldSelection);
    }

    public Map<String, TermData> transform(URL input, Map<String, Double> fieldSelection)
    {
        this.fieldSelection = fieldSelection;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(input.openConnection().getInputStream()));
            this.atts = new AttributesImpl();

            String line;
            this.counter = 0;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(TERM_MARKER)) {
                    if (this.counter > 0) {
                        storeCrtTerm();
                    }
                    ++this.counter;
                    continue;
                }
                String pieces[] = line.split(FIELD_NAME_VALUE_SEPARATOR, 2);
                if (pieces.length != 2) {
                    continue;
                }
                String name = pieces[0], value = pieces[1];
                loadField(name, value);
            }
            if (this.counter > 0) {
                storeCrtTerm();
            }
            if (isFieldSelected(TermData.TERM_CATEGORY_FIELD_NAME)) {
                propagateAncestors();
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            this.fieldSelection = null;
        }
        return this.data;
    }

    private void storeCrtTerm()
    {
        if (this.crtTerm.getId() != null) {
            this.data.put(this.crtTerm.getId(), this.crtTerm);
        }
        this.crtTerm = new TermData();
    }

    private void writeTerm(String id) throws SAXException
    {
        writeTerm(this.data.get(id));
    }

    private void writeTerm(TermData term) throws SAXException
    {
        startElement(TERM_ELEMENT_NAME);
        for (String fieldname : term.keySet()) {
            for (String value : term.get(fieldname)) {
                writeField(fieldname, value);
            }
        }
        endElement(TERM_ELEMENT_NAME);
    }

    private void writeField(String name, String value) throws SAXException
    {
        addAttribute(FIELD_ATTRIBUTE_NAME, name);
        addAttribute(FIELD_ATTRIBUTE_BOOST, this.fieldSelection.get(name) == null ? ParameterPreparer.DEFAULT_BOOST
            : this.fieldSelection.get(name));
        startElement(FIELD_ELEMENT_NAME);
        characters(value.replaceAll("\"([^\"]+)\".*", "$1"));
        endElement(FIELD_ELEMENT_NAME);
    }

    private boolean isFieldSelected(String name)
    {
        return this.fieldSelection.isEmpty() || this.fieldSelection.containsKey(name);
    }

    private void loadField(String name, String value)
    {
        if (!(isFieldSelected(name))) {
            return;
        }
        // System.out.println("Adding " + name + " " + value.replaceAll("\"([^\"]+)\".*", "$1"));
        this.crtTerm.addTo(name, value.replaceAll("\"([^\"]+)\".*", "$1"));
    }

    private void propagateAncestors()
    {
        for (String id : this.data.keySet()) {
            TermData term = this.data.get(id);
            term.expandTermCategories(this.data);
        }
    }

    private void startElement(String qName) throws SAXException
    {
        this.hd.startElement("", "", qName, this.atts);
    }

    private void endElement(String qName) throws SAXException
    {
        this.hd.endElement("", "", qName);
        this.atts.clear();
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
}
