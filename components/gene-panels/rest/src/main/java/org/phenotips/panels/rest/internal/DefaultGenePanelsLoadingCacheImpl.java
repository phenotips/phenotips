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
import org.phenotips.panels.internal.DefaultGenePanelFactoryImpl;
import org.phenotips.panels.rest.GenePanelsLoadingCache;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.drew.lang.Iterables;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Default implementation of the {@link GenePanelsLoadingCache} component.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Component
@Singleton
public class DefaultGenePanelsLoadingCacheImpl implements GenePanelsLoadingCache, Initializable
{
    @Inject
    private VocabularyManager vocabularyManager;

    /** The loading cache for gene panels. */
    private LoadingCache<String, JSONObject> loadingCache;

    @Override
    public void initialize() throws InitializationException
    {
        this.loadingCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, JSONObject>()
            {
                @Override
                public JSONObject load(@Nonnull final String key) throws Exception
                {
                    // Make the computation.
                    return generatePanelsData(key);
                }
            });
    }

    /**
     * Generates the data anew for the comma-separated string of HPO keys, after converting it to a list.
     * @param key a comma-separated string of HPO keys
     * @return a JSONObject containing gene panels data
     */
    private JSONObject generatePanelsData(final String key) throws Exception
    {
        // No need to perform a lookup if the key is not valid.
        if (StringUtils.isBlank(key)) {
            throw new Exception();
        }
        // Generate the gene panel data.
        final List<String> features = Iterables.toList(Splitter.on(",").trimResults().omitEmptyStrings().split(key));
        final GenePanel panel = new DefaultGenePanelFactoryImpl().makeGenePanel(features, this.vocabularyManager);
        // Don't want to store any empty values in the loading cache.
        if (panel.size() == 0) {
            throw new Exception();
        }
        // Return the stored value.
        return panel.toJSON();
    }

    @Override
    public LoadingCache<String, JSONObject> getCache()
    {
        return this.loadingCache;
    }
}
