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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;

import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Forbids access to VCF files if the user doesn't have at least edit rights on the patient record, since a VCF contains
 * very confidential information.
 *
 * @version $Id$
 * @since 1.1RC1
 */
@Component
@Named("vcf-access")
@Singleton
public class VCFAccessRestrictionEventListener extends AbstractEventListener
{
    /** The URL part separator. */
    private static final String SEPARATOR = "/";

    /** The name of the XWiki action used to download files. */
    private static final String ACTION = "download";

    /** Provides access to the patient record data model. */
    @Inject
    private PatientRepository patients;

    /** Checks the current user's access on the target patient record. */
    @Inject
    private PermissionsManager permissions;

    /** The threshold access level needed for getting access to the VCF. */
    @Inject
    @Named("edit")
    private AccessLevel edit;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public VCFAccessRestrictionEventListener()
    {
        super("vcf-access", new ActionExecutingEvent(ACTION), new ActionExecutingEvent(ACTION + "rev"));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;
        XWikiRequest request = context.getRequest();

        String path = request.getRequestURI();
        String filename;
        try {
            filename = URLDecoder.decode(getFileName(path), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // This really shouldn't happen, UTF-8 is mandatory
            return;
        }

        if (StringUtils.endsWithIgnoreCase(filename, ".vcf")) {
            XWikiDocument doc = context.getDoc();
            Patient patient = this.patients.getPatientById(doc.getDocumentReference().toString());
            PatientAccess access = this.permissions.getPatientAccess(patient);
            if (!access.hasAccessLevel(this.edit)) {
                ((CancelableEvent) event).cancel();
            }
        }
    }

    /**
     * Get the filename of the attachment from the path and the action.
     *
     * @param path the request URI
     * @return the filename of the attachment being downloaded
     */
    private static String getFileName(final String path)
    {
        final String subPath = path.substring(path.indexOf(SEPARATOR + ACTION));
        int pos = 0;
        for (int i = 0; i < 3; i++) {
            pos = subPath.indexOf(SEPARATOR, pos + 1);
        }
        if (subPath.indexOf(SEPARATOR, pos + 1) > 0) {
            return subPath.substring(pos + 1, subPath.indexOf(SEPARATOR, pos + 1));
        }
        return subPath.substring(pos + 1);
    }
}
