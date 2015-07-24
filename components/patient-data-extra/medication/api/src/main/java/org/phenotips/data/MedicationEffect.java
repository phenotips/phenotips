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

import org.xwiki.stability.Unstable;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * The effect that a medicine has on the patient's symptoms.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Unstable
public enum MedicationEffect
{
    /** Unknown effect. */
    UNKNOWN,
    /** The symptoms got worse. */
    WORSENING,
    /** No effect. */
    NONE,
    /** Slight improvement. */
    SLIGHT_IMPROVEMENT,
    /** Moderate improvement. */
    INTERMEDIATE_IMPROVEMENT,
    /** Strong improvement, the symptoms got visibly better. */
    STRONG_IMPROVEMENT;

    /**
     * Get the enum instance corresponding to either a CAPITAL_CASE or a camelCase version of its name.
     *
     * @param effect the serialized effect name
     * @return an instance of this enum, if one was found
     * @throws IllegalArgumentException if the effect name cannot be resolved to a known effect instance
     */
    public static MedicationEffect fromString(String effect)
    {
        if (StringUtils.isBlank(effect)) {
            return null;
        }
        return valueOf(effect.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString()
    {
        return String.format(this.name().toLowerCase().replaceAll("_.", "%S"),
            (Object[]) this.name().replaceAll("[^_]*_(.)[^-]*", "$1_").split("_"));
    }
}
