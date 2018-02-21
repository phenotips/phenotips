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

import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordSection;
import org.phenotips.entities.configuration.RecordSectionOption;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationBuilder;
import org.phenotips.entities.configuration.spi.RecordElementBuilder;
import org.phenotips.entities.configuration.spi.RecordSectionBuilder;
import org.phenotips.entities.configuration.spi.StaticRecordSection;

import org.xwiki.stability.Unstable;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a section that can be displayed in a record.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public class UIXRecordSectionBuilder extends RecordSectionBuilder
{
    private static final String TITLE_LABEL = "title";

    private static final String ORDER_LABEL = "order";

    private static final String FALSE_LABEL = "false";

    private static final String ENABLED_LABEL = "enabled";

    /** Lists the contained fields. */
    protected final UIExtensionManager uixManager;

    /** Sorts fields by their declared order. */
    protected final UIExtensionFilter orderFilter;

    private final PrimaryEntityConfigurationBuilder configuration;

    /** @see #getExtension() */
    private UIExtension extension;

    private String name;

    private EnumSet<RecordSectionOption> options = EnumSet.noneOf(RecordSectionOption.class);

    private Map<String, String> parameters = new HashMap<>();

    private List<RecordElementBuilder> elements;

    private boolean enabled;

    private final Logger logger = LoggerFactory.getLogger(UIXRecordElementBuilder.class);

    /**
     * Simple constructor, taking the UI {@code extension}, the {@code uixManager UI extension manager}, and the
     * {@code orderFilter UI extension filter} as parameters.
     *
     * @param configuration the configuration for which this builder is prepared
     * @param extension the {@link UIExtension} object defining this element
     * @param uixManager the {@link UIExtensionManager} object
     * @param orderFilter the {@link UIExtensionFilter} object for ordering sections and elements
     * @throws IllegalArgumentException if {@code extension} or {@code uixManager} or {@code orderFilter} are null
     */
    public UIXRecordSectionBuilder(PrimaryEntityConfigurationBuilder configuration, UIExtension extension,
        UIExtensionManager uixManager, UIExtensionFilter orderFilter)
    {
        if (configuration == null || extension == null || uixManager == null || orderFilter == null) {
            throw new IllegalArgumentException("RecordSectionBuilder constructor parameters must not be null");
        }
        this.configuration = configuration;
        this.extension = extension;
        this.uixManager = uixManager;
        this.orderFilter = orderFilter;
        this.enabled = !StringUtils.equals(FALSE_LABEL, extension.getParameters().get(ENABLED_LABEL));
        this.name = this.extension.getParameters().get(TITLE_LABEL);
        if (StringUtils.isBlank(this.name)) {
            this.name = StringUtils.capitalize(StringUtils.replaceChars(
                StringUtils.substringAfterLast(this.extension.getId(), "."), "_-", "  "));
        }
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
    public RecordSectionBuilder setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }

    @Override
    public List<RecordElementBuilder> getAllElements()
    {
        if (this.elements == null) {
            this.elements = collectElements();
        }

        return Collections.unmodifiableList(this.elements);
    }

    @Override
    public RecordSectionBuilder setElements(List<RecordElementBuilder> elements)
    {
        this.elements = elements;
        return this;
    }

    /**
     * Returns a list of {@link RecordElement} from a list of {@link UIExtension section elements}.
     *
     * @return a list of {@link RecordElement} objects
     */
    private List<RecordElementBuilder> collectElements()
    {
        List<RecordElementBuilder> result = new LinkedList<>();
        List<UIExtension> fields = this.uixManager.get(this.extension.getId());
        fields = this.orderFilter.filter(fields, ORDER_LABEL);
        for (UIExtension field : fields) {
            result.add(UIXRecordElementBuilder.with(this.configuration, field));
        }
        return result;
    }

    @Override
    public RecordSection build()
    {
        return new StaticRecordSection(this.name, this.enabled, this.options, this.parameters,
            this.getAllElements().stream().map(element -> element.build()).collect(Collectors.toList()));
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder(getName());
        result.append(" [");
        result.append(StringUtils.join(getAllElements(), ", "));
        result.append(']');
        return result.toString();
    }

    @Override
    public EnumSet<RecordSectionOption> getOptions()
    {
        return EnumSet.copyOf(this.options);
    }

    @Override
    public RecordSectionBuilder setOptions(EnumSet<RecordSectionOption> newOptions)
    {
        this.options = newOptions == null ? EnumSet.noneOf(RecordSectionOption.class) : EnumSet.copyOf(newOptions);
        return this;
    }

    @Override
    public RecordSectionBuilder withOption(RecordSectionOption option)
    {
        this.options.remove(option);
        return this;
    }

    @Override
    public RecordSectionBuilder withoutOption(RecordSectionOption option)
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
    public RecordSectionBuilder setParameters(Map<String, String> newParameters)
    {
        this.parameters = newParameters == null ? new HashMap<>() : new HashMap<>(newParameters);
        return this;
    }

    @Override
    public RecordSectionBuilder withParameter(String name, String value)
    {
        if (name == null) {
            this.logger.warn("Illegal parameter addition: parameter name must not be null");
            return this;
        }
        this.parameters.put(name, value);
        return this;
    }

    @Override
    public RecordSectionBuilder withoutParameter(String name)
    {
        if (name == null) {
            this.logger.warn("Illegal parameter removal: parameter name must not be null");
            return this;
        }
        this.parameters.remove(name);
        return this;
    }
}
