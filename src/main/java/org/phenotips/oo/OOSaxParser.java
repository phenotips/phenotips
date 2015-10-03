package org.phenotips.oo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OOSaxParser extends DefaultHandler
{
    Map<String, String> data;

    String tmpKey;

    String tmpValue;

    String tmpText;

    private String sourceFileName;

    private boolean readValue = false;

    public OOSaxParser(String sourceFileName)
    {
        this.sourceFileName = sourceFileName;
        this.data = new HashMap<String, String>();
        parseDocument();
    }

    private void parseDocument()
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(this.sourceFileName, this);
        } catch (ParserConfigurationException e) {
            System.out.println("ParserConfig error");
        } catch (SAXException e) {
            System.out.println("SAXException : xml not well formed");
        } catch (IOException e) {
            System.out.println("IO error");
        }
    }

    public String get(String key)
    {
        return this.data.get(key);
    }

    public Set<String> keySet()
    {
        return this.data.keySet();
    }

    public Set<Entry<String, String>> entrySet()
    {
        return this.data.entrySet();
    }

    @Override
    public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException
    {
        if (elementName.equalsIgnoreCase("ClassOfPrevalence") || elementName.equalsIgnoreCase("Reference")
            && this.tmpText.equalsIgnoreCase("OMIM")) {
            this.readValue = true;
        }
    }

    @Override
    public void endElement(String s, String s1, String elementName) throws SAXException
    {
        if (elementName.equalsIgnoreCase("OrphaNumber")) {
            this.tmpKey = this.tmpText;
        } else if (elementName.equalsIgnoreCase("ClassOfPrevalence")
            || elementName.equalsIgnoreCase("ExternalReference")) {
            this.readValue = false;
        } else if ((elementName.equalsIgnoreCase("Name") || elementName.equalsIgnoreCase("Reference"))
            && this.readValue) {
            if (this.tmpKey != null && this.tmpText != null) {
                this.tmpValue = this.tmpText;
                this.data.put(this.tmpKey, this.tmpValue);
            }
            this.tmpKey = null;
            this.tmpValue = null;
        }
    }

    @Override
    public void characters(char[] ac, int i, int j) throws SAXException
    {
        String tmpText = new String(ac, i, j).replaceAll("\n", "").trim();
        if (tmpText.length() > 0) {
            this.tmpText = tmpText;
        }
    }
}
