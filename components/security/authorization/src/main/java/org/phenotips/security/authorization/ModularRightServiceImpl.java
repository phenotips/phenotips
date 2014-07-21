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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;
import org.xwiki.security.internal.XWikiConstants;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

/**
 * A modular implementation of the legacy authorization checking service, forwarding the decision to instances of the
 * {@link AuthorizationModule} role.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class ModularRightServiceImpl extends XWikiCachingRightService implements XWikiRightService
{
    private final Logger logger = LoggerFactory.getLogger(ModularRightServiceImpl.class);

    @SuppressWarnings("deprecation")
    private UserManager userManager = Utils.getComponent(UserManager.class);

    @SuppressWarnings("deprecation")
    private DocumentReferenceResolver<String> userAndGroupReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "user");

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        DocumentReference userReference = getCurrentUser(context);
        User user = this.userManager.getUser(userReference != null ? userReference.toString() : null, true);
        Boolean decision = checkRights(user, actionToRight(action), doc.getDocumentReference().toString());
        if (decision != null) {
            return decision.booleanValue();
        }

        return super.checkAccess(action, doc, context);
    }

    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        User user = this.userManager.getUser(username, true);
        Boolean decision = checkRights(user, actionToRight(right), docname);
        if (decision != null) {
            return decision.booleanValue();
        }

        return super.hasAccessLevel(right, username, docname, context);
    }

    /**
     * Invoke the actual authorization modules in descending order of priority, until one of them responds with a non-
     * {@code null} decision. If none of the authorization modules responds, {@code null} is returned.
     *
     * @param user the user requesting access
     * @param access the requested access right
     * @param docname the document being accessed
     * @return {@code True} if access is granted, {@code False} if access is denied, {@code null} if no authorization
     *         module can determine if access should be granted or denied
     */
    @SuppressWarnings("deprecation")
    private Boolean checkRights(User user, Right access, String docname)
    {
        PatientRepository repo = Utils.getComponent(PatientRepository.class);
        Patient patient = repo.getPatientById(docname);
        if (patient != null) {
            List<AuthorizationModule> services = new LinkedList<>();
            services.addAll(Utils.getComponentList(AuthorizationModule.class));
            Collections.sort(services, AuthorizationModuleComparator.INSTANCE);
            for (AuthorizationModule service : services) {
                try {
                    Boolean decision = service.hasAccess(access, user, patient);
                    if (decision != null) {
                        return decision;
                    }
                } catch (Exception ex) {
                    // Don't fail because of bad authorization modules
                    this.logger.warn("Failed to invoke authorization service [{}]: {}",
                        service.getClass().getCanonicalName(), ex.getMessage());
                }
            }
        }
        return null;
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

        if (userReference == null && context.getMode() != XWikiContext.MODE_XMLRPC) {
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

        if (userReference != contextUserReference
            && (userReference == null || !userReference.equals(contextUserReference))) {
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

    private static final class AuthorizationModuleComparator implements Comparator<AuthorizationModule>
    {
        private static final AuthorizationModuleComparator INSTANCE = new AuthorizationModuleComparator();

        @Override
        public int compare(AuthorizationModule o1, AuthorizationModule o2)
        {
            if (o1 == null) {
                return (o2 == null) ? 0 : 1;
            }
            return (o2 == null) ? -1 : o2.getPriority() - o1.getPriority();
        }

    }
}
