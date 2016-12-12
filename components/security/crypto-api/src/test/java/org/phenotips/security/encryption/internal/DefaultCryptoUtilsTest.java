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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.jasypt.digest.StandardStringDigester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

public class DefaultCryptoUtilsTest
{
    @Rule
    public MockitoComponentMockingRule<CryptoUtils> mocker =
        new MockitoComponentMockingRule<CryptoUtils>(DefaultCryptoUtils.class);

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEncryption() throws ComponentLookupException
    {
        SystemPasswordConfiguration passwordConfig = this.mocker.getInstance(SystemPasswordConfiguration.class);
        when(passwordConfig.getSystemPassword()).thenReturn("X");
        String enc = this.mocker.getComponentUnderTest().encryptWithSystemKey("test");
        Assert.assertNotNull(enc);
        Assert.assertTrue(enc.length() >= 8);
        Assert.assertEquals("test", this.mocker.getComponentUnderTest().decryptWithSystemKey(enc));
    }

    @Test
    public void testDigester() throws ComponentLookupException
    {
        StandardStringDigester digester = this.mocker.getInstance(StandardStringDigester.class);
        when(digester.matches("test", "SHA-512")).thenReturn(true);
        String digest = this.mocker.getComponentUnderTest().digest("test");
        Assert.assertNotNull(digest);
        Assert.assertTrue(this.mocker.getComponentUnderTest().validateDigest("test", digest));
    }
}
