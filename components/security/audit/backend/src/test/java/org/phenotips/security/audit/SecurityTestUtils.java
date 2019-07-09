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
package org.phenotips.security.audit;

import java.util.Calendar;

/**
 * Utils class.
 *
 * @version $Id$
 */
public final class SecurityTestUtils
{
    private SecurityTestUtils()
    {
        // Empty constructor
    }

    /**
     * Creates a Calendar instance.
     * @param deltaTimeInMillis the number of milliseconds to sleep before creating a new instance
     * @return a new {@link Calendar} instance
     */
    public static Calendar getCalendar(long deltaTimeInMillis)
    {
        if (deltaTimeInMillis > 0) {
            try {
                Thread.sleep(deltaTimeInMillis);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }

        return Calendar.getInstance();
    }
}
