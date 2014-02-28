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
package org.phenotips.data;

import java.util.List;

import net.sf.json.JSONObject;

import net.sf.json.JSONObject;

/**
 * Non-essential pieces of custom patient data that can be part of the patient record.
 *
 * @param <T> the type of data being represented; usually a key-value pair, where the {@code key} is a String (name)
 * @see PatientDataController
 * @version $Id$
 * @since 1.0M10
 */
public interface PatientData<T> extends List<T>
{
    /**
     * The name of this custom data.
     *
     * @return a short string
     */
    String getName();
}
