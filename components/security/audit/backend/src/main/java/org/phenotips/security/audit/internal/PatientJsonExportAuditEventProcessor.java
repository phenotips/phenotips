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
package org.phenotips.security.audit.internal;

import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;

/**
 * If this is a request for {@code /get/PhenoTips/ExportPatient}, replaces the event with an "export (json)" action on
 * the actual patient record.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("patient-json-export")
@Singleton
public class PatientJsonExportAuditEventProcessor implements AuditEventProcessor
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> resolver;

    @Override
    public AuditEvent process(AuditEvent event)
    {
        String id = this.xcontextProvider.get().getRequest().getParameter("id");
        if ("get".equals(event.getAction()) && "ExportPatient".equals(event.getEntity().getName()) && id != null) {
            DocumentReference patient = this.resolver.resolve(id, new EntityReference("data", EntityType.SPACE));
            return new AuditEvent(event.getUser(), event.getIp(), "export", "json", patient,
                event.getTime());
        }
        return event;
    }
}
