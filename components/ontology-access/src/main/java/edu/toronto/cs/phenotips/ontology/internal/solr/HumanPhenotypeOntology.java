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
package edu.toronto.cs.phenotips.ontology.internal.solr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import edu.toronto.cs.phenotips.ontology.OntologyTerm;

/**
 * Provides access to the Human Phenotype Ontology (HPO). The ontology prefix is {@code HP}.
 * 
 * @version $Id$
 */
@Component
@Named("HP")
@Singleton
public class HumanPhenotypeOntology extends AbstractSolrOntologyService
{
    /**
     * The name of the Alternative ID field, used for older aliases of updated HPO terms.
     */
    protected static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

    @Override
    protected String getName()
    {
        return "hpo";
    }

    @Override
    public OntologyTerm getTerm(String id)
    {
        OntologyTerm result = super.getTerm(id);
        if (result == null) {
            Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put(ALTERNATIVE_ID_FIELD_NAME, id);
            Set<OntologyTerm> results = search(queryParameters);
            if (results != null && !results.isEmpty()) {
                result = search(queryParameters).iterator().next();
            }
        }
        return result;
    }
}
