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
import org.phenotips.entities.internal.AbstractPrimaryEntityManager;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation of patient data access service using XWiki as the storage backend, where patients in documents having
 * an object of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Named("Patient")
@Singleton
public class PatientEntityManager extends AbstractPrimaryEntityManager<Patient>
{
    @Override
    public EntityReference getDataSpace()
    {
        return Patient.DEFAULT_DATA_SPACE;
    }

    @Override
    protected Class<? extends Patient> getEntityClass()
    {
        return PhenoTipsPatient.class;
    }

    @Override
    protected DocumentReference getEntityXClassReference()
    {
        return this.referenceResolver.resolve(Patient.CLASS_REFERENCE);
    }
}
