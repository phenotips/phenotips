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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.jasypt.digest.StandardStringDigester;
import org.jasypt.util.text.StrongTextEncryptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.security.encryption.CryptoUtils;
import org.phenotips.security.encryption.SystemPasswordConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

public class DefaultCryptoUtilsTest {

	@Rule
	public MockitoComponentMockingRule<CryptoUtils> mocker = new 
		MockitoComponentMockingRule<CryptoUtils>(DefaultCryptoUtils.class);

	@Mock
	private SystemPasswordConfiguration passwordConfig;

	private StrongTextEncryptor encryptor;

	private StandardStringDigester digester;
	
	@Before
	public void initMocks() throws InitializationException
	{
		MockitoAnnotations.initMocks(this);
		this.encryptor = new StrongTextEncryptor();
        this.encryptor.setPassword(this.passwordConfig.getSystemPassword());

        this.digester = new StandardStringDigester();
        this.digester.setAlgorithm("SHA-512");
        this.digester.setIterations(100);
        this.digester.setSaltSizeBytes(16);
        this.digester.initialize();
	}
	
	@Test
	public void properlyEncryptsWithSystemKey() throws ComponentLookupException
	{	
		//when(this.encryptor.encrypt(null)).thenReturn(null);
		Assert.assertNotNull(this.mocker.getComponentUnderTest().encryptWithSystemKey(null));
	}
}
