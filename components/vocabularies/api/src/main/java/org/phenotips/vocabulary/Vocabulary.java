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
package org.phenotips.vocabulary;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to a vocabulary, such as the HUGO Gene Nomenclature, or the Human Phenotype Ontology.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
@Unstable
@Role
public interface Vocabulary
{
    /**
     * Access an individual term from the vocabulary, identified by its {@link VocabularyTerm#getId() term identifier}.
     *
     * @param id the term identifier, in the format {@code <vocabulary prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return the requested term, or {@code null} if the term doesn't exist in this vocabulary
     */
    VocabularyTerm getTerm(String id);

    /**
     * Access a list of terms from the vocabulary, identified by their {@link VocabularyTerm#getId() term identifiers}.
     *
     * @param ids a set of term identifiers, in the format {@code <vocabulary prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return a set with the requested terms that were found in the vocabulary, an empty set if no terms were found
     */
    Set<VocabularyTerm> getTerms(Collection<String> ids);

    /**
     * Generic search method, which looks for terms that match the specified term properties.
     *
     * @param fieldValues a map with term property values that must be matched by the returned terms; the keys are
     *            property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a
     *            single value, or a collection of values that can ({@code OR}) be matched by the term
     * @return the matching terms that were found in the vocabulary, an empty list if no terms were found
     */
    List<VocabularyTerm> search(Map<String, ?> fieldValues);

    /**
     * Generic search method, which looks for terms that match the specified meta-properties.
     *
     * @param fieldValues a map with term property values that must be matched by the returned terms; the keys are
     *            property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a
     *            single value, or a collection of values that can ({@code OR}) be matched by the term
     * @param queryOptions a map with additional query options, such as maximum number of terms to return or a different
     *            sort order; the accepted keys/values depend on the actual implementation details, and usually
     *            correspond to the settings accepted by the storage engine, for example {@code rows -> 10},
     *            {@code start -> 50}, {@code sort -> nameSort asc} for Solr-indexed vocabularies
     * @return the matching terms that were found in the vocabulary, an empty list if no terms were found
     */
    List<VocabularyTerm> search(Map<String, ?> fieldValues, Map<String, String> queryOptions);

    /**
     * Suggest the terms that best match the user's input.
     *
     * @param input the text that the user entered
     * @param maxResults the maximum number of terms to be returned
     * @param sort an optional sort parameter, in a format that depends on the actual engine that stores the vocabulary;
     *            usually a property name followed by {@code asc} or {@code desc}; may be {@code null}
     * @param customFilter a custom filter query to further restrict which terms may be returned, in a format that
     *            depends on the actual engine that stores the vocabulary; some vocabularies may not support a filter
     *            query; may be {@code null}
     * @return a list of suggestions, possibly empty.
     * @since 1.1-rc-1
     */
    List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter);

    /**
     * Get the number of terms that match a specific query.
     *
     * @param fieldValues a map with term property values that must be matched by the returned terms; the keys are
     *            property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a
     *            single value, or a collection of values that can (OR) be matched by the term
     * @return the number of terms matching the query, or {@code -1} if the query couldn't be run correctly
     */
    long count(Map<String, ?> fieldValues);

    /**
     * Find the distance between two terms identified by their {@link VocabularyTerm#getId() term identifiers}, if this
     * is a structured ontology that supports computing such a distance. The parameters are interchangeable.
     *
     * @param fromTermId the identifier of the term that is considered the start point
     * @param toTermId the identifier of the term that is considered the end point
     * @return the minimum number of edges that connect the two terms in the DAG representing the ontology, and
     *         {@code -1} if this is an unstructured vocabulary, the terms are not connected, or if at least one of the
     *         identifiers is invalid
     * @see #getDistance(VocabularyTerm, VocabularyTerm)
     */
    long getDistance(String fromTermId, String toTermId);

    /**
     * Find the distance between two terms, if this is a structured ontology that supports computing such a distance.
     * The parameters are interchangeable.
     *
     * @param fromTerm the term that is considered the start point
     * @param toTerm the term that is considered the end point
     * @return the minimum number of edges that connect the two terms in the DAG representing the ontology, and -1 if
     *         this is an unstructured vocabulary, the terms are not connected, or if at least one of the terms is
     *         {@code null}
     */
    long getDistance(VocabularyTerm fromTerm, VocabularyTerm toTerm);

    /**
     * The identifier of the vocabulary, used internally to differentiate between different vocabularies and different
     * implementations of the same vocabulary.
     *
     * @return a simple string, for example {@code hpo}, {@code omim}, or {@code hgncRemote}
     * @since 1.3M1
     */
    String getIdentifier();

    /**
     * The official name of the vocabulary.
     *
     * @return a string, for example {@code The Human Phenotype Ontology}
     * @since 1.3M1
     */
    String getName();

    /**
     * A vocabulary has an official name, but it can also have other aliases, for example the Human Phenotype Ontology
     * is known both as {@code HP}, which is the official prefix for its terms, {@code HPO}, which is its acronym, or
     * the lowercase {@code hpo}.
     *
     * @return a set of identifiers which can be used to reference this vocabulary, including the official name
     */
    Set<String> getAliases();

    /**
     * Get the size (i.e. total number of terms) in this vocabulary.
     *
     * @return the number of terms in the vocabulary
     */
    long size();

    /**
     * Reindex the whole vocabulary, fetching the source from the specified location.
     *
     * @param sourceUrl the URL to be indexed
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     *         specified URL is invalid
     */
    int reindex(String sourceUrl);

    /**
     * Get the default location where the sources for this vocabulary can be fetched from.
     *
     * @return the string containing the default URL for the vocabulary source
     */
    String getDefaultSourceLocation();

    /**
     * Get the available version of the vocabulary.
     *
     * @return a version identifier, or {@code null} if the version cannot be determined
     */
    String getVersion();

    /**
     * Get the website url for the vocabulary.
     *
     * @return a String representation of the url for the vocabulary website.
     */
    String getWebsite();

    /**
     * Get the citation for the vocabulary.
     *
     * @return the string containing the citation for the vocabulary.
     */
    String getCitation();
}
