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
package org.phenotips.security.audit.rest;

import org.phenotips.rest.ParentResource;
import org.phenotips.rest.RequiredAccess;

import org.xwiki.rest.resources.RootResource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Root resource for working with audit events.
 *
 * @version $Id$
 * @since 1.4
 */
@Path("/security/audit")
@ParentResource(RootResource.class)
public interface AuditResource
{
    /**
     * Retrieves audit events for filter parameters. Parameters fromTime and toTime define an interval for the time
     * stamp.
     *
     * @param start for large result set paging, the index of the first event to display in the returned page
     * @param number for large result set paging, how many events to display in the returned page
     * @param action the event type, for example {@code view}, {@code edit}, {@code export}, empty (meaning all)
     * @param userId the user whose events to retrieve, if empty events for all users returned
     * @param ip the ip where the request came from, if empty events for all ips returned
     * @param entityId a reference to the target entity
     * @param fromTime start of the interval for the time stamp filter. If parameter fromTime is {@code null}, matching
     *            events from the beginning will be retrieved.
     * @param toTime end of the interval for the time stamp filter. If parameter toTime is {@code null}, matching events
     *            until the present moment will be retrieved.
     * @return a list of audited events, may be empty
     */
    @GET
    @SuppressWarnings("checkstyle:ParameterNumber")
    @RequiredAccess("admin")
    Response listEvents(
        @QueryParam("start") @DefaultValue("0") int start,
        @QueryParam("number") @DefaultValue("50") int number,
        @QueryParam("action") @DefaultValue("") String action,
        @QueryParam("user") @DefaultValue("") String userId,
        @QueryParam("ip") @DefaultValue("") String ip,
        @QueryParam("entityId") @DefaultValue("") String entityId,
        @QueryParam("fromTime") @DefaultValue("") String fromTime,
        @QueryParam("toTime") @DefaultValue("") String toTime);
}
