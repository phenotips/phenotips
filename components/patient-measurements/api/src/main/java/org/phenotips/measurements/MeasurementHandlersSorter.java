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

import org.xwiki.component.annotation.Role;

import java.util.Comparator;

/**
 * Sorting utilities for measurement handlers using a configured ordering.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Role
public interface MeasurementHandlersSorter
{
    /**
     * @return a comparator instance for measurement handler objects. The configuration will be reloaded prior to the
     * instance being returned.
     */
    Comparator<MeasurementHandler> getMeasurementHandlerComparator();

    /**
     * @return a comparator instance for measurement names. The configuration will be reloaded prior to the instance
     * being returned.
     */
    Comparator<String> getMeasurementNameComparator();
}
