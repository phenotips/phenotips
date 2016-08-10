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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

/**
 * An improved factory class for automatically creating links between resources, depending on the permissions that the
 * current user has.
 *
 * @version $Id$
 * @since 1.3M2
 */
public class LinkBuilder
{
    private final RESTActionResolver actionResolver;

    private final UriInfo uriInfo;

    private AccessLevel accessLevel;

    private Class<?> rootInterface;

    private List<Class<?>> linkedActionableInterfaces;

    private Map<String, String> extraParameters = new HashMap<>();

    /**
     * Basic constructor, initializes a new factory instance with no link configuration.
     *
     * @param uriInfo the URI used for accessing the {@link #withRootInterface(Class) current resource}
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
        for (Entry<String, List<String>> entry : this.uriInfo.getPathParameters().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                this.extraParameters.put(entry.getKey(), entry.getValue().get(0));
            }
        }
    }

    /**
     * Get the relation type specified in the {@code @Relation} annotation on the target class.
     *
     * @param restInterface the class of a REST resource, which must have a {@code @Relation} annotation
     * @return the specified relation type, usually in the form of an URL, or {@code null} if not set
     */
    public static String getRel(Class<?> restInterface)
    {
        String relation = null;
        Relation relationAnnotation = restInterface.getAnnotation(Relation.class);
        if (relationAnnotation != null) {
            relation = relationAnnotation.value();
        }
        return relation;
    }

    /**
     * Set the access level that the current user has on the main entity. This access level limits which actions are
     * available, and thus can be linked to.
     *
     * @param accessLevel the access level of the current user
     * @return self, for chaining method calls
     */
    public LinkBuilder withAccessLevel(AccessLevel accessLevel)
    {
        this.accessLevel = accessLevel;
        return this;
    }

    /**
     * Set the resource for which to generate links. If this is set, then a {@code self} link will be generated.
     *
     * @param restInterface a class, may be {@code null}
     * @return self, for chaining method calls
     */
    public LinkBuilder withRootInterface(Class<?> restInterface)
    {
        this.rootInterface = restInterface;
        return this;
    }

    /**
     * Add other resources that should be linked to.
     *
     * @param restInterfaces a list of other REST resources to be added
     * @return self, for chaining method calls
     */
    public LinkBuilder withActionableResources(Class<?>... restInterfaces)
    {
        for (Class<?> arg : restInterfaces) {
            this.linkedActionableInterfaces.add(arg);
        }
        return this;
    }

    /**
     * Add or replace path parameter values that may be used in the link generation.
     *
     * @param parameters additional parameter values to be used, may be empty; the map keys are the parameter names, as
     *            used in the path specification, and the map values are the desired values
     * @return self, for chaining method calls
     */
    public LinkBuilder withExtraParameters(Map<String, String> parameters)
    {
        this.extraParameters.putAll(parameters);
        return this;
    }

    /**
     * Add or replace a path parameter value that may be used in the link generation.
     *
     * @param parameterName the name of the path parameter
     * @param value the value to use, replacing any previous value that may have been set before
     * @return self, for chaining method calls
     */
    public LinkBuilder withExtraParameters(String parameterName, String value)
    {
        this.extraParameters.put(parameterName, value);
        return this;
    }

    /**
     * Build the link collection.
     *
     * @return a collection of links, may be empty
     */
    public Collection<Link> build()
    {
        List<Link> links = new LinkedList<>();
        if (this.rootInterface != null) {
            links.add(this.getActionableLinkToSelf());
        }
        for (Class<?> endpoint : this.linkedActionableInterfaces) {
            links.add(this.getActionableLink(endpoint));
        }
        return links;
    }

    private Link getActionableLink(Class<?> endpoint)
    {
        Link link = new Link()
            .withHref(this.getPath(endpoint))
            .withRel(getRel(endpoint))
            .withAllowedMethods(this.getAllowedMethods(endpoint));

        return link;
    }

    private String getPath(Class<?> restInterface)
    {
        return this.uriInfo.getBaseUriBuilder().path(restInterface).buildFromMap(this.extraParameters).toString();
    }

    private Set<String> getAllowedMethods(Class<?> restInterface)
    {
        return this.actionResolver.resolveActions(restInterface, this.accessLevel);
    }

    private Link getActionableLinkToSelf()
    {
        return new Link()
            .withRel("self")
            .withAllowedMethods(this.getAllowedMethods(this.rootInterface))
            .withHref(this.uriInfo.getRequestUri().toString());
    }
}
