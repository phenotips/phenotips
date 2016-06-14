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
package org.phenotips.security.encryption;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Utilities for easy encryption and decryption.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Role
public interface CryptoUtils
{
    /**
     * Encrypt a message using the system-wide encryption key.
     *
     * @param message the message to encrypt; may be {@code null}
     * @return the encrypted message, or {@code null} if the input message was {@code null}
     */
    String encryptWithSystemKey(String message);

    /**
     * Decrypt a message using the system-wide encryption key.
     *
     * @param encryptedMessage the message to decrypt; may be {@code null}
     * @return the decrypted message, or {@code null} if the input message was {@code null}
     */
    String decryptWithSystemKey(String encryptedMessage);

    /**
     * Compute a strong digest (one-way hash) of a message.
     *
     * @param message the message to digest; may be {@code null}
     * @return the digest of the message, or {@code null} if the input message was {@code null}
     */
    String digest(String message);

    /**
     * Check that a given message does match a previously computed digest.
     *
     * @param message the message to validate; may be {@code null}
     * @param digest a previously computed digest; may be {@code null}
     * @return {@code true} if the message is not null and matches the digest, {@code false} otherwise
     */
    boolean validateDigest(String message, String digest);
}
