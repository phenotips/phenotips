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
package org.phenotips.entities.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Base class for implementing specific entities. By default, this uses the document name as the identifier, the
 * document title as the name, and either a {@code description} property in the main XObject, or the document content,
 * as the description. If two objects use the same document for storage, they are assumed to be equal, and no actual
 * data equality is checked.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New class and interface added in 1.3")
public abstract class AbstractPrimaryEntity implements PrimaryEntity
{
    /** The XDocument where this entity is stored. May be an in-memory only object. */
    protected final XWikiDocument document;

    /** Logging helper object. */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Simple constructor, passing in the required {@link #document}.
     *
     * @param document the document where the entity's data must be loaded from
     */
    protected AbstractPrimaryEntity(XWikiDocument document)
    {
        this.document = document;
    }

    protected AbstractPrimaryEntity(DocumentReference reference)
    {
        this.document =  this.getXWikiDocument(reference);
    }

    @Override
    public DocumentReference getDocument()
    {
        return this.document.getDocumentReference();
    }

    @Override
    public String getId()
    {
        return this.getDocument().getName();
    }

    @Override
    public String getName()
    {
        return this.document.getRenderedTitle(Syntax.PLAIN_1_0, getXContext());
    }

    @Override
    public String getFullName()
    {
        return this.getDocument().toString();
    }

    @Override
    public String getDescription()
    {
        String result;
        BaseObject obj = this.document.getXObject(getType());
        BaseClass cls = obj.getXClass(getXContext());
        if (cls.getField(JSON_KEY_DESCRIPTION) != null) {
            result = obj.getLargeStringValue(JSON_KEY_DESCRIPTION);
        } else {
            result = this.document.getContent();
        }
        return result;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        result.put(JSON_KEY_ID, getId());
        String name = getName();
        if (StringUtils.isNotEmpty(name)) {
            result.put(JSON_KEY_NAME, name);
        }
        String description = getDescription();
        if (StringUtils.isNotEmpty(description)) {
            result.put(JSON_KEY_DESCRIPTION, description);
        }
        return result;
    }

    @Override
    public int hashCode()
    {
        return this.getDocument().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof PrimaryEntity)) {
            return false;
        }
        return this.getDocument().equals(((PrimaryEntity) obj).getDocument());
    }

    @Override
    public String toString()
    {
        return getId() + "[" + getType().getName() + "]";
    }

    protected XWikiContext getXContext()
    {
        try {
            Provider<XWikiContext> xcontextProvider =
                ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiContext.TYPE_PROVIDER);
            return xcontextProvider.get();
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting the current context: {}", ex.getMessage());
        }
        return null;
    }

    protected EntityReferenceSerializer<String> getLocalSerializer()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting the local reference serializer: {}", ex.getMessage());
        }
        return null;
    }

    protected EntityReferenceSerializer<String> getFullSerializer()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(EntityReferenceSerializer.TYPE_STRING);
        } catch (Exception ex) {
            this.logger.error("Unexpected exception while getting the full reference serializer: {}", ex.getMessage());
        }
        return null;
    }

    private DocumentAccessBridge getBridge()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to look up DocumentAccessBridge.", e);
        }
        return null;
    }

    protected XWikiDocument getXWikiDocument(DocumentReference documentReference)
    {
        XWikiDocument xdocument;
        try {
            xdocument = (XWikiDocument) this.getBridge().getDocument(documentReference);
        } catch (Exception e) {
            this.logger.error("Could not read XWikiDocument from reference {}", documentReference.getName(),
                    e.getMessage());
            return null;
        }
        return xdocument;
    }
}
