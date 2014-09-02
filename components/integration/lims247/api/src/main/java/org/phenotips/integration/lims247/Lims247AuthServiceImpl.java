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

import org.phenotips.Constants;

import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Authentication class that trusts a registered LIMS 24/7 server for authentication. The workflow is:
 * <ol>
 * <li>The user logs in on LIMS.</li>
 * <li>When LIMS opens PhenoTips as an embedded page, it also sends the following authentication parameters in the URL:
 * <ul>
 * <li>pn=lims instance identifier</li>
 * <li>username=common username</li>
 * <li>auth_token=authentication token valid in LIMS</li>
 * </ul>
 * </li>
 * <li>PhenoTips checks if a LIMS instance with the given {@code pn} identifier is registered in the configuration. If
 * no, then this authentication fails.</li>
 * <li>Otherwise, the token is validated by sending back the username and token to the token check service on that
 * instance of LIMS.</li>
 * <li>If the token is validated, then the user identified by the username is successfully logged in PhenoTips, for the
 * duration of the current session.</li>
 * </ol>
 *
 * @version $Id$
 * @since 1.0M8
 */
public class Lims247AuthServiceImpl extends XWikiLDAPAuthServiceImpl implements XWikiAuthService
{
    /** Key used for storing the logged in user in the session. */
    public static final String SESSION_KEY = "lims247_user";

    /** Key used for storing the access (view or edit) granted to the user in the session. */
    public static final String ACCESS_KEY = "lims247_access";

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        XWikiUser user = getUserFromSession(context);
        if (user != null) {
            this.logger.debug("Previously authenticated LIMS user found in the session: [{}]", user.getUser());
            setupContextForLims(context);
            storeAccesMode(context);
            return user;
        }
        XWikiRequest request = context.getRequest();
        String pn = request.get(LimsServer.INSTANCE_IDENTIFIER_KEY);
        String username = request.get(LimsServer.USERNAME_KEY);
        String token = request.get(LimsServer.TOKEN_KEY);
        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(token)) {
            user = checkLocalToken(token, username, context);
            if (user == null && StringUtils.isNotEmpty(pn)) {
                user = checkRemoteToken(token, username, pn, context);
            }
            if (user != null) {
                storeUserInSession(new LimsAuthentication(token, user, pn), context);
                setupContextForLims(context);
                storeAccesMode(context);
                return user;
            }
        }
        // LIMS authentication failed, try with the default form-based authentication
        return super.checkAuth(context);
    }

    /**
     * Check if the current session already contains a logged in LIMS user. If yes, then the that user is returned.
     *
     * @param context the current request context
     * @return the user found in the session, or {@code null} if no LIMS user is logged in
     */
    private XWikiUser getUserFromSession(XWikiContext context)
    {
        try {
            LimsAuthentication cachedAuth =
                (LimsAuthentication) context.getRequest().getSession().getAttribute(SESSION_KEY);
            return cachedAuth != null ? cachedAuth.getUser() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Store a validated log in in the current session to reduce the number of checks sent to LIMS.
     *
     * @param auth the authentication information to store in the session
     * @param context the current request context, used for accessing the session
     */
    private void storeUserInSession(LimsAuthentication auth, XWikiContext context)
    {
        context.getRequest().getSession().setAttribute(SESSION_KEY, auth);
    }

    /**
     * Check if the authentication parameters sent in the URL are validated by the local XWiki server.
     *
     * @param token the authentication token
     * @param username the target username
     * @param context the current request context
     * @return a valid user if the authentication is validated, or {@code null} if the token is not valid
     */
    private XWikiUser checkLocalToken(String token, String username, XWikiContext context)
    {
        DocumentReference previousUserReference = context.getUserReference();
        boolean isValid = false;
        try {
            @SuppressWarnings("deprecation")
            CSRFToken tokenChecker = Utils.getComponent(CSRFToken.class);
            DocumentReference ref = new DocumentReference(context.getDatabase(), XWiki.SYSTEM_SPACE, username);
            context.setUserReference(ref);
            isValid = tokenChecker.isTokenValid(token);
            if (!isValid) {
                ref = ref.replaceParent(ref.getWikiReference(), new WikiReference("xwiki"));
                context.setUserReference(ref);
                isValid = tokenChecker.isTokenValid(token);
            }
        } finally {
            context.setUserReference(previousUserReference);
        }
        if (isValid) {
            return toXWikiUser(username, context);
        }
        return null;
    }

    /**
     * Check if the authentication parameters sent in the URL are validated by the remote LIMS server.
     *
     * @param token the authentication token
     * @param username the target username
     * @param pn the LIMS instance identifier
     * @param context the current request context
     * @return a valid user if the authentication is validated, or {@code null} in case of error: unknown instance
     *         identifier, invalid token
     */
    @SuppressWarnings("deprecation")
    private XWikiUser checkRemoteToken(String token, String username, String pn, XWikiContext context)
    {
        if (Utils.getComponent(LimsServer.class).checkToken(token, username, pn)) {
            return toXWikiUser(username, context);
        }
        return null;
    }

    /**
     * Configure the context so that the PhenoTips instance is better embedded inside LIMS.
     *
     * @param context the current request context
     */
    private void setupContextForLims(XWikiContext context)
    {
        if (context.getWiki().exists(
            new DocumentReference(context.getDatabase(), Constants.CODE_SPACE, "EmbeddableSkin"), context)) {
            context.put("skin", Constants.CODE_SPACE + ".EmbeddableSkin");
        }
    }

    /**
     * Store the specified access mode, if any, in the session. This influences the {@link Lims247RightServiceImpl
     * custom rights implementation}, overriding the granted rights.
     *
     * @param context the current request context
     */
    private void storeAccesMode(XWikiContext context)
    {
        XWikiRequest request = context.getRequest();
        String access = request.getParameter(LimsServer.ACCESS_MODE);
        if (access != null) {
            request.getSession().setAttribute(ACCESS_KEY, access);
        }
    }

    /**
     * Convert a username to an XWikiUser object, taking care of proper escapes.
     *
     * @param username the username to process
     * @return an XWikiUser object holding the specified username
     */
    private XWikiUser toXWikiUser(String username, XWikiContext context)
    {
        DocumentReference ref = new DocumentReference(context.getDatabase(), XWiki.SYSTEM_SPACE, username);
        return new XWikiUser(ref.toString());
    }
}
