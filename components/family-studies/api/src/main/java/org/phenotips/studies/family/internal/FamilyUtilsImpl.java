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
package org.phenotips.studies.family.internal;

import org.phenotips.Constants;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;

/**
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 */
@Component
@Singleton
public class FamilyUtilsImpl implements FamilyUtils
{
    private static final EntityReference RELATIVE_REFERENCE =
        new EntityReference("RelativeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String FAMILY_REFERENCE_FIELD = "reference";

    private static final String FAMILY_MEMBERS_FIELD = "members";

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Override
    public EntityReference getFamilyReference(XWikiDocument patientDoc) throws XWikiException
    {
        if (patientDoc == null) {
            throw new IllegalArgumentException("Document reference for the patient was null");
        }
        BaseObject familyPointer = patientDoc.getXObject(FAMILY_REFERENCE);
        if (familyPointer != null) {
            String familyDocName = familyPointer.getStringValue(FAMILY_REFERENCE_FIELD);
            if (StringUtils.isNotBlank(familyDocName)) {
                return this.referenceResolver.resolve(familyDocName, Family.DATA_SPACE);
            }
        }
        return null;
    }

    @Override
    public Collection<String> getRelatives(XWikiDocument patientDoc) throws XWikiException
    {
        if (patientDoc != null) {
            List<BaseObject> relativeObjects = patientDoc.getXObjects(RELATIVE_REFERENCE);
            if (relativeObjects == null) {
                return Collections.emptySet();
            }
            Set<String> relativeIds = new HashSet<String>();
            for (BaseObject relative : relativeObjects) {
                String id = relative.getStringValue("relative_of");
                if (StringUtils.isNotBlank(id)) {
                    relativeIds.add(id);
                }
            }
            return relativeIds;
        }
        return Collections.emptySet();
    }

    @Override
    public List<String> getFamilyMembers(XWikiDocument familyDoc) throws XWikiException
    {
        return this.getFamilyMembers(familyDoc.getXObject(FAMILY_CLASS));
    }

    @Override
    public List<String> getFamilyMembers(BaseObject familyObject) throws XWikiException
    {
        DBStringListProperty xwikiRelativesList = (DBStringListProperty) familyObject.get(FAMILY_MEMBERS_FIELD);
        return xwikiRelativesList == null ? new LinkedList<String>() : xwikiRelativesList.getList();
    }

    @Override
    public String getViewUrl(DocumentReference ref)
    {
        XWikiContext context = this.provider.get();
        return context.getWiki().getURL(ref, "view", context);
    }
}
