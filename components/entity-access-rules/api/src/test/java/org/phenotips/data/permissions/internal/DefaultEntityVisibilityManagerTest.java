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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.permissions.internal.visibility.MockVisibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link EntityVisibilityManager} implementation, {@link DefaultEntityVisibilityManager}.
 *
 * @version $Id$
 */
public class DefaultEntityVisibilityManagerTest
{
    private static final String VISIBILITY = "visibility";

    private static final String PUBLIC = "public";

    private static final String PRIVATE = "private";

    private static final String HIDDEN = "hidden";

    private static final String UNKNOWN = "unknown";

    private static final String OPEN = "open";

    private static final String WIKI_NAME = "xwiki";

    private static final String DATA = "data";

    private static final String PATIENT_1_ID = "P0000001";

    private static final String PATIENT_2_ID = "P0000002";

    private static final String PATIENT_3_ID = "P0000003";

    private static final String PHENOTIPS = "PhenoTips";

    private static final String VISIBILITY_TITLE = "Visibility";

    private static final AccessLevel EDIT_ACCESS = new EditAccessLevel();

    private static final AccessLevel VIEW_ACCESS = new ViewAccessLevel();

    private static final AccessLevel NO_ACCESS = new NoAccessLevel();

    /** The patient used for tests. */
    private static final DocumentReference PATIENT_REFERENCE_1 = new DocumentReference(WIKI_NAME, DATA, PATIENT_1_ID);

    private static final DocumentReference PATIENT_REFERENCE_2 = new DocumentReference(WIKI_NAME, DATA, PATIENT_2_ID);

    private static final DocumentReference PATIENT_REFERENCE_3 = new DocumentReference(WIKI_NAME, DATA, PATIENT_3_ID);

    private static final DocumentReference VISIBILITY_CLASS_1 = new DocumentReference(WIKI_NAME, PHENOTIPS,
        VISIBILITY_TITLE);

    private static final DocumentReference VISIBILITY_CLASS_2 = new DocumentReference(WIKI_NAME, PHENOTIPS,
        VISIBILITY_TITLE);

    private static final DocumentReference VISIBILITY_CLASS_3 = new DocumentReference(WIKI_NAME, PHENOTIPS,
        VISIBILITY_TITLE);

    private static final Visibility PUBLIC_VISIBILITY = new MockVisibility(PUBLIC, 50, VIEW_ACCESS, false);

    private static final Visibility PRIVATE_VISIBILITY = new MockVisibility(PRIVATE, 0, NO_ACCESS, false);

    private static final Visibility HIDDEN_VISIBILITY = new MockVisibility(HIDDEN, -1, NO_ACCESS, false);

    private static final Visibility DISABLED_OPEN_VISIBILITY = new MockVisibility(OPEN, 80, EDIT_ACCESS, true);

    @Rule
    public MockitoComponentMockingRule<EntityVisibilityManager> mocker =
        new MockitoComponentMockingRule<>(DefaultEntityVisibilityManager.class);

    @Mock
    private Patient entity1;

    @Mock
    private Patient entity2;

    @Mock
    private Patient entity3;

    @Mock
    private XWikiDocument entityDoc1;

    @Mock
    private XWikiDocument entityDoc2;

    @Mock
    private XWikiDocument entityDoc3;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    private EntityVisibilityManager component;

    private PermissionsConfiguration config;

    private ComponentManager componentManager;

    private EntityAccessHelper helper;

    private Visibility privateVisibility;

    private ParameterizedType entityResolverType = new DefaultParameterizedType(null, DocumentReferenceResolver.class,
        EntityReference.class);

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        // Context.
        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);

        this.config = this.mocker.getInstance(PermissionsConfiguration.class);

        // Injected components.
        this.privateVisibility = this.mocker.getInstance(Visibility.class, PRIVATE);
        this.componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        this.helper = this.mocker.getInstance(EntityAccessHelper.class);

        // Injected private visibility.
        when(this.privateVisibility.isDisabled()).thenReturn(false);

        // Entity resolver.
        final DocumentReferenceResolver<EntityReference> partialEntityResolver =
            this.mocker.getInstance(this.entityResolverType, "currentmixed");
        when(partialEntityResolver.resolve(Visibility.CLASS_REFERENCE, PATIENT_REFERENCE_1)).thenReturn(
            VISIBILITY_CLASS_1);
        when(partialEntityResolver.resolve(Visibility.CLASS_REFERENCE, PATIENT_REFERENCE_2)).thenReturn(
            VISIBILITY_CLASS_2);
        when(partialEntityResolver.resolve(Visibility.CLASS_REFERENCE, PATIENT_REFERENCE_3)).thenReturn(
            VISIBILITY_CLASS_3);

        // Getting visibility classes.
        when(this.componentManager.getInstance(Visibility.class, PUBLIC)).thenReturn(PUBLIC_VISIBILITY);
        when(this.componentManager.getInstance(Visibility.class, PRIVATE)).thenReturn(PRIVATE_VISIBILITY);
        when(this.componentManager.getInstance(Visibility.class, HIDDEN)).thenReturn(HIDDEN_VISIBILITY);

        // Entity method calls.
        when(this.entity1.getDocumentReference()).thenReturn(PATIENT_REFERENCE_1);
        when(this.entity1.getXDocument()).thenReturn(this.entityDoc1);

        when(this.entity2.getDocumentReference()).thenReturn(PATIENT_REFERENCE_2);
        when(this.entity2.getXDocument()).thenReturn(this.entityDoc2);

        when(this.entity3.getDocumentReference()).thenReturn(PATIENT_REFERENCE_3);
        when(this.entity3.getXDocument()).thenReturn(this.entityDoc3);
    }

    /** Basic tests for {@link EntityVisibilityManager#getVisibility(PrimaryEntity)}. */
    @Test
    public void getVisibility()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.component.resolveVisibility(PUBLIC)).thenReturn(PUBLIC_VISIBILITY);
        Assert.assertSame(PUBLIC_VISIBILITY, this.component.getVisibility(this.entity1));
    }

    /**
     * {@link EntityVisibilityManager#getVisibility(PrimaryEntity)} returns private visibility when the visibility isn't
     * specified.
     */
    @Test
    public void getVisibilityWithMissingVisibility()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(null);
        Assert.assertEquals(this.privateVisibility, this.component.getVisibility(this.entity1));

        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY))
            .thenReturn(StringUtils.EMPTY);
        Assert.assertEquals(this.privateVisibility, this.component.getVisibility(this.entity1));

        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY))
            .thenReturn(StringUtils.SPACE);
        Assert.assertEquals(this.privateVisibility, this.component.getVisibility(this.entity1));
    }

    /**
     * {@link EntityVisibilityManager#getVisibility(PrimaryEntity)} returns private visibility when the visibility isn't
     * valid.
     */
    @Test
    public void getVisibilityWithInvalidVisibility()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn("invalid");
        Assert.assertEquals(this.privateVisibility, this.component.getVisibility(this.entity1));
    }

    /** Basic tests for {@link EntityVisibilityManager#setVisibility(PrimaryEntity, Visibility)}. */
    @Test
    public void setVisibility() throws XWikiException
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PRIVATE);
        Assert.assertTrue(this.component.setVisibility(this.entity1, PUBLIC_VISIBILITY));
        verify(this.helper).setProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY, PUBLIC);
        verify(this.xwiki).saveDocument(this.entityDoc1, "Set visibility: " + PUBLIC, true, this.context);
    }

    /**
     * {@link EntityVisibilityManager#setVisibility(PrimaryEntity, Visibility)} visibility not set when changed to the
     * same one.
     */
    @Test
    public void setVisibilityToTheSameValue() throws XWikiException
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PRIVATE);
        Assert.assertTrue(this.component.setVisibility(this.entity1, PRIVATE_VISIBILITY));
        verify(this.helper, never()).setProperty(any(), any(), any(), any());
        verify(this.xwiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    /** Basic tests for {@link EntityVisibilityManager#setVisibility(PrimaryEntity, Visibility)}. */
    @Test
    public void setVisibilityWithNullVisibility() throws XWikiException
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PRIVATE);
        Assert.assertTrue(this.component.setVisibility(this.entity1, null));
        verify(this.helper).setProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY, StringUtils.EMPTY);
        verify(this.xwiki).saveDocument(this.entityDoc1, "Set visibility: " + StringUtils.EMPTY, true, this.context);
    }

    /** Basic tests for {@link EntityVisibilityManager#setVisibility(PrimaryEntity, Visibility)}. */
    @Test
    public void setVisibilityWithFailure()
    {
        doThrow(new RuntimeException()).when(this.helper).setProperty(eq(this.entityDoc1), any(), any(), anyString());
        Assert.assertFalse(this.component.setVisibility(this.entity1, PUBLIC_VISIBILITY));
    }

    /** Basic test for {@link EntityVisibilityManager#listVisibilityOptions()}. */
    @Test
    public void listVisibilityOptionsSkipsDisabledVisibilitiesAndReordersByPriority() throws ComponentLookupException
    {
        List<Visibility> visibilities = new ArrayList<>();
        visibilities.add(PUBLIC_VISIBILITY);
        visibilities.add(PRIVATE_VISIBILITY);
        visibilities.add(DISABLED_OPEN_VISIBILITY);
        when(this.componentManager.<Visibility>getInstanceList(Visibility.class)).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.component.listVisibilityOptions();
        Assert.assertEquals(2, returnedVisibilities.size());
        Iterator<Visibility> it = returnedVisibilities.iterator();
        Assert.assertSame(PRIVATE_VISIBILITY, it.next());
        Assert.assertSame(PUBLIC_VISIBILITY, it.next());
    }

    /** Basic test for {@link EntityVisibilityManager#listAllVisibilityOptions()}. */
    @Test
    public void listAllVisibilityOptionsReordersByPriority() throws ComponentLookupException
    {
        List<Visibility> visibilities = new ArrayList<>();
        visibilities.add(PUBLIC_VISIBILITY);
        visibilities.add(PRIVATE_VISIBILITY);
        visibilities.add(DISABLED_OPEN_VISIBILITY);
        when(this.componentManager.<Visibility>getInstanceList(Visibility.class)).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.component.listAllVisibilityOptions();
        Assert.assertEquals(3, returnedVisibilities.size());
        Iterator<Visibility> it = returnedVisibilities.iterator();
        Assert.assertSame(PRIVATE_VISIBILITY, it.next());
        Assert.assertSame(PUBLIC_VISIBILITY, it.next());
        Assert.assertSame(DISABLED_OPEN_VISIBILITY, it.next());
    }

    /**
     * {@link EntityVisibilityManager#listVisibilityOptions()} returns an empty list when no implementations available.
     */
    @Test
    public void listVisibilityOptionsWithNoComponentsEmptyList() throws ComponentLookupException
    {
        when(this.componentManager.<Visibility>getInstanceList(Visibility.class)).thenReturn(Collections.emptyList());
        Collection<Visibility> returnedVisibilities = this.component.listVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    /**
     * {@link EntityVisibilityManager#listAllVisibilityOptions()} returns an empty list when no implementations
     * available.
     */
    @Test
    public void listAllVisibilityOptionsWithNoComponentsReturnsEmptyList() throws ComponentLookupException
    {
        when(this.componentManager.<Visibility>getInstanceList(Visibility.class)).thenReturn(Collections.emptyList());
        Collection<Visibility> returnedVisibilities = this.component.listAllVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    /**
     * {@link EntityVisibilityManager#listVisibilityOptions()} returns an empty list when all visibilities are disabled.
     */
    @Test
    public void listVisibilityOptionsWithOnlyDisabledVisibilitiesReturnsEmptyList() throws ComponentLookupException
    {
        List<Visibility> visibilities = Collections.singletonList(DISABLED_OPEN_VISIBILITY);
        when(this.componentManager.<Visibility>getInstanceList(Visibility.class)).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.component.listVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    /**
     * {@link EntityVisibilityManager#listVisibilityOptions()} returns an empty list when looking up components fails.
     */
    @Test
    public void listVisibilityOptionsWithLookupExceptions() throws ComponentLookupException
    {
        when(this.componentManager.<Visibility>getInstanceList(Visibility.class))
            .thenThrow(new ComponentLookupException("None"));
        Collection<Visibility> returnedVisibilities = this.component.listVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    /**
     * {@link EntityVisibilityManager#listAllVisibilityOptions()} returns an empty list when looking up components
     * fails.
     */
    @Test
    public void listAllVisibilityOptionsWithLookupExceptions() throws ComponentLookupException
    {
        when(this.componentManager.<Visibility>getInstanceList(Visibility.class))
            .thenThrow(new ComponentLookupException("None"));
        Collection<Visibility> returnedVisibilities = this.component.listAllVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    @Test
    public void getDefaultVisibilityForwardsCalls()
    {
        when(this.config.getDefaultVisibility()).thenReturn(PUBLIC);
        Assert.assertSame(PUBLIC_VISIBILITY, this.component.getDefaultVisibility());
    }

    @Test
    public void getDefaultVisibilityReturnsPrivateVisibilityIfNoDefaultIsConfigured()
    {
        when(this.config.getDefaultVisibility()).thenReturn(null);
        Assert.assertSame(this.privateVisibility, this.component.getDefaultVisibility());
    }

    @Test
    public void getDefaultVisibilityReturnsPrivateVisibilityIfDefaultVisibilityIsInvalid()
    {
        when(this.config.getDefaultVisibility()).thenReturn("wrong");
        Assert.assertSame(this.privateVisibility, this.component.getDefaultVisibility());
    }

    /** {@link EntityVisibilityManager#resolveVisibility(String)} returns the right implementation. */
    @Test
    public void resolveVisibility()
    {
        Assert.assertSame(PUBLIC_VISIBILITY, this.component.resolveVisibility(PUBLIC));
    }

    /**
     * {@link EntityVisibilityManager#resolveVisibility(String)} returns private visibility if a null or blank
     * visibility is requested.
     */
    @Test
    public void resolveVisibilityWithNoAccess()
    {
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility(null));
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility(StringUtils.EMPTY));
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility(StringUtils.SPACE));
    }

    /**
     * {@link EntityVisibilityManager#resolveVisibility(String)} returns private visibility if an unknown visibility is
     * requested.
     */
    @Test
    public void resolveVisibilityWithUnknownVisibilityTest() throws ComponentLookupException
    {
        when(this.componentManager.getInstance(Visibility.class, UNKNOWN))
            .thenThrow(new ComponentLookupException("No such component"));
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility(UNKNOWN));
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Collection, Visibility)} returns empty collection if input
     * entities are empty.
     */
    @Test
    public void filterCollectionByVisibilityWithEmptyInputReturnsEmptyCollection()
    {
        Collection<? extends PrimaryEntity> result =
            this.component.filterByVisibility(Collections.emptyList(), PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns empty iterator if input
     * iterator is empty.
     */
    @Test
    public void filterIteratorByVisibilityWithEmptyInputReturnsEmptyIterator()
    {
        Iterator<? extends PrimaryEntity> result =
            this.component.filterByVisibility(Collections.emptyIterator(), PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Collection, Visibility)} returns empty collection if input
     * entities collection is null.
     */
    @Test
    public void filterCollectionByVisibilityWithNullInputReturnsEmptyCollection()
    {
        Collection<? extends PrimaryEntity> result =
            this.component.filterByVisibility((Collection<PrimaryEntity>) null, PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns empty iterator if input
     * entities iterator is null.
     */
    @Test
    public void filterIteratorByVisibilityWithNullInputReturnsEmptyIterator()
    {
        Iterator<? extends PrimaryEntity> result =
            this.component.filterByVisibility((Iterator<PrimaryEntity>) null, PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Collection, Visibility)} returns a collection of correctly
     * filtered entities.
     */
    @Test
    public void filterCollectionByVisibilityWithValidInputFiltersPrimaryEntities()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, this.entity3);

        Collection<? extends PrimaryEntity> result = this.component.filterByVisibility(input, PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<? extends PrimaryEntity> it = result.iterator();
        Assert.assertSame(this.entity1, it.next());
        Assert.assertSame(this.entity3, it.next());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns an iterator with correctly
     * filtered entities.
     */
    @Test
    public void filterIteratorByVisibilityWithValidInputFiltersPrimaryEntities()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, this.entity3);

        Iterator<? extends PrimaryEntity> result =
            this.component.filterByVisibility(input.iterator(), PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(this.entity1, result.next());
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(this.entity3, result.next());
        Assert.assertFalse(result.hasNext());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Collection, Visibility)} returns a collection of correctly
     * filtered entities when input contains a null entity.
     */
    @Test
    public void filterCollectionByVisibilityWithNullInInputFiltersValidPrimaryEntities()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, this.entity3, null);

        Collection<? extends PrimaryEntity> result = this.component.filterByVisibility(input, PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<? extends PrimaryEntity> it = result.iterator();
        Assert.assertSame(this.entity1, it.next());
        Assert.assertSame(this.entity3, it.next());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns an iterator with correctly
     * filtered entities when input contains a null entity.
     */
    @Test
    public void filterIteratorByVisibilityWithNullInInputFiltersValidPrimaryEntitys()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, this.entity3, null);

        Iterator<? extends PrimaryEntity> result =
            this.component.filterByVisibility(input.iterator(), PRIVATE_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(this.entity1, result.next());
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(this.entity3, result.next());
        Assert.assertFalse(result.hasNext());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Collection, Visibility)} returns an empty collection when input
     * doesn't match requested visibility.
     */
    @Test
    public void filterCollectionByVisibilityWithNonMatchingInputReturnsEmptyList()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, this.entity3);

        Collection<? extends PrimaryEntity> result = this.component.filterByVisibility(input, PUBLIC_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns an empty iterator when input
     * doesn't match requested visibility.
     */
    @Test
    public void filterIteratorByVisibilityWithNonMatchingInputReturnsEmptyIterator()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, this.entity3);

        Iterator<? extends PrimaryEntity> result =
            this.component.filterByVisibility(input.iterator(), PUBLIC_VISIBILITY);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Collection, Visibility)} returns the input unchanged with
     * null threshold.
     */
    @Test
    public void filterCollectionByVisibilityWithNullThresholdReturnsUnfilteredList()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, null, this.entity3);

        Collection<? extends PrimaryEntity> result = this.component.filterByVisibility(input, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());
        Iterator<? extends PrimaryEntity> it = result.iterator();
        Assert.assertSame(this.entity1, it.next());
        Assert.assertSame(this.entity2, it.next());
        Assert.assertNull(it.next());
        Assert.assertSame(this.entity3, it.next());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns the input unchanged with
     * null threshold.
     */
    @Test
    public void filterIteratorByVisibilityWithNullThresholdReturnsUnfilteredIterator()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, null, this.entity3);

        Iterator<? extends PrimaryEntity> result = this.component.filterByVisibility(input.iterator(), null);
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(this.entity1, result.next());
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(this.entity2, result.next());
        Assert.assertTrue(result.hasNext());
        Assert.assertNull(result.next());
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(this.entity3, result.next());
        Assert.assertFalse(result.hasNext());
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns an read-only iterator.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void filterIteratorByVisibilityReturnsReadonlyIterator()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);
        when(this.helper.getStringProperty(this.entityDoc2, VISIBILITY_CLASS_2, VISIBILITY)).thenReturn(HIDDEN);
        when(this.helper.getStringProperty(this.entityDoc3, VISIBILITY_CLASS_3, VISIBILITY)).thenReturn(PRIVATE);

        Collection<PrimaryEntity> input = Arrays.asList(this.entity1, this.entity2, null, this.entity3);

        Iterator<? extends PrimaryEntity> result =
            this.component.filterByVisibility(input.iterator(), PRIVATE_VISIBILITY);
        Assert.assertSame(this.entity1, result.next());
        result.remove();
    }

    /**
     * {@link EntityVisibilityManager#filterByVisibility(Iterator, Visibility)} returns a correct iterator.
     */
    @Test(expected = NoSuchElementException.class)
    public void filterIteratorByVisibilityReturnsCorrectIterator()
    {
        when(this.helper.getStringProperty(this.entityDoc1, VISIBILITY_CLASS_1, VISIBILITY)).thenReturn(PUBLIC);

        Collection<PrimaryEntity> input = Collections.singletonList(this.entity1);

        Iterator<? extends PrimaryEntity> result =
            this.component.filterByVisibility(input.iterator(), PRIVATE_VISIBILITY);
        Assert.assertSame(this.entity1, result.next());
        Assert.assertFalse(result.hasNext());
        result.next();
    }
}
