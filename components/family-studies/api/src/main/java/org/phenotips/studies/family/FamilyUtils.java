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

import org.phenotips.Constants;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.List;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Utility methods for manipulating families.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Role
public interface FamilyUtils
{
    /** XWiki class that represents a family. */
    EntityReference FAMILY_CLASS =
        new EntityReference("FamilyClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** XWiki class that represents objects that contain a string reference to a family document. */
    EntityReference FAMILY_REFERENCE =
        new EntityReference("FamilyReference", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * Interfaces with the old family studies, which recorded external patient id and relationship to the `patient`.
     *
     * @param patient whose relatives to get
     * @return a collection of relatives stored in the old family stuides in the record of `patient`
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    Collection<String> getRelatives(XWikiDocument patient) throws XWikiException;

    /**
     * Gets the reference to the document of a family that the patient belongs to.
     *
     * @param patientDoc document of the patient whose family is of interest
     * @return null if the patient does not belong to a family; a valid reference otherwise
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    EntityReference getFamilyReference(XWikiDocument patientDoc) throws XWikiException;

    /**
     * {@see #getFamilyMembers(BaseObject)}.
     *
     * @param familyDoc XWiki object containing family information will be extracted from this document
     * @return never {@link null}
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    List<String> getFamilyMembers(XWikiDocument familyDoc) throws XWikiException;

    /**
     * Lists the family members' ids.
     *
     * @param familyObject XWiki object present in family documents which contains the list of family members
     * @return never {@link null}
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    List<String> getFamilyMembers(BaseObject familyObject) throws XWikiException;

    /**
     * Returns a url to a document (view mode).
     *
     * @param ref cannot be null
     * @return relative url from root
     */
    String getViewUrl(DocumentReference ref);
}
