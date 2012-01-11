package edu.toronto.cs.cidb.solr;

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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;

import edu.toronto.cs.cidb.obo2solr.ParameterPreparer;
import edu.toronto.cs.cidb.obo2solr.SolrUpdateGenerator;
import edu.toronto.cs.cidb.obo2solr.TermData;

@Component
@Named("solr")
@Singleton
public class SolrScriptService implements ScriptService, Initializable
{
    @Inject
    private Logger logger;

    private SolrServer server;

    protected static final String FIELD_VALUE_SEPARATOR = ":";

    protected Map<String, String> getSolrQuery(Map<String, String> fieldValues)
    {
        return getSolrQuery(fieldValues, -1, 0);
    }

    protected Map<String, String> getSolrQuery(Map<String, String> fieldValues, int rows)
    {
        return getSolrQuery(fieldValues, rows, 0);
    }

    protected Map<String, String> getSolrQuery(Map<String, String> fieldValues, int rows, int start)
    {

        StringBuilder query = new StringBuilder();
        for (String field : fieldValues.keySet()) {
            String value = fieldValues.get(field);
            if (value == null) {
                value = "";
            }
            String pieces[] = value.replaceAll("[^a-zA-Z0-9 :]/", " ")
                .replace(FIELD_VALUE_SEPARATOR, "\\" + FIELD_VALUE_SEPARATOR).trim().split("\\s+");
            for (String val : pieces) {
                query.append(field).append(FIELD_VALUE_SEPARATOR).append(val).append(" ");
            }
        }
        return getSolrQuery(query.toString().trim(), rows, start);
    }

    protected Map<String, String> getSolrQuery(String query)
    {
        return getSolrQuery(query, -1, 0);
    }

    protected Map<String, String> getSolrQuery(String query, int rows)
    {
        return getSolrQuery(query, rows, 0);
    }

    protected Map<String, String> getSolrQuery(String query, int rows, int start)
    {
        Map<String, String> result = new HashMap<String, String>();
        result.put(CommonParams.START, start + "");
        if (rows > 0) {
            result.put(CommonParams.ROWS, rows + "");
        }
        result.put(CommonParams.Q, query);
        return result;
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.server = new CommonsHttpSolrServer("http://localhost:8080/solr/");
        } catch (MalformedURLException ex) {
            // TODO Auto-generated catch block
        }
    }

    public SolrDocumentList search(final Map<String, String> queryParameters)
    {
        return search(queryParameters, -1, 0);
    }

    public SolrDocumentList search(final Map<String, String> queryParameters, final int rows)
    {
        return search(queryParameters, rows, 0);
    }

    public SolrDocumentList search(final Map<String, String> queryParameters, final int rows, final int start)
    {
        SolrParams params = new MapSolrParams(getSolrQuery(queryParameters, rows, start));
        try {
            return this.server.query(params).getResults();
        } catch (SolrServerException ex) {
            // TODO Auto-generated catch block
        }
        return null;
    }

    public SolrDocument get(final Map<String, String> queryParameters)
    {
        SolrDocumentList all = search(queryParameters, 1, 0);
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    public SolrDocument get(final String id)
    {
        SolrDocumentList all = search(new HashMap<String, String>()
                    {
            {
                put("id", id);
            }
        }, 1, 0);
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    public int clear()
    {
        try {
            UpdateResponse r = this.server.deleteByQuery("*:*");
            this.server.commit();
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("Exception while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("Exception while clearing the Solr index", ex);
        }
        return 1;
    }

    public int index(String ontologyUrl, String fieldList) throws MalformedURLException
    {
        ParameterPreparer paramPrep = new ParameterPreparer();
        SolrUpdateGenerator generator = new SolrUpdateGenerator();
        Map<String, Double> fieldSelection = paramPrep.getFieldSelection(fieldList);
        Map<String, TermData> data = generator.transform(new URL(ontologyUrl), fieldSelection);
        Collection<SolrInputDocument> allTerms = new HashSet<SolrInputDocument>();
        for (Map.Entry<String, TermData> item : data.entrySet()) {
            SolrInputDocument doc = new SolrInputDocument();
            for (Map.Entry<String, Collection<String>> property : item.getValue().entrySet()) {
                String name = property.getKey();
                for (String value : property.getValue()) {
                    doc.addField(name, value, (fieldSelection.get(name) == null ? ParameterPreparer.DEFAULT_BOOST
                        : fieldSelection.get(name)).floatValue());
                }
            }
            allTerms.add(doc);
        }
        try {
            this.server.add(allTerms);
            this.server.commit();
            return 0;
        } catch (SolrServerException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        return 1;
    }
}
