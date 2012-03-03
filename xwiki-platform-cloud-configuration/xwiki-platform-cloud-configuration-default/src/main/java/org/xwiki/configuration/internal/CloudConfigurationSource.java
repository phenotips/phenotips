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
package org.xwiki.configuration.internal;

import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.configuration.BaseConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;

/**
 * This class is a configuration source that behaves like the default one but extends it by allowing to remap certain
 * configuration keys. For example, if a remap.KEY1=KEY1 is defined somewhere, then the value for KEY1 is taken from the
 * value of KEY2. This is useful in cloud environments where several parameters are passed at runtime through
 * environment variables with different names depending on the cloud provider.
 * 
 * @version $Id$
 */
@Component
@Named("cloud")
@Singleton
public class CloudConfigurationSource extends DefaultConfigurationSource
{
    /**
     * The prefix to be used in order to look for remapped properties.
     */
    private static final String REMAP_PREFIX = "remap.";

    /**
     * The file containing additional remappings. Remappings can be defined everywhere, this file is just a convenient
     * place where to store them all.
     */
    private static final String REMAPPING_FILE = "/WEB-INF/remapping.properties";

    /**
     * The environment for looking up remapping file.
     */
    @Inject
    private Environment environment;

    /**
     * System properties configuration source.
     */
    @Inject
    @Named("system-properties")
    private ConfigurationSource systemPropertiesConfigurationSource;

    /**
     * System environment configuration source.
     */
    @Inject
    @Named("system-environment")
    private ConfigurationSource systemEnvironmentConfigurationSource;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        addConfigurationSource(systemPropertiesConfigurationSource);
        addConfigurationSource(systemEnvironmentConfigurationSource);

        try {
            ConfigurationSource remappings = loadRemappings();
            if (remappings != null) {
                addConfigurationSource(remappings);
            }
        } catch (Exception e) {
            throw new InitializationException(String.format("Unable to read remappings %s", REMAPPING_FILE), e);
        }
    }

    /**
     * Load remapping definitions from the remapping file and provide them as a configuration source.
     * 
     * @return A configuration source containing the remappings. null if the file is not present.
     * @throws Exception if there is an error loading the file.
     */
    private ConfigurationSource loadRemappings() throws Exception
    {
        InputStream is = environment.getResourceAsStream(REMAPPING_FILE);
        if (is == null) {
            return null;
        }

        Properties properties = new Properties();
        try {
            properties.load(is);
        } catch (Exception e) {
            throw new InitializationException(String.format("Unable to read %s", REMAPPING_FILE), e);
        }

        BaseConfiguration configuration = new BaseConfiguration();
        for (String key : properties.stringPropertyNames()) {
            configuration.setProperty(key, properties.get(key));
        }

        CommonsConfigurationSource commonsConfigurationSource = new CommonsConfigurationSource();
        commonsConfigurationSource.setConfiguration(configuration);

        return commonsConfigurationSource;
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        T originalValue = super.getProperty(key, valueClass);

        String remappedKey = getRemappedKey(key);
        if (remappedKey == null) {
            return originalValue;
        }

        T remappedValue = super.getProperty(remappedKey, valueClass);
        if (remappedValue == null) {
            return originalValue;
        }

        return remappedValue;
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        T originalValue = super.getProperty(key, defaultValue);

        String remappedKey = getRemappedKey(key);
        if (remappedKey == null) {
            return originalValue;
        }

        T remappedValue = super.getProperty(remappedKey, defaultValue);
        if (remappedValue == null) {
            return originalValue;
        }

        return remappedValue;
    }

    @Override
    public <T> T getProperty(String key)
    {
        T originalValue = super.getProperty(key);

        String remappedKey = getRemappedKey(key);
        if (remappedKey == null) {
            return originalValue;
        }

        T remappedValue = super.getProperty(remappedKey);
        if (remappedValue == null) {
            return originalValue;
        }

        return remappedValue;
    }

    /**
     * Get the remapping for the key.
     * 
     * @param key The key we are looking a remapping for.
     * @return The remapped key.
     */
    private String getRemappedKey(String key)
    {
        String remappingKey = String.format("%s%s", REMAP_PREFIX, key);

        return super.getProperty(remappingKey);
    }
}
