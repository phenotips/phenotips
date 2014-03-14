/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal.controller;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.internal.PatientAccessHelper;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;

/**
 * Handle's the patient owner's contact information.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component(roles = { PatientDataController.class })
@Named("owner-contact")
@Singleton
public class ContactInformationController implements PatientDataController<ImmutablePair<String, String>>
{
    private static final String DATA_CONTACT = "contact";
    private static final String DATA_USER_ID = "user_id";
    private static final String DATA_EMAIL = "email";
    private static final String DATA_NAME = "name";
    private static final String DATA_INSTITUTION = "institution";
    private static final String ATTRIBUTE_INSTITUTION = "company";
    private static final String ATTRIBUTE_EMAIL_USER = DATA_EMAIL;
    private static final String ATTRIBUTE_EMAIL_GROUP = DATA_CONTACT;

    @Inject
    private Logger logger;

    @Inject
    private UserManager userManager;

    @Inject
    private GroupManager groupManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private PatientAccessHelper patientAccessHelper;

    @Override
    public PatientData<ImmutablePair<String, String>> load(Patient patient)
    {
        Owner owner = patientAccessHelper.getOwner(patient);
        return new SimpleNamedData<String>(DATA_CONTACT, getContactInfo(owner));
    }

    @Override
    public void save(Patient patient)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        PatientData<ImmutablePair<String, String>> data = patient.getData(DATA_CONTACT);
        if (data == null || data.isEmpty()) {
            return;
        }
        JSONObject container = json.getJSONObject(DATA_CONTACT);
        if (container == null || container.isNullObject()) {
            json.put(DATA_CONTACT, new JSONObject());
            container = json.getJSONObject(DATA_CONTACT);
        }
        for (ImmutablePair<String, String> item : data) {
            container.put(item.getKey(), item.getValue());
        }
    }

    @Override
    public PatientData<ImmutablePair<String, String>> readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    private List<ImmutablePair<String, String>> getContactInfo(Owner owner)
    {
        List<ImmutablePair<String, String>> contactInfo = new LinkedList<ImmutablePair<String, String>>();
        String ownerIdentifier = owner.getUsername();
        if (owner.isGroup()) {
            Group group = groupManager.getGroup(ownerIdentifier);
            if (group == null) {
                return null;
            }
            populateGroupInfo(contactInfo, group);
        } else {
            User user = userManager.getUser(ownerIdentifier);
            if (user == null) {
                return null;
            }
            populateUserInfo(contactInfo, user);
        }
        return contactInfo;
    }

    private void populateUserInfo(List<ImmutablePair<String, String>> contactInfo, User user)
    {
        String email = (String) user.getAttribute(ATTRIBUTE_EMAIL_USER);
        String institution = (String) user.getAttribute(ATTRIBUTE_INSTITUTION);

        addInfo(contactInfo, DATA_USER_ID, user.getUsername());
        addInfo(contactInfo, DATA_NAME, user.getName());
        addInfo(contactInfo, DATA_EMAIL, email);
        addInfo(contactInfo, DATA_INSTITUTION, institution);
    }

    private void populateGroupInfo(List<ImmutablePair<String, String>> contactInfo, Group group)
    {
        addInfo(contactInfo, DATA_NAME, group.getReference().getName());

        DocumentReference documentReference = group.getReference();
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(documentReference);
            BaseObject data = doc.getXObject(Group.CLASS_REFERENCE);
            addInfo(contactInfo, DATA_EMAIL, data.getStringValue(ATTRIBUTE_EMAIL_GROUP));
        } catch (Exception e) {
            logger.error("Could not find requested document");
        }
    }

    private void addInfo(List<ImmutablePair<String, String>> contactInfo, String key, String value)
    {
        if (StringUtils.isNotBlank(value)) {
            contactInfo.add(ImmutablePair.of(key, value));
        }
    }
}
