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
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Handles the information found in the family history section of the patient record.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { PatientDataController.class })
@Named("familyHistory")
@Singleton
public class FamilyHistoryController extends AbstractComplexController<Integer>
{
    private static final String CONSANGUINITY = "consanguinity";

    private static final String MISCARRIAGES = "miscarriages";

    private static final String AFFECTED_RELATIVES = "affectedRelatives";

    private List<String> booleans = Arrays.asList(CONSANGUINITY, MISCARRIAGES, AFFECTED_RELATIVES);

    @Override
    public String getName()
    {
        return "familyHistory";
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "family_history";
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList(CONSANGUINITY, MISCARRIAGES, AFFECTED_RELATIVES);
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return this.booleans;
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }
}
