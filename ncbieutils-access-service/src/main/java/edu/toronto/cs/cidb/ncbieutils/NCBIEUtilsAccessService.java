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
package edu.toronto.cs.cidb.ncbieutils;

import java.io.BufferedInputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

@Component
@Named("ncbieutils")
@Singleton
public class NCBIEUtilsAccessService implements ScriptService
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

    protected String getDatabaseName()
    {
        return "";
    }

    protected String getSuggestions(String query)
    {
        // Step 1: get spelling suggestions for query
        String correctedQuery = getCorrectedQuery(query);
        // Step 2: get ID list matching the corrected query
        List<String> idList = getMatches(correctedQuery);
        // step 3: get Summaries for ID list
        String result = getSummaries(idList);
        return result;
    }

    protected String getName(String id)
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
            this.logger.error("Error while trying to retrieve name for OMIM id " + id + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return id;
    }

    protected Map<String, String> getNames(List<String> idList)
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
                        this.logger.warn("Unrequested OMIM id " + id);
                    }
                }
            }
        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve name for OMIM ids " + queryList + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return result;
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
                if (attrs.getNamedItem("Name") != null
                    && "Title".equals(attrs.getNamedItem("Name").getNodeValue())) {
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

    protected String getCorrectedQuery(String query)
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
                if (result == null) {
                    result = "";
                } else {
                    result.trim();
                }
                return (!"".equals(result)) ? result : query;
            }

        } catch (Exception ex) {
            this.logger.error("Error while trying to retrieve corrected query for " + query + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return query;
    }

    protected List<String> getMatches(String query)
    {
        // response example at http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=omim&term=down
        // response type: XML
        // get corrected query from /eSearchResult/IdList/Id (multiple elements)
        String url = composeURL(TERM_SEARCH_QUERY_SCRIPT, TERM_SEARCH_PARAM_NAME, query);
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
            this.logger.error("Error while trying to retrieve matches for " + query + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
        }
        return result;
    }

    protected String getSummaries(List<String> idList)
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

    private String composeURL(String scriptName, String paramName, String query)
    {
        return SERVER_URL + scriptName + "?" + DB_PARAM_NAME + '=' + getDatabaseName() + "&" + paramName + "=" + query;
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
            this.logger.error("Error while trying to retrieve data from URL " + url + " "
                + ex.getClass().getName() + " " + ex.getMessage(), ex);
            return null;
        }
    }

    private static String fixCase(String text)
    {
        if (text == null || text.length() == 0) {
            return "";
        }
        return text.toUpperCase().substring(0, 1) + text.toLowerCase().substring(1);
    }

    private static String getSerializedList(List<String> list)
    {
        String result = "";
        if (list.size() > 0) {
            StringBuilder listBuilder = new StringBuilder();
            for (String item : list) {
                listBuilder.append(",").append(item);
            }
            result = listBuilder.substring(1);
        }
        return result;
    }

    public SpecializedNCBIEUtilsAccessService get(final String name)
    {
        return new SpecializedNCBIEUtilsAccessService()
        {
            @Override
            public String getDatabaseName()
            {
                return name;
            }
        };
    }
}
