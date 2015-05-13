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
package org.phenotips.vocabulary.internal;

import org.phenotips.vocabulary.OntologyManager;
import org.phenotips.vocabulary.OntologyService;
import org.phenotips.vocabulary.OntologyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link OntologyManager} component, which uses the {@link OntologyService ontologies}
 * registered in the component manager.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Singleton
public class DefaultOntologyManager implements OntologyManager, Initializable
{
    /** The currently available ontologies. */
    @Inject
    private Map<String, OntologyService> ontologies;

    @Override
    public void initialize() throws InitializationException
    {
        Map<String, OntologyService> newOntologiesMap = new HashMap<String, OntologyService>();
        for (OntologyService ontology : this.ontologies.values()) {
            for (String alias : ontology.getAliases()) {
                newOntologiesMap.put(alias, ontology);
            }
        }
        this.ontologies = newOntologiesMap;
    }

    @Override
    public OntologyTerm resolveTerm(String termId)
    {
        OntologyService ontology = getOntologyForTerm(termId);
        if (ontology != null) {
            return ontology.getTerm(termId);
        }
        return null;
    }

    @Override
    public OntologyService getOntology(String ontologyId)
    {
        return this.ontologies.get(ontologyId);
    }

    /**
     * Finds the owner ontology given a term identifier. The ontology is identified by the term ID prefix, for example
     * {@code HP} in {@code HP:0002066}.
     *
     * @param termId the term identifier to process
     * @return the owner ontology, or {@code null} if the term doesn't belong to a known ontology
     */
    private OntologyService getOntologyForTerm(String termId)
    {
        String ontologyId = StringUtils.substringBefore(termId, ":");
        if (StringUtils.isNotBlank(ontologyId)) {
            return this.ontologies.get(ontologyId);
        }
        return null;
    }
}
