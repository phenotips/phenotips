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
package org.xwiki.users.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.AbstractUser;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;

/**
 * Class representing a XWiki User based on wiki documents holding {@code XWiki.XWikiUsers} XObjects.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class WikiUser extends AbstractUser
{
    /** A stub for the reference to the user profile class. */
    private static final EntityReference USER_CLASS =
        new EntityReference("XWikiUsers", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /** A link to the XClass defining user profiles in the user's wiki. */
    private DocumentReference classReference;

    /** Model access, used for reading profile properties. */
    private DocumentAccessBridge bridge;

    /** Entity reference resolver, used for obtaining a full reference to the user profile class. */
    private EntityReferenceResolver<EntityReference> resolver;

    /**
     * Constructor.
     *
     * @param reference reference to the user profile document
     * @param serializer the entity reference serializer to use
     * @param bridge the model access bridge to use
     * @param resolver the reference resolver to use
     */
    public WikiUser(DocumentReference reference, EntityReferenceSerializer<String> serializer,
        DocumentAccessBridge bridge, EntityReferenceResolver<EntityReference> resolver)
    {
        this.profileReference = reference;
        this.serializer = serializer;
        this.bridge = bridge;
        this.resolver = resolver;
        if (this.profileReference != null) {
            this.classReference =
                new DocumentReference(this.resolver.resolve(USER_CLASS, EntityType.DOCUMENT, this.profileReference));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#exists()
     */
    @Override
    public boolean exists()
    {
        return (this.profileReference == null) ? false : this.bridge.exists(this.profileReference);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#getName()
     */
    @Override
    public String getName()
    {
        if (this.profileReference == null) {
            return "";
        }
        String firstName =
            String.valueOf(this.bridge.getProperty(this.profileReference, this.classReference, "first_name"));
        String lastName =
            String.valueOf(this.bridge.getProperty(this.profileReference, this.classReference, "last_name"));
        String result = StringUtils.trim(firstName) + " " + StringUtils.trim(lastName);
        if (StringUtils.isBlank(result)) {
            result = getUsername();
        }
        return result.trim();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#getAttribute(String)
     */
    @Override
    public Object getAttribute(String attributeName)
    {
        if (this.profileReference == null) {
            return null;
        }
        return this.bridge.getProperty(this.profileReference, this.classReference, attributeName);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.users.User#getProfileURI()
     */
    @Override
    public URI getProfileURI()
    {
        if (this.profileReference != null) {
            try {
                return new URI(this.bridge.getDocumentURL(this.profileReference, "view", null, null));
            } catch (URISyntaxException ex) {
                // Shouldn't happen, bug in the model bridge
            }
        }
        return null;
    }
}
