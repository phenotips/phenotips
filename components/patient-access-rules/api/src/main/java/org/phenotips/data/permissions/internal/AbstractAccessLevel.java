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

import org.phenotips.data.permissions.AccessLevel;

import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @version $Id$
 */
public abstract class AbstractAccessLevel implements AccessLevel
{
    /**
     * The level of permission this level grants to a user; lower values mean more restrictions, higher values mean more
     * permissions. Should be a number between 0 (no access) and 100 (full access).
     */
    private final int permissiveness;

    private final boolean assignable;

    /** Provides access to the current locale. */
    @Inject
    private LocalizationContext lc;

    /** Provides access to translations. */
    @Inject
    private LocalizationManager lm;

    /** Renders content blocks into plain strings. */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer renderer;

    /**
     * Simple constructor passing the permissiveness level.
     *
     * @param permissiveness the permissiveness to set, see {@link #permissiveness}
     */
    protected AbstractAccessLevel(int permissiveness, boolean assignable)
    {
        this.permissiveness = permissiveness;
        this.assignable = assignable;
    }

    @Override
    public String getLabel()
    {
        String key = "phenotips.permissions.accessLevels." + getName() + ".label";
        Translation translation = this.lm.getTranslation(key, this.lc.getCurrentLocale());
        if (translation == null) {
            return getName();
        }
        Block block = translation.render(this.lc.getCurrentLocale());

        // Render the block
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        this.renderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }

    @Override
    public String getDescription()
    {
        String key = "phenotips.permissions.accessLevels." + getName() + ".description";
        Translation translation = this.lm.getTranslation(key, this.lc.getCurrentLocale());
        if (translation == null) {
            return "";
        }
        Block block = translation.render(this.lc.getCurrentLocale());

        // Render the block
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        this.renderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }

    @Override
    public boolean isAssignable()
    {
        return this.assignable;
    }

    @Override
    public int compareTo(AccessLevel o)
    {
        if (o != null && o instanceof AbstractAccessLevel) {
            return this.permissiveness - ((AbstractAccessLevel) o).permissiveness;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof AccessLevel)) {
            return false;
        }
        AccessLevel otherLevel = (AccessLevel) other;
        return getName().equals(otherLevel.getName());
    }

    @Override
    public int hashCode()
    {
        return getName().hashCode() + (isAssignable() ? 13 : 29);
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
