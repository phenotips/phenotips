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
import org.phenotips.data.permissions.PermissionsManager;

import org.xwiki.component.annotation.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;

/**
 * Default implementation of the {@link RESTActionResolver}.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Singleton
public class DefaultRESTActionResolver implements RESTActionResolver
{

    @Inject
    private PermissionsManager pm;

    @Override
    public Set<String> resolveActions(Class restInterface, AccessLevel accessLevel)
    {
        Set<String> accessLevelMethods = this.getHTTPMethodsForAccessLevel(accessLevel);
        Set<String> interfaceMethods = this.getHTTPMethodsForInterface(restInterface);
        interfaceMethods.retainAll(accessLevelMethods);
        return interfaceMethods;

    }

    private Set<String> getHTTPMethodsForInterface(Class restInterface)
    {
        Method[] methods = restInterface.getMethods();
        Set<String> classAnnotations = new HashSet<>();
        for (Method method : methods) {
            for (Annotation annotation : method.getAnnotations()) {
                classAnnotations.add(annotation.annotationType().getSimpleName());
            }
        }
        return classAnnotations;
    }

    private Set<String> getHTTPMethodsForAccessLevel(AccessLevel level)
    {
        Set<String> result = new HashSet<>();
        if (level.compareTo(this.pm.resolveAccessLevel("view")) >= 0) {
            result.add(HttpMethod.GET);
        }
        if (level.compareTo(this.pm.resolveAccessLevel("manage")) >= 0) {
            result.add(HttpMethod.PUT);
            result.add(HttpMethod.POST);
            result.add(HttpMethod.DELETE);
        }
        return result;
    }
}
