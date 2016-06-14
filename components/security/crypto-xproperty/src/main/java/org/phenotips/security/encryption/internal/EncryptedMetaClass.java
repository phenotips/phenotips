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

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * Defines the meta properties of an {@link EncryptedClass encrypted XClass property}.
 *
 * @version $Id$
 * @since 1.3M2
 * @see EncryptedClass
 * @see EncryptedProperty
 */
@Component
@Named("Encrypted")
@Singleton
public class EncryptedMetaClass extends PropertyMetaClass
{
    /** The name of the metaproperty defining whether a field is transparently decrypted or not. */
    public static final String TRANSPARENT_DECRYPTION = "transparent";

    private static final long serialVersionUID = -704898783254023743L;

    /** Default constructor. Initializes the default meta properties of the Encrypted XClass property. */
    public EncryptedMetaClass()
    {
        setName(getClass().getAnnotation(Named.class).value());
        setPrettyName(getClass().getAnnotation(Named.class).value());

        NumberClass sizeClass = new NumberClass(this);
        sizeClass.setName("size");
        sizeClass.setPrettyName("Size");
        sizeClass.setSize(5);
        sizeClass.setNumberType("integer");
        safeput(sizeClass.getName(), sizeClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new EncryptedClass(this);
    }
}
