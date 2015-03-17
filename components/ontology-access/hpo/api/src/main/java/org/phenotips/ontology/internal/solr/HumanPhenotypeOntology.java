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
package org.phenotips.ontology.internal.solr;

import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

/**
 * Provides access to the Human Phenotype Ontology (HPO). The ontology prefix is {@code HP}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("hpo")
@Singleton
public class HumanPhenotypeOntology extends AbstractOBOSolrOntologyService
{
    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^HP:[0-9]+$", Pattern.CASE_INSENSITIVE);

    private static final Pattern LAST_WORD = Pattern.compile(".*\\W(\\w+)$");

    @Override
    protected String getName()
    {
        return "hpo";
    }

    @Override
    public String getDefaultOntologyLocation()
    {
        return "http://purl.obolibrary.org/obo/hp.obo";
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        /* This number should be sufficient to index the whole ontology in one go */
        return 15000;
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        result.add("HP");
        result.add("HPO");
        return result;
    }

    private Map<String, String> getStaticSolrParams()
    {
        String trueStr = "true";
        Map<String, String> params = new HashMap<>();
        params.put("spellcheck", trueStr);
        params.put("spellcheck.collate", trueStr);
        params.put("lowercaseOperators", "false");
        params.put("defType", "edismax");
        return params;
    }

    private Map<String, String> getStaticFieldSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put("pf",
            "name^20 nameSpell^36 nameExact^100 namePrefix^30 synonym^15 synonymSpell^25 synonymExact^70 "
                + "synonymPrefix^20 text^3 textSpell^5");
        params.put("qf", "name^10 nameSpell^18 synonym^6 synonymSpell^10 text^1 textSpell^2");
        return params;
    }

    private SolrParams produceDynamicSolrParams(String originalQuery, Integer rows, String sort, String customFq,
        boolean isId)
    {
        String fqStr = "fq";
        String query = originalQuery.trim();
        ModifiableSolrParams params = new ModifiableSolrParams();
        String escapedQuery = ClientUtils.escapeQueryChars(query);
//        String lastWord = escapedQuery.trim().();
        Matcher lastWordRegex = LAST_WORD.matcher(escapedQuery);
        String lastWord = "";
        if (lastWordRegex.matches()) {
            lastWord = lastWordRegex.group(1);
        }
        String q;
        if (StringUtils.isBlank(lastWord)) {
            lastWord = escapedQuery;
        }
        if (isId) {
            if (StringUtils.isNotBlank(customFq)) {
                params.add(fqStr, customFq);
            } else {
                String fq = new MessageFormat("id: {0} alt_id:{0}").format(new String[]{ escapedQuery });
                params.add(fqStr, fq);
            }
            q = new MessageFormat("{0} textSpell:{1}").format(new String[]{ escapedQuery, lastWord });
        } else {
            String bq = new MessageFormat("nameSpell:{0}*^14 synonymSpell:{0}*^7 text:{0}*^1 textSpell:{0}*^2").format(
                new String[]{ lastWord });
            q = new MessageFormat("{0}* textSpell:{1}*").format(new String[]{ escapedQuery, lastWord });
            params.add(fqStr, "term_category:HP\\:0000118");
            params.add("bq", bq);
        }
        params.add(CommonParams.Q, q);
        params.add(CommonParams.ROWS, rows.toString());
        if (StringUtils.isNotBlank(sort)) {
            params.add(CommonParams.SORT, sort);
        }
        return params;
    }

    @Override
    public Set<OntologyTerm> termSuggest(String query, Integer rows, String sort, String customFq)
    {
        if (StringUtils.isBlank(query)) {
            return new HashSet<>();
        }
        boolean isId = this.isId(query);
        Map<String, String> options = this.getStaticSolrParams();
        if (!isId) {
            options.putAll(this.getStaticFieldSolrParams());
        }
        Set<OntologyTerm> result = new LinkedHashSet<OntologyTerm>();
        for (SolrDocument doc : this.search(produceDynamicSolrParams(query, rows, sort, customFq, isId), options)) {
            result.add(new SolrOntologyTerm(doc, this));
        }
        return result;
    }

    private boolean isId(String query)
    {
        return ID_PATTERN.matcher(query).matches();
    }
}
