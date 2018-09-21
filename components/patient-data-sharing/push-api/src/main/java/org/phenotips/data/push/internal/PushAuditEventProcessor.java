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
package org.phenotips.data.push.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.push.PushPatientService;
import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * If the request is for {@code PhenoTips.PushPatientService} with a {@code do=push} request parameter, which is how
 * currently the push functionality is invoked, it will replace the event with a {@code push} event on the actual pushed
 * document, with all the useful extra information stored in the event details: the server that it was pushed to, the
 * local user, the remote user, the target owner, the pushed fields.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("push-info")
@Singleton
public class PushAuditEventProcessor implements AuditEventProcessor
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> resolver;

    // Used for getting the stored remote username
    @Inject
    private PushPatientService pushService;

    @Override
    public AuditEvent process(AuditEvent event)
    {
        DocumentReference originalEntity = event.getEntity();
        XWikiRequest request = this.xcontextProvider.get().getRequest();
        if ("PushPatientService".equals(originalEntity.getName())
            && "PhenoTips".equals(originalEntity.getParent().getName())
            && "push".equals(request.getParameter("do"))) {
            String pushedEntityName = request.getParameter("patientid");
            DocumentReference pushedEntity = this.resolver.resolve(pushedEntityName, Patient.DEFAULT_DATA_SPACE);
            JSONObject extraInfo = new JSONObject();
            String serverId = request.getParameter("serverid");
            extraInfo.put("server", serverId);
            extraInfo.put("group", request.getParameter("groupname"));
            extraInfo.put("fields", request.getParameter("fields"));
            String remoteUser = request.getParameter("usr");
            if (StringUtils.isEmpty(remoteUser)) {
                remoteUser = this.pushService.getRemoteUsername(serverId);
            }
            extraInfo.put("remoteUser", remoteUser);
            return new AuditEvent(event.getUser(), event.getIp(), "push", extraInfo.toString(), pushedEntity,
                event.getTime());
        }
        return event;
    }
}
