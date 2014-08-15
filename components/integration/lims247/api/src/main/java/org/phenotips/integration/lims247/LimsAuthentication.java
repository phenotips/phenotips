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

import org.xwiki.stability.Unstable;

import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Cached LIMS credentials, storing the original installation ID (the PN), the authentication token, and the resulting
 * PhenoTips user.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public final class LimsAuthentication
{
    /** @see #getToken() */
    private final String token;

    /** @see #getUser(). */
    private final XWikiUser user;

    /** @see #getPn() */
    private final String pn;

    /**
     * Constructor passing all the data.
     *
     * @param token the authentication token sent by LIMS
     * @param user the authenticated user
     * @param pn the LIMS instance identifier
     */
    public LimsAuthentication(String token, XWikiUser user, String pn)
    {
        this.token = token;
        this.user = user;
        this.pn = pn;
    }

    /**
     * The authentication token sent by LIMS that was used to authenticate the user.
     *
     * @return the token, validated by LIMS
     */
    public String getToken()
    {
        return this.token;
    }

    /**
     * An authenticated user, as used by XWiki/PhenoTips.
     *
     * @return the user, must not be {@code null}
     */
    public XWikiUser getUser()
    {
        return this.user;
    }

    /**
     * The LIMS instance identifier that originated the authentication.
     *
     * @return an identifier, with a corresponding LIMS Server configuration object present in the global administration
     */
    public String getPn()
    {
        return this.pn;
    }
}
