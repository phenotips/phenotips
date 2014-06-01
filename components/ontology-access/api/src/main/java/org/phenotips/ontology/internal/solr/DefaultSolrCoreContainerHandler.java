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

import org.phenotips.ontology.SolrCoreContainerHandler;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.environment.Environment;

import java.io.File;

import javax.inject.Inject;

import org.apache.solr.core.CoreContainer;

/**
 * Default implementation of {@link SolrCoreContainerHandler}, looking for the Solr configuration in a subdirectory of
 * the permanent directory called {@code solr}.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
public class DefaultSolrCoreContainerHandler implements SolrCoreContainerHandler, Initializable, Disposable
{
    /** Provides access to the configured permanent directory. */
    @Inject
    private Environment environment;

    /** The initialized core container. */
    private CoreContainer cores;

    @Override
    public void initialize()
    {
        File solrHome = new File(this.environment.getPermanentDirectory().getAbsolutePath(), "solr");
        this.cores = new CoreContainer(solrHome.getAbsolutePath());
        this.cores.load();
    }

    @Override
    public CoreContainer getContainer()
    {
        return this.cores;
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.cores.shutdown();
    }
}
