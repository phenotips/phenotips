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

    private String patientId;

    private List<Class<?>> linkedActionableInterfaces;

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
     * Set the patient whose permissions are being managed. Setting a patient is <b>mandatory if</b>
     * {@link #withActionableResources(Class...) links to other resources} are requested.
     *
     * @param patientId the {@link org.phenotips.data.Patient#getId() identifier} of the managed patient
     * @return self, for chaining method calls
     */
    public LinkBuilder withTargetPatient(String patientId)
    {
        this.patientId = patientId;
        return this;
    }

    /**
     * Add other resources that should be linked to. {@link #withTargetPatient(String) Setting a patient} is also
     * required if related resources are added.
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
     * Build the link collection, if the state of the builder is {@link #validateSelf() valid}.
     *
     * @return a collection of links, may be empty
     */
    public Collection<Link> build()
    {
        List<Link> links = new LinkedList<>();
        try {
            this.validateSelf();
        } catch (Exception e) {
            return links;
        }
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

    private String getPath(UriInfo uriInfo, Class<?> restInterface, Object... params)
    {
        return uriInfo.getBaseUriBuilder().path(restInterface).build(params).toString();
    }

    private Set<String> getAllowedMethods(Class<?> restInterface, AccessLevel accessLevel)
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
