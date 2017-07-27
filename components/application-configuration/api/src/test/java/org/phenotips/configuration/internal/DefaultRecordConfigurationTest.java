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
package org.phenotips.configuration.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.spi.UIXRecordSection;

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
 * Tests for {@link DefaultRecordConfiguration}.
 *
 * @version $Id $
 */
public class DefaultRecordConfigurationTest
{
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private RecordConfiguration component;

    private List<RecordSection> sections;

    @Mock
    private UIXRecordSection sectionA;

    @Mock
    private UIXRecordSection sectionB;

    @Mock
    private UIXRecordSection sectionC;

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

    @SuppressWarnings("deprecation")
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        this.component = new DefaultRecordConfiguration();
        this.sections = Arrays.asList((RecordSection) this.sectionA, this.sectionB, this.sectionC);

        when(this.elementA.isEnabled()).thenReturn(true);
        when(this.elementA.getDisplayedFields()).thenReturn(Collections.singletonList("a"));
        when(this.elementA.containsPrivateIdentifiableInformation()).thenReturn(Boolean.TRUE);
        when(this.elementB.isEnabled()).thenReturn(false);
        when(this.elementB.getDisplayedFields()).thenReturn(Collections.singletonList("b"));
        when(this.elementC.isEnabled()).thenReturn(false);
        when(this.elementC.getDisplayedFields()).thenReturn(Collections.singletonList("c"));
        when(this.elementD.isEnabled()).thenReturn(true);
        when(this.elementD.getDisplayedFields()).thenReturn(Collections.singletonList("d"));
        when(this.elementE.isEnabled()).thenReturn(true);
        when(this.elementE.getDisplayedFields()).thenReturn(Collections.singletonList("e"));

        when(this.sectionA.isEnabled()).thenReturn(true);
        when(this.sectionA.getAllElements()).thenReturn(Collections.singletonList(this.elementA));
        when(this.sectionA.getEnabledElements()).thenCallRealMethod();
        when(this.sectionB.isEnabled()).thenReturn(false);
        when(this.sectionB.getAllElements()).thenReturn(Collections.singletonList(this.elementB));
        when(this.sectionB.getEnabledElements()).thenCallRealMethod();
        when(this.sectionC.isEnabled()).thenReturn(true);
        when(this.sectionC.getAllElements()).thenReturn(Arrays.asList(this.elementC, this.elementD, this.elementE));
        when(this.sectionC.getEnabledElements()).thenCallRealMethod();
    }

    @Test
    public void getAllSectionsNoSectionsSet()
    {
        final List<RecordSection> results = this.component.getAllSections();
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void checkSectionsSetAndRetrievedCorrectly()
    {
        this.component.setSections(this.sections);
        final List<RecordSection> results = this.component.getAllSections();
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(this.sectionA, results.get(0));
        Assert.assertTrue(results.contains(this.sectionB));
        Assert.assertTrue(results.contains(this.sectionC));
    }

    @Test
    public void getEnabledSectionsNoneEnabled()
    {
        when(this.sectionA.isEnabled()).thenReturn(false);
        when(this.sectionC.isEnabled()).thenReturn(false);

        this.component.setSections(this.sections);

        final List<RecordSection> enabledSections = this.component.getEnabledSections();
        Assert.assertTrue(enabledSections.isEmpty());
    }

    @Test
    public void getEnabledSectionsSomeEnabled()
    {
        this.component.setSections(this.sections);

        final List<RecordSection> enabledSections = this.component.getEnabledSections();
        Assert.assertEquals(2, enabledSections.size());
        Assert.assertTrue(enabledSections.contains(this.sectionA));
        Assert.assertFalse(enabledSections.contains(this.sectionB));
        Assert.assertTrue(enabledSections.contains(this.sectionC));
    }

    @Test
    public void getEnabledFieldNames()
    {
        this.component.setSections(this.sections);

        final List<String> fields = this.component.getEnabledFieldNames();
        Assert.assertEquals(3, fields.size());
        List<String> expected = Arrays.asList("a", "d", "e");
        Assert.assertEquals(expected, fields);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getEnabledNonIdentifiableFieldNames()
    {
        this.component.setSections(this.sections);

        final List<String> fields = this.component.getEnabledNonIdentifiableFieldNames();
        Assert.assertEquals(2, fields.size());
        List<String> expected = Arrays.asList("d", "e");
        Assert.assertEquals(expected, fields);
    }

    @Test
    public void getAllFieldNames()
    {
        this.component.setSections(this.sections);

        final List<String> fields = this.component.getAllFieldNames();
        Assert.assertEquals(5, fields.size());
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        Assert.assertEquals(expected, fields);
    }

    @Test
    public void noDefaultPhenotypeMapping()
    {
        Assert.assertNull(this.component.getPhenotypeMapping());
    }

    @Test
    public void setPhenotypeMappingWorks()
    {
        DocumentReference mapping = new DocumentReference("xwiki", "PhenoTips", "Default phenotype mapping");
        this.component.setPhenotypeMapping(mapping);
        Assert.assertSame(mapping, this.component.getPhenotypeMapping());
    }

    @Test
    public void getISODateFormat()
    {
        Assert.assertEquals(DATE_FORMAT, this.component.getISODateFormat());
    }

    @Test
    public void isoFormatUsedAsDefaultDateFormat()
    {
        Assert.assertEquals(DATE_FORMAT, this.component.getDateOfBirthFormat());
    }

    @Test
    public void setDateOfBirthFormatWorks()
    {
        this.component.setDateOfBirthFormat("MMMM yyyy");
        Assert.assertEquals("MMMM yyyy", this.component.getDateOfBirthFormat());
    }

    @Test
    public void toStringListsContent()
    {
        this.component.setSections(this.sections);

        Assert.assertEquals("sectionA, sectionC", this.component.toString());
    }
}
