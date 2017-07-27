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
package org.phenotips.configuration.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.configuration.spi.RecordConfigurationModule;

import org.xwiki.component.annotation.Component;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * Default implementation for the {@link RecordConfigurationManager} component.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Component
@Singleton
public class DefaultRecordConfigurationManager implements RecordConfigurationManager
{
    /** The label for record of type "patient". */
    private static final String PATIENT_RECORD_LABEL = "patient";

    /** Logging helper. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<List<RecordConfigurationModule>> modules;

    @Override
    public RecordConfiguration getConfiguration(String recordType)
    {
        RecordConfiguration config = new DefaultRecordConfiguration();
        for (RecordConfigurationModule service : this.modules.get()) {
            try {
                if (service.supportsRecordType(recordType)) {
                    config = service.process(config);
                }
            } catch (Exception ex) {
                this.logger.warn("Failed to read the record configuration: {}", ex.getMessage());
            }
        }
        return config;
    }

    @Override
    public RecordConfiguration getActiveConfiguration()
    {
        return getConfiguration(PATIENT_RECORD_LABEL);
    }
}
