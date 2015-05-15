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
