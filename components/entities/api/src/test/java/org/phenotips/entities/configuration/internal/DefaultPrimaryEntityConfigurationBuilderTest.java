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
package org.phenotips.entities.configuration.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.configuration.PrimaryEntityConfiguration;
import org.phenotips.entities.configuration.RecordConfiguration;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationBuilder;
import org.phenotips.entities.configuration.spi.RecordConfigurationBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PrimaryEntityConfigurationBuilder} implementation,
 * {@link DefaultPrimaryEntityConfigurationBuilder}.
 *
 * @version $Id$
 */
public class DefaultPrimaryEntityConfigurationBuilderTest
{
    private PrimaryEntityConfigurationBuilder component;

    @Mock
    private PrimaryEntityManager<PrimaryEntity> entityManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        this.component = DefaultPrimaryEntityConfigurationBuilder.with(this.entityManager);
    }

    @Test
    public void providedEntityManagerIsReturned()
    {
        Assert.assertSame(this.entityManager, this.component.getEntityManager());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullEntityManagerIsIllegal()
    {
        DefaultPrimaryEntityConfigurationBuilder.with(null);
    }

    @Test
    public void newInstanceHasARecordConfigurationBuilder()
    {
        Assert.assertNotNull(this.component.getRecordConfiguration());
    }

    @Test
    public void setRecordConfigurationBuilderWorks()
    {
        RecordConfigurationBuilder rcb = mock(RecordConfigurationBuilder.class);
        this.component.setRecordConfiguration(rcb);
        Assert.assertSame(rcb, this.component.getRecordConfiguration());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRecordConfigurationBuilderRefusesNull()
    {
        this.component.setRecordConfiguration(null);
    }

    @Test
    public void newInstanceHasIdFormat()
    {
        Assert.assertEquals("%s%07d", this.component.getIdFormat());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIdFormatRefusesNull()
    {
        this.component.setIdFormat(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIdFormatRefusesEmptyFormat()
    {
        this.component.setIdFormat("");
    }

    @Test
    public void setIdFormatWorks()
    {
        this.component.setIdFormat("format");
        Assert.assertEquals("format", this.component.getIdFormat());
    }

    @Test
    public void newInstanceHasNameFormat()
    {
        Assert.assertEquals("$!{entity.identifier}", this.component.getNameFormat());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNameFormatRefusesNull()
    {
        this.component.setNameFormat(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNameFormatRefusesEmptyFormat()
    {
        this.component.setNameFormat("");
    }

    @Test
    public void setNameFormatWorks()
    {
        this.component.setNameFormat("format");
        Assert.assertEquals("format", this.component.getNameFormat());
    }

    @Test
    public void buildWithDefaultValuesWorks()
    {
        PrimaryEntityConfiguration result = this.component.build();
        Assert.assertEquals("%s%07d", result.getIdFormat());
        Assert.assertEquals("$!{entity.identifier}", result.getNameFormat());
        RecordConfiguration recordConfig = result.getRecordConfiguration();
        Assert.assertNotNull(recordConfig);
        Assert.assertTrue(recordConfig.getAllSections().isEmpty());
        Assert.assertTrue(recordConfig.getEnabledSections().isEmpty());
        Assert.assertTrue(recordConfig.getAllFields().isEmpty());
        Assert.assertTrue(recordConfig.getEnabledFields().isEmpty());
    }

    @Test
    public void buildWithSetValuesWorks()
    {
        RecordConfigurationBuilder rcb = mock(RecordConfigurationBuilder.class);
        RecordConfiguration rc = mock(RecordConfiguration.class);
        when(rcb.build()).thenReturn(rc);
        this.component.setRecordConfiguration(rcb);
        this.component.setIdFormat("idFormat");
        this.component.setNameFormat("nameFormat");

        PrimaryEntityConfiguration result = this.component.build();

        Assert.assertEquals("idFormat", result.getIdFormat());
        Assert.assertEquals("nameFormat", result.getNameFormat());
        RecordConfiguration recordConfig = result.getRecordConfiguration();
        Assert.assertSame(rc, recordConfig);
    }
}
