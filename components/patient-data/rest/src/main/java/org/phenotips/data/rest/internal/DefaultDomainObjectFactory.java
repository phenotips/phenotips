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
package org.phenotips.data.rest.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.model.PatientSummary;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DomainObjectFactory}.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Unstable
@Component
@Singleton
public class DefaultDomainObjectFactory implements DomainObjectFactory
{
    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    /** Provides access to the underlying data storage. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public PatientSummary createPatientSummary(Patient patient, UriInfo uriInfo)
    {
        PatientSummary result = new PatientSummary();

        User currentUser = this.users.getCurrentUser();

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            return null;
        }

        XWikiDocument doc;
        try {
            doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
        } catch (Exception e) {
            return null;
        }
        result.withId(patient.getId()).withEid(patient.getExternalId());
        result.withCreatedBy(String.valueOf(patient.getReporter())).withLastModifiedBy(
            String.valueOf(doc.getAuthorReference()));
        result.withVersion(doc.getVersion());
        result.withCreatedOn(new DateTime(doc.getCreationDate()).withZone(DateTimeZone.UTC));
        result.withLastModifiedOn(new DateTime(doc.getDate()).withZone(DateTimeZone.UTC));
        return result;
    }
}
