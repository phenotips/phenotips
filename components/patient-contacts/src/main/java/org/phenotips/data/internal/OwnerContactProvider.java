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
package org.phenotips.data.internal;

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
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
 * @since 1.3M5
 */
@Component
@Named("default")
@Singleton
public class OwnerContactProvider extends AbstractContactProvider
{
    private static final String ATTRIBUTE_INSTITUTION = "company";

    private static final String ATTRIBUTE_EMAIL_USER = "email";

    private static final String ATTRIBUTE_EMAIL_GROUP = "contact";

    @Inject
    protected Logger logger;

    @Inject
    protected PermissionsManager permissionsManager;

    @Inject
    private UserManager userManager;

    @Inject
    private GroupManager groupManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public String getName()
    {
        return "owner";
    }

    @Override
    public int getPriority()
    {
        return 100;
    }

    @Override
    public List<ContactInfo> getContacts(Patient patient)
    {
        PatientAccess referenceAccess = this.permissionsManager.getPatientAccess(patient);
        Owner owner = referenceAccess.getOwner();
        return Arrays.asList(getContactInfo(owner));
    }

    private ContactInfo getContactInfo(Owner owner)
    {
        DefaultContactInfo contactInfo = new DefaultContactInfo();
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
        return contactInfo;
    }

    private void populateUserInfo(DefaultContactInfo contactInfo, User user)
    {
        String email = (String) user.getAttribute(ATTRIBUTE_EMAIL_USER);
        String institution = (String) user.getAttribute(ATTRIBUTE_INSTITUTION);

        contactInfo.setUserId(user.getId());
        contactInfo.setName(user.getName());
        contactInfo.setEmails(Arrays.asList(email));
        contactInfo.setInstitution(institution);
    }

    private void populateGroupInfo(DefaultContactInfo contactInfo, Group group)
    {
        contactInfo.setName(group.getReference().getName());

        DocumentReference documentReference = group.getReference();
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(documentReference);
            BaseObject data = doc.getXObject(Group.CLASS_REFERENCE);
            contactInfo.setEmails(Arrays.asList(data.getStringValue(ATTRIBUTE_EMAIL_GROUP)));
        } catch (Exception e) {
            this.logger.error("Could not find requested document");
        }
    }
}
