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

import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.PermissionsManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PermissionsManager} implementation, {@link DefaultPermissionsManager}.
 *
 * @version $Id$
 */
public class DefaultPermissionsConfigurationTest
{
    @Rule
    public final MockitoComponentMockingRule<PermissionsConfiguration> mocker =
        new MockitoComponentMockingRule<PermissionsConfiguration>(DefaultPermissionsConfiguration.class);

    @Mock
    private DocumentReference visibilityConfigurationClassReference;

    @Mock
    private DocumentReference preferencesDocumentReference;

    @Mock
    private EntityReferenceSerializer<String> serializer;

    @Mock
    private DocumentReferenceResolver<String> resolver;

    private DocumentAccessBridge dab;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(PermissionsConfiguration.VISIBILITY_CONFIGURATION_CLASS_REFERENCE)).thenReturn(
            this.visibilityConfigurationClassReference);
        when(resolver.resolve(PermissionsConfiguration.PREFERENCES_DOCUMENT)).thenReturn(
            this.preferencesDocumentReference);

        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);

        Utils.setComponentManager(this.mocker);

        // Needed for making BaseObjectReference work
        this.mocker.registerComponent(EntityReferenceSerializer.TYPE_STRING, this.serializer);
        this.mocker.registerComponent(DocumentReferenceResolver.TYPE_STRING, this.resolver);
        when(this.serializer.serialize(this.visibilityConfigurationClassReference))
            .thenReturn("xwiki:PhenoTips.VisibilityConfigurationClass");
        when(this.resolver.resolve("xwiki:PhenoTips.VisibilityConfigurationClass"))
            .thenReturn(this.visibilityConfigurationClassReference);
    }

    @Test
    public void isDisabledChecksXWikiPreferencesConfiguration() throws ComponentLookupException
    {
        when(this.dab.getProperty(new ObjectPropertyReference("disabledLevels",
            new BaseObjectReference(this.visibilityConfigurationClassReference, 0, this.preferencesDocumentReference))))
                .thenReturn(Collections.singletonList("open"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().isVisibilityDisabled("private"));
        Assert.assertTrue(this.mocker.getComponentUnderTest().isVisibilityDisabled("open"));
    }

    @Test
    public void isDisabledReturnsFalseWhenConfigurationIsMissing() throws ComponentLookupException
    {
        when(this.dab.getProperty(new ObjectPropertyReference("disabledLevels",
            new BaseObjectReference(this.visibilityConfigurationClassReference, 0, this.preferencesDocumentReference))))
                .thenReturn(null);
        Assert.assertFalse(this.mocker.getComponentUnderTest().isVisibilityDisabled("private"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().isVisibilityDisabled("open"));
    }
}
