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
package org.phenotips.data.securestorage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Used to store information about the source of the given patient's data (e.g. which remote PhenoTips server the data
 * was pushed form, if any).
 *
 * @version $Id$
 * @since 1.0M10
 */
@Entity
public class PatientSourceServerInfo
{
    /** @see #getId() */
    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String patientGUID;

    @Column(nullable = false)
    private String sourceServerName;

    /** Default constructor used by Hibernate. */
    protected PatientSourceServerInfo()
    {
        // Nothing to do, Hibernate will populate all the fields from the database
    }

    /**
     * For use by SecureStorageManager
     *
     * @param
     */
    public PatientSourceServerInfo(String patientGUID, String sourceServerName)
    {
        this.patientGUID = patientGUID;
        this.sourceServerName = sourceServerName;
    }

    public String getSourceServerName()
    {
        return this.sourceServerName;
    }
}
