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
package org.phenotips.recordLocking.internal.authorization;

import org.phenotips.Constants;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * An authorization module to check if a given Patient Document has a lock on it. Will return false if a lock is found
 * regardless of which user is trying to edit the document.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("locked")
@Singleton
public class LockedAuthorizationModule implements AuthorizationModule
{
    /** The XClass used for patient lock objects. */
    private EntityReference lockClassReference = new EntityReference("PatientLock", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** Provides access to the current context. */
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public int getPriority()
    {
        return 110;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference entity)
    {
        if (!(entity instanceof DocumentReference)) {
            return null;
        }
        XWikiContext context = this.contextProvider.get();

        try {
            XWikiDocument doc = context.getWiki().getDocument((DocumentReference) entity, context);
            BaseObject lock = doc.getXObject(this.lockClassReference);
            if (lock != null && !access.isReadOnly()) {
                return Boolean.FALSE;
            }
        } catch (XWikiException | NullPointerException e) {
            return null;
        }

        return null;
    }
}
