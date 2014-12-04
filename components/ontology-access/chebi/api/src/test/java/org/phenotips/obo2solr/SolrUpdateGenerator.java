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
package org.phenotips.obo2solr;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class SolrUpdateGenerator
{
    private static final String TERM_MARKER = "[Term]";

    /** Not all entities are terms prompted by the presence of a {@link #TERM_MARKER} */
    private static final String ENTITY_SEPARATION_REGEX = "^\\[[a-zA-Z]+\\]$";

    private static final String ROOT_ELEMENT_NAME = "add";

    private static final String TERM_ELEMENT_NAME = "doc";

    private static final String FIELD_ELEMENT_NAME = "field";

    private static final String FIELD_ATTRIBUTE_NAME = "name";

    private static final String FIELD_ATTRIBUTE_BOOST = "boost";

    private static final String FIELD_NAME_VALUE_SEPARATOR = "\\s*:\\s+";

    private AttributesImpl atts;

    private ContentHandler hd;

    private int counter;

    private TermData crtTerm = new TermData();

    private Map<String, TermData> data = new LinkedHashMap<String, TermData>();

    private Map<String, Double> fieldSelection;

    public void transform(File input, File output, Map<String, Double> fieldSelection)
    {
        throw new NotImplementedException();
    }

    public Map<String, TermData> transform(String ontologyUrl, Map<String, Double> fieldSelection)
    {
        return new HashMap<String, TermData>();
    }

    public Map<String, TermData> transform(URL input, Map<String, Double> fieldSelection)
    {
        throw new NotImplementedException();
    }

    private void storeCrtTerm()
    {
        throw new NotImplementedException();
    }

    private void writeTerm(String id) throws SAXException
    {
        writeTerm(this.data.get(id));
    }

    private void writeTerm(TermData term) throws SAXException
    {
        throw new NotImplementedException();
    }

    private void writeField(String name, String value) throws SAXException
    {
        throw new NotImplementedException();
    }

    private boolean isFieldSelected(String name)
    {
        throw new NotImplementedException();
    }

    private void loadField(String name, String value)
    {
        throw new NotImplementedException();
    }

    private void propagateAncestors()
    {
        throw new NotImplementedException();
    }

    private void startElement(String qName) throws SAXException
    {
        throw new NotImplementedException();
    }

    private void endElement(String qName) throws SAXException
    {
        throw new NotImplementedException();
    }

    private void addAttribute(String qName, Object value) throws SAXException
    {
        throw new NotImplementedException();
    }

    private void characters(Object value) throws SAXException
    {
        throw new NotImplementedException();
    }
}
