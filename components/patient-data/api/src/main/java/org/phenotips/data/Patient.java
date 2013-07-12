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
package org.phenotips.data;

import java.util.Set;

import net.sf.json.JSONObject;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Information about a patient.
 * 
 * @version $Id$
 */
@Unstable
public interface Patient
{
    /** The XClass used for storing patient data. */
    EntityReference CLASS_REFERENCE = new EntityReference("PatientClass", EntityType.DOCUMENT, new EntityReference(
        "PhenoTips", EntityType.SPACE));

    /** The default template for creating a new patient. */
    EntityReference TEMPLATE_REFERENCE = new EntityReference("PatientTemplate", EntityType.DOCUMENT,
        CLASS_REFERENCE.getParent());

    /** The default space where patient data is stored. */
    EntityReference DEFAULT_DATA_SPACE = new EntityReference("data", EntityType.SPACE);

    /**
     * Returns a reference to the document where the patient data is stored.
     * 
     * @return a valid document reference
     */
    DocumentReference getDocument();

    /**
     * Returns a reference to the profile of the user that created the patient record.
     * 
     * @return a valid document reference
     */
    DocumentReference getReporter();

    /**
     * Returns the list of recorded features, both positive and negative observations.
     * 
     * @return an unmodifiable set of {@link Phenotype features}, or an empty set if no features are recorded yet
     */
    Set<? extends Phenotype> getPhenotypes();

    /**
     * Returns the list of recorded diseases.
     * 
     * @return an unmodifiable set of {@link Disease diseases}, or an empty set if no diseases have been identified yet
     */
    Set<? extends Disease> getDiseases();

    /**
     * Retrieve all the patient data in a JSON format. For example:
     * 
     * <pre>
     * {
     *   "id": "xwiki:data.P0000001",
     *   "reporter": "xwiki.XWiki.PatchAdams",
     *   "features": [
     *     // See the documentation for {@link Phenotype#toJSON()}
     *   ],
     *   "diseases": [
     *     // See the documentation for {@link Disease#toJSON()}
     *   ]
     * }
     * </pre>
     * 
     * @return the patient data, using the json-lib classes
     */
    JSONObject toJSON();
}
