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
package org.phenotips.translation.internal;

import org.phenotips.translation.TranslationManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Singleton
public class TranslationManagerImplementation implements TranslationManager
{
    @Inject
    private LocalizationManager localizationManager;

    @Inject
    private LocalizationContext localizationContext;

    /** Renders content blocks into plain strings. */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer renderer;

    @Override
    public String translate(String key)
    {
        Locale currentLocale = this.localizationContext.getCurrentLocale();
        Translation translation = this.localizationManager.getTranslation(key, currentLocale);
        if (translation == null) {
            return "";
        }
        Block block = translation.render(currentLocale);

        // Render the block
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        this.renderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }
}
