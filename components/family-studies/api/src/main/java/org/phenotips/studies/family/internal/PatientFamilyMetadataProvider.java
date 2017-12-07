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
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityMetadataProvider;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Metadata provider that reports the family that a patient belongs to.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("patient/family")
@Singleton
public class PatientFamilyMetadataProvider implements PrimaryEntityMetadataProvider
{
    @Inject
    private FamilyRepository familyRepository;

    @Override
    public Map<String, Object> provideMetadata(PrimaryEntity entity)
    {
        if (entity instanceof Patient) {
            Family f = this.familyRepository.getFamilyForPatient((Patient) entity);
            if (f != null) {
                return Collections.singletonMap("family", f.getId());
            }
        }
        return Collections.emptyMap();
    }
}
