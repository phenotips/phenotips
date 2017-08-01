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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Represents an XWiki document attachment.
 *
 * @version $Id$
 * @since 1.4
 */
public class Attachment
{
    private static final String JSON_FIELD_FILENAME = "filename";

    private static final String JSON_FIELD_FILESIZE = "filesize";

    private static final String JSON_FIELD_AUTHOR = "author";

    private static final String JSON_FIELD_DATE = "date";

    private static final String JSON_FIELD_CONTENT = "content";

    private static final DateTimeFormatter ISO_DATE_FORMAT = ISODateTimeFormat.dateTime().withZoneUTC();

    private final XWikiAttachment attachment;

    private final Map<String, Object> attributes = new HashMap<>();

    private final Provider<XWikiContext> contextProvider;

    private final DocumentReferenceResolver<String> userResolver;

    private final EntityReferenceSerializer<String> userSerializer;

    private final Logger logger = LoggerFactory.getLogger(Attachment.class);

    /**
     * Initializes with data from an existing {@link XWikiAttachment}.
     *
     * @param toWrap the attachment to wrap
     */
    Attachment(XWikiAttachment toWrap, DocumentReferenceResolver<String> userResolver,
        EntityReferenceSerializer<String> userSerializer, Provider<XWikiContext> contextProvider)
    {
        this.attachment = toWrap;
        this.userResolver = userResolver;
        this.userSerializer = userSerializer;
        this.contextProvider = contextProvider;
    }

    /**
     * Initializes with data from a JSON-serialized attachment.
     *
     * @param json the attachment to wrap
     * @throws IllegalArgumentException if the JSON does not contain all the needed fields
     */
    Attachment(JSONObject json, DocumentReferenceResolver<String> userResolver,
        EntityReferenceSerializer<String> userSerializer, Provider<XWikiContext> contextProvider)
        throws IllegalArgumentException
    {
        this.attachment = new XWikiAttachment();
        this.userResolver = userResolver;
        this.userSerializer = userSerializer;
        this.contextProvider = contextProvider;
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
            return this.attachment.getContentInputStream(this.contextProvider.get());
        } catch (XWikiException e) {
            return null;
        }
    }

    /**
     * Adds a new attribute. If the attribute was already defined, the old value is overwritten with the new value.
     *
     * @param key the name of the attribute to set
     * @param value the value to set
     */
    public void addAttribute(String key, Object value)
    {
        this.attributes.put(key, value);
    }

    /**
     * Gets the value set for an attribute, if any.
     *
     * @param key the name of the attribute to retrieve
     * @return the value set for an attribute, or {@code null} if no value was set
     */
    public Object getAttribute(String key)
    {
        return this.attributes.get(key);
    }

    /**
     * Gets all the attributes.
     *
     * @return a map containing all the values set for all attributes.
     */
    public Map<String, Object> getAllAttributes()
    {
        return this.attributes;
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
        result.put(JSON_FIELD_AUTHOR, this.userSerializer.serialize(this.getAuthorReference()));
        result.put(JSON_FIELD_DATE, ISO_DATE_FORMAT.print(this.getDate().getTime()));
        try {
            result.put(JSON_FIELD_CONTENT,
                Base64.getEncoder().encodeToString(IOUtils.toByteArray(this.getContent())));
        } catch (IOException ex) {
            this.logger.warn("Failed to access attachment content: {}", ex.getMessage());
        }
        for (Entry<String, Object> attribute : this.attributes.entrySet()) {
            result.put(attribute.getKey(), attribute.getValue());
        }
        return result;
    }

    private void readJSON(JSONObject json) throws IllegalArgumentException
    {
        if (!json.has(JSON_FIELD_FILENAME) || !json.has(JSON_FIELD_CONTENT) || !json.has(JSON_FIELD_DATE)
            || !json.has(JSON_FIELD_FILESIZE)) {
            throw new IllegalArgumentException("Invalid JSON, required fields are missing!");
        }
        this.attachment.setFilename(json.getString(JSON_FIELD_FILENAME));
        this.attachment.setFilesize(json.getInt(JSON_FIELD_FILESIZE));
        if (json.has(JSON_FIELD_AUTHOR)) {
            this.attachment.setAuthorReference(this.userResolver.resolve(json.getString(JSON_FIELD_AUTHOR)));
        } else {
            this.attachment.setAuthorReference(null);
        }
        this.attachment.setDate(ISO_DATE_FORMAT.parseDateTime(json.getString(JSON_FIELD_DATE)).toDate());
        try {
            this.attachment.setContent(
                new ByteArrayInputStream(Base64.getDecoder().decode(json.getString(JSON_FIELD_CONTENT))));
        } catch (JSONException | IOException ex) {
            this.logger.warn("Failed to set attachment content: {}", ex.getMessage());
        }
        List<String> knownKeys = Arrays.asList(JSON_FIELD_AUTHOR, JSON_FIELD_CONTENT, JSON_FIELD_DATE,
            JSON_FIELD_FILENAME, JSON_FIELD_FILESIZE);
        for (String key : json.keySet()) {
            if (knownKeys.contains(key)) {
                continue;
            }
            this.attributes.put(key, json.get(key));
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
