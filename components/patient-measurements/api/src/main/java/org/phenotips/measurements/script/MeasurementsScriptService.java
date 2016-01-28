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
import org.phenotips.measurements.MeasurementHandlersSorter;
import org.phenotips.measurements.internal.MeasurementUtils;

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

import org.slf4j.Logger;

import com.xpn.xwiki.api.Object;

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

    /** Provides sorting for measurement handlers using configured ordering. */
    @Inject
    private MeasurementHandlersSorter measurementHandlersSorter;

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
            Collections.sort(result, measurementHandlersSorter.getMeasurementHandlerComparator());
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
                Set<String> result = new TreeSet<String>(measurementHandlersSorter.getMeasurementNameComparator());
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
        return MeasurementUtils.getFuzzyValue(percentile);
    }

    /**
     * Convert a standard deviation number into a string grossly describing the value.
     *
     * @param deviation standard deviation value
     * @return the deviation description
     */
    public String getFuzzyValue(double deviation)
    {
        return MeasurementUtils.getFuzzyValue(deviation);
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
        return MeasurementUtils.convertAgeStrToNumMonths(age);
    }

    /**
     * Mechanism for sorting measurement objects according to age string, in ascending order according to the parsed
     * value of the age string, in months.
     *
     * @version $Id $
     */
    private static final class MeasurementObjectAgeComparator implements Comparator<Object>
    {
        /** Singleton instance. */
        private static MeasurementObjectAgeComparator instance = new MeasurementObjectAgeComparator();

        /** Key for accessing age property. */
        private static final String AGE_KEY = "age";

        @Override
        public int compare(Object o1, Object o2)
        {
            double age1 = MeasurementUtils.convertAgeStrToNumMonths((String) o1.getProperty(AGE_KEY).getValue());
            double age2 = MeasurementUtils.convertAgeStrToNumMonths((String) o2.getProperty(AGE_KEY).getValue());

            if (age1 > age2) {
                return 1;
            } else if (age1 < age2) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Sort a list of measurement objects.
     *
     * @param objects the list of XWiki objects to sort
     */
    public void sortMeasurementObjectsByAge(List<Object> objects)
    {
        Collections.sort(objects, MeasurementObjectAgeComparator.instance);
    }
}
