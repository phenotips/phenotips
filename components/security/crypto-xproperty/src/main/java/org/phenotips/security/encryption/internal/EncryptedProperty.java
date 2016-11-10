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

import org.phenotips.security.encryption.CryptoUtils;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.web.Utils;

/**
 * XProperty for storing a piece of text encrypted in the database. The maximum length of the values depends on the text
 * itself, but is large enough to support even a hundred megabytes.
 *
 * @see EncryptedMetaClass
 * @see EncryptedClass
 * @version $Id$
 * @since 1.3M2
 */
public class EncryptedProperty extends BaseStringProperty
{
    private static final long serialVersionUID = 8082063682046892242L;

    /** Will be pre-pended to the values of the encrypted properties to identify that they are indeed encrypted. */
    private static final String ENCRYPTED_IDENTIFIER = "e:";

    @Override
    public String getValue()
    {
        String value = super.getValue();
        if (isEncrypted(value)) {
            return decrypt(value);
        }
        return value;
    }

    @Override
    public void setValue(Object value)
    {
        if (value == null) {
            super.setValue(null);
            return;
        }
        String strValue = String.valueOf(value);
        if (isEncrypted(strValue)) {
            super.setValue(value);
        } else {
            super.setValue(encrypt(strValue));
        }
    }

    /**
     * Check if a value is already encrypted or not. This is determined by the presence of the
     * {@link #ENCRYPTED_IDENTIFIER} prefix.
     *
     * @param text the text to check
     * @return {@code true} if the encrypted marker prefix is present in the text, thus encrypted, {@code false}
     *         otherwise
     */
    private boolean isEncrypted(String text)
    {
        return text.startsWith(ENCRYPTED_IDENTIFIER);
    }

    /**
     * Encrypt a value with the {@link CryptoUtils#encryptWithSystemKey(String) system encryption key}.
     *
     * @param text the text to encrypt
     * @return the encrypted text, prefixed with the {@link #ENCRYPTED_IDENTIFIER encryption identifier}
     */
    private String encrypt(String text)
    {
        return ENCRYPTED_IDENTIFIER + getCryptoUtils().encryptWithSystemKey(text);
    }

    /**
     * Decrypt a value with the {@link CryptoUtils#decryptWithSystemKey(String) system encryption key}.
     *
     * @param text the text to decrypt, may be prefixed with the {@link #ENCRYPTED_IDENTIFIER encryption identifier},
     *            but not required to
     * @return the decrypted text
     */
    private String decrypt(String text)
    {
        return getCryptoUtils().decryptWithSystemKey(getRawValue(text));
    }

    /**
     * Removes {@link #ENCRYPTED_IDENTIFIER encryption identifier prefix}, if present.
     *
     * @param value the text from which to remove the prefix
     * @return the text with the prefix removed, or the original text if no prefix was present
     */
    private String getRawValue(String value)
    {
        return StringUtils.removeStart(value, ENCRYPTED_IDENTIFIER);
    }

    private CryptoUtils getCryptoUtils()
    {
        return Utils.getComponent(CryptoUtils.class);
    }
}
