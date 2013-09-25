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
package org.phenotips.ontology;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to an ontology, such as the Human Phenotype Ontology.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Role
public interface OntologyService
{
    /**
     * Access an individual term from the ontology, identified by its {@link OntologyTerm#getId() term identifier}.
     * 
     * @param id the term identifier, in the format {@code <ontology prefix>:<term id>}, for example {@code HP:0002066}
     * @return the requested term, or {@code null} if the term doesn't exist in this ontology
     */
    OntologyTerm getTerm(String id);

    /**
     * Access a list of terms from the ontology, identified by their {@link OntologyTerm#getId() term identifiers}.
     * 
     * @param ids a set of term identifiers, in the format {@code <ontology prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return a set with the requested terms that were found in the ontology, an empty set if no terms were found
     */
    Set<OntologyTerm> getTerms(Collection<String> ids);

    /**
     * Generic search method, which looks for terms that match the specified meta-properties.
     * 
     * @param fieldValues a map with term meta-property values that must be matched by the returned terms; the keys are
     *            property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a
     *            single value, or a collection of values that can (OR) be matched by the term;
     * @return a set with the matching terms that were found in the ontology, an empty set if no terms were found
     */
    Set<OntologyTerm> search(Map<String, ?> fieldValues);

    /**
     * Get the number of entries that match a specific query.
     * 
     * @param fieldValues a map with term meta-property values that must be matched by the returned terms; the keys are
     *            property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a
     *            single value, or a collection of values that can (OR) be matched by the term;
     * @return the number of entries matching the query
     */
    long count(Map<String, ?> fieldValues);

    /**
     * An ontology has an official name, but it can also have other aliases, for example the Human Phenotype Ontology is
     * known both as {@code HP}, which is the official prefix for its terms, {@code HPO}, which is its acronym, or the
     * lowercase {@code hpo}.
     * 
     * @return a set of identifiers which can be used to reference this ontology, including the official name
     */
    Set<String> getAliases();

    /**
     * Get the size (i.e. total number of entries) in the index.
     * 
     * @return the number of entries in the index
     */
    long size();

    /**
     * Reindex the whole ontology, fetching the latest version from the source.
     */
    void reindex();
}
