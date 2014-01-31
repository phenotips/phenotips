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
package org.phenotips.data.internal.serializer;

import org.phenotips.Constants;
import org.phenotips.data.PatientDataSerializer;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Has 3 main functions: parse data from XWiki doc (PatientClass only), export and import JSON.
 *
 * @version $Id$
 */

@Component
@Named("patient-class-serializer")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class PatientClassSerializer implements PatientDataSerializer
{
    /** The XClass used for storing patient data. */
    private static final EntityReference CLASS_REFERENCE = new EntityReference("PatientClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    private static final String ID = "identifier";

    private static final String EXTERNAL_ID = "external_id";

    private static final String FIRST_NAME = "first_name";

    private static final String LAST_NAME = "last_name";

    private static final String GENDER = "gender";

    private static final String DATE_OF_BIRTH = "date_of_birth";

    private static final String EXAM_DATE = "exam_date";

    private static final String AGE_OF_ONSET = "age_of_onset";

    private static final String MODE_OF_INHERITANCE = "mode_of_inheritance";

    //Skipping notes section for now

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    private Map<String, Object> patientClass;

    @Override
    public void readDocument(DocumentReference documentReference)
    {
        try {
            XWikiDocument doc = (XWikiDocument) documentAccessBridge.getDocument(documentReference);
            readXWikiPatient(doc);
        } catch (Exception e) {
            //todo
        }
    }

    /**
     * Reads the PatientClass object of an XWiki document and extracts data.
     *
     * @param doc XWiki patient document
     * @return Map of all the fields of interest from PatientClass. Could contain Maps as values.
     */
//    public Map<String, Object> readXWikiPatient(XWikiDocument doc)
    private void readXWikiPatient(XWikiDocument doc)
    {
        BaseObject data = doc.getXObject(CLASS_REFERENCE);
        if (data == null) {
            throw new NullPointerException("The patient does not have a PatientClass");
        }

        patientClass = new HashMap<String, Object>();

        //Reading PatientClass
        //JSONoptional applies to the line above. These comments will go soon.

        String id = data.getStringValue(ID);
        String externalId = data.getStringValue(EXTERNAL_ID);
        //JSONoptional

        String firstName = data.getStringValue(FIRST_NAME);
        String lastName = data.getStringValue(LAST_NAME);
        Map<String, String> patientName = new HashMap<String, String>();
        //JSONoptional
        patientName.put(FIRST_NAME, firstName);
        patientName.put(LAST_NAME, lastName);

        String dataGender = data.getStringValue(GENDER);
        String gender = (StringUtils.equalsIgnoreCase("F", dataGender) || StringUtils.equalsIgnoreCase("M", dataGender))
            ? dataGender : "U";

        String dateOfBirth = data.getStringValue(DATE_OF_BIRTH);
        //JSONoptional

        String examDate = data.getStringValue(EXAM_DATE);
        //JSONoptional

        String ageOfOnset = data.getStringValue(AGE_OF_ONSET);
        //JSONoptional
        String modeOfInheritance = data.getStringValue(MODE_OF_INHERITANCE);
        //JSONoptional

        //Storing all variables into the Map
        patientClass.put(ID, id);
        patientClass.put(EXTERNAL_ID, externalId);
        patientClass.put("patient_name", patientName);
        patientClass.put(GENDER, gender);
        patientClass.put(DATE_OF_BIRTH, dateOfBirth);
        patientClass.put(EXAM_DATE, examDate);
        patientClass.put(AGE_OF_ONSET, ageOfOnset);
        patientClass.put(MODE_OF_INHERITANCE, modeOfInheritance);
    }

    //These need to be rewritten as static and return write[json] and read[map]
    @Override
    public void writeJSON(JSONObject json)
    {
        json.putAll(patientClass);
    }

    @Override
    public void readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }
}
