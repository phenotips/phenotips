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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.translation.TranslationManager;

import org.xwiki.rendering.renderer.BlockRenderer;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

/**
 * @version $Id$
 */
public abstract class AbstractVisibility implements Visibility
{
    /**
     * @see #getPermissiveness()
     */
    private int permissiveness;

    /** Provides access to translations. */
    @Inject
    private TranslationManager tm;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer renderer;

    @Inject
    private PermissionsConfiguration configuration;

    protected AbstractVisibility(int permissiveness)
    {
        this.permissiveness = permissiveness;
    }

    @Override
    public String getLabel()
    {
        String key = "phenotips.permissions.visibility." + getName() + ".label";
        String translation = this.tm.translate(key);
        if (StringUtils.isBlank(translation)) {
            return StringUtils.capitalize(getName());
        }
        return translation;
    }

    @Override
    public String getDescription()
    {
        String key = "phenotips.permissions.visibility." + getName() + ".description";
        return this.tm.translate(key);
    }

    @Override
    public int compareTo(Visibility o)
    {
        if (o instanceof AbstractVisibility) {
            return this.permissiveness - ((AbstractVisibility) o).permissiveness;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Visibility)) {
            return false;
        }
        Visibility otherVisibility = (Visibility) other;
        return getName().equals(otherVisibility.getName())
            && getDefaultAccessLevel().equals(otherVisibility.getDefaultAccessLevel());
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode() + getDefaultAccessLevel().hashCode();
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public int getPermissiveness()
    {
        return this.permissiveness;
    }

    @Override
    public boolean isDisabled()
    {
        return this.configuration.isVisibilityDisabled(getName());
    }
}
