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
