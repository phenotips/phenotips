package org.phenotips.entities.configuration.spi;

import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordElementOption;

import org.xwiki.uiextension.UIExtension;

import java.util.EnumSet;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.Builder;

/**
 * Builder for a {@link RecordElement} implementation.
 *
 * @version $Id$
 * @since 1.4
 */
public abstract class RecordElementBuilder implements Builder<RecordElement>
{
    /**
     * The extension defining this element.
     *
     * @return a valid {@link UIExtension} object
     */
    @Nonnull
    public abstract UIExtension getExtension();

    /**
     * The name of this element, displayed in the form designer.
     *
     * @return a user-friendly name for this element
     * @see RecordElement#getName()
     */
    @Nonnull
    public abstract String getName();

    /**
     * Whether this element is going to be displayed in the record or not.
     *
     * @return {@code true} if this element must be displayed, {@code false} otherwise
     * @see RecordElement#isEnabled()
     */
    public abstract boolean isEnabled();

    /**
     * Sets whether this element is going to be displayed in the record or not. All changes are done in-memory for this
     * object only, the configuration will remain unchanged.
     *
     * @param enabled {@code true} if this element should be displayed, {@code false} otherwise
     * @return self, for chaining method calls
     * @see RecordElement#isEnabled()
     */
    @Nonnull
    public abstract RecordElementBuilder setEnabled(boolean enabled);

    /**
     * Configuration options for this element.
     *
     * @return a set of options, possibly empty.
     * @see RecordElement#getOptions()
     */
    @Nonnull
    public abstract EnumSet<RecordElementOption> getOptions();

    /**
     * Sets the options to use for this element. All changes are done in-memory for this object only, the configuration
     * will remain unchanged.
     *
     * @param options a new set of options; the set is copied, further changes to the original set will not be reflected
     *            in the configuration
     * @return self, for chaining method calls
     * @see RecordElement#getOptions()
     */
    @Nonnull
    public abstract RecordElementBuilder setOptions(@Nonnull EnumSet<RecordElementOption> options);

    /**
     * Enables one option for this element. All changes are done in-memory for this object only, the configuration will
     * remain unchanged.
     *
     * @param option an option to add to the {@link #getOptions() existing list of options}, if not previously set
     * @return self, for chaining method calls
     * @see RecordElement#getOptions()
     */
    @Nonnull
    public abstract RecordElementBuilder withOption(@Nonnull RecordElementOption option);

    /**
     * Disables one option for this element. All changes are done in-memory for this object only, the configuration will
     * remain unchanged.
     *
     * @param option an option to remove from the {@link #getOptions() existing list of options}, if previously set
     * @return self, for chaining method calls
     * @see RecordElement#getOptions()
     */
    @Nonnull
    public abstract RecordElementBuilder withoutOption(@Nonnull RecordElementOption option);

    /**
     * Other optional parameters affecting this element.
     *
     * @return a possibly empty map of parameters
     * @see RecordElement#getParameters()
     */
    @Nonnull
    public abstract Map<String, String> getParameters();

    /**
     * Sets the optional parameters to use for this element. All changes are done in-memory for this object only, the
     * configuration will remain unchanged.
     *
     * @param newParameters a new set of parameters; the map is copied, further changes to the original map will not be
     *            reflected in the configuration
     * @return self, for chaining method calls
     * @see RecordElement#getParameters()
     */
    @Nonnull
    public abstract RecordElementBuilder setParameters(@Nullable Map<String, String> newParameters);

    /**
     * Enables one option for this element. All changes are done in-memory for this object only, the configuration will
     * remain unchanged.
     *
     * @param name the name of the parameter to add or redefine to the {@link #getParameters() existing parameters}
     * @param value the value to set for the parameter
     * @return self, for chaining method calls
     * @see RecordElement#getParameters()
     */
    @Nonnull
    public abstract RecordElementBuilder withParameter(@Nonnull String name, @Nullable String value);

    /**
     * Removes one parameter for this element. All changes are done in-memory for this object only, the configuration
     * will remain unchanged.
     *
     * @param name the name of the parameter to remove from the {@link #getParameters() existing parameters}, if
     *            previously defined
     * @return self, for chaining method calls
     * @see RecordElement#getParameters()
     */
    @Nonnull
    public abstract RecordElementBuilder withoutParameter(@Nonnull String name);

    /**
     * Construct a {@link RecordElement} with the options set so far.
     *
     * @return a valid {@link RecordElement}
     */
    @Override
    @Nonnull
    public abstract RecordElement build();
}
