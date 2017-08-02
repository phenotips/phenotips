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
package org.phenotips.measurements;

import org.xwiki.stability.Unstable;

import java.util.Collection;

import javax.ws.rs.core.MultivaluedMap;


/**
 * Handles measurements that are computed, depending upon values entered by the user.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public interface ComputedMeasurementHandler
{
    /**
     * Get a list of computation dependencies for this measurement.
     *
     * @return the names of the computation dependencies for this measurement
     */
    Collection<String> getComputationDependencies();

    /**
     * Handle computations for measurements by fetching the computational dependencies, parsing
     * them, and passing them to the {@link #compute(double, double) compute} method.
     *
     * @param params a map of available measurements entered by the user. It should include the
     *            dependencies needed for the computation.
     * @return computed value
     * @throws IllegalArgumentException if dependencies are missing, or if they cannot be parsed.
     */
    double handleComputation(MultivaluedMap<String, String> params) throws IllegalArgumentException;

    /**
     * Compute the measurement given its computational dependencies. This method
     * can be overloaded to allow for the computation of methods with various
     * numbers of dependencies.
     * The formula for computing each measurement is unique to that measurement.
     *
     * @param dep1 the first computational dependency for the measurement
     * @param dep2 the second computational dependency for the measurement
     * @return the computed measurement
     */
    double compute(double dep1, double dep2);
}
