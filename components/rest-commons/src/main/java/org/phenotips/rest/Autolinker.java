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
package org.phenotips.rest;

import org.phenotips.rest.model.Link;

import org.xwiki.component.annotation.Role;
import org.xwiki.security.authorization.Right;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

/**
 * An improved factory class for automatically creating links between resources, depending on the permissions that the
 * current user has.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Role
public interface Autolinker
{
    /**
     * Set the resource for which to generate links. Calling this method is mandatory. If a {@code null} base resource
     * is given, then the resources to be linked to must be explicitly identified via calls to the
     * {@link #withActionableResources} method.
     *
     * @param baseResource a class, may be {@code null} if links for embedded resources are to be generated
     * @param uriInfo the URI currently being requested, with information needed for generating the links; must not be
     *    {@code null}
     * @return self, for chaining method calls
     */
    Autolinker forResource(Class<?> baseResource, UriInfo uriInfo);

    /**
     * Set the access level that the current user has on the main entity. This access level limits which actions are
     * available, and thus can be linked to.
     *
     * @param right the access level of the current user
     * @return self, for chaining method calls
     */
    Autolinker withGrantedRight(Right right);

    /**
     * Add other resources that should be linked to.
     *
     * @param restInterfaces a list of other REST resources to be added
     * @return self, for chaining method calls
     */
    Autolinker withActionableResources(Class<?>... restInterfaces);

    /**
     * Add or replace path parameter values that may be used in the link generation.
     *
     * @param parameters additional parameter values to be used, may be empty; the map keys are the parameter names, as
     *            used in the path specification, and the map values are the desired values
     * @return self, for chaining method calls
     */
    Autolinker withExtraParameters(Map<String, String> parameters);

    /**
     * Add or replace a path parameter value that may be used in the link generation.
     *
     * @param parameterName the name of the path parameter
     * @param value the value to use, replacing any previous value that may have been set before
     * @return self, for chaining method calls
     */
    Autolinker withExtraParameters(String parameterName, String value);

    /**
     * Build the link collection.
     *
     * @return a collection of links, may be empty
     */
    Collection<Link> build();
}
