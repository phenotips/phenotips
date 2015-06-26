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
package org.phenotips.studies.family.internal2;

import org.phenotips.data.Patient;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.annotation.Component;

import javax.inject.Singleton;

/**
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 */
@Component
@Singleton
public class XWikiFamilyRepository implements FamilyRepository
{

    @Override
    public Family getFamilyForPatient(Patient patient)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Family getFamilyById(String id)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Family createFamily()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
