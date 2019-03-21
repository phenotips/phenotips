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

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactProvider;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The representation of the case owner as the primary contact for a record.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("default")
@Singleton
public class OwnerContactProvider implements PatientContactProvider
{
    private static final String ATTRIBUTE_INSTITUTION = "company";

    private static final String ATTRIBUTE_EMAIL_USER = "email";

    private static final String ATTRIBUTE_EMAIL_GROUP = "contact";

    @Inject
    protected Logger logger;

    @Inject
    protected EntityPermissionsManager entityPermissionsManager;

    @Inject
    private UserManager userManager;

    @Inject
    private GroupManager groupManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public int getPriority()
    {
        return 100;
    }

    @Override
    public List<ContactInfo> getContacts(Patient patient)
    {
        EntityAccess referenceAccess = this.entityPermissionsManager.getEntityAccess(patient);
        Owner owner = referenceAccess.getOwner();
        ContactInfo result = getContactInfo(owner);
        if (result == null) {
            return null;
        }
        return Arrays.asList(result);
    }

    private ContactInfo getContactInfo(Owner owner)
    {
        ContactInfo.Builder contactInfo = new ContactInfo.Builder();
        if (owner == null || owner.getUser() == null) {
            return null;
        }
        if (owner.isGroup()) {
            Group group = this.groupManager.getGroup((DocumentReference) owner.getUser());
            if (group == null) {
                return null;
            }
            populateGroupInfo(contactInfo, group);
        } else {
            User user = this.userManager.getUser(owner.getUser().toString());
            if (user == null) {
                return null;
            }
            populateUserInfo(contactInfo, user);
        }
        return contactInfo.build();
    }

    private void populateUserInfo(ContactInfo.Builder contactInfo, User user)
    {
        String email = (String) user.getAttribute(ATTRIBUTE_EMAIL_USER);
        String institution = (String) user.getAttribute(ATTRIBUTE_INSTITUTION);

        contactInfo.withUserId(user.getId());
        contactInfo.withName(user.getName());
        contactInfo.withEmail(email);
        contactInfo.withInstitution(institution);
    }

    private void populateGroupInfo(ContactInfo.Builder contactInfo, Group group)
    {
        contactInfo.withUserId(this.serializer.serialize(group.getReference()));
        contactInfo.withName(group.getReference().getName());

        DocumentReference documentReference = group.getReference();
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(documentReference);
            contactInfo.withName(doc.getTitle());

            BaseObject data = doc.getXObject(Group.CLASS_REFERENCE);
            contactInfo.withEmail(data.getStringValue(ATTRIBUTE_EMAIL_GROUP));
        } catch (Exception ex) {
            this.logger.error("Could not load group information: {}", ex.getMessage(), ex);
        }
    }
}
