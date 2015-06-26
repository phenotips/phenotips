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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.studies.family.internal2;

import org.phenotips.data.Patient;
import org.phenotips.studies.family.Family;

import org.xwiki.model.reference.DocumentReference;

import java.util.List;

/**
 * XWiki implementation of Family.
 *
 * @version $Id$
 */
public class XWikiFamily implements Family
{

    @Override
    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getMembers()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isMember(Patient patient)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addMember(Patient patient)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeMember(Patient patient)
    {
        // TODO Auto-generated method stub
        return false;
    }
}
