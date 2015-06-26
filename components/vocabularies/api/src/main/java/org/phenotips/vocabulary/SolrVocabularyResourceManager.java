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

import org.xwiki.cache.Cache;
import org.xwiki.component.annotation.Role;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.stability.Unstable;

import org.apache.solr.client.solrj.SolrClient;

/**
 * Provides methods for initializing the resources needed by vocabularies stored in a Solr index.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M10)
 */
@Unstable
@Role
public interface SolrVocabularyResourceManager
{
    /**
     * Initializes the resources needed by a Solr-indexed vocabulary. This method must be called once before using the
     * resources it exposes.
     *
     * @param vocabularyName the name of the vocabulary being managed
     * @throws InitializationException if an error happens during initialization
     */
    void initialize(String vocabularyName) throws InitializationException;

    /**
     * Get the cache instance created for the target vocabulary.
     *
     * @return a cache instance
     */
    Cache<VocabularyTerm> getTermCache();

    /**
     * Get the Solr core used for the target vocabulary.
     *
     * @return a Solr client for communication with the target core
     */
    SolrClient getSolrConnection();
}
