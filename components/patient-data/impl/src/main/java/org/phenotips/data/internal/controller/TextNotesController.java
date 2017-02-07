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
package org.phenotips.data.internal.controller;

import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Handles textual notes, containing longer prose describing various aspects of the patient's information.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("notes")
@Singleton
public class TextNotesController extends AbstractSimpleController
{
    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList("indication_for_referral", "family_history", "prenatal_development", "medical_history",
            "diagnosis_notes", "genetic_notes");
    }

    @Override
    public String getName()
    {
        return "notes";
    }

    @Override
    protected String getJsonPropertyName()
    {
        return getName();
    }
}
