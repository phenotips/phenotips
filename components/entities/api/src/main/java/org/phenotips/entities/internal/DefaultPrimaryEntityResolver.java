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
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.entities.spi.AbstractPrimaryEntityResolver;

import org.xwiki.component.annotation.Component;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Default implementation of the {@link PrimaryEntityResolver} component, which uses all the {@link PrimaryEntityManager
 * entity managers} registered in the component manager.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultPrimaryEntityResolver extends AbstractPrimaryEntityResolver
{
    private static final String SECURE = "secure";

    @Override
    protected boolean isValidManager(@Nonnull final PrimaryEntityManager<?> manager)
    {
        return !manager.getClass().getAnnotation(Named.class).value().endsWith(SECURE);
    }
}
