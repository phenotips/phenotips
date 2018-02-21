package org.phenotips.entities.configuration.spi;

import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordSection;
import org.phenotips.entities.configuration.RecordSectionOption;

import org.xwiki.uiextension.UIExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.Builder;

/**
 * Builder for a {@link RecordSection}.
 *
 * @version $Id$
 * @since 1.4
 */
public abstract class RecordSectionBuilder implements Builder<RecordSection>
{
    /**
     * The extension defining this section.
     *
     * @return a valid {@link UIExtension} object
     */
    public abstract UIExtension getExtension();

    /**
     * The name of this section, displayed in the record and in the form designer.
     *
     * @return a user-friendly title for this section
     * @see RecordSection#getName()
     */
    @Nonnull
    public abstract String getName();

    /**
     * Whether this section and its elements are going to be displayed in the record or not.
     *
     * @return {@code true} if this section must be displayed, {@code false} otherwise
     * @see RecordSection#isEnabled()
     */
    public abstract boolean isEnabled();

    /**
     * Set whether this section and its elements are going to be displayed in the record or not. All changes are done
     * in-memory for this object only, the configuration will remain unchanged.
     *
     * @param enabled {@code true} if this section must be displayed, {@code false} otherwise
     * @return self, for chaining method calls
     * @see RecordSection#isEnabled()
     */
    @Nonnull
    public abstract RecordSectionBuilder setEnabled(boolean enabled);

    /**
     * The list of elements configured in this section, whether they are enabled or not.
     *
     * @return an unmodifiable ordered list of {@link RecordElement elements}, empty if this section doesn't have any
     *         elements
     * @see RecordSection#getAllElements()
     */
    @Nonnull
    public abstract List<RecordElementBuilder> getAllElements();

    /**
     * Set the list of elements configured in this section. Each element still decides whether it is enabled or not. All
     * changes are done in-memory for this object only, the configuration will remain unchanged.
     *
     * @param elements the new list of elements to be included in this section, may be empty; the list is copied,
     *            further changes to the original list will not be reflected in the configuration
     * @return self, for chaining method calls
     * @see RecordSection#getAllElements()
     */
    @Nonnull
    public abstract RecordSectionBuilder setElements(@Nullable List<RecordElementBuilder> elements);

    /**
     * Configuration options for this section.
     *
     * @return a set of options, possibly empty.
     * @see RecordSection#getOptions()
     */
    @Nonnull
    public abstract EnumSet<RecordSectionOption> getOptions();

    /**
     * Sets the options to use for this section. All changes are done in-memory for this object only, the configuration
     * will remain unchanged.
     *
     * @param options a new set of options; the set is copied, further changes to the original set will not be reflected
     *            in the configuration
     * @return self, for chaining method calls
     * @see RecordSection#getOptions()
     */
    @Nonnull
    public abstract RecordSectionBuilder setOptions(@Nullable EnumSet<RecordSectionOption> options);

    /**
     * Enables one option for this section. All changes are done in-memory for this object only, the configuration will
     * remain unchanged.
     *
     * @param option an option to add to the {@link #getOptions() existing list of options}, if not previously set
     * @return self, for chaining method calls
     * @see RecordSection#getOptions()
     */
    @Nonnull
    public abstract RecordSectionBuilder withOption(@Nonnull RecordSectionOption option);

    /**
     * Disables one option for this section. All changes are done in-memory for this object only, the configuration will
     * remain unchanged.
     *
     * @param option an option to remove from the {@link #getOptions() existing list of options}, if previously set
     * @return self, for chaining method calls
     * @see RecordSection#getOptions()
     */
    @Nonnull
    public abstract RecordSectionBuilder withoutOption(@Nonnull RecordSectionOption option);

    /**
     * Other optional parameters affecting this section.
     *
     * @return a possibly empty map of parameters
     * @see RecordSection#getParameters()
     */
    @Nonnull
    public abstract Map<String, String> getParameters();

    /**
     * Sets the optional parameters to use for this section. All changes are done in-memory for this object only, the
     * configuration will remain unchanged.
     *
     * @param newParameters a new set of parameters; the map is copied, further changes to the original map will not be
     *            reflected in the configuration
     * @return self, for chaining method calls
     * @see RecordSection#getParameters()
     */
    @Nonnull
    public abstract RecordSectionBuilder setParameters(@Nullable Map<String, String> newParameters);

    /**
     * Defines or redefines a parameter for this section. All changes are done in-memory for this object only, the
     * configuration will remain unchanged.
     *
     * @param name the name of the parameter to add or redefine to the {@link #getParameters() existing parameters}
     * @param value the value to set for the parameter
     * @return self, for chaining method calls
     * @see RecordSection#getParameters()
     */
    @Nonnull
    public abstract RecordSectionBuilder withParameter(@Nonnull String name, @Nullable String value);

    /**
     * Removes one parameter for this section. All changes are done in-memory for this object only, the configuration
     * will remain unchanged.
     *
     * @param name the name of the parameter to remove from the {@link #getParameters() existing parameters}, if
     *            previously defined
     * @return self, for chaining method calls
     * @see RecordSection#getParameters()
     */
    @Nonnull
    public abstract RecordSectionBuilder withoutParameter(@Nonnull String name);

    /**
     * Construct a {@link RecordSection} and its {@code RecordElement}s with the options set so far.
     *
     * @return a valid {@link RecordSection}
     */
    @Override
    @Nonnull
    public abstract RecordSection build();
}
