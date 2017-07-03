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

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.stability.Unstable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides access to attached medical reports.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New controller in 1.4M1, parameter type will likely change")
@Component(roles = { PatientDataController.class })
@Named("medicalReports")
@Singleton
public class MedicalReportsController implements PatientDataController<MedicalReportsController.Attachment>
{
    private static final String DATA_NAME = "medical_reports";

    private static final String FIELD_NAME = "reports_history";

    @Inject
    private static Provider<XWikiContext> contextProvider;

    @Inject
    @Named("user/current")
    private static DocumentReferenceResolver<String> userResolver;

    @Inject
    @Named("compactwiki")
    private static EntityReferenceSerializer<String> userSerializer;

    /**
     * Logging helper object.
     */
    @Inject
    private static Logger logger;

    @Override
    public PatientData<Attachment> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            // Getting the documents which are reports instead of just getting all attachments
            @SuppressWarnings("unchecked")
            List<String> reports = data.getListValue(FIELD_NAME);
            List<Attachment> result = new ArrayList<>(reports.size());

            for (String report : reports) {
                XWikiAttachment xattachment = doc.getAttachment(report);
                if (xattachment != null) {
                    result.add(new Attachment(xattachment));
                }
            }

            return new IndexedPatientData<>(getName(), result);
        } catch (Exception e) {
            MedicalReportsController.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        PatientData<Attachment> reports = patient.getData(getName());
        if (reports == null) {
            return;
        }
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE, true, contextProvider.get());

            List<String> result = new ArrayList<>(reports.size());

            for (Attachment report : reports) {
                XWikiAttachment xattachment = doc.getAttachment(report.getFilename());
                if (xattachment == null) {
                    xattachment = new XWikiAttachment(doc, report.getFilename());
                    doc.addAttachment(xattachment);
                }
                xattachment.setContent(report.getContent());
                DocumentReference author = report.getAuthorReference();
                if (author != null && !contextProvider.get().getWiki().exists(author, contextProvider.get())) {
                    author = contextProvider.get().getUserReference();
                }
                xattachment.setAuthorReference(author);
                xattachment.setDate(report.getDate());
                xattachment.setFilesize((int) report.getFilesize());
                result.add(report.getFilename());
            }
            data.setDBStringListValue(FIELD_NAME, result);
        } catch (Exception ex) {
            MedicalReportsController.logger.error("Failed to save attachment: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null
            && !(selectedFieldNames.contains(getName()) || selectedFieldNames.contains(FIELD_NAME))) {
            return;
        }

        PatientData<Attachment> reportsData = patient.getData(getName());
        JSONArray reportsJsonArray = new JSONArray();
        if (reportsData == null || !reportsData.isIndexed() || reportsData.size() == 0) {
            if (selectedFieldNames != null) {
                json.put(DATA_NAME, reportsJsonArray);
            }
            return;
        }

        for (Attachment report : reportsData) {
            reportsJsonArray.put(report.toJSON());
        }
        json.put(DATA_NAME, reportsJsonArray);
    }

    @Override
    public PatientData<Attachment> readJSON(JSONObject json)
    {
        if (!json.has(DATA_NAME) || json.optJSONArray(DATA_NAME) == null) {
            return null;
        }
        List<Attachment> result = new ArrayList<>();

        JSONArray reportsArray = json.getJSONArray(DATA_NAME);
        for (Object report : reportsArray) {
            result.add(new Attachment((JSONObject) report));
        }

        return new IndexedPatientData<>(getName(), result);
    }

    @Override
    public String getName()
    {
        return "medicalReports";
    }

    /**
     * Represents an XWiki document attachment.
     */
    public static final class Attachment
    {
        private static final String JSON_FIELD_FILENAME = "filename";

        private static final String JSON_FIELD_FILESIZE = "filesize";

        private static final String JSON_FIELD_AUTHOR = "author";

        private static final String JSON_FIELD_DATE = "date";

        private static final String JSON_FIELD_CONTENT = "content";

        private static final DateTimeFormatter ISO_DATE_FORMAT = ISODateTimeFormat.dateTime().withZoneUTC();

        private final XWikiAttachment attachment;

        Attachment(XWikiAttachment toWrap)
        {
            this.attachment = toWrap;
        }

        Attachment(JSONObject json)
        {
            this.attachment = new XWikiAttachment();
            readJSON(json);
        }

        /**
         * The attachment's file name.
         *
         * @return a simple filename
         */
        public String getFilename()
        {
            return this.attachment.getFilename();
        }

        /**
         * Returns the user that uploaded the attachment.
         *
         * @return a document reference, may be {@code null} if the author was unauthenticated
         */
        public DocumentReference getAuthorReference()
        {
            return this.attachment.getAuthorReference();
        }

        /**
         * Returns the size of the attachment, in bytes.
         *
         * @return a positive number
         */
        public long getFilesize()
        {
            return this.attachment.getFilesize();
        }

        /**
         * Returns the date when the attachment was uploaded.
         *
         * @return a valid date
         */
        public Date getDate()
        {
            return this.attachment.getDate();
        }

        /**
         * Returns the content of this attachment.
         *
         * @return an input stream for reading the attachment content
         */
        public InputStream getContent()
        {
            try {
                return this.attachment.getContentInputStream(MedicalReportsController.contextProvider.get());
            } catch (XWikiException e) {
                return null;
            }
        }

        /**
         * Serialize this attachment as JSON. The JSON will look like:
         *
         * <pre>
         * {@code
         * {
         *   "filename": "file.ext",
         *   "filesize": 123,
         *   "author": "XWiki.Admin",
         *   "date": "2017-07-01T12:00:00.000Z",
         *   "content": "SGVsbG8gd29ybGQK" // base 64 encoded file content
         * }
         * }
         * </pre>
         *
         * @return a JSON object in the format described above
         */
        public JSONObject toJSON()
        {
            JSONObject result = new JSONObject();
            result.put(JSON_FIELD_FILENAME, this.getFilename());
            result.put(JSON_FIELD_FILESIZE, this.getFilesize());
            result.put(JSON_FIELD_AUTHOR, userSerializer.serialize(this.getAuthorReference()));
            result.put(JSON_FIELD_DATE, ISO_DATE_FORMAT.print(this.getDate().getTime()));
            try {
                result.put(JSON_FIELD_CONTENT,
                    Base64.getEncoder().encodeToString(IOUtils.toByteArray(this.getContent())));
            } catch (IOException ex) {
                logger.warn("Failed to access attachment content: {}", ex.getMessage());
            }
            return result;
        }

        private void readJSON(JSONObject json)
        {
            this.attachment.setFilename(json.getString(JSON_FIELD_FILENAME));
            this.attachment.setFilesize(json.getInt(JSON_FIELD_FILESIZE));
            if (json.has(JSON_FIELD_AUTHOR)) {
                this.attachment.setAuthorReference(userResolver.resolve(json.getString(JSON_FIELD_AUTHOR)));
            } else {
                this.attachment.setAuthorReference(null);
            }
            this.attachment.setDate(ISO_DATE_FORMAT.parseDateTime(json.getString(JSON_FIELD_DATE)).toDate());
            try {
                this.attachment
                    .setContent(
                        new ByteArrayInputStream(Base64.getDecoder().decode(json.getString(JSON_FIELD_CONTENT))));
            } catch (JSONException | IOException ex) {
                logger.warn("Failed to set attachment content: {}", ex.getMessage());
            }
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof Attachment)) {
                return false;
            }
            Attachment o = (Attachment) other;
            try {
                return new EqualsBuilder()
                    .append(this.getFilename(), o.getFilename())
                    .append(this.getFilesize(), o.getFilesize())
                    .append(this.getAuthorReference(), o.getAuthorReference())
                    .append(this.getDate(), o.getDate())
                    .appendSuper(IOUtils.contentEquals(this.getContent(), o.getContent()))
                    .isEquals();
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public int hashCode()
        {
            HashCodeBuilder h = new HashCodeBuilder()
                .append(this.getFilename())
                .append(this.getFilesize())
                .append(this.getAuthorReference())
                .append(this.getDate());
            try {
                h.append(IOUtils.toByteArray(this.getContent()));
            } catch (IOException e) {
                // The content isn't mandatory for the hash, and this exception shouldn't happen
            }
            return h.toHashCode();
        }
    }
}
