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
package org.phenotips.data.permissions.rest.internal;

import org.phenotips.Constants;
import org.phenotips.data.permissions.Owner;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Helper class for extracting a name and email for an owner. This is needed because this information is stored
 * differently depending on the {@link Owner#getType() type} of owner.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component(roles = NameAndEmailExtractor.class)
public class NameAndEmailExtractor
{
    private static final EntityReference USER_OBJECT_REFERENCE =
        new EntityReference("XWikiUsers", EntityType.DOCUMENT, Constants.XWIKI_SPACE_REFERENCE);

    private static final EntityReference GROUP_OBJECT_REFERENCE =
        new EntityReference("PhenoTipsGroupClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> referenceResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * Extract the name and email of the specified {@link Owner owner}.
     *
     * @param type the owner type, should be one of {@code user} or {@code group}
     * @param reference a reference to the owner's backing document
     * @return a pair with the name on the left and the email on the right, or {@code null} if this information cannot
     *         be obtained for the specified owner
     */
    public Pair<String, String> getNameAndEmail(String type, EntityReference reference)
    {
        try {
            DocumentReference userRef = this.referenceResolver.resolve(reference);
            XWikiDocument entityDocument = (XWikiDocument) this.documentAccessBridge.getDocument(userRef);
            if (StringUtils.equals("group", type)) {
                return fetchFromGroup(entityDocument);
            } else if (StringUtils.equals("user", type)) {
                return fetchFromUser(entityDocument);
            }
        } catch (Exception ex) {
            this.logger.error("Could not load user's or group's document", ex.getMessage());
        }
        return null;
    }

    private Pair<String, String> fetchFromUser(XWikiDocument document)
    {
        BaseObject userObj = document.getXObject(USER_OBJECT_REFERENCE);
        String email = userObj.getStringValue("email");
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(userObj.getStringValue("first_name"));
        nameBuilder.append(" ");
        nameBuilder.append(userObj.getStringValue("last_name"));
        String name = nameBuilder.toString().trim();
        return Pair.of(name, email);
    }

    private Pair<String, String> fetchFromGroup(XWikiDocument document)
    {
        BaseObject groupObject = document.getXObject(GROUP_OBJECT_REFERENCE);
        String email = null;
        // if the group is of "PhenoTipsGroupClass"
        if (groupObject != null) {
            email = groupObject.getStringValue("contact");
        }
        String name = document.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontextProvider.get());
        return Pair.of(name, email);
    }
}
