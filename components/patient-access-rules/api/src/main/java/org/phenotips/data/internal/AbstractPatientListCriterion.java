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
import org.phenotips.data.PatientListCriterion;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public abstract class AbstractPatientListCriterion implements PatientListCriterion
{
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public List<Patient> getAddList()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Patient> getRemoveList()
    {
        return Collections.emptyList();
    }

    protected QueryManager getQueryManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(QueryManager.class);
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to access the query manager: {}", ex.getMessage(), ex);
        }
        return null;
    }

    protected UserManager getUserManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(UserManager.class);
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to access the user manager: {}", ex.getMessage(), ex);
        }
        return null;
    }
}
