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
import org.phenotips.entities.configuration.RecordConfiguration;
import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordSection;
import org.phenotips.entities.configuration.spi.RecordConfigurationBuilder;
import org.phenotips.entities.configuration.spi.RecordSectionBuilder;

import org.xwiki.model.reference.ClassPropertyReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link RecordConfigurationBuilder} implementation, {@link DefaultRecordConfigurationBuilder}.
 *
 * @version $Id$
 */
public class DefaultRecordConfigurationBuilderTest
{
    private RecordConfigurationBuilder component;

    @Mock
    private PrimaryEntityManager<PrimaryEntity> entityManager;

    @Mock
    private RecordSectionBuilder rsb1;

    @Mock
    private RecordSectionBuilder rsb2;

    @Mock
    private RecordSection rs1;

    @Mock
    private RecordSection rs2;

    @Mock
    private RecordElement re1;

    @Mock
    private RecordElement re2;

    @Mock
    private RecordElement re3;

    @Mock
    private ClassPropertyReference f1;

    @Mock
    private ClassPropertyReference f2;

    @Mock
    private ClassPropertyReference f3;

    @Mock
    private ClassPropertyReference f4;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(this.rsb1.build()).thenReturn(this.rs1);
        when(this.rs1.isEnabled()).thenReturn(false);
        when(this.rs1.getAllElements()).thenReturn(Collections.singletonList(this.re1));
        when(this.rs1.getEnabledElements()).thenReturn(Collections.singletonList(this.re1));
        when(this.re1.getDisplayedFields()).thenReturn(Collections.singletonList(this.f1));
        when(this.rsb2.build()).thenReturn(this.rs2);
        when(this.rs2.isEnabled()).thenReturn(true);
        when(this.rs2.getAllElements()).thenReturn(Arrays.asList(this.re2, this.re3));
        when(this.rs2.getEnabledElements()).thenReturn(Collections.singletonList(this.re3));
        when(this.re2.getDisplayedFields()).thenReturn(Collections.singletonList(this.f2));
        when(this.re3.getDisplayedFields()).thenReturn(Arrays.asList(this.f3, this.f4));
        this.component = new DefaultRecordConfigurationBuilder();
    }

    @Test
    public void newInstanceHasEmptySectionList()
    {
        Assert.assertNotNull(this.component.getAllSections());
        Assert.assertTrue("%s%07d", this.component.getAllSections().isEmpty());
    }

    @Test
    public void setSectionsWorks()
    {
        this.component.setSections(Arrays.asList(this.rsb1, this.rsb2));
        Assert.assertEquals(Arrays.asList(this.rsb1, this.rsb2), this.component.getAllSections());
    }

    @Test
    public void setSectionsCopiesInput()
    {
        List<RecordSectionBuilder> sections = new LinkedList<>();
        sections.add(this.rsb1);
        this.component.setSections(sections);
        Assert.assertEquals(Arrays.asList(this.rsb1), this.component.getAllSections());
        sections.remove(0);
        Assert.assertEquals(Arrays.asList(this.rsb1), this.component.getAllSections());
        sections.add(this.rsb2);
        Assert.assertEquals(Arrays.asList(this.rsb1), this.component.getAllSections());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setSectionsReturnsReadOnlyOutput()
    {
        List<RecordSectionBuilder> sections = new LinkedList<>();
        sections.add(this.rsb1);
        this.component.setSections(sections);
        sections = this.component.getAllSections();
        sections.remove(0);
    }

    @Test
    public void setSectionsTransformsNullInputIntoEmptyList()
    {
        this.component.setSections(Arrays.asList(this.rsb1, this.rsb2));
        Assert.assertEquals(Arrays.asList(this.rsb1, this.rsb2), this.component.getAllSections());
        this.component.setSections(null);
        Assert.assertNotNull(this.component.getAllSections());
        Assert.assertTrue(this.component.getAllSections().isEmpty());
    }

    @Test
    public void buildWithDefaultValuesWorks()
    {
        RecordConfiguration result = this.component.build();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getAllSections().isEmpty());
        Assert.assertTrue(result.getEnabledSections().isEmpty());
        Assert.assertTrue(result.getAllFields().isEmpty());
        Assert.assertTrue(result.getEnabledFields().isEmpty());
    }

    @Test
    public void buildWithSetValuesWorks()
    {
        this.component.setSections(Arrays.asList(this.rsb1, this.rsb2));
        RecordConfiguration result = this.component.build();
        Assert.assertNotNull(result);
        Assert.assertEquals(Arrays.asList(this.rs1, this.rs2), result.getAllSections());
        Assert.assertEquals(Arrays.asList(this.rs2), result.getEnabledSections());
        Assert.assertEquals(Arrays.asList(this.f1, this.f2, this.f3, this.f4), result.getAllFields());
        Assert.assertEquals(Arrays.asList(this.f3, this.f4), result.getEnabledFields());
    }
}
