/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.phenotips.measurements;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

/**
 * Bridge offering access to specific {@link MeasurementHandler measurement handlers} to scripts.
 * 
 * @version $Id$
 * @since 1.0M3
 */
@Component
@Named("measurements")
@Singleton
public class MeasurementsScriptService implements ScriptService
{
    /** Fuzzy value representing a measurement value considered extremely below normal. */
    private static final String VALUE_EXTREME_BELOW_NORMAL = "extreme-below-normal";

    /** Fuzzy value representing a measurement value considered below normal, but not extremely. */
    private static final String VALUE_BELOW_NORMAL = "below-normal";

    /** Fuzzy value representing a measurement value considered normal. */
    private static final String VALUE_NORMAL = "normal";

    /** Fuzzy value representing a measurement value considered above normal, but not extremely. */
    private static final String VALUE_ABOVE_NORMAL = "above-normal";

    /** Fuzzy value representing a measurement value considered extremely above normal. */
    private static final String VALUE_EXTREME_ABOVE_NORMAL = "extreme-above-normal";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the different measurement handlers by name at runtime. */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    /**
     * Get the handler for a specific kind of measurements.
     * 
     * @param measurementType the type of measurement to return
     * @return the requested handler, {@code null} if not found
     */
    public MeasurementHandler get(String measurementType)
    {
        try {
            return this.componentManager.get().getInstance(MeasurementHandler.class, measurementType);
        } catch (ComponentLookupException ex) {
            this.logger.warn("Requested unknown measurement type [{}]", measurementType);
            return null;
        }
    }

    /**
     * Get all the measurements handlers.
     * 
     * @return a list of all the measurement handlers, or an empty list if there was a problem retrieving the actual
     *         list
     */
    public List<MeasurementHandler> getAvailableMeasurementHandlers()
    {
        try {
            return this.componentManager.get().getInstanceList(MeasurementHandler.class);
        } catch (ComponentLookupException ex) {
            this.logger.warn("Failed to list available measurement types", ex);
            return Collections.emptyList();
        }
    }

    public Set<String> getAvailableMeasurementNames()
    {
        try {
            return this.componentManager.get().getInstanceMap(MeasurementHandler.class).keySet();
        } catch (ComponentLookupException ex) {
            this.logger.warn("Failed to list available measurement types", ex);
            return Collections.emptySet();
        }
    }

    /**
     * Convert a percentile number into a string grossly describing the value.
     * 
     * @param percentile a number between 0 and 100
     * @return the percentile description
     */
    public String getFuzzyValue(int percentile)
    {
        String returnValue = VALUE_NORMAL;
        if (percentile <= 1) {
            returnValue = VALUE_EXTREME_BELOW_NORMAL;
        } else if (percentile <= 3) {
            returnValue = VALUE_BELOW_NORMAL;
        } else if (percentile >= 99) {
            returnValue = VALUE_EXTREME_ABOVE_NORMAL;
        } else if (percentile >= 97) {
            returnValue = VALUE_ABOVE_NORMAL;
        }
        return returnValue;
    }

    /**
     * Convert a standard deviation number into a string grossly describing the value.
     * 
     * @param deviation standard deviation value
     * @return the deviation description
     */
    public String getFuzzyValue(double deviation)
    {
        String returnValue = VALUE_NORMAL;
        if (deviation <= -3.0) {
            returnValue = VALUE_EXTREME_BELOW_NORMAL;
        } else if (deviation <= -2.0) {
            returnValue = VALUE_BELOW_NORMAL;
        } else if (deviation >= 3.0) {
            returnValue = VALUE_EXTREME_ABOVE_NORMAL;
        } else if (deviation >= 2.0) {
            returnValue = VALUE_ABOVE_NORMAL;
        }
        return returnValue;
    }
}
