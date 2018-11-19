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
package org.phenotips.data;

import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * The information about a contact person/organization for a patient record.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable("New API introduced in 1.3")
public interface ContactInfo
{
    /**
     * Get the user id for the contact, for instance if the owner is a local PhenoTips user.
     *
     * @return the (potentially-null) user id
     */
    String getUserId();

    /**
     * Get the contact's full name.
     *
     * @return the (potentially-null) full name of the contact
     */
    String getName();

    /**
     * Get the contact's institution.
     *
     * @return the (potentially-null) institution of the contact
     */
    String getInstitution();

    /**
     * Get the contact's email addresses.
     *
     * @return the list of the contact's email addresses, may be empty but not {@code null}
     */
    List<String> getEmails();

    /**
     * Get the contact's URL.
     *
     * @return the (potentially-null) URL for the contact
     */
    String getUrl();

    /**
     * Get the contact details in JSON format.
     *
     * @return a JSONObject serialization of the contact details
     */
    JSONObject toJSON();

    /**
     * Builder for {@link ContactInfo} objects.
     */
    class Builder
    {
        private DefaultContactInfo contact = new DefaultContactInfo();

        /**
         * Sets the user id for the contact.
         *
         * @param userId the user identifier to set, may be {@code null}
         * @return self, for method chaining
         */
        public Builder withUserId(String userId)
        {
            this.contact.setUserId(userId);
            return this;
        }

        /**
         * Sets the contact's full name.
         *
         * @param name the user identifier to set, may be {@code null}
         * @return self, for method chaining
         */
        public Builder withName(String name)
        {
            this.contact.setName(name);
            return this;
        }

        /**
         * Sets the contact's institution.
         *
         * @param institution the institution to set, may be {@code null}
         * @return self, for method chaining
         */
        public Builder withInstitution(String institution)
        {
            this.contact.setInstitution(institution);
            return this;
        }

        /**
         * Sets a single email address as the contact's email.
         *
         * @param email an email address to set, may be {@code null}
         * @return self, for method chaining
         */
        public Builder withEmail(String email)
        {
            if (StringUtils.isNotBlank(email)) {
                this.contact.setEmails(Collections.singletonList(email));
            } else {
                this.contact.setEmails(Collections.<String>emptyList());
            }
            return this;
        }

        /**
         * Sets the target emails.
         *
         * @param emails the list of emails to set, may be {@code null} or empty
         * @return self, for method chaining
         */
        public Builder withEmails(List<String> emails)
        {
            this.contact.setEmails(emails);
            return this;
        }

        /**
         * Sets the target contact's URL.
         *
         * @param url the URL to set, may be {@code null}
         * @return self, for method chaining
         */
        public Builder withUrl(String url)
        {
            this.contact.setUrl(url);
            return this;
        }

        /**
         * Returns the contact information built so far.
         *
         * @return the built contact info
         */
        public ContactInfo build()
        {
            return this.contact;
        }

        /**
         * A default representation of {@link ContactInfo}.
         *
         * @version $Id$
         * @since 1.3
         */
        private static class DefaultContactInfo implements ContactInfo
        {
            private String userId;

            private String name;

            private String institution;

            private List<String> emails;

            private String url;

            private void setUserId(String userId)
            {
                this.userId = StringUtils.defaultIfBlank(userId, null);
            }

            @Override
            public String getUserId()
            {
                return this.userId;
            }

            private void setName(String name)
            {
                this.name = StringUtils.defaultIfBlank(name, null);
            }

            @Override
            public String getName()
            {
                return this.name;
            }

            private void setInstitution(String institution)
            {
                this.institution = StringUtils.defaultIfBlank(institution, null);
            }

            @Override
            public String getInstitution()
            {
                return this.institution;
            }

            private void setEmails(List<String> emails)
            {
                List<String> collectedEmails = new ArrayList<String>();
                for (String email : emails) {
                    for (String parsedEmail : StringUtils.split(email, ",|;")) {
                        collectedEmails.add(parsedEmail.trim());
                    }
                }
                this.emails = collectedEmails;
            }

            @Override
            public List<String> getEmails()
            {
                return this.emails == null ? Collections.<String>emptyList() : this.emails;
            }

            private void setUrl(String url)
            {
                this.url = StringUtils.defaultIfBlank(url, null);
            }

            @Override
            public String getUrl()
            {
                return this.url;
            }

            @Override
            public JSONObject toJSON()
            {
                JSONObject info = new JSONObject();
                if (StringUtils.isNotBlank(this.userId)) {
                    info.put("id", this.getUserId());
                }
                if (StringUtils.isNotBlank(this.name)) {
                    info.put("name", this.getName());
                }
                if (StringUtils.isNotBlank(this.institution)) {
                    info.put("institution", this.getInstitution());
                }
                if (this.emails != null && !this.emails.isEmpty()) {
                    info.put("email", StringUtils.join(this.getEmails(), ", "));
                }
                if (StringUtils.isNotBlank(this.url)) {
                    info.put("url", this.getUrl());
                }
                return info;
            }

            @Override
            public String toString()
            {
                StringBuilder output = new StringBuilder();
                if (StringUtils.isNotBlank(this.name)) {
                    output.append(this.name);
                } else if (StringUtils.isNotBlank(this.userId)) {
                    output.append(this.userId);
                }
                if (!getEmails().isEmpty() && StringUtils.isNotBlank(this.emails.get(0))) {
                    output.append(" <").append(this.emails.get(0)).append(">");
                }
                return StringUtils.defaultIfEmpty(output.toString().trim(), "[empty contact]");
            }
        }
    }
}
