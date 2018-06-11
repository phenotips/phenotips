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
package org.phenotips.security.audit.rest.internal;

import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.AuditStore;
import org.phenotips.security.audit.rest.AuditStoreResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation for {@link AuditStoreResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.security.audit.rest.internal.HibernateAuditStoreResource")
@Singleton
public class HibernateAuditStoreResource extends XWikiResource implements AuditStoreResource
{
    private static final List<String> ACTION_VALUES = Arrays.asList("view", "edit", "get", "export");

    @Inject
    private AuditStore auditStore;

    @Inject
    private UserManager users;

    /** Resolves unprefixed document names to the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Override
    @SuppressWarnings("ParameterNumber")
    public Response listEvents(int start, int number, String action, String userId, String ip, String entityId,
        String fromTime, String toTime)
    {
        JSONArray eventsList = new JSONArray();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
        Calendar from = Calendar.getInstance();
        try {
            from.setTime(sdf.parse(fromTime));
        } catch (Exception e) {
            from.setTimeInMillis(0);
        }

        Calendar to = Calendar.getInstance();
        try {
            if (to.after(from)) {
                to.setTime(sdf.parse(toTime));
            } else {
                to.setTimeInMillis(System.currentTimeMillis());
            }
        } catch (Exception e) {
            to.setTimeInMillis(System.currentTimeMillis());
        }

        DocumentReference entity = StringUtils.isNotBlank(entityId) ? this.resolver.resolve(entityId) : null;
        User user = StringUtils.isNotBlank(userId) ? this.users.getUser(userId) : null;
        String actionId = ACTION_VALUES.contains(action) ? action : null;
        String ipValue = StringUtils.isNotBlank(ip) ? ip : null;

        AuditEvent eventTemplate = new AuditEvent(user, ipValue, actionId, null, entity, null);
        List<AuditEvent> results = this.auditStore.getEvents(eventTemplate, from, to, start, number);

        for (AuditEvent event : results) {
            eventsList.put(event.toJSON());
        }

        JSONObject response = new JSONObject();
        response.put("data", eventsList);

        return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
