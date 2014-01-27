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

import org.phenotips.ontology.OntologyServiceInitializer;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.slf4j.Logger;

/**
 * Initializes cache and server connection for starting a Solr ontology service.
 *
 * @version $Id$
 * @since FIXME and version
 */
@Component
@Named("solr")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SolrOntologyServiceInitializer implements OntologyServiceInitializer
{
    protected static final String HTTP_DELIMITER = "/";

    /** The Solr server instance used. */
    protected SolrServer server;

    /**
     * Cache for the recently accessed terms; useful since the ontology rarely changes, so a search should always return
     * the same thing.
     */
    protected Cache<OntologyTerm> cache;

    /** Cache factory needed for creating the term cache. */
    @Inject
    protected CacheManager cacheFactory;

    @Inject
    @Named("xwikiproperties")
    protected ConfigurationSource configuration;

    @Inject
    private Logger logger;

    @Override
    public void initialize(String serverName) throws InitializationException
    {
        this.getSolrLocation();
        try {
            this.server = new HttpSolrServer(this.getSolrLocation() + serverName + HTTP_DELIMITER);
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
        } catch (RuntimeException ex) {
            throw new InitializationException("Invalid URL specified for the Solr server: {}");
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        }
    }

    @Override
    public String getSolrLocation()
    {
        String wikiSolrUrl = configuration.getProperty("solr.remote.url", String.class);
        String[] urlParts = wikiSolrUrl.trim().split(HTTP_DELIMITER);
        int length = urlParts.length;
        String[] newUrlParts = new String[length - 1];
        for (int i = 0; i < length - 1; i++) {
            newUrlParts[i] = (urlParts[i]);
        }
        String solrUrl = StringUtils.join(newUrlParts, HTTP_DELIMITER);
        return solrUrl + HTTP_DELIMITER;
    }

    @Override
    public Cache<OntologyTerm> getCache()
    {
        return this.cache;
    }

    @Override
    public SolrServer getServer()
    {
        return this.server;
    }
}
