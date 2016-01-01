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
package org.phenotips.ncbieutils.internal;

import org.phenotips.ncbieutils.NCBIEUtilsService;

import java.io.BufferedInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base implementation for {@link NCBIEUtilsService}.
 *
 * @version $Id$
 */
public abstract class AbstractSpecializedNCBIEUtilsAccessService implements NCBIEUtilsService
{
    @Inject
    private Logger logger;

    protected static final String SERVER_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

    protected static final String TERM_SEARCH_QUERY_SCRIPT = "esearch.fcgi";

    protected static final String TERM_SEARCH_PARAM_NAME = "term";

    protected static final String TERM_SUMMARY_QUERY_SCRIPT = "esummary.fcgi";

    protected static final String TERM_SUMMARY_PARAM_NAME = "id";

    protected static final String SPELL_CHECK_QUERY_SCRIPT = "espell.fcgi";

    protected static final String SPELL_CHECK_PARAM_NAME = "term";

    protected static final String DB_PARAM_NAME = "db";

    protected abstract String getDatabaseName();

    @Override
    public List<Map<String, Object>> getSuggestions(final String query)
    {
        return getSuggestions(query, 10, 0);
    }

    @Override
    public List<Map<String, Object>> getSuggestions(final String query, final int rows, final int start)
    {
        // Step 1: get spelling suggestions for query
        String correctedQuery = getCorrectedQuery(query);
        // Step 2: get ID list matching the corrected query
        List<String> idList = getMatches(correctedQuery, rows, start);
        // step 3: get Summaries for ID list
        List<Map<String, Object>> result = getSummaries(idList);
        return result;
    }

    @Override
    public String getSuggestionsXML(final String query)
    {
        return getSuggestionsXML(query, 10, 0);
    }

    @Override
    public String getSuggestionsXML(final String query, final int rows, final int start)
    {
        // Step 1: get spelling suggestions for query
        String correctedQuery = getCorrectedQuery(query);
        // Step 2: get ID list matching the corrected query
        List<String> idList = getMatches(correctedQuery, rows, start);
        // step 3: get Summaries for ID list
        String result = getSummariesXML(idList);
        return result;
    }

    @Override
    public String getName(String id)
    {
        String url = composeURL(TERM_SUMMARY_QUERY_SCRIPT, TERM_SUMMARY_PARAM_NAME, id);
        String result = id;
        try {
            Document response = readXML(url);
            NodeList nodes = response.getElementsByTagName("DocSum");
            if (nodes.getLength() > 0) {
                Map<String, String> idToName = this.getNameForId(nodes.item(0));
                result = idToName.get(id);
                if (result != null && !id.equals(result)) {
                    return result;
                }
            }
            this.logger.warn("Name not found for OMIM id " + id);
            return id;
        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve name for " + getDatabaseName() + " id " + id + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return id;
    }

    @Override
    public Map<String, String> getNames(List<String> idList)
    {
        Map<String, String> result = new HashMap<String, String>();
        String queryList = getSerializedList(idList);
        String url = composeURL(TERM_SUMMARY_QUERY_SCRIPT, TERM_SUMMARY_PARAM_NAME, queryList);
        try {
            Document response = readXML(url);
            NodeList nodes = response.getElementsByTagName("DocSum");
            for (int i = 0; i < nodes.getLength(); ++i) {
                Map<String, String> idToName = this.getNameForId(nodes.item(i));
                for (String id : idToName.keySet()) {
                    if (idList.contains(id)) {
                        result.put(id, idToName.get(id));
                    } else {
                        this.logger.warn("Unrequested " + getDatabaseName() + " id " + id);
                    }
                }
            }
        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve name for " + getDatabaseName() + " ids " + queryList
                + " " + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return result;
    }

    @Override
    public String getCorrectedQuery(String query)
    {
        // response example at http://eutils.ncbi.nlm.nih.gov/entrez/eutils/espell.fcgi?db=omim&term=atention+sindrom
        // response type: XML
        // get corrected query from /eSpellResult/CorrectedQuery (single element)
        // use original query if this element is empty
        String url = composeURL(SPELL_CHECK_QUERY_SCRIPT, SPELL_CHECK_PARAM_NAME, query);
        try {
            Document response = readXML(url);
            NodeList nodes = response.getElementsByTagName("CorrectedQuery");
            if (nodes.getLength() > 0) {
                String result = nodes.item(0).getTextContent();
                result = StringUtils.trim(result);
                return StringUtils.isNotEmpty(result) ? result : query;
            }
        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve corrected query for " + query + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return query;
    }

    @Override
    public List<String> getMatches(final String query, final int rows, final int start)
    {
        // response example at http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=omim&term=down
        // response type: XML
        // get corrected query from /eSearchResult/IdList/Id (multiple elements)
        String url = composeURL(TERM_SEARCH_QUERY_SCRIPT, TERM_SEARCH_PARAM_NAME, query, rows, start);
        List<String> result = new ArrayList<String>();
        try {
            Document response = readXML(url);
            NodeList nodes = response.getElementsByTagName("IdList");
            if (nodes.getLength() > 0) {
                nodes = nodes.item(0).getChildNodes();
                for (int i = 0; i < nodes.getLength(); ++i) {
                    Node n = nodes.item(i);
                    if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("Id")) {
                        result.add(n.getTextContent());
                    }
                }
            }

        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve matches for " + query + " " + ex.getClass().getName()
                + " " + ex.getMessage(), ex);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getSummaries(List<String> idList)
    {
        // response example at
        // http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=omim&id=190685,605298,604829,602917,601088,602523,602259
        // response type: XML
        // return it
        String queryList = getSerializedList(idList);
        String url = composeURL(TERM_SUMMARY_QUERY_SCRIPT, TERM_SUMMARY_PARAM_NAME, queryList);
        try {
            Document response = readXML(url);
            NodeList nodes = response.getElementsByTagName("Item");
            // OMIM titles are all UPPERCASE, try to fix this
            for (int i = 0; i < nodes.getLength(); ++i) {
                Node n = nodes.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    if (n.getFirstChild() != null) {
                        n.replaceChild(response.createTextNode(fixCase(n.getTextContent())), n.getFirstChild());
                    }
                }
            }
            List<Map<String, Object>> result = new LinkedList<Map<String, Object>>();
            nodes = response.getElementsByTagName("DocSum");
            for (int i = 0; i < nodes.getLength(); ++i) {
                Element n = (Element) nodes.item(i);
                Map<String, Object> doc = new HashMap<String, Object>();
                doc.put("id", n.getElementsByTagName("Id").item(0).getTextContent());
                NodeList items = n.getElementsByTagName("Item");
                for (int j = 0; j < items.getLength(); ++j) {
                    Element item = (Element) items.item(j);
                    if ("List".equals(item.getAttribute("Type"))) {
                        NodeList subitems = item.getElementsByTagName("Item");
                        if (subitems.getLength() > 0) {
                            List<String> values = new ArrayList<String>(subitems.getLength());
                            for (int k = 0; k < subitems.getLength(); ++k) {
                                values.add(subitems.item(k).getTextContent());
                            }
                            doc.put(item.getAttribute("Name"), values);
                        }
                    } else {
                        String value = item.getTextContent();
                        if (StringUtils.isNotEmpty(value)) {
                            doc.put(item.getAttribute("Name"), value);
                        }
                    }
                }
                result.add(doc);
            }
            return result;
        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve summaries for ids " + idList + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return Collections.emptyList();
    }

    protected String getSummariesXML(List<String> idList)
    {
        // response example at
        // http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=omim&id=190685,605298,604829,602917,601088,602523,602259
        // response type: XML
        // return it
        String queryList = getSerializedList(idList);
        String url = composeURL(TERM_SUMMARY_QUERY_SCRIPT, TERM_SUMMARY_PARAM_NAME, queryList);
        try {
            Document response = readXML(url);
            NodeList nodes = response.getElementsByTagName("Item");
            // OMIM titles are all UPPERCASE, try to fix this
            for (int i = 0; i < nodes.getLength(); ++i) {
                Node n = nodes.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    if (n.getFirstChild() != null) {
                        n.replaceChild(response.createTextNode(fixCase(n.getTextContent())), n.getFirstChild());
                    }
                }
            }
            Source source = new DOMSource(response);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve summaries for ids " + idList + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return "";
    }

    private Map<String, String> getNameForId(Node source)
    {
        Map<String, String> result = new HashMap<String, String>();
        String id = null, name = null;
        NodeList data = source.getChildNodes();
        for (int i = 0; i < data.getLength(); ++i) {
            Node n = data.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("Id".equals(n.getNodeName())) {
                id = n.getTextContent();
            } else if ("Item".equals(n.getNodeName())) {
                NamedNodeMap attrs = n.getAttributes();
                if (attrs.getNamedItem("Name") != null && "Title".equals(attrs.getNamedItem("Name").getNodeValue())) {
                    name = n.getTextContent();
                }
            }
        }
        if (id != null) {
            if (name != null) {
                result.put(id, fixCase(name));
            } else {
                result.put(id, id);
            }
        }
        return result;
    }

    private String composeURL(String scriptName, String paramName, String query)
    {
        try {
            return SERVER_URL + scriptName + "?" + DB_PARAM_NAME + '=' + getDatabaseName() + "&" + paramName + "="
                + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return SERVER_URL + scriptName + "?" + DB_PARAM_NAME + '=' + getDatabaseName() + "&" + paramName + "="
                + query;
        }
    }

    private String composeURL(String scriptName, String paramName, String query, final int rows, final int start)
    {
        try {
            return SERVER_URL + scriptName + "?" + DB_PARAM_NAME + '=' + getDatabaseName() + "&" + paramName + "="
                + URLEncoder.encode(query, "UTF-8") + "&RetMax=" + rows + "&RetStart=" + start;
        } catch (UnsupportedEncodingException ex) {
            return SERVER_URL + scriptName + "?" + DB_PARAM_NAME + '=' + getDatabaseName() + "&" + paramName + "="
                + query + "&RetMax=" + rows + "&RetStart=" + start;
        }
    }

    private org.w3c.dom.Document readXML(String url)
    {
        try {
            BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document result = dBuilder.parse(in);
            result.getDocumentElement().normalize();
            return result;
        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve data from URL " + url + " " + ex.getClass().getName()
                + " " + ex.getMessage(), ex);
            return null;
        }
    }

    private static String fixCase(String text)
    {
        if (text == null || text.length() == 0) {
            return "";
        }
        if (StringUtils.isAllUpperCase(text.replaceAll("[^a-zA-Z]", ""))) {
            return StringUtils.capitalize(text.toLowerCase());
        }
        return text;
    }

    private static String getSerializedList(List<String> list)
    {
        String result = "";
        if (!list.isEmpty()) {
            StringBuilder listBuilder = new StringBuilder();
            for (String item : list) {
                listBuilder.append(',').append(item);
            }
            result = listBuilder.substring(1);
        }
        return result;
    }
}
