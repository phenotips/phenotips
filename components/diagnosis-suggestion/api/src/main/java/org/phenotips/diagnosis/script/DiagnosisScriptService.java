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
package org.phenotips.diagnosis.script;

import org.phenotips.diagnosis.DiagnosisService;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Service that suggests plausible diagnoses for a set of features.
 *
 * @see DiagnosisService
 * @since 1.1M1
 * @version $Id$
 */
@Component
@Named("diagnosis")
@Singleton
public class DiagnosisScriptService implements ScriptService
{
    @Inject
    private DiagnosisService service;

    /**
     * Get a list of plausible diagnoses given a list of present phenotypes.
     *
     * @param phenotypes a list of phenotype term IDs observed in the patient; each phenotype is represented as a String
     *            in the format {@code <ontology prefix>:<term id>}, for example {@code HP:0002066}
     * @param nonstandardPhenotypes a list of non-standard phenotype terms observed in the patient
     * @param limit the maximum number of diagnoses to return; must be a positive number
     * @return a list of suggested diagnoses
     */
    public List<OntologyTerm> get(List<String> phenotypes, List<String> nonstandardPhenotypes, int limit)
    {
        return this.service.getDiagnosis(phenotypes, nonstandardPhenotypes, limit);
    }
}
