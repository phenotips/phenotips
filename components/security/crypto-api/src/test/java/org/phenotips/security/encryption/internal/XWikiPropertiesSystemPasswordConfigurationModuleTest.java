package org.phenotips.security.encryption.internal;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mock;
import org.xwiki.configuration.ConfigurationSource;

public class XWikiPropertiesSystemPasswordConfigurationModuleTest {

	@Mock
	private ConfigurationSource config;
	
	private static final String PROPERTY = "crypto.encryption.systemPassword";
	
	@Test
	public void getsCorrectSystemPassword() {
	
	}

}
