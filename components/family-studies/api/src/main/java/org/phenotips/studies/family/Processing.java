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
package org.phenotips.studies.family;

import org.phenotips.studies.family.internal.JSONResponse;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

import net.sf.json.JSONObject;

/**
 * Provides a single method as entry point to main logic and processing.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Role
public interface Processing
{
    /**
     * Performs several operations on the passed in data, and eventually saves it into appropriate documents.
     *
     * @param patientId of patient to get a family to process from. If a patient does not belong to a family, a new
     *            family if created for the patient.
     * @param json (data) part of the pedigree JSON
     * @param image svg part of the pedigree JSON
     * @return {@link JSONResponse} with one of many possible statuses
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    JSONResponse processPatientPedigree(String patientId, JSONObject json, String image)
        throws XWikiException;
}
