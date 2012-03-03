package org.xwiki.configuration.internal;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link SystemEnvironmentConfigurationSource}.
 * 
 * @version $Id$
 */
public class SystemEnvironmentConfigurationSourceTest extends AbstractComponentTestCase
{
    /**
     * Check that all System environment properties are correctly accessible through the configuration source.
     * 
     * @throws Exception If the configuration source cannot be looked up.
     */
    @Test
    public void testGetSystemEnvironmentProperties() throws Exception
    {
        ConfigurationSource source = getComponentManager().lookup(ConfigurationSource.class, "system-environment");
        
        Map<String, String> environment = System.getenv();
        for (String key : environment.keySet()) {
            Assert.assertEquals(environment.get(key), source.getProperty(key));
        }
    }
}
