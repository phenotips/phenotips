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
package org.phenotips.measurements.script;

import org.phenotips.measurements.MeasurementHandler;
import org.phenotips.measurements.internal.AbstractMeasurementHandler;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

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
            List<MeasurementHandler> result = this.componentManager.get().getInstanceList(MeasurementHandler.class);
            if (result == null) {
                result = Collections.emptyList();
            }
            Collections.sort(result, MeasurementSorter.instance);
            return result;
        } catch (ComponentLookupException ex) {
            this.logger.warn("Failed to list available measurements", ex);
            return Collections.emptyList();
        }
    }

    /**
     * Get the names of all the measurements handlers.
     *
     * @return a set with the names of all the measurement handlers, or an empty set if there was a problem retrieving
     *         the actual values
     */
    public Set<String> getAvailableMeasurementNames()
    {
        try {
            Map<String, MeasurementHandler> handlers =
                this.componentManager.get().getInstanceMap(MeasurementHandler.class);
            if (handlers != null) {
                Set<String> result = new TreeSet<String>(MeasurementNameSorter.instance);
                result.addAll(handlers.keySet());
                return result;
            }
        } catch (ComponentLookupException ex) {
            this.logger.warn("Failed to list available measurement types", ex);
        }
        return Collections.emptySet();
    }

    /**
     * Convert a percentile number into a string grossly describing the value.
     *
     * @param percentile a number between 0 and 100
     * @return the percentile description
     */
    public String getFuzzyValue(int percentile)
    {
        return AbstractMeasurementHandler.getFuzzyValue(percentile);
    }

    /**
     * Convert a standard deviation number into a string grossly describing the value.
     *
     * @param deviation standard deviation value
     * @return the deviation description
     */
    public String getFuzzyValue(double deviation)
    {
        return AbstractMeasurementHandler.getFuzzyValue(deviation);
    }

    /**
     * Get the number of months corresponding to a period string.
     *
     * @param age the ISO-8601 period string, without leading 'P'
     * @throws IllegalArgumentException if the age cannot be parsed
     * @return number of months
     */
    public Double convertAgeStrToNumMonths(String age) throws IllegalArgumentException
    {
        return AbstractMeasurementHandler.convertAgeStrToNumMonths(age);
    }

    /**
     * Temporary mechanism for sorting measurements, uses a hardcoded list of measurements in the desired order.
     *
     * @version $Id$
     */
    private static final class MeasurementSorter implements Comparator<MeasurementHandler>
    {
        /** Hardcoded list of measurements and their order. */
        private static final String[] TARGET_ORDER = new String[] {"weight", "height", "bmi", "armspan", "sitting",
            "hc", "philtrum", "ear", "ocd", "icd", "pfl", "ipd", "hand", "palm", "foot"};

        /** Singleton instance. */
        private static MeasurementSorter instance = new MeasurementSorter();

        @Override
        public int compare(MeasurementHandler o1, MeasurementHandler o2)
        {
            String n1 = ((AbstractMeasurementHandler) o1).getName();
            String n2 = ((AbstractMeasurementHandler) o2).getName();
            int p1 = ArrayUtils.indexOf(TARGET_ORDER, n1);
            int p2 = ArrayUtils.indexOf(TARGET_ORDER, n2);
            if (p1 == -1 && p2 == -1) {
                return n1.compareTo(n2);
            } else if (p1 == -1) {
                return 1;
            } else if (p2 == -1) {
                return -1;
            }
            return p1 - p2;
        }
    }

    /**
     * Temporary mechanism for sorting measurements, uses a hardcoded list of measurements in the desired order.
     *
     * @version $Id$
     */
    private static final class MeasurementNameSorter implements Comparator<String>
    {
        /** Singleton instance. */
        private static MeasurementNameSorter instance = new MeasurementNameSorter();

        @Override
        public int compare(String n1, String n2)
        {
            int p1 = ArrayUtils.indexOf(MeasurementSorter.TARGET_ORDER, n1);
            int p2 = ArrayUtils.indexOf(MeasurementSorter.TARGET_ORDER, n2);
            if (p1 == -1 && p2 == -1) {
                return n1.compareTo(n2);
            } else if (p1 == -1) {
                return 1;
            } else if (p2 == -1) {
                return -1;
            }
            return p1 - p2;
        }
    }
}
