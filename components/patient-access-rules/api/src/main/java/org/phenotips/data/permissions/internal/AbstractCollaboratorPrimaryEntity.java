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
package org.phenotips.data.permissions.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.internal.AbstractPrimaryEntity;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.doc.XWikiDocument;

public abstract class AbstractCollaboratorPrimaryEntity implements Collaborator
{
    /** The key used in the JSON serialization for the {@link #getAccessLevel()}. */
    public static final String JSON_KEY_ACCESS_LEVEL = "accessLevel";

    protected final EntityReference user;

    protected final AccessLevel access;

    private PrimaryEntity collaboratorAsPrimaryEntity;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractCollaboratorPrimaryEntity(EntityReference user, AccessLevel access)
    {
        this.user = user;
        this.access = access;

        DocumentReferenceResolver<EntityReference> referenceResolver = this.getReferenceResolver();
        DocumentAccessBridge bridge = this.getBridge();
        DocumentReference document = referenceResolver.resolve(user);
        XWikiDocument xwikiDoc = null;
        try {
            xwikiDoc = (XWikiDocument) bridge.getDocument(document);
        } catch (Exception e) {
            this.logger.error("Failed to create collaboratorAsPrimaryEntity.", e);
        }

        this.collaboratorAsPrimaryEntity = new AbstractPrimaryEntity(xwikiDoc)
        {
            @Override
            public EntityReference getType()
            {
                return this.getType();
            }

            @Override
            public void updateFromJSON(JSONObject json)
            {
                this.updateFromJSON(json);
            }
        };
    }

    @Override
    public EntityReference getUser()
    {
        return this.user;
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return this.access;
    }

    @Override
    public EntityReference getType()
    {
        return Collaborator.CLASS_REFERENCE;
    }

    @Override
    public DocumentReference getDocument()
    {
        return this.collaboratorAsPrimaryEntity.getDocument();
    }

    @Override
    public String getId()
    {
        return this.collaboratorAsPrimaryEntity.getId();
    }

    @Override
    public String getName()
    {
        return this.collaboratorAsPrimaryEntity.getName();
    }

    @Override
    public String getDescription()
    {
        return this.collaboratorAsPrimaryEntity.getDescription();
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject json = this.collaboratorAsPrimaryEntity.toJSON();
        json.put(JSON_KEY_ACCESS_LEVEL, this.getAccessLevel());
        return json;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        // TODO
    }

    private DocumentReferenceResolver<EntityReference> getReferenceResolver()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(
                    DocumentReferenceResolver.TYPE_REFERENCE, "current");
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to look up DocumentReferenceResolver<EntityReference>.", e);
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
}
