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
package org.phenotips.security.authorization;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;
import org.xwiki.security.internal.XWikiConstants;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

/**
 * A bridge implementation of the legacy authorization checking service, forwarding the decision to the
 * {@link AuthorizationService} role.
 *
 * @version $Id$
 * @since 1.0M13
 */
public class ModularRightServiceImpl extends XWikiCachingRightService implements XWikiRightService
{
    /** Logging helper object. */
    private final Logger logger = LoggerFactory.getLogger(ModularRightServiceImpl.class);

    /** Converts usernames into proper {@link User} objects. */
    @SuppressWarnings("deprecation")
    private UserManager userManager = Utils.getComponent(UserManager.class);

    /** Resolver for document references. */
    @SuppressWarnings("deprecation")
    private DocumentReferenceResolver<String> documentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /** Converts usernames into document references pointing to the corresponding profile document. */
    @SuppressWarnings("deprecation")
    private DocumentReferenceResolver<String> userAndGroupReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "user");

    /** The actual component dealing with the authorization decision. */
    @SuppressWarnings("deprecation")
    private AuthorizationService service = Utils.getComponent(AuthorizationService.class);

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        DocumentReference userReference = getCurrentUser(context);
        User user = this.userManager.getUser(userReference != null ? userReference.toString() : null, true);
        boolean result = this.service.hasAccess(user, actionToRight(action), doc.getDocumentReference());
        if (!result && context.getUserReference() == null && !"login".equals(context.getAction())) {
            this.logger.debug("Redirecting unauthenticated user to login, since it have been denied [{}] on [{}].",
                actionToRight(action), doc.getDocumentReference());
            context.getWiki().getAuthService().showLogin(context);
        }

        return result;
    }

    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        User user = this.userManager.getUser(StringUtils.endsWith(username, "XWikiGuest") ? null : username, true);
        WikiReference wikiReference = new WikiReference(context.getDatabase());
        DocumentReference document = resolveDocumentName(docname, wikiReference);
        return this.service.hasAccess(user, actionToRight(right), document);
    }

    /**
     * Get the current user associated with this context.
     *
     * @param context the current request context
     * @return a reference to the current user's profile, or {@code null} if the user isn't authenticated (guest)
     */
    private DocumentReference getCurrentUser(XWikiContext context)
    {
        DocumentReference contextUserReference = context.getUserReference();
        DocumentReference userReference = contextUserReference;

        if (userReference == null) {
            try {
                XWikiUser user = context.getWiki().checkAuth(context);
                if (user != null) {
                    userReference = resolveUserName(user.getUser(), new WikiReference(context.getDatabase()));
                }
            } catch (XWikiException e) {
                // Authentication failure, this should have been logged downstream
            }
        }

        if (userReference != null && XWikiConstants.GUEST_USER.equals(userReference.getName())) {
            // Public users (not logged in) should be passed as null in the new API. It may happen that badly
            // design code, and poorly written API does not take care, so we prevent security issue here.
            userReference = null;
        }

        if (userReference != contextUserReference) {
            context.setUserReference(userReference);
        }

        return userReference;
    }

    /**
     * Convert usernames into document references.
     *
     * @param username name as a string
     * @param wikiReference default wiki, if not explicitly specified in the username
     * @return a document reference that uniquely identifies the user
     */
    private DocumentReference resolveUserName(String username, WikiReference wikiReference)
    {
        return this.userAndGroupReferenceResolver.resolve(username, wikiReference);
    }

    /**
     * @param docname name of the document as string.
     * @param wikiReference the default wiki where the document will be assumed do be located, unless explicitly
     *            specified in docname.
     * @return the document reference.
     */
    private DocumentReference resolveDocumentName(String docname, WikiReference wikiReference)
    {
        return this.documentReferenceResolver.resolve(docname, wikiReference);
    }
}
