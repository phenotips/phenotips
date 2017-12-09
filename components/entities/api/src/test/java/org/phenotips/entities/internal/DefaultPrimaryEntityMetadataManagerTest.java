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
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityMetadataManager;
import org.phenotips.entities.PrimaryEntityMetadataProvider;

import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPrimaryEntityMetadataManager}.
 */
public class DefaultPrimaryEntityMetadataManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityMetadataManager> mocker =
        new MockitoComponentMockingRule<>(DefaultPrimaryEntityMetadataManager.class);

    @Mock
    private PrimaryEntityMetadataProvider provider1;

    @Mock
    private PrimaryEntityMetadataProvider provider2;

    @Mock
    private PrimaryEntity entity;

    private PrimaryEntityMetadataManager component;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.mocker.registerComponent(PrimaryEntityMetadataProvider.class, "provider1", this.provider1);
        this.mocker.registerComponent(PrimaryEntityMetadataProvider.class, "provider2", this.provider2);

        this.component = this.mocker.getComponentUnderTest();
    }

    @Test
    public void getMetadataGathersMetadataFromAllProviders()
    {
        when(this.provider1.provideMetadata(this.entity)).thenReturn(Collections.singletonMap("key1", "value1"));
        when(this.provider2.provideMetadata(this.entity)).thenReturn(Collections.singletonMap("key2", 2));
        Map<String, Object> result = this.component.getMetadata(this.entity);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("value1", result.get("key1"));
        Assert.assertEquals(2, result.get("key2"));
    }

    @Test
    public void getMetadataReturnsEmptyMapWhenNoMetadataIsAvailable()
    {
        when(this.provider1.provideMetadata(this.entity)).thenReturn(Collections.emptyMap());
        when(this.provider2.provideMetadata(this.entity)).thenReturn(Collections.emptyMap());
        Map<String, Object> result = this.component.getMetadata(this.entity);
        Assert.assertTrue(result.isEmpty());
    }
}
