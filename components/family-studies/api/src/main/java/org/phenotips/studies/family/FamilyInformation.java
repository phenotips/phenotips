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

import org.xwiki.component.annotation.Role;

import java.util.Map;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;

/**
 * Used for creating getting information about a family to be passed on to a different component.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Role
public interface FamilyInformation
{
    /**
     * Generates the response that describes the family and its members.
     *
     * @param familyDoc the XWiki document of the family
     * @return JSON with info about the family, each member and the current user's permissions
     * @throws XWikiException could occur while getting the warning message
     */
    JSON getBasicInfo(XWikiDocument familyDoc) throws XWikiException;

    /**
     * A family page should be able to represent patient medical reports.
     *
     * @param familyDoc to determine which patient medical reports to get
     * @return patient ids mapped to medical reports, which in turn are maps of report name to its link
     * @throws XWikiException could occur while getting family members
     */
    Map<String, Map<String, String>> getMedicalReports(XWikiDocument familyDoc) throws XWikiException;
}

