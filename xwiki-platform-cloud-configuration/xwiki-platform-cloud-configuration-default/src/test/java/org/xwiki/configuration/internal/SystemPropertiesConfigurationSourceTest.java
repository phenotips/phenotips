package org.xwiki.configuration.internal;

import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link SystemPropertiesConfigurationSource}.
 * 
 * @version $Id$
 */
public class SystemPropertiesConfigurationSourceTest extends AbstractComponentTestCase
{
    /**
     * Check that all System properties are correctly accessible through the configuration source.
     * 
     * @throws Exception If the configuration source cannot be looked up.
     */
    @Test
    public void testGetSystemProperties() throws Exception
    {
        ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class, "system-properties");

        Properties properties = System.getProperties();
        for (String key : properties.stringPropertyNames()) {
            Assert.assertEquals(properties.get(key), source.getProperty(key));
        }
    }
}
