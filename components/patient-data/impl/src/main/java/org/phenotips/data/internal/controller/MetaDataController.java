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
package org.phenotips.data.internal.controller;

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Exposes the metainfo about the document.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component(roles = { PatientDataController.class })
@Named("metadata")
@Singleton
public class MetaDataController extends AbstractSimpleController implements PatientDataController<String>
{
    private static final String UNKNOWN_USER = "Unknown user";

    private static final String DOCUMENT_NAME = "doc.name";

    private static final String DOCUMENT_NAME_STRING = "report_id";

    private static final String CREATION_DATE = "creationDate";

    private static final String AUTHOR = "author";

    private static final String AUTHOR_STRING = "last_modified_by";

    private static final String DATE = "date";

    private static final String DATE_STRING = "last_modification_date";

    private static final String CONTROLLER_NAME = "metadata";

    @Inject
    private Logger logger;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            // TODO change to getDocument
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocumentReference());

            Map<String, String> result = new LinkedHashMap<>();

            DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

            result.put(DOCUMENT_NAME, doc.getDocumentReference().getName());

            result.put(CREATION_DATE, dateFormatter.print(new DateTime(doc.getCreationDate())));

            result.put(AUTHOR, (doc.getAuthorReference() != null)
                ? doc.getAuthorReference().getName() : UNKNOWN_USER);

            result.put(DATE, dateFormatter.print(new DateTime(doc.getDate())));

            return new DictionaryPatientData<>(getName(), result);

        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<String> patientData = patient.<String>getData(getName());
        if (patientData != null && patientData.isNamed()) {
            Iterator<Entry<String, String>> values = patientData.dictionaryIterator();

            while (values.hasNext()) {
                Entry<String, String> datum = values.next();
                if (StringUtils.isNotBlank(datum.getValue())
                    && (selectedFieldNames == null || selectedFieldNames.contains(datum.getKey()))) {
                    json.put(getJSONkey(datum.getKey()), datum.getValue());
                }
            }
        }

    }

    @Override
    protected List<String> getProperties()
    {
        // Not used, since there's a custom load method
        return Collections.emptyList();
    }

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return null;
    }

    private String getJSONkey(String key)
    {
        switch (key) {
            case DOCUMENT_NAME:
                return DOCUMENT_NAME_STRING;
            case CREATION_DATE:
                return DATE;
            case AUTHOR:
                return AUTHOR_STRING;
            case DATE:
                return DATE_STRING;
            default:
                return key;
        }
    }
}
