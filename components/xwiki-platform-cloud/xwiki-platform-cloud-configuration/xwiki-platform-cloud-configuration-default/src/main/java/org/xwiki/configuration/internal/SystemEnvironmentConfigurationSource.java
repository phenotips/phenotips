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

import javax.inject.Named;

import org.apache.commons.configuration.BaseConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * A configuration source that takes properties from the host environment.
 * 
 * @version $Id$
 */
@Component
@Named("system-environment")
public class SystemEnvironmentConfigurationSource extends CommonsConfigurationSource implements Initializable
{
    @Override
    public void initialize() throws InitializationException
    {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setDelimiterParsingDisabled(true);

        Map<String, String> environment = System.getenv();
        for (String key : environment.keySet()) {
            configuration.setProperty(key, environment.get(key));
        }

        setConfiguration(configuration);
    }

}
