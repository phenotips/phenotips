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
package org.phenotips.rest.internal;

import org.phenotips.rest.AllowedActionsResolver;
import org.phenotips.rest.Autolinker;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.RelatedResources;
import org.phenotips.rest.Relation;
import org.phenotips.rest.model.Link;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.security.authorization.Right;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriInfo;

/**
 * An improved factory class for automatically creating links between resources, depending on the permissions that the
 * current user has.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultAutolinker implements Autolinker
{
    @Inject
    private AllowedActionsResolver actionResolver;

    @Inject
    private Provider<List<XWikiRestComponent>> resources;

    private UriInfo uriInfo;

    private boolean subresource;

    private Class<?> baseResource;

    private Right grantedRight;

    private Set<Class<?>> linkedActionableInterfaces = new LinkedHashSet<>();

    private Map<String, String> extraParameters = new HashMap<>();

    @Override
    public DefaultAutolinker forResource(Class<?> baseResource, UriInfo uriInfo)
    {
        this.baseResource = findResourceInterface(baseResource);
        this.uriInfo = uriInfo;
        for (Entry<String, List<String>> entry : this.uriInfo.getPathParameters().entrySet()) {
            if (!entry.getValue().isEmpty() && !this.extraParameters.containsKey(entry.getKey())) {
                this.extraParameters.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return this;
    }

    @Override
    public DefaultAutolinker forSecondaryResource(Class<?> subResource, UriInfo uriInfo)
    {
        forResource(subResource, uriInfo);
        this.subresource = true;
        return this;
    }

    @Override
    public DefaultAutolinker withGrantedRight(Right right)
    {
        this.grantedRight = right;
        return this;
    }

    @Override
    public DefaultAutolinker withActionableResources(Class<?>... restInterfaces)
    {
        for (Class<?> arg : restInterfaces) {
            this.linkedActionableInterfaces.add(arg);
        }
        return this;
    }

    @Override
    public DefaultAutolinker withExtraParameters(Map<String, String> parameters)
    {
        this.extraParameters.putAll(parameters);
        return this;
    }

    @Override
    public DefaultAutolinker withExtraParameters(String parameterName, String value)
    {
        this.extraParameters.put(parameterName, value);
        return this;
    }

    @Override
    public Collection<Link> build()
    {
        List<Link> links = new LinkedList<>();
        if (this.subresource) {
            return buildForSecondaryResource();
        }
        if (this.baseResource != null) {
            links.add(this.getActionableLinkToSelf());
        }
        Set<Class<?>> endpoints = new LinkedHashSet<>(findChildResources());
        endpoints.add(getParentResource());
        endpoints.addAll(this.linkedActionableInterfaces);
        endpoints.addAll(getRelatedResources());
        for (Class<?> endpoint : endpoints) {
            if (endpoint != null) {
                Link link = this.getActionableLink(endpoint);
                if (link != null) {
                    links.add(link);
                }
            }
        }
        return links;
    }

    private Collection<Link> buildForSecondaryResource()
    {
        List<Link> links = new LinkedList<>();
        Set<Class<?>> endpoints = new LinkedHashSet<>();
        if (this.baseResource != null) {
            endpoints.add(this.baseResource);
        }
        endpoints.addAll(this.linkedActionableInterfaces);
        for (Class<?> endpoint : endpoints) {
            if (endpoint != null) {
                Link link = this.getActionableLink(endpoint);
                if (link != null) {
                    links.add(link);
                }
            }
        }
        return links;
    }

    private Link getActionableLink(Class<?> endpoint)
    {
        try {
            Link link = new Link()
                .withHref(this.getPath(endpoint))
                .withRel(getRel(endpoint))
                .withAllowedMethods(this.getAllowedMethods(endpoint));

            return link;
        } catch (IllegalArgumentException ex) {
            // The resource may need additional parameters, let's skip this link
            return null;
        }
    }

    private String getPath(Class<?> restInterface)
    {
        return this.uriInfo.getBaseUriBuilder().path(restInterface).buildFromMap(this.extraParameters).toString();
    }

    private Set<String> getAllowedMethods(Class<?> restInterface)
    {
        return this.actionResolver.resolveActions(restInterface, this.grantedRight);
    }

    private Link getActionableLinkToSelf()
    {
        return new Link()
            .withRel("self")
            .withAllowedMethods(this.getAllowedMethods(this.baseResource))
            .withHref(this.uriInfo.getRequestUri().toString());
    }

    private Class<?> getParentResource()
    {
        if (this.baseResource == null) {
            return null;
        }
        ParentResource parent = this.baseResource.getAnnotation(ParentResource.class);
        if (parent != null) {
            return parent.value();
        }
        return null;
    }

    private Set<Class<?>> getRelatedResources()
    {
        if (this.baseResource == null) {
            return Collections.emptySet();
        }
        Set<Class<?>> result = new LinkedHashSet<>();
        RelatedResources related = this.baseResource.getAnnotation(RelatedResources.class);
        if (related == null) {
            return result;
        }
        for (Class<?> resource : related.value()) {
            Class<?> clazz = findResourceInterface(resource);
            if (clazz != null) {
                result.add(findResourceInterface(resource));
            }
        }
        return result;
    }

    private Set<Class<?>> findChildResources()
    {
        Set<Class<?>> result = new LinkedHashSet<>();
        for (XWikiRestComponent resource : this.resources.get()) {
            Class<?> clazz = resource.getClass();
            while (clazz != null) {
                for (Class<?> i : clazz.getInterfaces()) {
                    ParentResource parentAnnotation = i.getAnnotation(ParentResource.class);
                    if (parentAnnotation != null && parentAnnotation.value().equals(this.baseResource)) {
                        result.add(findResourceInterface(resource));
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
        return result;
    }

    private Class<?> findResourceInterface(Object instance)
    {
        return findResourceInterface(instance.getClass());
    }

    private Class<?> findResourceInterface(Class<?> instance)
    {
        if (instance != null && instance.getAnnotation(Path.class) != null) {
            return instance;
        }
        Class<?> clazz = instance;
        while (clazz != null) {
            for (Class<?> i : clazz.getInterfaces()) {
                if (i.getAnnotation(Path.class) != null) {
                    return i;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Get the relation type specified in the {@code @Relation} annotation on the target class or one of its implemented
     * interfaces.
     *
     * @param resource the class of a REST resource, which must have a {@code @Relation} annotation
     * @return the specified relation type, usually in the form of an URL, or {@code null} if not set
     */
    private String getRel(Class<?> resource)
    {
        String relation = null;
        Relation relationAnnotation = resource.getAnnotation(Relation.class);
        if (relationAnnotation != null) {
            relation = relationAnnotation.value();
        }
        return relation;
    }
}
