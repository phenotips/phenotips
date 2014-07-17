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

import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.internal.XWikiCachingRightService;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Set;
import java.util.TreeSet;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 * @since 1.0RC1
 */
public class ModularRightServiceImpl extends XWikiCachingRightService implements XWikiRightService
{
    private UserManager userManager;

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        User user = getUserManager().getCurrentUser();
        Boolean decision = checkRights(actionToRight(action), user, doc.getDocumentReference().toString());
        if (decision != null) {
            return decision.booleanValue();
        }

        return super.checkAccess(action, doc, context);
    }

    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        User user = getUserManager().getUser(username, true);
        Boolean decision = checkRights(actionToRight(right), user, docname);
        if (decision != null) {
            return decision.booleanValue();
        }

        return super.hasAccessLevel(right, username, docname, context);
    }

    @SuppressWarnings("deprecation")
    private Boolean checkRights(Right access, User user, String docname)
    {
        PatientRepository repo = Utils.getComponent(PatientRepository.class);
        Patient patient = repo.getPatientById(docname);
        if (patient != null) {
            Set<AuthorizationService> services = new TreeSet<AuthorizationService>();
            services.addAll(Utils.getComponentList(AuthorizationService.class));
            for (AuthorizationService service : services) {
                Boolean decision = service.hasAccess(access, user, patient);
                if (decision != null) {
                    return decision;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private UserManager getUserManager()
    {
        if (this.userManager == null) {
            this.userManager = Utils.getComponent(UserManager.class);
        }
        return this.userManager;
    }
}
