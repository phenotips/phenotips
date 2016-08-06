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
package org.phenotips.groups.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Document reference resolver that resolves either a user or a group reference. If neither a user or a group exists
 * with the specified ID, then {@code null} is returned instead of a valid reference.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("userOrGroup")
public class UserOrGroupDocumentEntityResolver implements DocumentReferenceResolver<String>
{
    private static final EntityReference USERS_SPACE = new EntityReference("XWiki", EntityType.SPACE);

    private static final EntityReference GROUPS_SPACE = new EntityReference("Groups", EntityType.SPACE);

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentResolver;

    @Inject
    private DocumentAccessBridge bridge;

    @Override
    public DocumentReference resolve(String id, Object... parameters)
    {
        DocumentReference user = resolveUser(id);
        if (user != null) {
            return user;
        }
        return resolveGroup(id);
    }

    private DocumentReference resolveUser(String id)
    {
        DocumentReference userReference = this.currentResolver.resolve(id, USERS_SPACE);
        return this.bridge.exists(userReference) ? userReference : null;
    }

    private DocumentReference resolveGroup(String id)
    {
        DocumentReference groupReference = this.currentResolver.resolve(id, GROUPS_SPACE);
        return this.bridge.exists(groupReference) ? groupReference : null;
    }
}
