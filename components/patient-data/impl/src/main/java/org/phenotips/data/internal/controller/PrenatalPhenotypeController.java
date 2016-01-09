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
import org.phenotips.data.VocabularyProperty;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.LinkedList;
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
@Named("prenatalPhenotype")
@Singleton
public class PrenatalPhenotypeController extends AbstractComplexController<List<VocabularyProperty>>
{
    private static final String PRENATAL_PHENOTYPE = "prenatal_phenotype";

    private static final String NEGATIVE_PRENATAL_PHENOTYPE = "negative_prenatal_phenotype";

    private List<String> hpoCodes = Arrays.asList(PRENATAL_PHENOTYPE, NEGATIVE_PRENATAL_PHENOTYPE);

    @Override
    public String getName()
    {
        return "prenatalPerinatalPhenotype";
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "prenatal_perinatal_phenotype";
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList(PRENATAL_PHENOTYPE, NEGATIVE_PRENATAL_PHENOTYPE);
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return new LinkedList<>();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return this.hpoCodes;
    }

}
