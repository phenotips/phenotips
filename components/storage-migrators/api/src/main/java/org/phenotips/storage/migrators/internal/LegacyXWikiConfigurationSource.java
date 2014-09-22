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
package org.phenotips.storage.migrators.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConversionException;
import org.xwiki.environment.Environment;
import org.xwiki.properties.ConverterManager;
import org.xwiki.stability.Unstable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Exposes the configuration used by the old XWiki core through the modern
 * {@link org.xwiki.configuration.ConfigurationSource} API.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable("This class belongs in XWiki, or at least in another module, not in the storage migrators module.")
@Component
@Named("legacy")
@Singleton
public class LegacyXWikiConfigurationSource implements ConfigurationSource, Initializable
{
    /** The location of the configuration file, relative to the webapp root directory. */
    private static final String XWIKI_CFG_FILE = "/WEB-INF/xwiki.cfg";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the configuration file. */
    @Inject
    private Environment environment;

    /** Performs type conversions. */
    @Inject
    private ConverterManager converterManager;

    private Properties properties = new Properties();

    @Override
    public void initialize() throws InitializationException
    {
        InputStream data = null;
        try {
            data = this.environment.getResourceAsStream(XWIKI_CFG_FILE);
            if (data != null) {
                this.properties.load(data);
            } else {
                // We use a debug logging level here since we consider it's ok that there's no XWIKI_CFG_FILE available,
                // in which case default values are used.
                this.logger.debug("No configuration file [{}] found. Using default configuration values.",
                    XWIKI_CFG_FILE);
            }
        } catch (Exception ex) {
            // Note: if we cannot read the configuration file for any reason we log a warning but continue since XWiki
            // will use default values for all configurable elements.
            this.logger.warn("Failed to load configuration file [{}]. Using default configuration values. "
                + "Internal error [{}]", XWIKI_CFG_FILE, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        String property = this.properties.getProperty(key);
        if (property == null) {
            return defaultValue;
        }
        if (defaultValue != null) {
            return getProperty(key, (Class<T>) defaultValue.getClass());
        } else {
            return getProperty(key);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        String value = this.properties.getProperty(key);
        if (value == null) {
            return null;
        }
        T result = null;

        try {
            if (String.class.getName().equals(valueClass.getName())) {
                result = (T) value;
            } else if (List.class.isAssignableFrom(valueClass)) {
                String[] values = StringUtils.split(value, ",");
                List<String> resultList = new ArrayList<>();
                for (String v : values) {
                    resultList.add(StringUtils.trim(v));
                }
                result = (T) resultList;
            } else {
                result = (T) this.converterManager.convert(valueClass, getProperty(key));
            }
        } catch (org.apache.commons.configuration.ConversionException
            | org.xwiki.properties.converter.ConversionException ex) {
            throw new ConversionException("Key [" + key + "] is not of type ["
                + valueClass.getName() + "]", ex);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key)
    {
        return (T) this.properties.getProperty(key);
    }

    @Override
    public List<String> getKeys()
    {
        List<String> result = new ArrayList<>();
        for (Object key : this.properties.keySet()) {
            result.add(String.valueOf(key));
        }
        return result;
    }

    @Override
    public boolean containsKey(String key)
    {
        return this.properties.containsKey(key);
    }

    @Override
    public boolean isEmpty()
    {
        return this.properties.isEmpty();
    }
}
