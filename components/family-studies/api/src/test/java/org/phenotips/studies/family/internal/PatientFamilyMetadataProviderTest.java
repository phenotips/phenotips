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
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.entities.PrimaryEntityMetadataProvider;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiException;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientFamilyMetadataProvider} component.
 *
 * @version $Id$
 */
public class PatientFamilyMetadataProviderTest
{
    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityMetadataProvider> mocker =
        new MockitoComponentMockingRule<>(PatientFamilyMetadataProvider.class);

    @Mock
    private Patient patient;

    @Mock
    private Family family;

    private FamilyRepository familyRepository;

    @Before
    public void setup() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        when(this.family.getId()).thenReturn("FAM0001234");

        this.familyRepository = this.mocker.getInstance(FamilyRepository.class);
    }

    @Test
    public void metadataReportedCorrectly() throws ComponentLookupException
    {
        when(this.familyRepository.getFamilyForPatient(this.patient)).thenReturn(this.family);
        Map<String, Object> result = this.mocker.getComponentUnderTest().provideMetadata(this.patient);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("FAM0001234", result.get("family"));
    }

    @Test
    public void emptyMapReturnedIfFamilyUnknown() throws ComponentLookupException
    {
        when(this.familyRepository.getFamilyForPatient(this.patient)).thenReturn(null);
        Map<String, Object> result = this.mocker.getComponentUnderTest().provideMetadata(this.patient);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void emptyMapReturnedForNonPatient() throws ComponentLookupException
    {
        Map<String, Object> result = this.mocker.getComponentUnderTest().provideMetadata(this.family);
        Assert.assertTrue(result.isEmpty());
        Mockito.verifyZeroInteractions(this.familyRepository);
    }
}
