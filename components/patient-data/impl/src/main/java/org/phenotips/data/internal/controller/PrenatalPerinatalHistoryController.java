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
 * Handles the information found in the "prenatal and perinatal history" section of the patient record.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { PatientDataController.class })
@Named("prenatalPerinatalHistory")
@Singleton
public class PrenatalPerinatalHistoryController extends AbstractComplexController<String>
{
    private static final String IVF = "ivf";

    private static final String ICSI = "icsi";

    private static final String ASSISTED_REPRODUCTION_IUI = "assistedReproduction_iui";

    private static final String ASSISTED_REPRODUCTION_FERTILITY_MEDS = "assistedReproduction_fertilityMeds";

    private static final String ASSISTED_REPRODUCTION_SURROGACY = "assistedReproduction_surrogacy";

    private static final String ASSISTED_REPRODUCTION_DONOR_EGG = "assistedReproduction_donoregg";

    private static final String ASSISTED_REPRODUCTION_DONOR_SPERM = "assistedReproduction_donorsperm";

    private static final String MULTIPLE_GESTATION = "multipleGestation";

    private static final String GESTATION_TWIN = "twinNumber";

    private List<String> booleans =
        Arrays.asList(MULTIPLE_GESTATION, ASSISTED_REPRODUCTION_IUI, IVF, ICSI, ASSISTED_REPRODUCTION_FERTILITY_MEDS,
            ASSISTED_REPRODUCTION_SURROGACY, ASSISTED_REPRODUCTION_DONOR_EGG, ASSISTED_REPRODUCTION_DONOR_SPERM);

    @Override
    public String getName()
    {
        return "prenatalPerinatalHistory";
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "prenatal_perinatal_history";
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList("gestation", MULTIPLE_GESTATION, GESTATION_TWIN, ASSISTED_REPRODUCTION_IUI, IVF, ICSI,
            ASSISTED_REPRODUCTION_FERTILITY_MEDS, ASSISTED_REPRODUCTION_SURROGACY, ASSISTED_REPRODUCTION_DONOR_EGG,
            ASSISTED_REPRODUCTION_DONOR_SPERM);
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
