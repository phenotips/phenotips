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
import org.phenotips.data.PatientContactProvider;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The representation of the case reporter (document creator) as the contact for a record.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("default")
@Singleton
public class ReporterContactProvider implements PatientContactProvider
{
    @Inject
    private UserManager userManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public int getPriority()
    {
        return 900;
    }

    @Override
    public List<ContactInfo> getContacts(Patient patient)
    {
        DocumentReference reporter = patient.getReporter();
        if (reporter == null) {
            return Collections.emptyList();
        }

        List<ContactInfo> list = new ArrayList<>();
        ContactInfo.Builder contactInfo = new ContactInfo.Builder();
        contactInfo.withUserId(this.serializer.serialize(reporter));

        User user = this.userManager.getUser(reporter.toString());
        if (user != null) {
            String email = (String) user.getAttribute("email");
            String institution = (String) user.getAttribute("company");

            contactInfo.withName(user.getName());
            contactInfo.withEmail(email);
            contactInfo.withInstitution(institution);
            // FIXME URL is missing
        }
        // Otherwise, if null user -> user is no longer valid, all we know is the username, already added before the if

        list.add(contactInfo.build());
        return list;
    }
}
