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
package org.phenotips.storage.migrators;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Identifies a type of data store, where a {@link #getDataType() type of data} is stored in a {@link #getStoreType()
 * storage resource}.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public final class Type
{
    private final String dataType;

    private final String storeType;

    /**
     * Simple constructor passing all the required information.
     *
     * @param dataType a short name for the data type, such as {@code "attachments"} or {@code "deleted attachments"}
     * @param storeType a short identifier for the storage technology, such as {@code "hibernate"}, {@code file}, or
     *            {@code "void"}
     */
    public Type(final String dataType, final String storeType)
    {
        this.dataType = dataType;
        this.storeType = storeType;
    }

    /**
     * The type of data being managed by a {@link DataReader} or {@link DataWriter}.
     *
     * @return a short name for the data type, such as {@code "attachments"} or {@code "deleted attachments"}
     */
    public String getDataType()
    {
        return this.dataType;
    }

    /**
     * The storage technology where the {@link DataReader} reads from, or the {@link DataWriter} writes to.
     *
     * @return a short identifier for the storage technology, such as {@code "hibernate"}, {@code file}, or
     *         {@code "void"}
     */
    public String getStoreType()
    {
        return this.storeType;
    }

    @Override
    public String toString()
    {
        return this.dataType + '/' + this.storeType;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other.getClass() != getClass()) {
            return false;
        }
        Type otherType = (Type) other;

        return new EqualsBuilder()
            .append(this.dataType, otherType.dataType)
            .append(this.storeType, otherType.storeType)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 17)
            .append(this.dataType)
            .append(this.storeType)
            .toHashCode();
    }
}
