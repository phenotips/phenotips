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
public interface SolrOntologyServiceInitializable
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
