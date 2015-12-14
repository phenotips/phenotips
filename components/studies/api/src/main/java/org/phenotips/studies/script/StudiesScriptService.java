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
package org.phenotips.studies.script;

import org.phenotips.studies.internal.StudiesRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */

@Component
@Named("studies")
@Singleton
public class StudiesScriptService implements ScriptService
{
    @Inject
    private StudiesRepository studiesRepository;

    /**
     * Returns a JSON object with a list of studies, all with ids that fit a search criterion.
     *
     * @param input the beginning of the study id
     * @param resultsLimit maximal length of list
     * @return JSON object with a list of studies
     */
    public String searchStudies(String input, int resultsLimit)
    {
        return studiesRepository.searchStudies(input, resultsLimit);
    }

}
