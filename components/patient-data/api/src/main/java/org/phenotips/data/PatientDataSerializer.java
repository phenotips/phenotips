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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import net.sf.json.JSONObject;

/**
 * API that provides access to patient data. No access rights are checked here.
 * Make sure any component that extends this class is instantiated per lookup.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Role
public interface PatientDataSerializer
{
    /**
     * Plays the role of initialization function.
     * Given an XWiki document reference, extracts data and stores it in itself.
     *
     * @param documentReference The XWiki document reference object pointing the the patient object of interest.
     */
    void readDocument(DocumentReference documentReference);

    /**
     * Creates json from internal objects.
     *
     * @param json Existing json object to which the data will be appended.
     */
    void writeJSON(JSONObject json);

    /**
     * Reads json and stores it in the internal class schema.
     *
     * @param json The json that is to be imported.
     */
    void readJSON(JSONObject json);
}
