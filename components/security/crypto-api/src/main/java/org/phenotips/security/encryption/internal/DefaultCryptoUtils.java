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
import org.phenotips.security.encryption.SystemPasswordConfiguration;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jasypt.digest.StandardStringDigester;
import org.jasypt.util.text.StrongTextEncryptor;

/**
 * The straight-forward implementation of the {@link CryptoUtils} role.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Singleton
public class DefaultCryptoUtils implements CryptoUtils, Initializable
{
    @Inject
    private SystemPasswordConfiguration passwordConfig;

    private StrongTextEncryptor encryptor;

    private StandardStringDigester digester;

    @Override
    public void initialize() throws InitializationException
    {
        this.encryptor = new StrongTextEncryptor();
        this.encryptor.setPassword(this.passwordConfig.getSystemPassword());

        this.digester = new StandardStringDigester();
        this.digester.setAlgorithm("SHA-512");
        this.digester.setIterations(100);
        this.digester.setSaltSizeBytes(16);
        this.digester.initialize();
    }

    @Override
    public String encryptWithSystemKey(String message)
    {
        return this.encryptor.encrypt(message);
    }

    @Override
    public String decryptWithSystemKey(String encryptedMessage)
    {
        return this.encryptor.decrypt(encryptedMessage);
    }

    @Override
    public String digest(String message)
    {
        return this.digester.digest(message);
    }

    @Override
    public boolean validateDigest(String message, String digest)
    {
        return digest == null ? false : this.digester.matches(message, digest);
    }
}
