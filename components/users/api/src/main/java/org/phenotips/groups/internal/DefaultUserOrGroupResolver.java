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

import org.phenotips.groups.UserOrGroupResolver;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link UserOrGroupResolver}.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
public class DefaultUserOrGroupResolver implements UserOrGroupResolver
{
    private static final EntityReference XWIKI_SPACE = new EntityReference("XWiki", EntityType.SPACE);

    private static final EntityReference GROUPS_SPACE = new EntityReference("Groups", EntityType.SPACE);

    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public EntityReference resolve(String id)
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        EntityReference user = resolveUser(id, this.currentResolver, wiki, context);
        if (user != null) {
            return user;
        }
        return resolveGroup(id, this.currentResolver, wiki, context);
    }

    private static EntityReference resolveUser(String id, EntityReferenceResolver<String> resolver,
        XWiki wiki, XWikiContext context)
    {
        EntityReference userReference = resolver.resolve(id, EntityType.DOCUMENT, XWIKI_SPACE);
        return returnIfExists(userReference, wiki, context);
    }

    private static EntityReference resolveGroup(String id, EntityReferenceResolver<String> resolver,
        XWiki wiki, XWikiContext context)
    {
        EntityReference groupReference = resolver.resolve(id, EntityType.DOCUMENT, GROUPS_SPACE);
        return returnIfExists(groupReference, wiki, context);
    }

    private static EntityReference returnIfExists(EntityReference reference, XWiki wiki, XWikiContext context)
    {
        if (wiki.exists(new DocumentReference(reference), context)) {
            return reference;
        }
        return null;
    }
}
