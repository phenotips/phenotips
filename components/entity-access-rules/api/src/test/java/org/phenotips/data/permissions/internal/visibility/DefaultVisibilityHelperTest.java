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
package org.phenotips.data.permissions.internal.visibility;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.PermissionsHelper;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultVisibilityHelper}.
 */
public class DefaultVisibilityHelperTest
{
    private static final String CONTEXT = "context";

    private static final String CURRENT_MIXED = "currentmixed";

    private static final String VISIBILITY = "visibility";

    private static final String HIDDEN = "hidden";

    private static final String PRIVATE = "private";

    private static final String PUBLIC = "public";

    private static final String OPEN = "open";

    private static final String NONE = "none";

    @Rule
    public MockitoComponentMockingRule<VisibilityHelper> mocker =
        new MockitoComponentMockingRule<>(DefaultVisibilityHelper.class);

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiContext context;

    @Mock
    private Patient patient1;

    @Mock
    private Patient patient2;

    @Mock
    private Patient patient3;

    @Mock
    private Patient patient4;

    @Mock
    private DocumentReference patientDocRef;

    @Mock
    private XWikiDocument patientXDoc1;

    @Mock
    private XWikiDocument patientXDoc2;

    @Mock
    private XWikiDocument patientXDoc3;

    @Mock
    private XWikiDocument patientXDoc4;

    @Mock
    private DocumentReference visibilityDocRef;

    private VisibilityHelper component;

    private Logger logger;

    private ComponentManager cm;

    private PermissionsHelper permissionsHelper;

    private PermissionsConfiguration configuration;

    private Visibility hiddenVisibility;

    private Visibility privateVisibility;

    private Visibility publicVisibility;

    private Visibility openVisibility;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.cm = this.mocker.getInstance(ComponentManager.class, CONTEXT);
        this.permissionsHelper = this.mocker.getInstance(PermissionsHelper.class);
        this.configuration = this.mocker.getInstance(PermissionsConfiguration.class);

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);

        final ParameterizedType entityResolverType = new DefaultParameterizedType(null,
            DocumentReferenceResolver.class, EntityReference.class);

        // Set up patient1.
        when(this.patient1.getDocumentReference()).thenReturn(this.patientDocRef);
        when(this.patient2.getDocumentReference()).thenReturn(this.patientDocRef);
        when(this.patient3.getDocumentReference()).thenReturn(this.patientDocRef);
        when(this.patient4.getDocumentReference()).thenReturn(this.patientDocRef);
        when(this.patient1.getXDocument()).thenReturn(this.patientXDoc1);
        when(this.patient2.getXDocument()).thenReturn(this.patientXDoc2);
        when(this.patient3.getXDocument()).thenReturn(this.patientXDoc3);
        when(this.patient4.getXDocument()).thenReturn(this.patientXDoc4);

        final DocumentReferenceResolver<EntityReference> partialEntityResolver =
            this.mocker.getInstance(entityResolverType, CURRENT_MIXED);
        when(partialEntityResolver.resolve(Visibility.CLASS_REFERENCE, this.patientDocRef))
            .thenReturn(this.visibilityDocRef);

        // Visibility for patient1.
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(PUBLIC);
        // Visibility for patient2.
        when(this.permissionsHelper.getStringProperty(this.patientXDoc2, this.visibilityDocRef, VISIBILITY))
            .thenReturn(PRIVATE);
        // Visibility for patient3.
        when(this.permissionsHelper.getStringProperty(this.patientXDoc3, this.visibilityDocRef, VISIBILITY))
            .thenReturn(HIDDEN);
        // Visibility for patient4.
        when(this.permissionsHelper.getStringProperty(this.patientXDoc4, this.visibilityDocRef, VISIBILITY))
            .thenReturn(NONE);

        // Visibility options.
        this.hiddenVisibility = spy(new HiddenVisibility());
        this.privateVisibility = spy(new PrivateVisibility());
        this.publicVisibility = spy(new PublicVisibility());
        this.openVisibility = spy(new OpenVisibility());
        doReturn(false).when(this.hiddenVisibility).isDisabled();
        doReturn(false).when(this.privateVisibility).isDisabled();
        doReturn(false).when(this.publicVisibility).isDisabled();
        doReturn(false).when(this.openVisibility).isDisabled();
        when(this.cm.getInstance(Visibility.class, HIDDEN)).thenReturn(this.hiddenVisibility);
        when(this.cm.getInstance(Visibility.class, PRIVATE)).thenReturn(this.privateVisibility);
        when(this.cm.getInstance(Visibility.class, PUBLIC)).thenReturn(this.publicVisibility);
        when(this.cm.getInstance(Visibility.class, OPEN)).thenReturn(this.openVisibility);
        when(this.cm.getInstance(Visibility.class, NONE)).thenThrow(new ComponentLookupException(NONE));

        // Default visibility config.
        when(this.configuration.getDefaultVisibility()).thenReturn(PRIVATE);

        // Wiki mocks.
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    public void listVisibilityOptionsReturnsEmptyCollectionOnComponentLookupException() throws ComponentLookupException
    {
        when(this.cm.<Visibility>getInstanceList(Visibility.class)).thenThrow(new ComponentLookupException(NONE));
        final Collection<Visibility> visibilities = this.component.listVisibilityOptions();
        Assert.assertTrue(visibilities.isEmpty());
    }

    @Test
    public void listVisibilityOptionsReturnsEmptyCollectionIfInstanceListIsEmpty() throws ComponentLookupException
    {
        when(this.cm.<Visibility>getInstanceList(Visibility.class)).thenReturn(Collections.emptyList());
        final Collection<Visibility> visibilities = this.component.listVisibilityOptions();
        Assert.assertTrue(visibilities.isEmpty());
    }

    @Test
    public void listVisibilityOptionsReturnsEmptyCollectionIfInstanceListOnlyHasDisabledVisibilities()
        throws ComponentLookupException
    {
        doReturn(true).when(this.hiddenVisibility).isDisabled();
        doReturn(true).when(this.privateVisibility).isDisabled();

        when(this.cm.<Visibility>getInstanceList(Visibility.class)).thenReturn(Arrays.asList(this.hiddenVisibility,
            this.privateVisibility));
        final Collection<Visibility> visibilities = this.component.listVisibilityOptions();
        Assert.assertTrue(visibilities.isEmpty());
    }

    @Test
    public void listVisibilityOptionsDoesNotReturnDisabledVisibilities()
        throws ComponentLookupException
    {
        doReturn(true).when(this.hiddenVisibility).isDisabled();
        doReturn(true).when(this.privateVisibility).isDisabled();

        when(this.cm.<Visibility>getInstanceList(Visibility.class)).thenReturn(Arrays.asList(this.hiddenVisibility,
            this.privateVisibility, this.publicVisibility, this.openVisibility));
        final Collection<Visibility> visibilities = this.component.listVisibilityOptions();
        Assert.assertEquals(2, visibilities.size());
        Assert.assertTrue(visibilities.contains(this.publicVisibility));
        Assert.assertTrue(visibilities.contains(this.openVisibility));
    }

    @Test
    public void listAllVisibilityOptionsReturnsEmptyCollectionOnComponentLookupException() throws ComponentLookupException
    {
        when(this.cm.<Visibility>getInstanceList(Visibility.class)).thenThrow(new ComponentLookupException(NONE));
        final Collection<Visibility> visibilities = this.component.listAllVisibilityOptions();
        Assert.assertTrue(visibilities.isEmpty());
    }

    @Test
    public void listAllVisibilityOptionsReturnsEmptyCollectionIfInstanceListIsEmpty() throws ComponentLookupException
    {
        when(this.cm.<Visibility>getInstanceList(Visibility.class)).thenReturn(Collections.emptyList());
        final Collection<Visibility> visibilities = this.component.listAllVisibilityOptions();
        Assert.assertTrue(visibilities.isEmpty());
    }

    @Test
    public void listAllVisibilityOptionsReturnsDisabledVisibilities()
        throws ComponentLookupException
    {
        doReturn(true).when(this.hiddenVisibility).isDisabled();
        doReturn(true).when(this.privateVisibility).isDisabled();

        when(this.cm.<Visibility>getInstanceList(Visibility.class)).thenReturn(Arrays.asList(this.hiddenVisibility,
            this.privateVisibility, this.publicVisibility, this.openVisibility));
        final Collection<Visibility> visibilities = this.component.listAllVisibilityOptions();
        Assert.assertEquals(4, visibilities.size());
        Assert.assertTrue(visibilities.contains(this.hiddenVisibility));
        Assert.assertTrue(visibilities.contains(this.privateVisibility));
        Assert.assertTrue(visibilities.contains(this.publicVisibility));
        Assert.assertTrue(visibilities.contains(this.openVisibility));
    }

    @Test
    public void getDefaultVisibilityReturnsPrivateVisibilityIfNoDefaultIsConfigured()
    {
        when(this.configuration.getDefaultVisibility()).thenReturn(null);
        final Visibility visibility = this.component.getDefaultVisibility();
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void getDefaultVisibilityReturnsPrivateVisibilityIfDefaultVisibilityIsInvalid()
        throws ComponentLookupException
    {
        when(this.configuration.getDefaultVisibility()).thenReturn(NONE);
        final Visibility visibility = this.component.getDefaultVisibility();
        verify(this.logger, times(1)).warn("Invalid patient visibility requested: {}", NONE);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void getDefaultVisibilityReturnsCorrectVisibilityHidden()
        throws ComponentLookupException
    {
        when(this.configuration.getDefaultVisibility()).thenReturn(HIDDEN);
        final Visibility visibility = this.component.getDefaultVisibility();
        Assert.assertEquals(HIDDEN, visibility.getName());
    }

    @Test
    public void getDefaultVisibilityReturnsCorrectVisibilityPrivate()
        throws ComponentLookupException
    {
        when(this.configuration.getDefaultVisibility()).thenReturn(PRIVATE);
        final Visibility visibility = this.component.getDefaultVisibility();
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void getDefaultVisibilityReturnsCorrectVisibilityPublic()
        throws ComponentLookupException
    {
        when(this.configuration.getDefaultVisibility()).thenReturn(PUBLIC);
        final Visibility visibility = this.component.getDefaultVisibility();
        Assert.assertEquals(PUBLIC, visibility.getName());
    }

    @Test
    public void getDefaultVisibilityReturnsCorrectVisibilityOpen()
        throws ComponentLookupException
    {
        when(this.configuration.getDefaultVisibility()).thenReturn(OPEN);
        final Visibility visibility = this.component.getDefaultVisibility();
        Assert.assertEquals(OPEN, visibility.getName());
    }

    @Test
    public void resolveVisibilityReturnsPrivateVisibilityIfVisibilityNameIsNull()
    {
        final Visibility visibility = this.component.resolveVisibility(null);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void resolveVisibilityReturnsPrivateVisibilityIfVisibilityNameIsInvalid() throws ComponentLookupException
    {
        final Visibility visibility = this.component.resolveVisibility(NONE);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void resolveVisibilityReturnsHiddenVisibilityWhenRequested() throws ComponentLookupException
    {
        final Visibility visibility = this.component.resolveVisibility(HIDDEN);
        Assert.assertEquals(HIDDEN, visibility.getName());
    }

    @Test
    public void resolveVisibilityReturnsPrivateVisibilityWhenRequested() throws ComponentLookupException
    {
        final Visibility visibility = this.component.resolveVisibility(PRIVATE);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void resolveVisibilityReturnsPublicVisibilityWhenRequested() throws ComponentLookupException
    {
        final Visibility visibility = this.component.resolveVisibility(PUBLIC);
        Assert.assertEquals(PUBLIC, visibility.getName());
    }

    @Test
    public void resolveVisibilityReturnsOpenVisibilityWhenRequested() throws ComponentLookupException
    {
        final Visibility visibility = this.component.resolveVisibility(OPEN);
        Assert.assertEquals(OPEN, visibility.getName());
    }

    @Test
    public void setVisibilityNullVisibilitySetsEmptyStringForProperty() throws Exception
    {
        final boolean isSet = this.component.setVisibility(this.patient1, null);
        verify(this.permissionsHelper, times(1)).setProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY,
            StringUtils.EMPTY);
        verify(this.wiki, times(1)).saveDocument(this.patientXDoc1, "Set visibility: ", true, this.context);
        Assert.assertTrue(isSet);
    }

    @Test
    public void setVisibilityCatchesExceptions()
    {
        doThrow(new NullPointerException()).when(this.hiddenVisibility).getName();
        final boolean isSet = this.component.setVisibility(this.patient1, this.hiddenVisibility);
        Assert.assertFalse(isSet);
    }

    @Test
    public void setVisibilityDocumentNotSavedWhenNewVisibilityIsTheSameAsOldOne() throws Exception
    {
        final boolean isSet = this.component.setVisibility(this.patient1, this.publicVisibility);
        verify(this.permissionsHelper, never()).setProperty(any(), any(), anyString(), anyString());
        verify(this.wiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
        Assert.assertTrue(isSet);
    }

    @Test
    public void setVisibilityWorksAsExpected() throws Exception
    {
        final boolean isSet = this.component.setVisibility(this.patient1, this.hiddenVisibility);
        verify(this.permissionsHelper, times(1)).setProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY,
            HIDDEN);
        verify(this.wiki, times(1)).saveDocument(this.patientXDoc1, "Set visibility: " + HIDDEN, true, this.context);
        Assert.assertTrue(isSet);
    }

    @Test
    public void getVisibilityStoredVisibilityIsNullReturnsDefaultVisibility()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(null);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void getVisibilityStoredVisibilityIsEmptyReturnsDefaultVisibility()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(StringUtils.EMPTY);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void getVisibilityStoredVisibilityIsBlankReturnsDefaultVisibility()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(StringUtils.SPACE);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void getVisibilityStoredVisibilityIsHidden()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(HIDDEN);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(HIDDEN, visibility.getName());
    }

    @Test
    public void getVisibilityStoredVisibilityIsPrivate()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(PRIVATE);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void getVisibilityStoredVisibilityIsPublic()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(PUBLIC);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(PUBLIC, visibility.getName());
    }

    @Test
    public void getVisibilityStoredVisibilityIsOpen()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(OPEN);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(OPEN, visibility.getName());
    }

    @Test
    public void getVisibilityStoredVisibilityIsInvalid()
    {
        when(this.permissionsHelper.getStringProperty(this.patientXDoc1, this.visibilityDocRef, VISIBILITY))
            .thenReturn(NONE);
        final Visibility visibility = this.component.getVisibility(this.patient1);
        Assert.assertEquals(PRIVATE, visibility.getName());
    }

    @Test
    public void filterByVisibilityEntityCollectionIsNullResultsInEmptyCollection()
    {
        final Collection<PrimaryEntity> filtered = this.component.filterByVisibility((Collection<PrimaryEntity>) null,
            this.openVisibility);
        Assert.assertTrue(filtered.isEmpty());
    }

    @Test
    public void filterByVisibilityEntityCollectionIsEmptyResultsInEmptyCollection()
    {
        final Collection<PrimaryEntity> filtered = this.component.filterByVisibility(Collections.emptyList(),
            this.openVisibility);
        Assert.assertTrue(filtered.isEmpty());
    }

    @Test
    public void filterByVisibilityWithNullVisibilityReturnsUnchangedEntityCollection()
    {
        final Collection<PrimaryEntity> entities = Arrays.asList(this.patient1, this.patient2, this.patient3,
            this.patient4);
        final Collection<PrimaryEntity> filtered = this.component.filterByVisibility(entities, null);
        Assert.assertTrue(entities.equals(filtered));
    }

    @Test
    public void filterByVisibilityRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanOpen()
    {
        final Collection<PrimaryEntity> entities = Arrays.asList(this.patient1, this.patient2, this.patient3, null,
            this.patient4);
        final Collection<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.openVisibility);
        Assert.assertTrue(filtered.isEmpty());
    }

    @Test
    public void filterByVisibilityRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanPublic()
    {
        final Collection<PrimaryEntity> entities = Arrays.asList(this.patient1, this.patient2, this.patient3, null,
            this.patient4);
        final Collection<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.publicVisibility);
        final Collection<PrimaryEntity> expected = Collections.singletonList(this.patient1);
        Assert.assertTrue(expected.equals(filtered));
    }

    @Test
    public void filterByVisibilityRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanPrivate()
    {
        final Collection<PrimaryEntity> entities = Arrays.asList(this.patient1, this.patient2, this.patient3, null,
            this.patient4);
        final Collection<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.privateVisibility);
        final Collection<PrimaryEntity> expected = Arrays.asList(this.patient1, this.patient2, this.patient4);
        Assert.assertTrue(expected.equals(filtered));
    }

    @Test
    public void filterByVisibilityRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanHidden()
    {
        final Collection<PrimaryEntity> entities = Arrays.asList(this.patient1, this.patient2, this.patient3, null,
            this.patient4);
        final Collection<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.hiddenVisibility);
        final Collection<PrimaryEntity> expected = Arrays.asList(this.patient1, this.patient2, this.patient3,
            this.patient4);
        Assert.assertTrue(expected.equals(filtered));
    }

    @Test
    public void filterByVisibilityEntityIteratorIsNullResultsInEmptyIterator()
    {
        final Iterator<PrimaryEntity> filtered = this.component.filterByVisibility((Iterator<PrimaryEntity>) null,
            this.openVisibility);
        Assert.assertFalse(filtered.hasNext());
    }

    @Test
    public void filterByVisibilityEntityIteratorIsEmptyResultsInEmptyIterator()
    {
        final Iterator<PrimaryEntity> filtered = this.component.filterByVisibility(Collections.emptyIterator(),
            this.openVisibility);
        Assert.assertFalse(filtered.hasNext());
    }

    @Test
    public void filterByVisibilityWithNullVisibilityReturnsUnchangedIterator()
    {
        final Iterator<PrimaryEntity> entities = Arrays.<PrimaryEntity>asList(this.patient1, this.patient2,
            this.patient3, this.patient4).iterator();
        final Iterator<PrimaryEntity> filtered = this.component.filterByVisibility(entities, null);
        Assert.assertTrue(entities.equals(filtered));
    }

    @Test
    public void filterByVisibilityGetsIteratorThatRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanOpen()
    {
        final Iterator<PrimaryEntity> entities = Arrays.<PrimaryEntity>asList(this.patient1, this.patient2,
            this.patient3, null, this.patient4).iterator();
        final Iterator<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.openVisibility);
        Assert.assertFalse(filtered.hasNext());
    }

    @Test
    public void filterByVisibilityGetsIteratorThatRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanPublic()
    {
        final Iterator<PrimaryEntity> entities = Arrays.<PrimaryEntity>asList(this.patient1, this.patient2,
            this.patient3, null, this.patient4).iterator();
        final Iterator<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.publicVisibility);
        Assert.assertTrue(filtered.next().equals(this.patient1));
        Assert.assertFalse(filtered.hasNext());
    }

    @Test
    public void filterByVisibilityGetsIteratorThatRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanPrivate()
    {
        final Iterator<PrimaryEntity> entities = Arrays.<PrimaryEntity>asList(this.patient1, this.patient2,
            this.patient3, null, this.patient4).iterator();
        final Iterator<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.privateVisibility);
        Assert.assertTrue(filtered.next().equals(this.patient1));
        Assert.assertTrue(filtered.next().equals(this.patient2));
        Assert.assertTrue(filtered.next().equals(this.patient4));
        Assert.assertFalse(filtered.hasNext());
    }

    @Test
    public void filterByVisibilityGetsIteratorThatRemovesNullsAndEntitiesWithVisibilitiesMoreRestrictiveThanHidden()
    {
        final Iterator<PrimaryEntity> entities = Arrays.<PrimaryEntity>asList(this.patient1, this.patient2,
            this.patient3, null, this.patient4).iterator();
        final Iterator<PrimaryEntity> filtered = this.component.filterByVisibility(entities, this.hiddenVisibility);
        Assert.assertTrue(filtered.next().equals(this.patient1));
        Assert.assertTrue(filtered.next().equals(this.patient2));
        Assert.assertTrue(filtered.next().equals(this.patient3));
        Assert.assertTrue(filtered.next().equals(this.patient4));
        Assert.assertFalse(filtered.hasNext());
    }

}
