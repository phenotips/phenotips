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

import org.phenotips.data.VocabularyProperty;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation of {@link AbstractComplexController} used for testing. Specifically, this Implementation overrides the
 * isCodeFieldsOnly() method to return true in order to test the functionality of {@link AbstractComplexController} when
 * all fields can be converted to vocabulary objects
 */
@Component
@Named("codeFieldsTest")
@Singleton
public class AbstractComplexControllerCodeFieldsTestImplementation
    extends AbstractComplexController<List<VocabularyProperty>>
{
    protected static final String DATA_NAME = "codeFieldsTest";

    protected static final String PROPERTY_1 = AbstractSimpleControllerTestImplementation.PROPERTY_1;

    protected static final String PROPERTY_2 = AbstractSimpleControllerTestImplementation.PROPERTY_2;

    @Override
    protected List<String> getBooleanFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return getProperties();
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList(PROPERTY_1, PROPERTY_2);
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

    @Override
    protected boolean isCodeFieldsOnly()
    {
        return true;
    }
}
