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
package org.phenotips.studies.family.script.response;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.studies.family.script.JSONResponse;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import java.util.Locale;

import org.json.JSONObject;

/**
 * Passed around to preserve important error information. Holds onto a status (modelled after HTTP statuses), a message,
 * and an error type.
 *
 * @version $Id$
 */
public abstract class AbstractJSONResponse implements JSONResponse
{
    protected enum PedigreeScriptServiceErrorMessage
    {
        /**
         * Duplicate patient in list.
         */
        DUPLICATE_PATIENT(),

        /**
         * Patient cannot be added to a family because it is already associated with another family.
         */
        ALREADY_HAS_FAMILY(),

        /**
         * Patient cannot be added to a family because current user has insufficient permissions on family.
         */
        INSUFFICIENT_PERMISSIONS_ON_FAMILY(),

        /**
         * Patient cannot be added to a family because current user has no edit permissions for the patient.
         */
        INSUFFICIENT_PERMISSIONS_ON_PATIENT_EDIT(),

        /**
         * Information about patient family can not be returned because current user has no view rights for the patient.
         */
        INSUFFICIENT_PERMISSIONS_ON_PATIENT_VIEW(),

        /**
         * Invalid patient id.
         */
        INVALID_PATIENT_ID(),

        /**
         * Invalid family id.
         */
        INVALID_FAMILY_ID(),

        /**
         * Invalid input JSON.
         */
        INVALID_INPUT_JSON(),

        /**
         * Currently this patient has no family.
         */
        PATIENT_HAS_NO_FAMILY(),

        /**
         * Unknown error.
         */
        UNKNOWN_ERROR();
    };

    private static LocalizationManager localizationManager;

    private static LocalizationContext localizationContext;

    /** Renders content blocks into plain strings. */
    private static BlockRenderer renderer;

    static {
        try {
            AbstractJSONResponse.localizationManager =
                ComponentManagerRegistry.getContextComponentManager().getInstance(LocalizationManager.class);
            AbstractJSONResponse.localizationContext =
                ComponentManagerRegistry.getContextComponentManager().getInstance(LocalizationContext.class);
            AbstractJSONResponse.renderer =
                ComponentManagerRegistry.getContextComponentManager().getInstance(BlockRenderer.class, "plain/1.0");
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    private static String translate(String key, Object... parameters)
    {
        Locale currentLocale = AbstractJSONResponse.localizationContext.getCurrentLocale();
        Translation translation = AbstractJSONResponse.localizationManager.getTranslation(key, currentLocale);
        if (translation == null) {
            return "";
        }
        Block block = translation.render(AbstractJSONResponse.localizationContext.getCurrentLocale(), parameters);

        // Render the block
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        AbstractJSONResponse.renderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }

    protected String getErrorMessage(PedigreeScriptServiceErrorMessage messageCode, Object... parameters)
    {
        String messageKey = messageCode.getClass().getSimpleName() + "." + messageCode.toString();
        return AbstractJSONResponse.translate(messageKey, parameters);
    }

    protected JSONObject baseErrorJSON(String message)
    {
        JSONObject json = new JSONObject();
        json.put("error", true);
        json.put("errorMessage", message);
        return json;
    }
}
