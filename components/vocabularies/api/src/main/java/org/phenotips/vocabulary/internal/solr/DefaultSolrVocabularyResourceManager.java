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
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.extension.distribution.internal.DistributionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

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
@Singleton
public class DefaultSolrVocabularyResourceManager implements SolrVocabularyResourceManager
{
    /** List of config Solr files. */
    public static final List<String> CONFIG_FILES = Arrays.asList("/conf/schema.xml", "/conf/solrconfig.xml",
        "/conf/solrcore.properties", "/conf/protwords.txt", "/conf/stopwords.txt", "/conf/synonyms.txt",
        "/core.properties");

    /** @see #getSolrConnection() */
    private Map<String, SolrClient> cores = new HashMap<>();

    /** @see #getTermCache() */
    private Map<String, Cache<VocabularyTerm>> caches = new HashMap<>();

    /** Provides access to the Solr cores. */
    @Inject
    private SolrCoreContainerHandler coreContainer;

    /** Cache factory needed for creating the term cache. */
    @Inject
    private CacheManager cacheFactory;

    @Inject
    private Environment environment;

    @Inject
    private DistributionManager distribution;

    private void initialize(String vocabularyName) throws InitializationException
    {
        CoreContainer container = this.coreContainer.getContainer();
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
                if (solrCore != null) {
                    container.reload(vocabularyName);
                } else {
                    // container.create will fail if core.properties is already there, so we temporarily delete it
                    // FIXME We should first read the properties file as a map and pass it to container.create
                    Files.delete(dest.toPath().resolve(vocabularyName + "/core.properties"));
                    container.create(vocabularyName, Collections.<String, String>emptyMap());
                }
            }

            SolrClient core = new EmbeddedSolrServer(container, vocabularyName);
            this.cores.put(vocabularyName, core);
            Cache<VocabularyTerm> cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
            this.caches.put(vocabularyName, cache);
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new InitializationException("Invalid Solr resource: " + ex.getMessage(), ex);
        } finally {
            if (solrCore != null) {
                solrCore.close();
            }
        }
    }

    @Override
    public Cache<VocabularyTerm> getTermCache(String vocabularyId)
    {
        if (!this.caches.containsKey(vocabularyId)) {
            try {
                initialize(vocabularyId);
            } catch (InitializationException ex) {
                return null;
            }
        }
        return this.caches.get(vocabularyId);
    }

    @Override
    public SolrClient getSolrConnection(String vocabularyId)
    {
        if (!this.cores.containsKey(vocabularyId)) {
            try {
                initialize(vocabularyId);
            } catch (InitializationException ex) {
                return null;
            }
        }
        return this.cores.get(vocabularyId);
    }
}
