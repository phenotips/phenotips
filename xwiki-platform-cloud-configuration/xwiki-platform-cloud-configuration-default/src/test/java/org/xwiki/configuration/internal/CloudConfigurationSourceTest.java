package org.xwiki.configuration.internal;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.configuration.BaseConfiguration;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.environment.Environment;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link SystemEnvironmentConfigurationSource}.
 * 
 * @version $Id$
 */
public class CloudConfigurationSourceTest extends AbstractComponentTestCase
{
    /**
     * The remapping file.
     */
    private static final String REMAPPING_FILE = "/WEB-INF/remapping.properties";

    /**
     * Test string for properties.
     */
    private static final String FOO = "foo";

    /**
     * Test string for properties.
     */
    private static final String BAR = "bar";

    /**
     * The cloud configuration source to be used in tests.
     */
    private CloudConfigurationSource source;

    /**
     * The configuration used to inject items into the configuration source.
     */
    private BaseConfiguration configuration;

    /**
     * The environment injected in the configuration source.
     */
    private Environment environment;

    /**
     * Setup the cloud configuration source for tests.
     * 
     * @throws Exception If the cloud configuration source cannot be looked up.
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        final ConverterManager converterManager = getComponentManager().lookup(ConverterManager.class);
        environment = getMockery().mock(Environment.class);

        /* Build a fake remapping file containing a remap entry */
        final ByteArrayInputStream remappingFileContent =
            new ByteArrayInputStream(String.format("remap.%s=%s", FOO, BAR).getBytes());
        getMockery().checking(new Expectations()
        {
            {
                allowing(environment).getResourceAsStream(REMAPPING_FILE);
                will(returnValue(remappingFileContent));
            }
        });

        final Logger logger = getMockery().mock(Logger.class);

        CommonsConfigurationSource dummySource = new CommonsConfigurationSource();
        ReflectionUtils.setFieldValue(dummySource, "converterManager", converterManager);
        configuration = new BaseConfiguration();
        dummySource.setConfiguration(configuration);

        SystemPropertiesConfigurationSource systemPropertiesConfigurationSource =
            new SystemPropertiesConfigurationSource();
        systemPropertiesConfigurationSource.initialize();

        SystemEnvironmentConfigurationSource systemEnvironmentConfigurationSource =
            new SystemEnvironmentConfigurationSource();
        systemEnvironmentConfigurationSource.initialize();

        source = new CloudConfigurationSource();
        ReflectionUtils.setFieldValue(source, "environment", environment);
        ReflectionUtils.setFieldValue(source, "logger", logger);
        ReflectionUtils.setFieldValue(source, "xwikiPropertiesSource", dummySource);
        ReflectionUtils.setFieldValue(source, "wikiPreferencesSource", dummySource);
        ReflectionUtils.setFieldValue(source, "spacePreferencesSource", dummySource);
        ReflectionUtils.setFieldValue(source, "systemPropertiesConfigurationSource",
            systemPropertiesConfigurationSource);
        ReflectionUtils.setFieldValue(source, "systemEnvironmentConfigurationSource",
            systemEnvironmentConfigurationSource);

        source.initialize();
    }

    /**
     * Check that all System environment properties are correctly accessible through the configuration source.
     * 
     * @throws Exception If the configuration source cannot be looked up.
     */
    @Test
    public void testGetSystemEnvironmentProperties() throws Exception
    {
        Map<String, String> systemEnvironment = System.getenv();
        for (String key : systemEnvironment.keySet()) {
            Assert.assertEquals(systemEnvironment.get(key), source.getProperty(key));
        }
    }

    /**
     * Check that all System properties are correctly accessible through the configuration source.
     * 
     * @throws Exception If the configuration source cannot be looked up.
     */
    @Test
    public void testGetSystemProperties() throws Exception
    {
        Properties properties = System.getProperties();
        for (String key : properties.stringPropertyNames()) {
            Assert.assertEquals(properties.get(key), source.getProperty(key));
        }
    }

    /**
     * Check remapping. This checks that the remapping mechanism works and also that the remapping file is taken into
     * account.
     */
    @Test
    public void testRemapping()
    {
        configuration.setProperty(FOO, FOO);
        configuration.setProperty(BAR, BAR);

        Assert.assertEquals(BAR, source.getProperty(FOO));
    }
}
