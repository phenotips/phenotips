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
package org.phenotips.entities.configuration.spi.uix;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordElementOption;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationBuilder;
import org.phenotips.entities.configuration.spi.RecordElementBuilder;
import org.phenotips.entities.configuration.spi.StaticRecordElement;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.uiextension.UIExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RecordElement} {@link RecordElementBuilder builder} which uses the data set in a {@link UIExtension} as the
 * basis for the built record element. The {@link #getName() name} is taken from the {@code title} property, or, if not
 * set, extracted from the extension ID as the part after the last {@code _} character. The element is
 * {@link #isEnabled() disabled} if it contains an {@code enabled} property set to {@code false}. Other
 * {@link #getOptions()} are extracted from specific extension parameters, and all the extension parameters are loaded
 * into the {@link #getParameters() optional parameters} map.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public final class UIXRecordElementBuilder extends RecordElementBuilder
{
    private static final String FALSE_LABEL = "false";

    private static final String ENABLED_LABEL = "enabled";

    private static final String TITLE_LABEL = "title";

    private static final String FIELDS_LABEL = "fields";

    private static final String CLASS_LABEL = "pt_class";

    private static final String RESOLVER_TO_USE = "current";

    private final PrimaryEntityConfigurationBuilder configuration;

    private final UIExtension extension;

    private final String name;

    private final DocumentReferenceResolver<EntityReference> entityResolver;

    private final DocumentReferenceResolver<String> stringResolver;

    private EnumSet<RecordElementOption> options = EnumSet.noneOf(RecordElementOption.class);

    private Map<String, String> parameters = new HashMap<>();

    private boolean enabled;

    private final Logger logger = LoggerFactory.getLogger(UIXRecordElementBuilder.class);

    /**
     * Simple constructor, taking a UI {@code extension} and the parent record {@code section} as parameters.
     *
     * @param configuration the configuration for which this builder is prepared
     * @param extension the {@link UIExtension UI extension} object defining this record element
     * @throws IllegalArgumentException if {@code extension} or {@code section} are null
     */
    private UIXRecordElementBuilder(@Nonnull final PrimaryEntityConfigurationBuilder configuration,
        @Nonnull final UIExtension extension)
    {
        if (configuration == null || extension == null) {
            throw new IllegalArgumentException("DefaultRecordElement constructor parameters must not be null");
        }
        this.configuration = configuration;
        this.extension = extension;
        this.enabled = !StringUtils.equals(FALSE_LABEL, this.extension.getParameters().get(ENABLED_LABEL));
        this.name = constructName();
        try {
            this.entityResolver = ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DocumentReferenceResolver.TYPE_REFERENCE, RESOLVER_TO_USE);
            this.stringResolver = ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DocumentReferenceResolver.TYPE_STRING, RESOLVER_TO_USE);
        } catch (ComponentLookupException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Creates a new {@link RecordElementBuilder} for the given configuration, starting from a {@link UIExtension}
     * object.
     *
     * @param configuration the configuration for which this builder is prepared
     * @param extension the UI Extension object from which to copy the initial data
     * @return the new record element builder
     */
    public static UIXRecordElementBuilder with(final PrimaryEntityConfigurationBuilder configuration,
        final UIExtension extension)
    {
        return new UIXRecordElementBuilder(configuration, extension);
    }

    @Override
    public UIExtension getExtension()
    {
        return this.extension;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public RecordElementBuilder setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }

    @Override
    public RecordElement build()
    {
        return new StaticRecordElement(this.extension, this.name, this.enabled, this.options, this.parameters,
            constructFields());
    }

    @Override
    public String toString()
    {
        return getName();
    }

    /**
     * Returns the name of the section as string.
     *
     * @return the name of the section as string
     */
    private String constructName()
    {
        String result = this.extension.getParameters().get(TITLE_LABEL);
        if (StringUtils.isBlank(result)) {
            result = StringUtils.capitalize(StringUtils.replaceChars(
                StringUtils.substringAfterLast(this.extension.getId(), "."), "_-", "  "));
        }
        return result;
    }

    private List<ClassPropertyReference> constructFields()
    {
        final String fieldsStr = this.extension.getParameters().get(FIELDS_LABEL);
        DocumentReference xclass =
            this.entityResolver.resolve(this.configuration.getEntityManager().getTypeReference());
        String customXClass = this.extension.getParameters().get(CLASS_LABEL);
        if (StringUtils.isNotBlank(customXClass)) {
            xclass = this.stringResolver.resolve(customXClass);
        }
        final List<ClassPropertyReference> elementFields = new LinkedList<>();
        if (StringUtils.isNotBlank(fieldsStr)) {
            final String[] fieldNames = StringUtils.split(fieldsStr, ",");
            for (final String fieldName : fieldNames) {
                ClassPropertyReference cp = new ClassPropertyReference(StringUtils.trim(fieldName), xclass);
                elementFields.add(cp);
            }
        }
        return elementFields;
    }

    @Override
    public EnumSet<RecordElementOption> getOptions()
    {
        return EnumSet.copyOf(this.options);
    }

    @Override
    public RecordElementBuilder setOptions(EnumSet<RecordElementOption> newOptions)
    {
        this.options = newOptions == null ? EnumSet.noneOf(RecordElementOption.class) : EnumSet.copyOf(newOptions);
        return this;
    }

    @Override
    public RecordElementBuilder withOption(RecordElementOption option)
    {
        this.options.remove(option);
        return this;
    }

    @Override
    public RecordElementBuilder withoutOption(RecordElementOption option)
    {
        this.options.add(option);
        return this;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters);
    }

    @Override
    public RecordElementBuilder setParameters(Map<String, String> newParameters)
    {
        this.parameters = newParameters == null ? new HashMap<>() : new HashMap<>(newParameters);
        return this;
    }

    @Override
    public RecordElementBuilder withParameter(String name, String value)
    {
        if (name == null) {
            this.logger.warn("Illegal parameter addition: parameter name must not be null");
            return this;
        }
        this.parameters.put(name, value);
        return this;
    }

    @Override
    public RecordElementBuilder withoutParameter(String name)
    {
        if (name == null) {
            this.logger.warn("Illegal parameter removal: parameter name must not be null");
            return this;
        }
        this.parameters.remove(name);
        return this;
    }
}
