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
import org.phenotips.vocabulary.Vocabulary;
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

import org.apache.commons.io.FileUtils;
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
    private static final String SOLR = "solr/";

    private static final String TEMP = "_temp";

    /** List of config Solr files. */
    private static final List<String> CONFIG_FILES = Arrays.asList("/conf/schema.xml", "/conf/solrconfig.xml",
        "/conf/solrcore.properties", "/conf/protwords.txt", "/conf/stopwords.txt", "/conf/synonyms.txt",
        "/conf/managed-schema.xml", "/core.properties");

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

    private void initialize(Vocabulary vocabulary) throws InitializationException
    {
        final CoreContainer container = this.coreContainer.getContainer();
        final String coreIdentifier = vocabulary.getIdentifier();
        SolrCore solrCore = container.getCore(coreIdentifier);

        String phenotipsCoreVersion =
            (solrCore != null) ? solrCore.getCoreDescriptor().getCoreProperty("phenotips.version", "") : "";

        try {
            String phenotipsVersion = this.distribution.getDistributionExtension().getId().getVersion().toString();

            // Check if the core version differs from phenotips version
            if (!phenotipsVersion.equals(phenotipsCoreVersion)) {

                // Get data Solr home path
                File solrHome = new File(this.environment.getPermanentDirectory().getAbsolutePath(), "solr");
                File dest = solrHome;
                Files.createDirectories(dest.toPath().resolve(coreIdentifier + "/conf"));

                for (String file : CONFIG_FILES) {
                    InputStream in = vocabulary.getClass().getResourceAsStream("/" + coreIdentifier + file);
                    if (in == null) {
                        continue;
                    }
                    Files.copy(in, dest.toPath().resolve(coreIdentifier + file),
                        StandardCopyOption.REPLACE_EXISTING);
                }
                if (solrCore != null) {
                    container.reload(coreIdentifier);
                } else {
                    // container.create will fail if core.properties is already there, so we temporarily delete it
                    // FIXME We should first read the properties file as a map and pass it to container.create
                    Files.delete(dest.toPath().resolve(coreIdentifier + "/core.properties"));
                    container.create(coreIdentifier, Collections.<String, String>emptyMap());
                }
            }

            SolrClient core = new EmbeddedSolrServer(container, coreIdentifier);
            this.cores.put(coreIdentifier, core);
            Cache<VocabularyTerm> cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
            this.caches.put(coreIdentifier, cache);
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
    public Cache<VocabularyTerm> getTermCache(Vocabulary vocabulary)
    {
        if (!this.caches.containsKey(vocabulary.getIdentifier())) {
            try {
                initialize(vocabulary);
            } catch (InitializationException ex) {
                return null;
            }
        }
        return this.caches.get(vocabulary.getIdentifier());
    }

    @Override
    public SolrClient getSolrConnection(Vocabulary vocabulary)
    {
        if (!this.cores.containsKey(vocabulary.getIdentifier())) {
            try {
                initialize(vocabulary);
            } catch (InitializationException ex) {
                return null;
            }
        }
        return this.cores.get(vocabulary.getIdentifier());
    }

    private SolrClient getSolrConnection(String coreId)
    {
        if (!this.cores.containsKey(coreId)) {
            return null;
        }
        return this.cores.get(coreId);
    }

    @Override
    public void createReplacementCore(Vocabulary vocabulary) throws InitializationException
    {
        try {
            final String replacementCoreId = vocabulary.getIdentifier() + TEMP;
            final String absPath = this.environment.getPermanentDirectory().getAbsolutePath();
            File tempDirectory = new File(absPath, SOLR + replacementCoreId);

            if (!tempDirectory.exists()) {
                Files.createDirectories(tempDirectory.toPath());
            }

            CoreContainer container = this.coreContainer.getContainer();

            File configOrigin = new File(absPath, SOLR + vocabulary.getIdentifier() + "/conf");
            File configTemp = new File(absPath, SOLR + replacementCoreId + "/conf");
            FileUtils.copyDirectory(configOrigin, configTemp);

            container.create(replacementCoreId, Collections.<String, String>emptyMap());

            SolrClient core = new EmbeddedSolrServer(container, replacementCoreId);
            this.cores.put(replacementCoreId, core);
        } catch (IOException ex) {
            throw new InitializationException("Invalid Solr resource: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void replaceCore(Vocabulary vocabulary) throws InitializationException
    {
        final String absPath = this.environment.getPermanentDirectory().getAbsolutePath();
        final File indexOrigin = new File(absPath, SOLR + vocabulary.getIdentifier() + "/data");
        final File indexTemp = new File(absPath, SOLR + vocabulary.getIdentifier() + TEMP + "/data");
        try {
            CoreContainer container = this.coreContainer.getContainer();
            SolrCore solrCore = container.getCore(vocabulary.getIdentifier());
            if (solrCore != null) {
                solrCore.close();
            }
            container.unload(vocabulary.getIdentifier(), true, false, false);
            FileUtils.copyDirectory(indexTemp, indexOrigin);
            initialize(vocabulary);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public SolrClient getReplacementSolrConnection(Vocabulary vocabulary)
    {
        return this.getSolrConnection(vocabulary.getIdentifier() + TEMP);
    }

    @Override
    public void discardReplacementCore(Vocabulary vocabulary)
    {
        final String replacementCoreId = vocabulary.getIdentifier() + TEMP;
        if (this.cores.containsKey(replacementCoreId)) {
            CoreContainer container = this.coreContainer.getContainer();
            SolrCore solrCore = container.getCore(replacementCoreId);
            if (solrCore != null) {
                solrCore.close();
            }
            container.unload(replacementCoreId, true, true, true);
            this.cores.remove(replacementCoreId);
            this.caches.remove(replacementCoreId);
        }
    }
}
