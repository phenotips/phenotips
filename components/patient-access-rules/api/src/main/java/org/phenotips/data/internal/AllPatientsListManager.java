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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientListCriterion;

import org.xwiki.component.annotation.Component;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Finds the total list of patients current user can view.
 *
 * @version $Id$
 */
@Component(roles = {AllPatientsListManager.class})
@Singleton
public class AllPatientsListManager
{
    @Inject
    private PatientsListCriteriaProvider criteriaProvider;

    /**
     * Calculates and returns the total list of patients current user can view. The method runs over all
     * {@link PatientListCriterion}-s and gets a list of patients to add/remove to/from total list. A patient
     * will appear in the final list if it appears in any {@link PatientListCriterion#getAddList} list and
     * does not appear in any {@link PatientListCriterion#getRemoveList} list.
     *
     * @return a list of patients
     */
    public List<Patient> getAllPatientsList()
    {
        List<Patient> addList = new LinkedList<>();
        List<Patient> removeList = new LinkedList<>();

        for (PatientListCriterion criterion : this.criteriaProvider.get()) {
            addList.addAll(criterion.getAddList());
            removeList.addAll(criterion.getRemoveList());
        }

        addList.removeAll(removeList);
        return addList;
    }
}
