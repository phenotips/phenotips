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
package org.phenotips.groups.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.phenotips.templates.data.Template;
import org.phenotips.templates.data.TemplateRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.users.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;

/**
 * Default implementation for {@link Group}.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class DefaultGroup implements Group
{
    /** The xObject under which members are saved. */
    private static final DocumentReference MEMBERS_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiGroups");

    /** @see #getReference() */
    private final DocumentReference reference;

    /**
     * Simple constructor.
     *
     * @param reference the reference to the document where this group is defined
     */
    public DefaultGroup(DocumentReference reference)
    {
        this.reference = reference;
    }

    @Override
    public DocumentReference getReference()
    {
        return this.reference;
    }

    @Override
    public String toString()
    {
        return "Group " + getReference().getName();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof DefaultGroup)) {
            return false;
        }
        DefaultGroup otherGroup = (DefaultGroup) o;
        return this.reference.getName().equals(otherGroup.getReference().getName());
    }

    @Override
    public int hashCode()
    {
        return this.reference.getName().hashCode();
    }

    @Override
    public boolean isUserInGroup(User user)
    {
        Set<Group> groups = this.getGroupManager().getGroupsForUser(user);
        if (groups == null) {
            return false;
        }
        return groups.contains(this);
    }

    @Override
    public Collection<String> getAllUserNames()
    {
        DocumentAccessBridge bridge = this.getBridge();
        DocumentReferenceResolver<String> resolver = this.getStringResolver();
        Logger logger = this.getLogger();
        UsersAndGroups usersAndGroups = this.getUsersAndGroups();

        Set<String> usersSet = new HashSet<String>();
        Stack<DocumentReference> groupsToProcess = new Stack<DocumentReference>();
        DocumentReference groupReference = this.getReference();
        groupsToProcess.add(groupReference);

        try {
            while (!groupsToProcess.isEmpty()) {
                DocumentReference currentGroup = groupsToProcess.pop();
                XWikiDocument groupDocument = (XWikiDocument) bridge.getDocument(currentGroup);
                Collection<BaseObject> members = groupDocument.getXObjects(MEMBERS_REFERENCE);
                if (members == null) {
                    continue;
                }
                for (BaseObject member : members) {
                    if (member != null) {
                        StringProperty field = (StringProperty) member.getField("member");
                        String value = field.getValue();
                        if (StringUtils.isEmpty(value)) {
                            continue;
                        }
                        DocumentReference userOrGroup = resolver.resolve(value, GROUP_SPACE);
                        if (UsersAndGroups.USER.equals(usersAndGroups.getType(userOrGroup))) {
                            usersSet.add(userOrGroup.toString());
                        } else {
                            groupsToProcess.push(userOrGroup);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting users for {}", groupReference.getName(), e.getMessage());
        }
        return usersSet;
    }

    @Override
    public Collection<Template> getTemplates()
    {
        DocumentAccessBridge bridge = this.getBridge();
        XWikiDocument groupDocument = null;
        try {
            groupDocument = (XWikiDocument) bridge.getDocument(this.reference);
        } catch (Exception e) {
            this.getLogger().error("Error reading current group {}", this.reference, e.getMessage());
            return null;
        }

        @SuppressWarnings("unchecked")
        List<String> templatesList = groupDocument.getListValue("templates");
        List<Template> templates = new ArrayList<Template>();
        TemplateRepository templateRepository = getTemplateRepository();
        for (String id : templatesList) {
            templates.add(templateRepository.get(id));
        }

        return templates;
    }

    private DocumentReferenceResolver<String> getStringResolver()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(
                DocumentReferenceResolver.TYPE_STRING, "current");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private DocumentAccessBridge getBridge()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private GroupManager getGroupManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(GroupManager.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private Logger getLogger()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(Logger.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private UsersAndGroups getUsersAndGroups()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(UsersAndGroups.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private TemplateRepository getTemplateRepository()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager().getInstance(TemplateRepository.class,
                "Template");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }
}
