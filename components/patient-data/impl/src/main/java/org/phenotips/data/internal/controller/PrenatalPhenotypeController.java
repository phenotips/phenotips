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
package org.phenotips.data.internal.controller;

import org.phenotips.data.OntologyProperty;
import org.phenotips.data.PatientDataController;

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
public class PrenatalPhenotypeController extends AbstractComplexController<List<OntologyProperty>>
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

    @Override
    protected boolean isCodeFieldsOnly()
    {
        return true;
    }
}
