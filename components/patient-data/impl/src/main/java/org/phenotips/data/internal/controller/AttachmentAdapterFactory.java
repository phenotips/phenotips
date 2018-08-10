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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Factory class that exposes {@link XWikiAttachment}, with additional attributes, as simpler POJOs, with roundtrip
 * de-serialization through JSON.
 *
 * @version $Id$
 * @since 1.4
 */
@Role
@Component(roles = AttachmentAdapterFactory.class)
@Singleton
public class AttachmentAdapterFactory
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("user/current")
    private DocumentReferenceResolver<String> userResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> userSerializer;

    @Inject
    private Logger logger;

    /**
     * Construct a new attachment adapter for an existing {@link XWikiAttachment}. Additional attributes can be added to
     * the resulting adapter with {@link Attachment#addAttribute(String, Object)}.
     *
     * @param attachment the attachment to wrap
     * @return a new adapter
     */
    public Attachment fromXWikiAttachment(XWikiAttachment attachment)
    {
        if (attachment == null) {
            return null;
        }
        return new Attachment(attachment, this.userResolver, this.userSerializer, this.contextProvider);
    }

    /**
     * Construct a new attachment adapter from a JSON-serialized attachment.
     *
     * @param attachment the attachment to deserialize
     * @return a new adapter, or null if provided JSON is not valid or does not include attachment content
     */
    public Attachment fromJSON(JSONObject attachment)
    {
        if (attachment == null) {
            return null;
        }
        try {
            return new Attachment(attachment, this.userResolver, this.userSerializer, this.contextProvider);
        } catch (IllegalArgumentException ex) {
            this.logger.warn("Invalid JSON for deserializing an attachment: {}", attachment);
            return null;
        }
    }
}
