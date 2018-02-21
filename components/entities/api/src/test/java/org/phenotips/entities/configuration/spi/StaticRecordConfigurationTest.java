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
package org.phenotips.entities.configuration.spi;

import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordSection;

import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for {@link StaticRecordConfiguration}.
 *
 * @version $Id$
 */
public class StaticRecordConfigurationTest
{
    private List<RecordSection> sections;

    @Mock
    private StaticRecordSection sectionA;

    @Mock
    private StaticRecordSection sectionB;

    @Mock
    private StaticRecordSection sectionC;

    @Mock
    private RecordElement elementA;

    @Mock
    private RecordElement elementB;

    @Mock
    private RecordElement elementC;

    @Mock
    private RecordElement elementD;

    @Mock
    private RecordElement elementE;

    private DocumentReference classReference = new DocumentReference("instance", "PhenoTips", "EntityClass");

    private ClassPropertyReference field1 = new ClassPropertyReference("P1", this.classReference);

    private ClassPropertyReference field2 = new ClassPropertyReference("P2", this.classReference);

    private ClassPropertyReference field3 = new ClassPropertyReference("P3", this.classReference);

    private ClassPropertyReference field4 = new ClassPropertyReference("P4", this.classReference);

    private ClassPropertyReference field5 = new ClassPropertyReference("P5", this.classReference);

    private ClassPropertyReference field6 = new ClassPropertyReference("P6", this.classReference);

    private ClassPropertyReference field7 = new ClassPropertyReference("P7", this.classReference);

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        this.sections = Arrays.asList(this.sectionA, this.sectionB, this.sectionC);

        when(this.elementA.isEnabled()).thenReturn(true);
        when(this.elementA.getDisplayedFields()).thenReturn(Collections.singletonList(this.field1));
        when(this.elementB.isEnabled()).thenReturn(false);
        when(this.elementB.getDisplayedFields()).thenReturn(Collections.singletonList(this.field2));
        when(this.elementC.isEnabled()).thenReturn(false);
        when(this.elementC.getDisplayedFields()).thenReturn(Collections.singletonList(this.field3));
        when(this.elementD.isEnabled()).thenReturn(true);
        when(this.elementD.getDisplayedFields()).thenReturn(Collections.singletonList(this.field4));
        when(this.elementE.isEnabled()).thenReturn(true);
        when(this.elementE.getDisplayedFields()).thenReturn(Arrays.asList(this.field5, this.field6, this.field7));

        when(this.sectionA.isEnabled()).thenReturn(true);
        when(this.sectionA.getAllElements()).thenReturn(Collections.singletonList(this.elementA));
        when(this.sectionA.getEnabledElements()).thenReturn(Collections.singletonList(this.elementA));
        when(this.sectionB.isEnabled()).thenReturn(false);
        when(this.sectionB.getAllElements()).thenReturn(Collections.singletonList(this.elementB));
        when(this.sectionB.getEnabledElements()).thenReturn(Collections.emptyList());
        when(this.sectionC.isEnabled()).thenReturn(true);
        when(this.sectionC.getAllElements()).thenReturn(Arrays.asList(this.elementC, this.elementD, this.elementE));
        when(this.sectionC.getEnabledElements()).thenReturn(Arrays.asList(this.elementD, this.elementE));
    }

    @Test
    public void getAllSectionsReturnsEmptyListWhenNullProvided()
    {
        final List<RecordSection> results = new StaticRecordConfiguration(null).getAllSections();
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getEnabledSectionsReturnsEmptyListWhenNullProvided()
    {
        final List<RecordSection> results = new StaticRecordConfiguration(null).getEnabledSections();
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void getEnabledSectionsNoneEnabled()
    {
        when(this.sectionA.isEnabled()).thenReturn(false);
        when(this.sectionC.isEnabled()).thenReturn(false);

        final List<RecordSection> enabledSections = new StaticRecordConfiguration(this.sections).getEnabledSections();
        Assert.assertTrue(enabledSections.isEmpty());
    }

    @Test
    public void getEnabledSectionsSomeEnabled()
    {
        final List<RecordSection> enabledSections = new StaticRecordConfiguration(this.sections).getEnabledSections();
        Assert.assertEquals(2, enabledSections.size());
        Assert.assertTrue(enabledSections.contains(this.sectionA));
        Assert.assertFalse(enabledSections.contains(this.sectionB));
        Assert.assertTrue(enabledSections.contains(this.sectionC));
    }

    @Test
    public void getEnabledFieldNames()
    {
        final List<ClassPropertyReference> fields = new StaticRecordConfiguration(this.sections).getEnabledFields();
        Assert.assertEquals(5, fields.size());
        List<ClassPropertyReference> expected =
            Arrays.asList(this.field1, this.field4, this.field5, this.field6, this.field7);
        Assert.assertEquals(expected, fields);
    }

    @Test
    public void getAllFields()
    {
        final List<ClassPropertyReference> fields = new StaticRecordConfiguration(this.sections).getAllFields();
        Assert.assertEquals(7, fields.size());
        List<ClassPropertyReference> expected =
            Arrays.asList(this.field1, this.field2, this.field3, this.field4, this.field5, this.field6, this.field7);
        Assert.assertEquals(expected, fields);
    }

    @Test
    public void toStringListsContent()
    {
        Assert.assertEquals("sectionA, sectionC", new StaticRecordConfiguration(this.sections).toString());
    }
}
