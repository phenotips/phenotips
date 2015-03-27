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
package org.phenotips.studies.family;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

@Role
public interface FamilyUtils
{
    EntityReference FAMILY_CLASS =
        new EntityReference("FamilyClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    EntityReference PEDIGREE_CLASS =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    EntityReference FAMILY_REFERENCE =
        new EntityReference("FamilyReference", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    EntityReference RIGHTS_CLASS =
        new EntityReference("XWikiRights", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    XWikiDocument getDoc(EntityReference docRef) throws XWikiException;

    XWikiDocument getFromDataSpace(String id) throws XWikiException;

    XWikiDocument getFamilyDoc(XWikiDocument anchorDoc) throws XWikiException;

    XWikiDocument getFamilyOfPatient(String patientId) throws XWikiException;

    JSONObject getPedigree(XWikiDocument doc);

    Collection<String> getRelatives(XWikiDocument patient) throws XWikiException;

    XWikiDocument createFamilyDoc(String patientId) throws NamingException, QueryException, XWikiException;

    XWikiDocument createFamilyDoc(XWikiDocument patient) throws NamingException, QueryException, XWikiException;

    XWikiDocument createFamilyDoc(boolean save) throws NamingException, QueryException, XWikiException;

    EntityReference getFamilyReference(XWikiDocument patientDoc) throws XWikiException;

    List<String> getFamilyMembers(XWikiDocument familyDoc) throws XWikiException;

    List<String> getFamilyMembers(BaseObject familyObject) throws XWikiException;

    void setFamilyReference(XWikiDocument patientDoc, XWikiDocument familyDoc, XWikiContext context)
        throws XWikiException;

    void setFamilyMembers(XWikiDocument familyDoc, List<String> members) throws XWikiException;

    List<Set<String>> getEntitiesWithEditAccess(XWikiDocument patientDoc);
}
