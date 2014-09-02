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
package org.phenotips.integration.medsavant;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.List;

import net.sf.json.JSONArray;

/**
 * Communication with a MedSavant server.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Role
public interface MedSavantServer
{
    /**
     * Upload the VCF files attached to a patient to MedSavant.
     *
     * @param patient the patient to process
     * @return {@code true} if the upload was successful, {@code false} otherwise
     */
    boolean uploadVCF(Patient patient);

    /**
     * Check if there are any variants stored for a patient in MedSavant.
     *
     * @param patient the patient to check
     * @return {@code true} if there are variants for this patient, {@code false} otherwise
     */
    boolean hasVCF(Patient patient);

    /**
     * Get all the variants stored for a patient.
     *
     * @param patient the patient to process
     * @return a list of raw variants, as obtained from MedSavant
     */
    List<JSONArray> getPatientVariants(Patient patient);

    /**
     * Get all the "interesting" variants stored for a patient, where "interesting" means that certain quality, rarity
     * and harmfulness thresholds have been passed.
     *
     * @param patient the patient to process
     * @return a list of raw variants, as obtained from MedSavant
     */
    List<JSONArray> getFilteredVariants(Patient patient);
}
