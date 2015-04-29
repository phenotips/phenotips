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
package org.phenotips.ontology;

import org.xwiki.cache.Cache;
import org.xwiki.component.annotation.Role;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.stability.Unstable;

import org.apache.solr.client.solrj.SolrServer;

/**
 * Provides methods for initializing an ontology service.
 *
 * @version $Id$
 * @since FIXME and version
 */
@Unstable
@Role
public interface SolrOntologyServiceInitializer
{
    /**
     * Initializes connection to the Solr server and new cache.
     *
     * @param serverName the suffix for the Solr server
     * @throws InitializationException if an error happens during initialization
     */
    void initialize(String serverName) throws InitializationException;

    /**
     * Gets cache instance.
     *
     * @return cache instance
     */
    Cache<OntologyTerm> getCache();

    /**
     * Gets server instance.
     *
     * @return server instance
     */
    SolrServer getServer();
}
