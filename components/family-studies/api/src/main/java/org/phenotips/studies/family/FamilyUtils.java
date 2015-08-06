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

import com.xpn.xwiki.XWikiContext;
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

    /** XWiki class that contains rights to XWiki documents. */
    EntityReference RIGHTS_CLASS =
        new EntityReference("XWikiRights", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /** The set of rights awarded to any user that holds edit rights on any patient record that belongs to a family. */
    String DEFAULT_RIGHTS = "view,edit";

    /**
     * A wrapper around {@link com.xpn.xwiki.XWiki#getDocument(EntityReference, XWikiContext)}.
     *
     * @param docRef cannot be null
     * @return the result of calling {@link com.xpn.xwiki.XWiki#getDocument(EntityReference, XWikiContext)}
     * @throws XWikiException that can be thrown by
     *             {@link com.xpn.xwiki.XWiki#getDocument(EntityReference, XWikiContext)}
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    XWikiDocument getDoc(EntityReference docRef) throws XWikiException;

    /**
     * Returns a family document tied to the `anchorDoc`. If the `anchorDoc` is a family document, returns `anchorDoc`.
     * If it is a patient document, attempts to get the family document to which the patient belongs.
     *
     * @param anchorDoc a document used as a starting point for searching for a family document
     * @return {@link null} if the `anchorDoc` is not a family document or a patient document (that belongs to a
     *         family). Otherwise returns the family document.
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    XWikiDocument getFamilyDoc(XWikiDocument anchorDoc) throws XWikiException;

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
     * Inserts a reference to a family document into a patient document. Does not save the patient document.
     *
     * @param patientDoc which should be linked to the family
     * @param familyDoc family document which the patient is a member of
     * @param context {@link XWikiContext}
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    void setFamilyReference(XWikiDocument patientDoc, XWikiDocument familyDoc, XWikiContext context)
        throws XWikiException;

    /**
     * Overwrites the list of family members to the one passed in. Saves the family document.
     *
     * @param familyDoc whose members should be updated
     * @param members the new list of family members
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    void setFamilyMembers(XWikiDocument familyDoc, List<String> members) throws XWikiException;

    /**
     * Some pedigrees may contain sensitive information, which should be displayed on every edit of the pedigree.
     *
     * @param familyDoc which might contain a warning message
     * @return if there is a warning to display, then returns the warning message, otherwise an empty string
     * @throws XWikiException one of many possible reasons for XWiki to fail
     */
    String getWarningMessage(XWikiDocument familyDoc) throws XWikiException;

    /**
     * Returns a url to a document (view mode).
     *
     * @param ref cannot be null
     * @return relative url from root
     */
    String getViewUrl(DocumentReference ref);
}
