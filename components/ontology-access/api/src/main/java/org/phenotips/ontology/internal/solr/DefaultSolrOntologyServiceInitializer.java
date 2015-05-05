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
package org.phenotips.ontology.internal.solr;

import org.phenotips.ontology.OntologyTerm;
import org.phenotips.ontology.SolrCoreContainerHandler;
import org.phenotips.ontology.SolrOntologyServiceInitializer;

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
 * Initializes cache and server connection for starting a Solr ontology service.
 *
 * @version $Id$
 * @since FIXME and version
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultSolrOntologyServiceInitializer implements SolrOntologyServiceInitializer
{
    /** The Solr server instance used. */
    private SolrClient server;

    /**
     * Cache for the recently accessed terms; useful since the ontology rarely changes, so a search should always return
     * the same thing.
     */
    private Cache<OntologyTerm> cache;

    @Inject
    private SolrCoreContainerHandler cores;

    /** Cache factory needed for creating the term cache. */
    @Inject
    private CacheManager cacheFactory;

    @Override
    public void initialize(String serverName) throws InitializationException
    {
        try {
            this.server = new EmbeddedSolrServer(this.cores.getContainer(), serverName);
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
        } catch (RuntimeException ex) {
            throw new InitializationException("Invalid URL specified for the Solr server: {}");
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        }
    }

    @Override
    public Cache<OntologyTerm> getCache()
    {
        return this.cache;
    }

    @Override
    public SolrClient getServer()
    {
        return this.server;
    }
}
