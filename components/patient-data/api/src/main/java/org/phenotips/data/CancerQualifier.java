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
package org.phenotips.data;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

/**
 * Information about the {@link Patient patient}'s {@link CancerQualifier qualifier}.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public interface CancerQualifier extends VocabularyProperty
{
    /** The XClass used for storing cancer qualifier data. */
    EntityReference CLASS_REFERENCE = new EntityReference("CancerQualifierClass", EntityType.DOCUMENT,
            Constants.CODE_SPACE_REFERENCE);

    /**
     * The supported qualifier properties.
     */
    enum CancerQualifierProperty
    {
        /** The cancer that the qualifier is associated with. */
        CANCER("cancer")
        {
            @Nullable
            @Override
            public String extractValue(@Nonnull final BaseObject qualifier)
            {
                final BaseStringProperty field = (BaseStringProperty) qualifier.getField(getProperty());
                return StringUtils.defaultIfBlank(field == null ? null : field.getValue(), null);
            }

            @Nullable
            @Override
            public String extractValue(@Nonnull final JSONObject qualifier)
            {
                final String value = qualifier.optString(getJsonProperty());
                return StringUtils.defaultIfBlank(value, null);
            }

            @Override
            public boolean valueIsValid(@Nullable final Object value)
            {
                // This is the id, and should never be missing.
                return value instanceof String && StringUtils.isNotBlank((String) value);
            }
        },

        /** The age at which the cancer is diagnosed, as string. */
        AGE_AT_DIAGNOSIS("ageAtDiagnosis")
        {
            @Nullable
            @Override
            public String extractValue(@Nonnull final BaseObject qualifier)
            {
                final BaseStringProperty field = (BaseStringProperty) qualifier.getField(getProperty());
                return StringUtils.defaultIfBlank(field == null ? null : field.getValue(), null);
            }

            @Nullable
            @Override
            public String extractValue(@Nonnull final JSONObject qualifier)
            {
                final String value = qualifier.optString(getJsonProperty(), null);
                return StringUtils.defaultIfBlank(value, null);
            }

            @Override
            public boolean valueIsValid(@Nullable final Object value)
            {
                return value == null || value instanceof String;
            }
        },

        /** The numeric age estimate at which the cancer is diagnosed. */
        NUMERIC_AGE_AT_DIAGNOSIS("numericAgeAtDiagnosis")
        {
            @Nullable
            @Override
            public Integer extractValue(@Nonnull final BaseObject qualifier)
            {
                final int value = qualifier.getIntValue(getProperty(), -1);
                // If value is less than zero, then no age specified.
                return value < 0 ? null : value;
            }

            @Nullable
            @Override
            public Integer extractValue(@Nonnull final JSONObject qualifier)
            {
                final int value = qualifier.optInt(getJsonProperty(), -1);
                return value < 0 ? null : value;
            }

            @Override
            public boolean valueIsValid(@Nullable final Object value)
            {
                return value == null || value instanceof Integer && (Integer) value >= 0;
            }
        },

        /** The type of cancer -- can be primary (primary = true) or metastasized (primary = false). */
        PRIMARY("primary")
        {
            @Nonnull
            @Override
            public Boolean extractValue(@Nonnull final BaseObject qualifier)
            {
                // Assumed primary unless otherwise specified.
                final int value = qualifier.getIntValue(getProperty(), 1);
                return value == 1;
            }

            @Nonnull
            @Override
            public Boolean extractValue(@Nonnull final JSONObject qualifier)
            {
                return qualifier.optBoolean(getJsonProperty(), true);
            }

            @Override
            public void writeValue(@Nonnull final BaseObject qualifier,
                                   @Nullable final Object value,
                                   @Nonnull final XWikiContext context)
            {
                // Set to true unless explicitly set to false.
                final int intValue = Boolean.FALSE.equals(value) ? 0 : 1;
                super.writeValue(qualifier, intValue, context);
            }

            @Override
            public boolean valueIsValid(@Nullable final Object value)
            {
                return value instanceof Boolean;
            }
        },

        /** The localization with respect to the side of the body of the specified cancer. */
        LATERALITY("laterality")
        {
            /** Permitted laterality values. */
            private final List<String> allowedValues = Arrays.asList("l", "r", "u", "bi", StringUtils.EMPTY);

            @Nullable
            @Override
            public String extractValue(@Nonnull final BaseObject qualifier)
            {
                final BaseStringProperty field = (BaseStringProperty) qualifier.getField(getProperty());
                final String value = field == null ? null : field.getValue();
                return this.allowedValues.contains(value) ? value : null;
            }

            @Nullable
            @Override
            public String extractValue(@Nonnull final JSONObject qualifier)
            {
                final String value = qualifier.optString(getJsonProperty(), null);
                return this.allowedValues.contains(value) ? value : null;
            }

            @Override
            public boolean valueIsValid(@Nullable final Object value)
            {
                return value == null || value instanceof String && this.allowedValues.contains(value);
            }
        },

        /** Any notes entered for the qualifier. */
        NOTES("notes")
        {
            @Nullable
            @Override
            public String extractValue(@Nonnull final BaseObject qualifier)
            {
                final BaseStringProperty field = (BaseStringProperty) qualifier.getField(getProperty());
                return StringUtils.defaultIfBlank(field == null ? null : field.getValue(), null);
            }

            @Nullable
            @Override
            public String extractValue(@Nonnull final JSONObject qualifier)
            {
                final String value = qualifier.optString(getJsonProperty());
                return StringUtils.defaultIfBlank(value, null);
            }

            @Override
            public boolean valueIsValid(@Nullable final Object value)
            {
                return value == null || value instanceof String;
            }
        };

        /** A logger for the property class. */
        private final Logger logger = LoggerFactory.getLogger(CancerQualifier.CancerQualifierProperty.class);

        /** @see #getProperty() */
        private final String property;

        /**
         * Constructor that initializes the property.
         *
         * @param property the name of the qualifier property
         * @see #getName()
         */
        CancerQualifierProperty(@Nonnull final String property)
        {
            this.property = property;
        }

        /**
         * Extracts a value from {@code qualifier} for {@link #getProperty()}, iff the stored value in {@code qualifier}
         * is valid for the property.
         *
         * @param qualifier the {@link BaseObject} qualifier that contains various properties
         * @return a value for {@link #getProperty()} stored in {@code qualifier}; {@code null} if no such value stored
         */
        @Nullable
        public abstract Object extractValue(@Nonnull BaseObject qualifier);

        /**
         * Extracts a value from {@code qualifier} for {@link #getProperty()}.
         *
         * @param qualifier the {@link JSONObject} qualifier that contains various properties
         * @return a value for {@link #getProperty()} stored in {@code qualifier}; {@code null} if no such value stored
         */
        @Nullable
        public abstract Object extractValue(@Nonnull JSONObject qualifier);

        /**
         * Writes a value to {@code qualifier} for {@link #getProperty()}. Does not check validity prior to writing.
         *
         * @param qualifier the {@link BaseObject} qualifier object where data will be written
         * @param value the value to write
         * @param context the current {@link XWikiContext}
         */
        public void writeValue(@Nonnull final BaseObject qualifier,
                               @Nullable final Object value,
                               @Nonnull final XWikiContext context)
        {
            // No validations performed.
            qualifier.set(getProperty(), value, context);
        }

        /**
         * Writes a value to {@code cancer} for {@link #getProperty()}. Checks validity before attempting to write.
         *
         * @param qualifier the {@link BaseObject} cancer object where data will be written
         * @param value the value to write
         * @param context the current {@link XWikiContext}
         * @param checkValid checks validity iff true
         */
        public void writeValue(@Nonnull final BaseObject qualifier,
                               @Nullable final Object value,
                               @Nonnull final XWikiContext context,
                               boolean checkValid)
        {
            if (checkValid) {
                if (valueIsValid(value)) {
                    writeValue(qualifier, value, context);
                } else {
                    this.logger.warn("Could not write value [{}] for property [{}]", value, getJsonProperty());
                }
            } else {
                writeValue(qualifier, value, context);
            }
        }

        /**
         * Checks if the value selected for the type of property is valid.
         *
         * @param value the potential value for {@link #getProperty()}
         * @return true iff the value is valid
         */
        public abstract boolean valueIsValid(@Nullable Object value);

        @Override
        public String toString()
        {
            return getJsonProperty();
        }

        /**
         * Get the name of this property.
         *
         * @return the name of the property
         */
        @Nonnull
        public String getProperty()
        {
            return this.property;
        }

        /**
         * Returns the name of this property for JSON.
         *
         * @return the name of the property
         */
        @Nonnull
        public String getJsonProperty()
        {
            return this.property;
        }
    }

    /**
     * Writes cancer qualifier data to {@code doc}.
     *
     * @param doc the {@link XWikiDocument} that will store cancer qualifier data
     * @param context the current {@link XWikiContext}
     */
    void write(@Nonnull XWikiDocument doc, @Nonnull XWikiContext context);

    /**
     * Gets the value associated with the provided {@code property}, {@code null} if the {@code property} has no value
     * associated with it.
     *
     * @param property the {@link CancerQualifierProperty} of interest
     * @return the {@link Object value} associted with the {@code property}, {@code null} if no such value exists
     */
    @Nullable
    Object getProperty(@Nonnull CancerQualifierProperty property);
}
