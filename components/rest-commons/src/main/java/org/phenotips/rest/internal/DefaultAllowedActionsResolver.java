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
import org.phenotips.rest.RequiredAccess;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;

/**
 * Default implementation of the {@link AllowedActionsResolver}.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Singleton
public class DefaultAllowedActionsResolver implements AllowedActionsResolver
{
    @Override
    public Set<String> resolveActions(Class<?> restInterface, Right grantedRight)
    {
        Method[] methods = restInterface.getMethods();
        Set<String> result = new HashSet<>();
        for (Method method : methods) {
            for (Annotation annotation : method.getAnnotations()) {
                HttpMethod httpMethod = annotation.annotationType().getAnnotation(HttpMethod.class);
                if (httpMethod != null) {
                    RequiredAccess rightAnnotation = method.getAnnotation(RequiredAccess.class);
                    if (grantedRight != null && rightAnnotation != null) {
                        Right right = Right.toRight(rightAnnotation.value());
                        if (right == grantedRight || grantedRight.getImpliedRights() != null
                            && grantedRight.getImpliedRights().contains(right)) {
                            result.add(httpMethod.value());
                        }
                    } else {
                        result.add(httpMethod.value());
                    }
                }
            }
        }
        return result;
    }
}
