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
package org.phenotips.boqa;

import org.xwiki.component.annotation.Role;

import java.util.List;

/**
 * Created by meatcar on 9/3/14.
 * @version $Id$
 */
@Role
public interface DiagnosisService
{

    /**
     * Get a list of suggest diagnosies given a list of present phenotypes. Each phenotype is represented as a String
     * in the format {@code <ontology prefix>:<term id>}, for example
     *            {@code HP:0002066}
     *
     * @param presentPhenotypes A List of String phenotypes observed in the patient
     * @param absentPhenotypes A List of String phenotypes not observed in the patient
     * @return A list of suggested diagnosies
     */
    public List<String> getDiagnosis(List<String> presentPhenotypes, List<String> absentPhenotypes);
}
