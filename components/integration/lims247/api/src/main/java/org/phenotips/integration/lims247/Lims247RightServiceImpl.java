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
package org.phenotips.integration.lims247;

import org.phenotips.data.Patient;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

/**
 * Rights checking class that respects the access level granted by a LIMS 24/7 server.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class Lims247RightServiceImpl extends XWikiCachingRightService implements XWikiRightService
{
    /** Resolver for user and group document references. */
    @SuppressWarnings("deprecation")
    private DocumentReferenceResolver<String> userAndGroupReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "user");

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        if (context != null && context.getRequest() != null) {
            XWikiUser user = context.getWiki().checkAuth(context);
            if (user != null) {
                DocumentReference userReference =
                    this.userAndGroupReferenceResolver
                        .resolve(user.getUser(), new WikiReference(context.getDatabase()));
                context.setUserReference(userReference);
            } else {
                context.setUserReference(null);
            }
            LimsAuthentication limsAuth =
                (LimsAuthentication) context.getRequest().getSession().getAttribute(Lims247AuthServiceImpl.SESSION_KEY);
            String access = (String) context.getRequest().getSession().getAttribute(Lims247AuthServiceImpl.ACCESS_KEY);
            if (doc.getXObject(Patient.CLASS_REFERENCE) != null && limsAuth != null
                && StringUtils.isNotEmpty(access)) {
                Right requested = actionToRight(action);
                Right granted = actionToRight(access);
                return requested.compareTo(granted) <= 0;
            }
        }

        return super.checkAccess(action, doc, context);
    }

    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        if (context != null && context.getRequest() != null && context.getRequest().getSession() != null) {
            LimsAuthentication limsAuth =
                (LimsAuthentication) context.getRequest().getSession().getAttribute(Lims247AuthServiceImpl.SESSION_KEY);
            @SuppressWarnings("deprecation")
            XWikiDocument doc = context.getWiki().getDocument(docname, context);
            String access = (String) context.getRequest().getSession().getAttribute(Lims247AuthServiceImpl.ACCESS_KEY);
            if (doc.getXObject(Patient.CLASS_REFERENCE) != null && limsAuth != null
                && StringUtils.equals(limsAuth.getUser().getUser(), username) && StringUtils.isNotEmpty(access)) {
                Right requested = actionToRight(right);
                Right granted = actionToRight(access);
                return requested.compareTo(granted) <= 0;
            }
        }

        return super.hasAccessLevel(right, username, docname, context);
    }
}
