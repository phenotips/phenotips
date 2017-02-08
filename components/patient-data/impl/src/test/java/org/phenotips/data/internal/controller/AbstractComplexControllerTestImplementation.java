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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation of {@link AbstractComplexController} used for testing.
 */
@Component
@Named("complexTest")
@Singleton
public class AbstractComplexControllerTestImplementation extends AbstractComplexController<String>
{
    protected static final String DATA_NAME = "test";

    protected static final String PROPERTY_1 = "property1";

    protected static final String PROPERTY_2 = "property2";

    protected static final String PROPERTY_3 = "property3";

    protected static final String PROPERTY_4 = "property4";

    protected static final String PROPERTY_5 = "property5";

    @Override
    protected List<String> getBooleanFields()
    {
        return Arrays.asList(PROPERTY_3, PROPERTY_4, PROPERTY_5);
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList(PROPERTY_1, PROPERTY_2, PROPERTY_3, PROPERTY_4, PROPERTY_5);
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
