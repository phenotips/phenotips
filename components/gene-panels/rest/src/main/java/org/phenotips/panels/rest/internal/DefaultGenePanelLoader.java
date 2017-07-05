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
package org.phenotips.panels.rest.internal;

import org.phenotips.panels.GenePanel;
import org.phenotips.panels.GenePanelFactory;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Default implementation of the {@link GenePanelLoader} component.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Singleton
public class DefaultGenePanelLoader implements GenePanelLoader, Initializable
{
    @Inject
    private GenePanelFactory genePanelFactory;

    @Inject
    private VocabularyManager vocabularyManager;

    /** The loading cache for gene panels. Absent term data is ignored. */
    private LoadingCache<List<String>, GenePanel> loadingCache;

    @Override
    public void initialize() throws InitializationException
    {
        this.loadingCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<List<String>, GenePanel>()
            {
                @Override
                public GenePanel load(@Nonnull final List<String> key) throws Exception
                {
                    // Make the computation.
                    return generatePanelsData(key);
                }
            });
    }

    /**
     * Generates a new {@link GenePanel} object, given a {@code key} that is a list of HPO term IDs.
     *
     * @param key an unmodifiable list of HPO term IDs as strings
     * @return a {@link GenePanel} object for the provided {@code key}
     * @throws Exception if {@code key} is empty or {@code null}, or if the generated {@link GenePanel} contains no data
     */
    private GenePanel generatePanelsData(@Nonnull final List<String> key) throws Exception
    {
        // No need to perform a lookup if the key is not valid.
        if (CollectionUtils.isEmpty(key)) {
            throw new Exception();
        }
        // Generate the gene panel data.
        final GenePanel panel = this.genePanelFactory.build(buildTermsFromIDs(key),
            Collections.<VocabularyTerm>emptyList());
        // Don't want to store any empty values in the loading cache.
        if (panel.size() == 0) {
            throw new Exception();
        }
        // Return the stored value.
        return panel;
    }

    /**
     * Builds a set of {@link VocabularyTerm} objects from a collection of term ID strings.
     *
     * @param termIds a collection of term IDs as strings
     * @return a set of {@link VocabularyTerm} objects corresponding with the provided term IDs
     */
    private Set<VocabularyTerm> buildTermsFromIDs(@Nonnull final Collection<String> termIds)
    {
        final Set<VocabularyTerm> terms = new HashSet<>();
        for (final String termId : termIds) {
            if (StringUtils.isBlank(termId)) {
                continue;
            }
            final VocabularyTerm term = this.vocabularyManager.resolveTerm(termId);
            if (term != null) {
                terms.add(term);
            }
        }
        return Collections.unmodifiableSet(terms);
    }

    @Override
    public GenePanel get(@Nonnull final List<String> termList) throws ExecutionException
    {
        return this.loadingCache.get(termList);
    }

    @Override
    public void invalidateAll()
    {
        this.loadingCache.invalidateAll();
    }

    @Override
    public void invalidate(@Nonnull final Object key)
    {
        this.loadingCache.invalidate(key);
    }

    @Override
    public long size()
    {
        return this.loadingCache.size();
    }
}
