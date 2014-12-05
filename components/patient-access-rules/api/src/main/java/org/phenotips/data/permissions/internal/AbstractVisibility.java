/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.Visibility;

import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

/**
 * @version $Id$
 */
public abstract class AbstractVisibility implements Visibility
{
    /** @see #getPermissiveness() */
    private int permissiveness;

    @Inject
    private LocalizationContext lc;

    @Inject
    private LocalizationManager lm;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer renderer;

    protected AbstractVisibility(int permissiveness)
    {
        this.permissiveness = permissiveness;
    }

    @Override
    public String getLabel()
    {
        String key = "phenotips.permissions.visibility." + getName() + ".label";
        Translation translation = this.lm.getTranslation(key, this.lc.getCurrentLocale());
        if (translation == null) {
            return StringUtils.capitalize(getName());
        }
        Block block = translation.render(this.lc.getCurrentLocale());

        // Render the block
        DefaultWikiPrinter wikiPrinter = new DefaultWikiPrinter();
        this.renderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }

    @Override
    public String getDescription()
    {
        String key = "phenotips.permissions.visibility." + getName() + ".description";
        Translation translation = this.lm.getTranslation(key, this.lc.getCurrentLocale());
        if (translation == null) {
            return "";
        }
        Block block = translation.render(this.lc.getCurrentLocale());

        // Render the block
        DefaultWikiPrinter wikiPrinter = new DefaultWikiPrinter();
        this.renderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }

    @Override
    public int compareTo(Visibility o)
    {
        if (o != null && o instanceof AbstractVisibility) {
            return this.permissiveness - ((AbstractVisibility) o).permissiveness;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof Visibility)) {
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
}
