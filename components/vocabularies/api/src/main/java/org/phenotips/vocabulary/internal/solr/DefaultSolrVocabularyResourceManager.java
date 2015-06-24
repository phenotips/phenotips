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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
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
    /** @see #getSolrConnection() */
    private SolrClient core;

    /** @see #getTermCache() */
    private Cache<VocabularyTerm> cache;

    private SolrCore score;

    /** Provides access to the Solr cores. */
    @Inject
    private SolrCoreContainerHandler cores;

    /** Cache factory needed for creating the term cache. */
    @Inject
    private CacheManager cacheFactory;

    @Inject
    private Environment environment;

    @Override
    public void initialize(String vocabularyName) throws InitializationException
    {
        // Get jars home path to where the jar resources are stored
        Class<DefaultSolrVocabularyResourceManager> clazz = DefaultSolrVocabularyResourceManager.class;
        String jarPath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        String jarsHome = jarPath.substring(0, jarPath.lastIndexOf('/'));
        // Get data Solr home path
        File solrHome = new File(this.environment.getPermanentDirectory().getAbsolutePath(), "solr");
        File dest = solrHome;

        CoreContainer container = this.cores.getContainer();

        // Check if the core doesn't exist already
        if (container.getCore(vocabularyName) != null) {
            return;
        }

        try {
            File dir = new File(jarsHome);
            File[] directoryListing = dir.listFiles();

            if (directoryListing == null) {
                return;
            }

            for (File file : directoryListing) {
                JarFile jar = new JarFile(file);
                String jarName = jar.getName().substring(jarsHome.length());
                if ("jar".equals(FilenameUtils.getExtension(file.toPath().toString()))
                    && jarName.startsWith("vocabulary-" + vocabularyName)) {
                    copyConfigsFromJar(jar, vocabularyName, dest);
                }
            }

            CoreDescriptor dcore =
                new CoreDescriptor(container, vocabularyName, solrHome.toPath().resolve(vocabularyName).toString());

            this.core = new EmbeddedSolrServer(container, vocabularyName);

            this.score = container.create(dcore);

            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
        } catch (RuntimeException ex) {
            throw new InitializationException("Invalid Solr core: " + ex.getMessage());
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        } catch (IOException ex) {
            throw new InitializationException("Invalid Solr resource: " + ex.getMessage());
        }
    }

    /**
     * Copy configuration files from a jar file specified by jar to a destination folder on the filesystem dest.
     *
     * @param jar the jar file
     * @param vocabularyName the name of the vocabulary being managed
     * @param dest the destination
     * @throws IOException
     */
    private static void copyConfigsFromJar(JarFile jar, String vocabularyName, File dest) throws IOException
    {
        // return if vocabulary directory already exists
        if (Files.isDirectory(dest.toPath().resolve(vocabularyName))) {
            return;
        }

        for (JarEntry entry : Collections.list(jar.entries())) {
            if (entry.getName().startsWith(vocabularyName)) {
                if (entry.isDirectory()) {
                    Files.createDirectories(dest.toPath().resolve(entry.getName()));
                } else {
                    Files.copy(jar.getInputStream(entry), dest.toPath().resolve(entry.getName()));
                }
            }
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

    /**
     * Get the Solr core of the vocabulary.
     *
     * @return a Solr core
     */
    public SolrCore getSolrCore()
    {
        return this.score;
    }
}
