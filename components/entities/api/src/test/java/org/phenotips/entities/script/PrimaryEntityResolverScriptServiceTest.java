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
package org.phenotips.entities.script;

import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PrimaryEntityResolverScriptService}.
 */
public class PrimaryEntityResolverScriptServiceTest
{
    private static final String SECURE = "secure";

    private static final String P001 = "P001";

    private static final String PATIENTS = "patients";

    @Rule
    public MockitoComponentMockingRule<ScriptService> mocker =
        new MockitoComponentMockingRule<>(PrimaryEntityResolverScriptService.class);

    private PrimaryEntityResolverScriptService component;

    private PrimaryEntityResolver resolver;

    private Logger logger;

    @Before
    public void setUp() throws Exception
    {
        this.component = (PrimaryEntityResolverScriptService) this.mocker.getComponentUnderTest();

        this.logger = this.mocker.getMockedLogger();
        this.resolver = this.mocker.getInstance(PrimaryEntityResolver.class, SECURE);
    }

    @Test
    public void resolveForwardsCalls()
    {
        this.component.resolve(P001);
        verify(this.resolver, times(1)).resolveEntity(P001);
        verifyNoMoreInteractions(this.resolver);
    }

    @Test
    public void resolveCatchesSecurityExceptions()
    {
        when(this.resolver.resolveEntity(P001)).thenThrow(new SecurityException());
        this.component.resolve(P001);
        verify(this.resolver, times(1)).resolveEntity(P001);
        verify(this.logger, times(1)).error("Unauthorized access for [{}]", P001);
        verifyNoMoreInteractions(this.resolver);
    }

    @Test
    public void getEntityManagerForwardsCalls()
    {
        this.component.getEntityManager(PATIENTS);
        verify(this.resolver, times(1)).getEntityManager(PATIENTS);
        verifyNoMoreInteractions(this.resolver);
    }

    @Test
    public void hasEntityManagerForwardsCalls()
    {
        this.component.hasEntityManager(PATIENTS);
        verify(this.resolver, times(1)).hasEntityManager(PATIENTS);
        verifyNoMoreInteractions(this.resolver);
    }
}
