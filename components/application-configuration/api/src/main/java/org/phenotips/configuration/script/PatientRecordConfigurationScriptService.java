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
package org.phenotips.configuration.script;

import org.phenotips.configuration.PatientRecordConfiguration;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Named("patientConfiguration")
@Singleton
public class PatientRecordConfigurationScriptService implements ScriptService
{
    @Inject
    private PatientRecordConfiguration configuration;

    /**
     * The list of fields displayed in the patient record.
     * 
     * @return an unmodifiable ordered list of field names, empty if none are enabled or the configuration is missing
     */
    public List<String> getEnabledFieldNames()
    {
        try {
            return this.configuration.getEnabledFieldNames();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    /**
     * The list of possible fields defined in the application. This doesn't include metadata stored in separate
     * entities, such as measurements, relatives, additional files, etc.
     * 
     * @return an unmodifiable ordered list of field names, empty if none are available or the configuration is missing
     */
    List<String> getAllFieldNames()
    {
        try {
            return this.configuration.getAllFieldNames();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
