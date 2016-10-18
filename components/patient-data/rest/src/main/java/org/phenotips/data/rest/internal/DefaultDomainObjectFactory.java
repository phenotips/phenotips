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
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.model.Alternative;
import org.phenotips.data.rest.model.Alternatives;
import org.phenotips.data.rest.model.PatientSummary;
import org.phenotips.rest.Autolinker;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DomainObjectFactory}.
 *
 * @version $Id$
 * @since 1.2M5
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

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public PatientSummary createPatientSummary(Patient patient, UriInfo uriInfo)
    {
        if (patient == null) {
            return null;
        }
        User currentUser = this.users.getCurrentUser();

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocumentReference())) {
            return null;
        }

        PatientSummary result = new PatientSummary();

        XWikiDocument doc;
        try {
            // TODO use getDocument()
            doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocumentReference());
        } catch (Exception e) {
            return null;
        }
        result.withId(patient.getId()).withEid(patient.getExternalId());
        result.withCreatedBy(String.valueOf(patient.getReporter())).withLastModifiedBy(
            String.valueOf(doc.getAuthorReference()));
        result.withVersion(doc.getVersion());
        result.withCreatedOn(new DateTime(doc.getCreationDate()).withZone(DateTimeZone.UTC));
        result.withLastModifiedOn(new DateTime(doc.getDate()).withZone(DateTimeZone.UTC));
        result.withLinks(this.autolinker.get().forSecondaryResource(PatientResource.class, uriInfo)
            .withExtraParameters("patient-id", patient.getId())
            .build());
        return result;
    }

    @Override
    public PatientSummary createPatientSummary(Object[] summaryData, UriInfo uriInfo)
    {
        if (summaryData == null || summaryData.length != 7
            || !(summaryData[3] instanceof Date && summaryData[6] instanceof Date)) {
            return null;
        }
        PatientSummary result = new PatientSummary();
        User currentUser = this.users.getCurrentUser();
        DocumentReference doc = this.stringResolver.resolve(String.valueOf(summaryData[0]));

        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(), doc)) {
            return null;
        }

        result.withId(doc.getName()).withEid(StringUtils.defaultString((String) summaryData[1]));
        result.withCreatedBy(String.valueOf(summaryData[2])).withLastModifiedBy(
            String.valueOf(summaryData[5]));
        result.withVersion(String.valueOf(summaryData[4]));
        result.withCreatedOn(new DateTime(summaryData[3]).withZone(DateTimeZone.UTC));
        result.withLastModifiedOn(new DateTime(summaryData[6]).withZone(DateTimeZone.UTC));
        result.withLinks(this.autolinker.get().forSecondaryResource(PatientResource.class, uriInfo)
            .withExtraParameters("patient-id", doc.getName())
            .build());
        return result;
    }

    @Override
    public Alternatives createAlternatives(List<String> alternativeIdentifiers, UriInfo uriInfo)
    {
        Alternatives result = new Alternatives();
        User currentUser = this.users.getCurrentUser();
        result.withLinks(this.autolinker.get().forResource(getClass(), uriInfo).build());
        for (String id : alternativeIdentifiers) {
            DocumentReference reference = this.stringResolver.resolve(id, Patient.DEFAULT_DATA_SPACE);
            if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
                reference)) {
                continue;
            }
            result.getPatients().add(createAlternative(reference.getName(), uriInfo));
        }
        return result;
    }

    @Override
    public Alternative createAlternative(String id, UriInfo uriInfo)
    {
        Alternative result = new Alternative();
        result.withId(id);
        result.withLinks(this.autolinker.get().forSecondaryResource(PatientResource.class, uriInfo)
            .withExtraParameters("patient-id", id)
            .build());
        return result;
    }
}
