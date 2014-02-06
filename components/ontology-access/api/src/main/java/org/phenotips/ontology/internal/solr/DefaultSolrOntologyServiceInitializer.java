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

import org.phenotips.ontology.OntologyTerm;
import org.phenotips.ontology.SolrOntologyServiceInitializer;

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
    /** Character used in URLs to delimit path segments. */
    private static final String URL_PATH_SEPARATOR = "/";

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

    @Override
    public void initialize(String serverName) throws InitializationException
    {
        try {
            this.server = new HttpSolrServer(this.getSolrLocation() + serverName + URL_PATH_SEPARATOR);
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
    public SolrServer getServer()
    {
        return this.server;
    }

    /**
     * Get the URL where the Solr server can be reached, without any core name.
     *
     * @return an URL as a String
     */
    protected String getSolrLocation()
    {
        String wikiSolrUrl = this.configuration.getProperty("solr.remote.url", String.class);
        if (StringUtils.isBlank(wikiSolrUrl)) {
            return "http://localhost:8080/solr/";
        }
        return StringUtils.substringBeforeLast(StringUtils.removeEnd(wikiSolrUrl, URL_PATH_SEPARATOR),
            URL_PATH_SEPARATOR) + URL_PATH_SEPARATOR;
    }
}
