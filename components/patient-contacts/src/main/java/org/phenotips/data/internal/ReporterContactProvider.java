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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

/**
 * The representation of the case owner as the primary contact for a record.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Component
@Named("default")
@Singleton
public class ReporterContactProvider extends AbstractContactProvider
{
    @Inject
    protected Logger logger;

    @Inject
    private UserManager userManager;

    @Override
    public String getName()
    {
        return "reporter";
    }

    @Override
    public int getPriority()
    {
        return 900;
    }

    @Override
    public List<ContactInfo> getContacts(Patient patient)
    {
        List<ContactInfo> list = new ArrayList<>();
        DefaultContactInfo contactInfo = new DefaultContactInfo();
        DocumentReference reporter = patient.getReporter();
        if (reporter != null) {
            contactInfo.setUserId(reporter.getName());

            User user = this.userManager.getUser(reporter.getName());

            String email = (String) user.getAttribute("email");
            String institution = (String) user.getAttribute("company");

            contactInfo.setName(user.getName());
            contactInfo.setEmails(Arrays.asList(email));
            contactInfo.setInstitution(institution);
        }
        list.add(contactInfo);
        return list;
    }
}
