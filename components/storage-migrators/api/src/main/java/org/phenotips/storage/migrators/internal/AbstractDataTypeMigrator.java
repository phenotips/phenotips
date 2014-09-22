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
package org.phenotips.storage.migrators.internal;

import org.phenotips.storage.migrators.DataReader;
import org.phenotips.storage.migrators.DataTypeMigrator;
import org.phenotips.storage.migrators.DataWriter;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;

/**
 * Implementation for the {@link DataTypeMigrator} role, which tries to use all available {@link DataReader}s that
 * {@link DataReader#hasData() have data} and {@link DataWriter#storeEntity(Object) write} the retrieved data to the
 * currently enabled {@link DataWriter storage engine}.
 *
 * @param <T> the type of data managed by this migrator, one of the classes from the data model
 * @version $Id$
 * @since 1.0RC1
 */
public abstract class AbstractDataTypeMigrator<T> implements DataTypeMigrator<T>
{
    /** The current default storage engine assumed by XWiki if no specific store is enabled. */
    private static final String DEFAULT_STORE = "hibernate";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access tot the configuration file where the storage engines are configured. */
    @Inject
    @Named("legacy")
    private ConfigurationSource config;

    /** Needed for accessing the available data readers and writers. */
    @Inject
    private Provider<ComponentManager> cm;

    @Override
    public boolean migrate()
    {
        DataWriter<T> writer = getCurrentWriter();
        if (writer == null) {
            // No writer found, keep data in place
            return true;
        }

        boolean allDataMigrated = true;
        Map<String, DataReader<T>> readers = getReaders();
        if (readers == null) {
            // Failed to retrieve the available readers from the component manager
            return false;
        }
        for (Map.Entry<String, DataReader<T>> entry : readers.entrySet()) {
            DataReader<T> reader = entry.getValue();
            if (reader.getType().equals(writer.getType()) || !reader.hasData()) {
                continue;
            }

            Iterator<T> data = reader.getData();
            while (data.hasNext()) {
                T item = data.next();
                if (writer.storeEntity(item)) {
                    reader.discardEntity(item);
                } else {
                    allDataMigrated = false;
                }
            }
        }
        return allDataMigrated;
    }

    private DataWriter<T> getCurrentWriter()
    {
        String hint = this.config.getProperty(getStoreConfigurationKey(), DEFAULT_STORE);
        try {
            return this.cm.get().getInstance(
                new DefaultParameterizedType(null, DataWriter.class, getImplementationType()),
                getDataType() + "/" + hint);
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    private Map<String, DataReader<T>> getReaders()
    {
        try {
            return this.cm.get().getInstanceMap(
                new DefaultParameterizedType(null, DataReader.class, getImplementationType()));
        } catch (ComponentLookupException e) {
            return null;
        }
    }

    /**
     * The real value behind {@code <T>} used by the implementation.
     *
     * @return the type declared by the end implementation of this migrator
     */
    private Type getImplementationType()
    {
        return ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * The key used in {@code xwiki.cfg} to configure the storage engine for this type of data.
     *
     * @return a key valid in {@code xwiki.cfg}, such as {@code xwiki.store.attachment.hint}
     */
    protected abstract String getStoreConfigurationKey();
}
