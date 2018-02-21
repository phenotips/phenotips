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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.spi.uix.UIXRecordElementBuilder;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.uiextension.UIExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link UIXRecordElementBuilder} class.
 *
 * @version $Id$
 */
@NotThreadSafe
public class RecordElementBuilderTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private PrimaryEntityConfigurationBuilder configuration;

    @Mock
    private PrimaryEntityManager<? extends PrimaryEntity> entityManager;

    @Mock
    private UIExtension uiExtension;

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private DocumentReferenceResolver<String> stringResolver;

    @Mock
    private EntityReference stubReference;

    @Mock
    private DocumentReference fullReference;

    @Mock
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    private Map<String, String> parameters = new HashMap<>();

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current"))
            .thenReturn(this.referenceResolver);
        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current")).thenReturn(this.stringResolver);

        when(this.uiExtension.getParameters()).thenReturn(this.parameters);
        Mockito.doReturn(this.entityManager).when(this.configuration).getEntityManager();
        when(this.entityManager.getTypeReference()).thenReturn(this.stubReference);
        when(this.referenceResolver.resolve(this.stubReference)).thenReturn(this.fullReference);
    }

    @Test
    public void getExtension()
    {
        RecordElementBuilder s = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertSame(this.uiExtension, s.getExtension());
    }

    @Test
    public void nullExtensionThrowsException()
    {
        this.thrown.expect(IllegalArgumentException.class);
        UIXRecordElementBuilder.with(this.configuration, null);
    }

    @Test
    public void nullConfigurationThrowsException()
    {
        this.thrown.expect(IllegalArgumentException.class);
        UIXRecordElementBuilder.with(null, this.uiExtension);
    }

    @Test
    public void getNameReturnsTitle()
    {
        this.parameters.put("title", "Age of onset");
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertEquals("Age of onset", e.getName());
    }

    @Test
    public void getNameWithMissingTitleReturnsExtensionNameSuffix()
    {
        when(this.uiExtension.getId()).thenReturn("org.phenotips.patientSheet.field.exam_date");

        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertEquals("Exam date", e.getName());
    }

    @Test
    public void isEnabledReturnsTrueForNullSetting()
    {
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertTrue(e.isEnabled());
    }

    @Test
    public void isEnabledReturnsTrueForEmptySetting()
    {
        this.parameters.put("enabled", "");
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertTrue(e.isEnabled());
    }

    @Test
    public void isEnabledReturnsTrueForTrueSetting()
    {
        this.parameters.put("enabled", "true");
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertTrue(e.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseForFalseSetting()
    {
        this.parameters.put("enabled", "false");
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertFalse(e.isEnabled());
    }

    @Test
    public void setEnabledOverridesDefaultSetting()
    {
        this.parameters.put("enabled", "false");
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);

        Assert.assertFalse(e.isEnabled());
        e.setEnabled(true);
        Assert.assertTrue(e.isEnabled());

        this.parameters.put("enabled", "true");
        e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        Assert.assertTrue(e.isEnabled());
        e.setEnabled(false);
        Assert.assertFalse(e.isEnabled());
    }

    /** {@link RecordElement#getDisplayedFields()} returns the fields listed in the extension "fields" property. */
    @Test
    public void getDisplayedFields()
    {
        this.parameters.put("fields", ",first_name ,, last_name,");
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        final RecordElement element = e.build();

        final List<ClassPropertyReference> result = element.getDisplayedFields();

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("first_name", result.get(0).getName());
        Assert.assertEquals("last_name", result.get(1).getName());
    }

    /** {@link RecordElement#getDisplayedFields()} returns an empty list when there's no "fields" property. */
    @Test
    public void getDisplayedFieldsWithMissingProperty()
    {
        RecordElementBuilder e = UIXRecordElementBuilder.with(this.configuration, this.uiExtension);
        RecordElement element = e.build();
        List<ClassPropertyReference> result = element.getDisplayedFields();
        Assert.assertTrue(result.isEmpty());
    }
}
