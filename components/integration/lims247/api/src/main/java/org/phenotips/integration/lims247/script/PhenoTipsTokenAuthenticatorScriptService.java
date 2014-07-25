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
package org.phenotips.integration.lims247.script;

import org.phenotips.integration.lims247.Lims247AuthServiceImpl;
import org.phenotips.integration.lims247.LimsAuthentication;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;

/**
 * Validates a token for a specified user, in response to an authentication check sent by a remote LIMS server.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Component
@Named("tokenValidator")
@Singleton
public class PhenoTipsTokenAuthenticatorScriptService implements ScriptService
{
    /** Does the actual token validation. */
    @Inject
    private CSRFToken token;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /**
     * Check if the specified token is valid for the specified user.
     *
     * @param username the username whose authentication to check
     * @param token the token to check
     * @return {@code true} if the token is valid for the user, {@code false} otherwise
     */
    public boolean isTokenValid(String username, String token)
    {
        XWikiContext context =
            (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

        // First check if the token is valid on LIMS
        LimsAuthentication limsAuth =
            (LimsAuthentication) context.getRequest().getSession().getAttribute(Lims247AuthServiceImpl.SESSION_KEY);
        if (limsAuth != null && StringUtils.equals(limsAuth.getToken(), token)
            && StringUtils.equals(StringUtils.substringAfter(limsAuth.getUser().getUser(), "."), username)) {
            return true;
        }
        DocumentReference previousUserReference = context.getUserReference();

        // Check if the token is valid in PhenoTips
        boolean result = false;
        try {
            DocumentReference ref = new DocumentReference(context.getDatabase(), "XWiki", username);
            context.setUserReference(ref);
            result = this.token.isTokenValid(token);
            if (!result) {
                ref = ref.replaceParent(ref.getWikiReference(), new WikiReference("xwiki"));
                context.setUserReference(ref);
                result = this.token.isTokenValid(token);
            }
        } finally {
            context.setUserReference(previousUserReference);
        }
        return result;
    }
}
