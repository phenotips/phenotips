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
package org.phenotips.templates.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.templates.data.Template;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class DefaultTemplate implements Template
{
    private static final String XWIKI_STRING = "xwiki:";

    private String templateId;

    private XWikiDocument templateObject;

    private DocumentReference templateReference;

    /**
     * Create a template.
     *
     * @param templateId id of template
     */
    public DefaultTemplate(String templateId)
    {
        this.templateId = this.removeXwikiFromString(templateId);
        this.templateObject = this.getTemplateObject();
        this.templateReference = this.templateObject.getDocumentReference();
    }

    @Override
    public String getId()
    {
        return this.templateId;
    }

    @Override
    public String getName()
    {
        return this.templateId.split("\\.")[1];
    }

    @Override
    public String getTitle()
    {
        return this.templateObject.getTitle();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.templateReference;
    }

    private XWikiDocument getTemplateObject()
    {
        DocumentReference reference = this.getStringResolver().resolve(this.templateId, Template.DEFAULT_DATA_SPACE);
        try {
            return (XWikiDocument) this.getBridge().getDocument(reference);
        } catch (Exception ex) {
            this.getLogger().warn("Failed to access project with id [{}]: {}", templateId, ex.getMessage(), ex);
        }
        return null;
    }

    private DocumentReferenceResolver<String> getStringResolver()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private DocumentAccessBridge getBridge()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private Logger getLogger()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(Logger.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DefaultTemplate)) {
            return false;
        }
        DefaultTemplate other = (DefaultTemplate) obj;
        return this.getId().equals(other.getId());
    }

    @Override
    public int hashCode()
    {
        return this.getId().hashCode();
    }

    private String removeXwikiFromString(String id) {
        if (id.startsWith(DefaultTemplate.XWIKI_STRING)) {
            return id.substring(DefaultTemplate.XWIKI_STRING.length());
        } else {
            return id;
        }
    }

}
