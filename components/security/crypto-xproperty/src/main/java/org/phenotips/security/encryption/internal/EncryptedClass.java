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
package org.phenotips.security.encryption.internal;

import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * Defines an XProperty that will be stored encrypted in the database, for extra security in case the server is hacked.
 * The values are encrypted using the {@link org.phenotips.security.encryption.CryptoUtils#encryptWithSystemKey(String)
 * system encryption key}.
 *
 * @see EncryptedMetaClass
 * @see EncryptedProperty
 * @version $Id$
 * @since 1.3M2
 */
public class EncryptedClass extends StringClass
{
    private static final long serialVersionUID = 3345074814276863098L;

    /**
     * Main constructor, uses the passed meta-class.
     *
     * @param metaClass the meta class that defines the list of meta properties associated with this property type
     */
    public EncryptedClass(PropertyMetaClass metaClass)
    {
        super("encrypted", "Encrypted", metaClass);
        setxWikiClass(metaClass);
    }

    /** Default constructor. */
    public EncryptedClass()
    {
        this(null);
    }

    @Override
    public EncryptedProperty newProperty()
    {
        EncryptedProperty property = new EncryptedProperty();
        property.setName(getName());
        return property;
    }
}
