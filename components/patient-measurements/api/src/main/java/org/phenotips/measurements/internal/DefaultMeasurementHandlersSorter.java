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

package org.phenotips.measurements.internal;

import org.phenotips.measurements.MeasurementHandler;
import org.phenotips.measurements.MeasurementHandlersSorter;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Implementation of {@link MeasurementHandlersSorter} that performs an XWQL query to retrieve the configured
 * measurement ordering.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class DefaultMeasurementHandlersSorter implements MeasurementHandlersSorter
{
    /** Name of the XClass used to store measurements configuration. */
    private static final String CONFIG_CLASS_NAME = "PhenoTips.MeasurementsConfigurationClass";

    /** Name of the key on the configuration object that stores the ordering. */
    private static final String CONFIG_KEY_NAME = "ordering";

    /** Used for searching for getting the relevant config object. */
    @Inject
    private QueryManager qm;

    /** Used for logging. */
    @Inject
    private Logger logger;

    /** The ordered list of measurement names as retrieved from the configuration. */
    private List<String> orderedMeasurementNames;

    /** Comparator class to be used for comparing measurement handlers. */
    private MeasurementHandlerComparator measurementHandlerComparator;

    /** Comparator class to be used for comparing measurement names. */
    private MeasurementNameComparator measurementNameComparator;

    /**
     * Default constructor that instantiates the two comparators.
     */
    public DefaultMeasurementHandlersSorter()
    {
        this.measurementHandlerComparator = new MeasurementHandlerComparator();
        this.measurementNameComparator = new MeasurementNameComparator();
    }

    /**
     * Refresh the ordered list of measurement names from the configuration.
     */
    private void loadOrderedMeasurementsFromConfig()
    {
        try {
            Query q = this.qm.createQuery("select obj." + CONFIG_KEY_NAME + " from Document as doc, "
                + "doc.object(" + CONFIG_CLASS_NAME + ") as obj", Query.XWQL);
            this.orderedMeasurementNames = new ArrayList<>(q.<String>execute());
        } catch (QueryException ex) {
            this.logger.warn("Failed to fetch ordering configuration for measurement types");
            this.orderedMeasurementNames = new ArrayList<>();
        }
    }

    /**
     * Compare two measurement names using the ordered list of names.
     *
     * @param n1 the first measurement name
     * @param n2 the second measurement name
     * @return a negative int if n1 < n2 0 if n1 == n2 a positive int if n1 > n2
     */
    private int compareMeasurementNames(String n1, String n2)
    {
        int p1 = this.orderedMeasurementNames.indexOf(n1);
        int p2 = this.orderedMeasurementNames.indexOf(n2);
        if (p1 == -1 && p2 == -1) {
            return n1.compareTo(n2);
        } else if (p1 == -1) {
            return 1;
        } else if (p2 == -1) {
            return -1;
        }
        return p1 - p2;
    }

    /**
     * @return the singleton instance of the measurement handler comparator, reloading the configuration prior to
     *         returning.
     */
    public MeasurementHandlerComparator getMeasurementHandlerComparator()
    {
        this.loadOrderedMeasurementsFromConfig();

        return measurementHandlerComparator;
    }

    /**
     * @return the singleton instance of the measurement name comparator, reloading the configuration prior to
     *         returning.
     */
    public MeasurementNameComparator getMeasurementNameComparator()
    {
        this.loadOrderedMeasurementsFromConfig();

        return measurementNameComparator;
    }

    /**
     * Inner measurement handler comparator class.
     */
    public final class MeasurementHandlerComparator implements Comparator<MeasurementHandler>
    {
        @Override
        public int compare(MeasurementHandler o1, MeasurementHandler o2)
        {
            return DefaultMeasurementHandlersSorter.this.compareMeasurementNames(o1.getName(), o2.getName());
        }
    }

    /**
     * Inner measurement name comparator class.
     */
    public final class MeasurementNameComparator implements Comparator<String>
    {
        @Override
        public int compare(String o1, String o2)
        {
            return DefaultMeasurementHandlersSorter.this.compareMeasurementNames(o1, o2);
        }
    }
}
