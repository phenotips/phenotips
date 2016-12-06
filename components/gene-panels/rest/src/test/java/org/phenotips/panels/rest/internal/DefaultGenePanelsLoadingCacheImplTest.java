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
package org.phenotips.panels.rest.internal;

import org.phenotips.vocabulary.VocabularyManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmock.auto.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DefaultGenePanelsLoadingCacheImpl}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultGenePanelsLoadingCacheImplTest
{
    @InjectMocks
    private DefaultGenePanelsLoadingCacheImpl component;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void generatePanelsDataNoInput()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final Method generatePanelsData =
            DefaultGenePanelsLoadingCacheImpl.class.getDeclaredMethod("generatePanelsData", String.class);
        generatePanelsData.setAccessible(true);

        this.exception.expect(Exception.class);
        generatePanelsData.invoke(this.component, "");
    }
}
