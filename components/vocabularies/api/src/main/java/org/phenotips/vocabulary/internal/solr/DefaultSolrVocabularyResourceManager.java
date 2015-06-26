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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.SolrCoreContainerHandler;
import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;

/**
 * Default implementation for the {@link SolrVocabularyResourceManager} component.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M10)
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultSolrVocabularyResourceManager implements SolrVocabularyResourceManager
{
    /** @see #getSolrConnection() */
    private SolrClient core;

    /** @see #getTermCache() */
    private Cache<VocabularyTerm> cache;

    /** Provides access to the Solr cores. */
    @Inject
    private SolrCoreContainerHandler cores;

    /** Cache factory needed for creating the term cache. */
    @Inject
    private CacheManager cacheFactory;

    @Override
    public void initialize(String vocabularyName) throws InitializationException
    {
        try {
            this.core = new EmbeddedSolrServer(this.cores.getContainer(), vocabularyName);
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
        } catch (RuntimeException ex) {
            throw new InitializationException("Invalid Solr core: " + ex.getMessage());
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        }
    }

    @Override
    public Cache<VocabularyTerm> getTermCache()
    {
        return this.cache;
    }

    @Override
    public SolrClient getSolrConnection()
    {
        return this.core;
    }
}
