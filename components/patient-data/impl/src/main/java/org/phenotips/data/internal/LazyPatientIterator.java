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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An iterator on a lazy, immutable patients collection.
 *
 * @version $Id$
 */
public class LazyPatientIterator implements Iterator<Patient>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LazyPatientIterator.class);

    private static final PatientRepository PATIENT_REPOSITORY;

    private Iterator<String> iterator;

    static {
        PatientRepository patientRepository = null;
        try {
            patientRepository =
                ComponentManagerRegistry.getContextComponentManager().getInstance(PatientRepository.class);
        } catch (ComponentLookupException e) {
            LOGGER.error("Error loading static components: {}", e.getMessage(), e);
        }
        PATIENT_REPOSITORY = patientRepository;
    }
    /**
     * Default constructor.
     *
     * @param patientIds a collection of ids of patients to be contained in the lazy collection.
     */
    public LazyPatientIterator(List<String> patientIds)
    {
        this.iterator = patientIds.iterator();
    }

    @Override
    public boolean hasNext()
    {
        return this.iterator.hasNext();
    }

    @Override
    public Patient next()
    {
        String patientId = this.iterator.next();
        Patient patient = LazyPatientIterator.PATIENT_REPOSITORY.getPatientById(patientId);
        return patient;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
