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
package org.phenotips.data.permissions.rest.internal.utils;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.rest.internal.utils.annotations.Relation;
import org.phenotips.data.permissions.rest.model.Link;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

/**
 * Created by matthew on 2016-03-30.
 */
public class LinkBuilder
{
    private final RESTActionResolver actionResolver;

    private final UriInfo uriInfo;

    private AccessLevel accessLevel;

    private Class rootInterface;

    private String patientId;

    private List<Class> linkedActionableInterfaces;

    /**
     * Basic constructor, initializes a new factory instance with no link configuration.
     *
     * @param uriInfo the URI used for accessing the current resource
     * @param actionResolver the action resolver instance to use
     */
    public LinkBuilder(UriInfo uriInfo, RESTActionResolver actionResolver)
    {
        if (uriInfo == null) {
            throw new IllegalArgumentException("uriInfo cannot be null");
        }
        this.uriInfo = uriInfo;
        if (actionResolver == null) {
            throw new IllegalArgumentException("actionResolver cannot be null");
        }
        this.actionResolver = actionResolver;
        this.linkedActionableInterfaces = new LinkedList<>();
    }

    public static String getRel(Class restInterface)
    {
        String relation = null;
        Relation relationAnnotation = (Relation) restInterface.getAnnotation(Relation.class);
        if (relationAnnotation != null) {
            relation = relationAnnotation.value();
        }
        return relation;
    }

    public LinkBuilder withAccessLevel(AccessLevel accessLevel)
    {
        this.accessLevel = accessLevel;
        return this;
    }

    public LinkBuilder withRootInterface(Class restInterface)
    {
        this.rootInterface = restInterface;
        return this;
    }

    public LinkBuilder withTargetPatient(String patientId)
    {
        this.patientId = patientId;
        return this;
    }

    public LinkBuilder withActionableResources(Class... restInterfaces)
    {
        for (Class arg : restInterfaces) {
            this.linkedActionableInterfaces.add(arg);
        }
        return this;
    }

    public Collection<Link> build()
    {
        try {
            this.validateSelf();
        } catch (Exception e) {
            return null;
        }
        List<Link> links = new LinkedList<>();
        if (this.rootInterface != null) {
            links.add(this.getActionableLinkToSelf());
        }
        for (Class endpoint : this.linkedActionableInterfaces) {
            links.add(this.getActionableLink(endpoint));
        }
        return links;
    }

    private Link getActionableLink(Class endpoint)
    {
        Link link = new Link();

        link.withHref(this.getPath(this.uriInfo, endpoint, this.patientId));
        link.withRel(LinkBuilder.getRel(endpoint));
        link.withAllowedMethods(this.getAllowedMethods(endpoint, this.accessLevel));

        return link;
    }

    private void validateSelf() throws Exception
    {
        if (!this.linkedActionableInterfaces.isEmpty()) {
            // has actionable links, make sure other fields are present
            if (this.accessLevel == null || this.patientId == null) {
                throw new Exception();
            }
        }
    }

    private String getPath(UriInfo uriInfo, Class restInterface, String... params)
    {
        return uriInfo.getBaseUriBuilder().path(restInterface).build(params).toString();
    }

    private Set<String> getAllowedMethods(Class restInterface, AccessLevel accessLevel)
    {
        return this.actionResolver.resolveActions(restInterface, accessLevel);
    }

    private Link getActionableLinkToSelf()
    {
        return new Link()
            .withRel("self")
            .withAllowedMethods(this.getAllowedMethods(this.rootInterface, this.accessLevel))
            .withHref(this.uriInfo.getRequestUri().toString());
    }
}
