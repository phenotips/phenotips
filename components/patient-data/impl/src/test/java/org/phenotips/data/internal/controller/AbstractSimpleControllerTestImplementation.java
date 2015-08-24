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
package org.phenotips.data.internal.controller;

import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

@Component
@Named("test")
@Singleton
public class AbstractSimpleControllerTestImplementation extends AbstractSimpleController
{
    protected static final String DATA_NAME = "test";

    protected static final String PROPERTY_1 = "property1";

    protected static final String PROPERTY_2 = "property2";

    protected static final String PROPERTY_3 = "property3";

    @Override
    protected List<String> getProperties()
    {
        List<String> properties = new ArrayList<>();
        properties.add(PROPERTY_1);
        properties.add(PROPERTY_2);
        properties.add(PROPERTY_3);
        return properties;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return DATA_NAME;
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
