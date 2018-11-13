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

import org.phenotips.Constants;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
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
import net.jcip.annotations.NotThreadSafe;

import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link EntityPermissionsManager} implementation, {@link DefaultEntityPermissionsManager}.
 *
 * @version $Id$
 */
@NotThreadSafe
public class DefaultPermissionsConfigurationTest
{
    @Rule
    public final MockitoComponentMockingRule<PermissionsConfiguration> mocker =
        new MockitoComponentMockingRule<>(DefaultPermissionsConfiguration.class);

    @Mock
    private DocumentReference visibilityClassReference;

    @Mock
    private DocumentReference visibilityConfigurationClassReference;

    @Mock
    private DocumentReference preferencesDocumentReference;

    @Mock
    private DocumentReference patientTemplateReference;

    @Mock
    private EntityReferenceSerializer<String> serializer;

    @Mock
    private DocumentReferenceResolver<String> resolver;

    private DocumentAccessBridge dab;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        DocumentReferenceResolver<EntityReference> stringResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(stringResolver.resolve(Visibility.CLASS_REFERENCE)).thenReturn(this.visibilityClassReference);
        when(stringResolver.resolve(PermissionsConfiguration.VISIBILITY_CONFIGURATION_CLASS_REFERENCE)).thenReturn(
            this.visibilityConfigurationClassReference);
        when(stringResolver.resolve(PermissionsConfiguration.PREFERENCES_DOCUMENT)).thenReturn(
            this.preferencesDocumentReference);
        when(stringResolver.resolve(
            new EntityReference("PatientTemplate", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE)))
                .thenReturn(this.patientTemplateReference);

        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);

        Utils.setComponentManager(this.mocker);

        // Needed for making BaseObjectReference work
        this.mocker.registerComponent(EntityReferenceSerializer.TYPE_STRING, this.serializer);
        this.mocker.registerComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki", this.serializer);
        this.mocker.registerComponent(DocumentReferenceResolver.TYPE_STRING, this.resolver);
        when(this.serializer.serialize(this.visibilityConfigurationClassReference, this.preferencesDocumentReference))
            .thenReturn("xwiki:PhenoTips.VisibilityConfigurationClass");
        when(this.resolver.resolve("xwiki:PhenoTips.VisibilityConfigurationClass"))
            .thenReturn(this.visibilityConfigurationClassReference);
        when(this.serializer.serialize(this.visibilityClassReference, this.patientTemplateReference))
            .thenReturn("xwiki:PhenoTips.VisibilityClass");
        when(this.resolver.resolve("xwiki:PhenoTips.VisibilityClass"))
            .thenReturn(this.visibilityClassReference);
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

    @Test
    public void getDefaultVisibilityChecksPatientTemplate() throws ComponentLookupException
    {
        when(this.dab.getProperty(new ObjectPropertyReference("visibility",
            new BaseObjectReference(this.visibilityClassReference, 0, this.patientTemplateReference))))
                .thenReturn("public");
        Assert.assertEquals("public", this.mocker.getComponentUnderTest().getDefaultVisibility());
    }

    @Test
    public void getDefaultVisibilityWithMissingConfigurationReturnsNull() throws ComponentLookupException
    {
        when(this.dab.getProperty(new ObjectPropertyReference("visibility",
            new BaseObjectReference(this.visibilityClassReference, 0, this.patientTemplateReference))))
                .thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getDefaultVisibility());
    }

    @Test
    public void getDefaultVisibilityWithEmptyConfigurationReturnsNull() throws ComponentLookupException
    {
        when(this.dab.getProperty(new ObjectPropertyReference("visibility",
            new BaseObjectReference(this.visibilityClassReference, 0, this.patientTemplateReference))))
                .thenReturn("", " ");
        Assert.assertNull(this.mocker.getComponentUnderTest().getDefaultVisibility());
        Assert.assertNull(this.mocker.getComponentUnderTest().getDefaultVisibility());
    }
}
