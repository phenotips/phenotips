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
import org.xwiki.environment.Environment;
import org.xwiki.extension.distribution.internal.DistributionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;

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
    /** List of config Solr files. */
    public static final List<String> CONFIG_FILES = Arrays.asList("/conf/schema.xml", "/conf/solrconfig.xml",
        "/conf/solrcore.properties", "/conf/protwords.txt", "/conf/stopwords.txt", "/conf/synonyms.txt",
        "/core.properties");

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

    @Inject
    private Environment environment;

    @Inject
    private DistributionManager distribution;

    @Override
    public void initialize(String vocabularyName) throws InitializationException
    {
        CoreContainer container = this.cores.getContainer();
        SolrCore solrCore = container.getCore(vocabularyName);

        String phenotipsCoreVersion =
            (solrCore != null) ? solrCore.getCoreDescriptor().getCoreProperty("phenotips.version", "") : "";

        try {
            String phenotipsVersion = this.distribution.getDistributionExtension().getId().getVersion().toString();

            // Check if the core version differs from phenotips version
            if (!phenotipsVersion.equals(phenotipsCoreVersion)) {

                // Get data Solr home path
                File solrHome = new File(this.environment.getPermanentDirectory().getAbsolutePath(), "solr");
                File dest = solrHome;
                Files.createDirectories(dest.toPath().resolve(vocabularyName + "/conf"));

                for (String file : CONFIG_FILES) {
                    InputStream in = this.getClass().getResourceAsStream("/" + vocabularyName + file);
                    if (in == null) {
                        continue;
                    }
                    Files.copy(in, dest.toPath().resolve(vocabularyName + file), StandardCopyOption.REPLACE_EXISTING);
                }
                container.reload(vocabularyName);
            }

            this.core = new EmbeddedSolrServer(container, vocabularyName);
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        } catch (IOException ex) {
            throw new InitializationException("Invalid Solr resource: ", ex);
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
