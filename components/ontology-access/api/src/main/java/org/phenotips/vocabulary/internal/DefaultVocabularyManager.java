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

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link VocabularyManager} component, which uses the {@link Vocabulary ontologies}
 * registered in the component manager.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
@Component
@Singleton
public class DefaultVocabularyManager implements VocabularyManager, Initializable
{
    /** The currently available vocabularies. */
    @Inject
    private Map<String, Vocabulary> ontologies;

    @Override
    public void initialize() throws InitializationException
    {
        Map<String, Vocabulary> newOntologiesMap = new HashMap<String, Vocabulary>();
        for (Vocabulary ontology : this.ontologies.values()) {
            for (String alias : ontology.getAliases()) {
                newOntologiesMap.put(alias, ontology);
            }
        }
        this.ontologies = newOntologiesMap;
    }

    @Override
    public VocabularyTerm resolveTerm(String termId)
    {
        Vocabulary ontology = getOntologyForTerm(termId);
        if (ontology != null) {
            return ontology.getTerm(termId);
        }
        return null;
    }

    @Override
    public Vocabulary getVocabulary(String ontologyId)
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
    private Vocabulary getOntologyForTerm(String termId)
    {
        String ontologyId = StringUtils.substringBefore(termId, ":");
        if (StringUtils.isNotBlank(ontologyId)) {
            return this.ontologies.get(ontologyId);
        }
        return null;
    }
}
