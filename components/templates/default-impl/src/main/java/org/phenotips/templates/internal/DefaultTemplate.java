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
import org.phenotips.entities.internal.AbstractPrimaryEntity;
import org.phenotips.templates.data.Template;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class DefaultTemplate extends AbstractPrimaryEntity implements Template
{
    /**
     * Create a template.
     *
     * @param document of template
     */
    public DefaultTemplate(XWikiDocument document)
    {
        super(document);
    }

    /**
     * TODO Temporary factory method.
     *
     * @param templateId template id
     * @return new default template
     */
    public static Template getTemplateById(String templateId)
    {
        DocumentReferenceResolver<String> stringResolver = null;
        DocumentAccessBridge dab = null;
        Logger logger = LoggerFactory.getLogger(DefaultTemplate.class);
        try {
            ComponentManager cm = ComponentManagerRegistry.getContextComponentManager();
            stringResolver = cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
            dab = cm.getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            logger.error("Failed to create a new DefaultTemplate.");
            return null;
        }

        DocumentReference reference = stringResolver.resolve(templateId, Template.DEFAULT_DATA_SPACE);
        XWikiDocument document;
        try {
            document = (XWikiDocument) dab.getDocument(reference);
        } catch (Exception e) {
            logger.error("Failed to create a new DefaultTemplate.");
            return null;
        }

        return new DefaultTemplate(document);
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

    @Override
    public EntityReference getType()
    {
        return Template.CLASS_REFERENCE;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        // TODO Auto-generated method stub
    }
}
