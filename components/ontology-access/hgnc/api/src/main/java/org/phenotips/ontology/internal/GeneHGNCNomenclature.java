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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.ontology.internal;

import org.phenotips.ontology.OntologyTerm;
import org.phenotips.ontology.internal.solr.AbstractHGNCSolrOntologyService;
import org.phenotips.ontology.internal.solr.SolrOntologyTerm;

import org.xwiki.component.annotation.Component;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.SpellingParams;

/**
 * Provides access to the HUGO Gene Nomenclature Committee's GeneNames ontology. The ontology prefix is {@code HGNC}.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Named("hgnc")
@Singleton
public class GeneHGNCNomenclature extends AbstractHGNCSolrOntologyService
{
    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^HGNC:[0-9]+$", Pattern.CASE_INSENSITIVE);

    @Override
    public String getDefaultOntologyLocation()
    {
        return "http://www.genenames.org/cgi-bin/download?col=gd_hgnc_id&col=gd_app_sym&col=gd_app_name"
            + "&col=gd_prev_sym&col=gd_aliases&col=gd_pub_acc_ids&col=gd_pub_eg_id&col=gd_pub_ensembl_id&col="
            + "gd_pub_refseq_ids&col=family.id&col=family.name&col=md_eg_id&col=md_mim_id&col=md_refseq_id&col="
            + "md_prot_id&col=md_ensembl_id&status=Approved&status_opt=2&where=&order_by=gd_app_sym_sort&format="
            + "text&hgnc_dbtag=on&submit=submit";
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        return 15000;
    }

    @Override
    protected String getName()
    {
        return "hgnc";
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        result.add("HGNC");
        return result;
    }

    private Map<String, String> getStaticSolrParams()
    {
        String trueStr = "true";
        Map<String, String> params = new HashMap<>();
        params.put("spellcheck", trueStr);
        params.put(SpellingParams.SPELLCHECK_COLLATE, trueStr);
        params.put(SpellingParams.SPELLCHECK_COUNT, "100");
        params.put(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        params.put("lowercaseOperators", "false");
        params.put("defType", "edismax");
        return params;
    }

    private Map<String, String> getStaticFieldSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put("pf", "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        params.put("qf",
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        return params;
    }

    private SolrParams produceDynamicSolrParams(String originalQuery, Integer rows, String sort, String customFq,
        boolean isId)
    {
        String query = originalQuery.trim();
        ModifiableSolrParams params = new ModifiableSolrParams();
        String escapedQuery = ClientUtils.escapeQueryChars(query);
        if (isId) {
            params.add(CommonParams.FQ, StringUtils.defaultIfBlank(customFq,
                new MessageFormat("id:{0} alt_id:{0}").format(new String[] { escapedQuery })));
        } else {
            params.add(CommonParams.FQ, StringUtils.defaultIfBlank(customFq, "term_category:HGNC\\:0000118"));
        }
        params.add(CommonParams.Q, escapedQuery);
        params.add(SpellingParams.SPELLCHECK_Q, query);
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

    @Override
    public long getDistance(String fromTermId, String toTermId)
    {
        // Flat nomenclature
        return -1;
    }

    @Override
    public long getDistance(OntologyTerm fromTerm, OntologyTerm toTerm)
    {
        // Flat nomenclature
        return -1;
    }

}
