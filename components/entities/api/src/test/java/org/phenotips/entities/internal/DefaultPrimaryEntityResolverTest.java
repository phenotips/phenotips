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
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPrimaryEntityResolver}.
 */
public class DefaultPrimaryEntityResolverTest
{
    private static final String XWIKI = "xwiki";

    private static final String DATA = "data";

    private static final String FAMILY_ID_PREFIX = "FAM";

    private static final String PATIENT_ID_PREFIX = "P";

    private static final String INVALID_ID = "09FAM0909";

    private static final String NONEXISTENT_ID = "09FAM0909";

    private static final String NUMERIC_ID = "123";

    private static final String FAMILY_1_ID = "FAM001";

    private static final String PATIENT_1_ID = "xwiki:data.P001";

    private static final String PATIENT_2_ID = "P002";

    private static final String FAMILIES = "families";

    private static final String PATIENTS = "patients";

    private static final String WRONG = "wrong";

    private static final DocumentReference P002 = new DocumentReference(XWIKI, DATA, PATIENT_2_ID);

    private static final DocumentReference P001 = new DocumentReference(XWIKI, DATA, "P001");

    private static final DocumentReference FAM001 = new DocumentReference(XWIKI, DATA, FAMILY_1_ID);

    private static final DocumentReference NO_NAME = new DocumentReference(XWIKI, DATA, NUMERIC_ID);

    private static final DocumentReference ABC123123 = new DocumentReference(XWIKI, DATA, NONEXISTENT_ID);

    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityResolver> mocker =
        new MockitoComponentMockingRule<>(DefaultPrimaryEntityResolver.class);

    @Mock
    private PrimaryEntityManager familyResolver;

    @Mock
    private PrimaryEntityManager patientResolver;

    @Mock
    private PrimaryEntity patient1;

    @Mock
    private PrimaryEntity family1;

    private PrimaryEntityResolver component;

    private DocumentReferenceResolver<String> referenceResolver;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        when(this.familyResolver.getIdPrefix()).thenReturn(FAMILY_ID_PREFIX);
        when(this.patientResolver.getIdPrefix()).thenReturn(PATIENT_ID_PREFIX);

        when(this.familyResolver.getType()).thenReturn(FAMILIES);
        when(this.patientResolver.getType()).thenReturn(PATIENTS);

        when(this.familyResolver.get(FAMILY_1_ID)).thenReturn(this.family1);
        when(this.patientResolver.get(PATIENT_1_ID)).thenReturn(this.patient1);

        this.mocker.registerComponent(PrimaryEntityManager.class, "Family", this.familyResolver);
        this.mocker.registerComponent(PrimaryEntityManager.class, "Patient", this.patientResolver);

        this.referenceResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");

        this.component = this.mocker.getComponentUnderTest();

        when(this.referenceResolver.resolve(PATIENT_2_ID)).thenReturn(P002);
        when(this.referenceResolver.resolve(PATIENT_1_ID)).thenReturn(P001);
        when(this.referenceResolver.resolve(FAMILY_1_ID)).thenReturn(FAM001);
        when(this.referenceResolver.resolve(NUMERIC_ID)).thenReturn(NO_NAME);
        when(this.referenceResolver.resolve(NONEXISTENT_ID)).thenReturn(ABC123123);
    }

    @Test(expected = InitializationException.class)
    public void initializeThrowsExceptionIfSomePrimaryEntityManagerHasNullIdPrefix() throws Exception
    {
        when(this.familyResolver.getIdPrefix()).thenReturn(null);
        ((Initializable) this.component).initialize();
    }

    @Test(expected = InitializationException.class)
    public void initializeThrowsExceptionIfSomePrimaryEntityManagerHasEmptyIdPrefix() throws Exception
    {
        when(this.familyResolver.getIdPrefix()).thenReturn(StringUtils.EMPTY);
        ((Initializable) this.component).initialize();
    }

    @Test(expected = InitializationException.class)
    public void initializeThrowsExceptionIfSomePrimaryEntityManagerHasBlankIdPrefix() throws Exception
    {
        when(this.familyResolver.getIdPrefix()).thenReturn(StringUtils.SPACE);
        ((Initializable) this.component).initialize();
    }

    @Test(expected = InitializationException.class)
    public void initializeThrowsExceptionIfPrimaryEntityManagersHaveDuplicatePrefix() throws Exception
    {
        when(this.familyResolver.getIdPrefix()).thenReturn(PATIENT_ID_PREFIX);
        ((Initializable) this.component).initialize();
    }

    @Test
    public void resolveEntityReturnsNullWhenEntityIdIsNull()
    {
        Assert.assertNull(this.component.resolveEntity(null));
    }

    @Test
    public void resolveEntityReturnsNullWhenEntityIdIsEmpty()
    {
        Assert.assertNull(this.component.resolveEntity(StringUtils.EMPTY));
    }

    @Test
    public void resolveEntityReturnsNullWhenEntityIdIsBlank()
    {
        Assert.assertNull(this.component.resolveEntity(StringUtils.SPACE));
    }

    @Test
    public void resolveEntityReturnsNullWhenEntityIdCannotBeResolved()
    {
        when(this.referenceResolver.resolve(NUMERIC_ID)).thenReturn(null);
        Assert.assertNull(this.component.resolveEntity(NUMERIC_ID));
    }

    @Test
    public void resolveEntityReturnsNullWhenEntityIdHasInvalidFormat()
    {
        Assert.assertNull(this.component.resolveEntity(INVALID_ID));
        Assert.assertNull(this.component.resolveEntity(NUMERIC_ID));
        verify(this.familyResolver, never()).get(anyString());
        verify(this.patientResolver, never()).get(anyString());
        Assert.assertNull(this.component.resolveEntity(PATIENT_ID_PREFIX));
        Assert.assertNull(this.component.resolveEntity(FAMILY_ID_PREFIX));
    }

    @Test
    public void resolveEntityReturnsNullWhenPrimaryEntityManagerDoesNotExistForRequestedEntityType()
    {
        Assert.assertNull(this.component.resolveEntity(NONEXISTENT_ID));
        verify(this.familyResolver, never()).get(anyString());
        verify(this.patientResolver, never()).get(anyString());
    }

    @Test
    public void resolveEntityReturnsNullWhenRepositoryDoesNotHaveEntity()
    {
        Assert.assertNull(this.component.resolveEntity(PATIENT_2_ID));
        verify(this.familyResolver, never()).get(anyString());
        verify(this.patientResolver, times(1)).get(PATIENT_2_ID);
    }

    @Test
    public void resolveEntityReturnsCorrectEntityWhenPatientIdIsValid()
    {
        Assert.assertEquals(this.patient1, this.component.resolveEntity(PATIENT_1_ID));
        verify(this.familyResolver, never()).get(anyString());
        verify(this.patientResolver, times(1)).get(PATIENT_1_ID);
    }

    @Test
    public void resolveEntityReturnsCorrectEntityWhenFamilyIdIsValid()
    {
        Assert.assertEquals(this.family1, this.component.resolveEntity(FAMILY_1_ID));
        verify(this.familyResolver, times(1)).get(FAMILY_1_ID);
        verify(this.patientResolver, never()).get(anyString());
    }

    @Test
    public void getEntityManagerReturnsNullWhenEntityTypeIsNull()
    {
        Assert.assertNull(this.component.getEntityManager(null));
    }

    @Test
    public void getEntityManagerReturnsNullWhenEntityTypeIsEmpty()
    {
        Assert.assertNull(this.component.getEntityManager(StringUtils.EMPTY));
    }

    @Test
    public void getEntityManagerReturnsNullWhenEntityTypeIsBlank()
    {
        Assert.assertNull(this.component.getEntityManager(StringUtils.SPACE));
    }

    @Test
    public void getEntityManagerReturnsNullWhenEntityTypeIsInvalid()
    {
        Assert.assertNull(this.component.getEntityManager(WRONG));
    }

    @Test
    public void getEntityManagerReturnsCorrectManagerForEntityType()
    {
        Assert.assertEquals(this.familyResolver, this.component.getEntityManager(FAMILIES));
        Assert.assertEquals(this.patientResolver, this.component.getEntityManager(PATIENTS));
    }

    @Test
    public void hasEntityManagerReturnsFalseIfEntityTypeIsNull()
    {
        Assert.assertFalse(this.component.hasEntityManager(null));
    }

    @Test
    public void hasEntityManagerReturnsFalseIfEntityTypeIsEmpty()
    {
        Assert.assertFalse(this.component.hasEntityManager(StringUtils.EMPTY));
    }

    @Test
    public void hasEntityManagerReturnsFalseIfEntityTypeIsBlank()
    {
        Assert.assertFalse(this.component.hasEntityManager(StringUtils.SPACE));
    }

    @Test
    public void hasEntityManagerReturnsFalseIfEntityTypeIsInvalid()
    {
        Assert.assertFalse(this.component.hasEntityManager(WRONG));
    }

    @Test
    public void hasEntityManagerReturnsTrueIfEntityTypeIsValid()
    {
        Assert.assertTrue(this.component.hasEntityManager(FAMILIES));
        Assert.assertTrue(this.component.hasEntityManager(PATIENTS));
    }
}
