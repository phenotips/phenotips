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
package org.phenotips.studies.family.response;

import org.phenotips.components.ComponentManagerRegistry;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import java.util.Locale;

/**
 * Passed around to preserve important error information. Holds onto a status (modelled after HTTP statuses), a message,
 * and an error type.
 *
 * @version $Id$
 */
public enum StatusResponse
{

    /**
     * Patient can be added to a family.
     */
    OK(200, ""),

    /**
     * Duplicate patient in list.
     */
    DUPLICATE_PATIENT(400, "duplicate"),

    /**
     * Patient cannot be added to a family because it is already associated with another family.
     */
    ALREADY_HAS_FAMILY(501, "familyConflict"),

    /**
     * Patient cannot be added to a family because current user has insufficient permissions on family.
     */
    INSUFFICIENT_PERMISSIONS_ON_FAMILY(401, "familyPermissions"),

    /**
     * Patient cannot be added to a family because current user has insufficient permissions on family.
     */
    INSUFFICIENT_PERMISSIONS_ON_PATIENT(401, "patientPermissions"),

    /**
     * No members to add to family.
     **/
    FAMILY_HAS_NO_MEMBERS(402, "invalidUpdate"),

    /**
     * Invalid patient id.
     */
    INVALID_PATIENT_ID(404, "invalidPatientId"),

    /**
     * Invalid family id.
     */
    INVALID_FAMILY_ID(404, "invalidFamilyId"),

    /**
     * Unknown error.
     */
    UNKNOWN_ERROR(500, "unknown");

    private static LocalizationManager localizationManager;

    private static LocalizationContext localizationContext;

    /** Renders content blocks into plain strings. */
    private static BlockRenderer renderer;

    private int statusCode;

    private String errorType;

    static {
        try {
            StatusResponse.localizationManager =
                ComponentManagerRegistry.getContextComponentManager().getInstance(LocalizationManager.class);
            StatusResponse.localizationContext =
                ComponentManagerRegistry.getContextComponentManager().getInstance(LocalizationContext.class);
            StatusResponse.renderer =
                ComponentManagerRegistry.getContextComponentManager().getInstance(BlockRenderer.class, "plain/1.0");
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    StatusResponse(int statusCode, String errorType)
    {
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    /**
     * @return status code of the response
     */
    public int getStatusCode()
    {
        return this.statusCode;
    }

    /**
     * @return error type of the response
     */
    public String getErrorType()
    {
        return this.errorType;
    }

    /**
     * @return format of message
     */
    public String getMessageFormat()
    {
        String messageKey = this.getClass().getSimpleName() + "." + this.toString();
        return StatusResponse.translate(messageKey);
    }

    /**
     * Checks if the response is valid.
     *
     * @return true is the response is valid.
     */
    public boolean isValid()
    {
        return this.getStatusCode() == StatusResponse.OK.getStatusCode();
    }

    private static String translate(String key)
    {
        Locale currentLocale = StatusResponse.localizationContext.getCurrentLocale();
        Translation translation = StatusResponse.localizationManager.getTranslation(key, currentLocale);
        if (translation == null) {
            return "";
        }
        Block block = translation.render(StatusResponse.localizationContext.getCurrentLocale());

        // Render the block
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        StatusResponse.renderer.render(block, wikiPrinter);

        return wikiPrinter.toString();
    }

};
