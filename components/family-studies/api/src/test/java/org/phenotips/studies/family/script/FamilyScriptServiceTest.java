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
package org.phenotips.studies.family.script;

import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link FamilyScriptService} component.
 *
 * @version $Id$
 */
public class FamilyScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<FamilyScriptService> mocker =
        new MockitoComponentMockingRule<>(FamilyScriptService.class);

    @Mock
    private Family family;

    private FamilyRepository repo;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.repo = this.mocker.getInstance(FamilyRepository.class, "secure");
        when(this.family.getId()).thenReturn("FAM0123456");
    }

    @Test
    public void getForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.get("FAM0123456")).thenReturn(this.family);
        Assert.assertSame(this.family, this.mocker.getComponentUnderTest().get("FAM0123456"));
    }

    @Test
    public void createForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.create()).thenReturn(this.family);
        Assert.assertSame(this.family, this.mocker.getComponentUnderTest().create());
    }

    @Test
    public void deleteForwardsCalls() throws ComponentLookupException
    {
        when(this.repo.delete(this.family, false)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().delete(this.family));
    }
}
