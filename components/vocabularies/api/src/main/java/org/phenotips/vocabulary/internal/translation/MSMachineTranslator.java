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
package org.phenotips.vocabulary.internal.translation;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implements the {@link MachineTranslator} interface to provide translation services
 * through microsoft translate.
 *
 * @version $Id$
 */
@Component
@Singleton
public class MSMachineTranslator extends AbstractMachineTranslator
{
    /**
     * The supported languages.
     */
    private static final Collection<String> SUPPORTED_LANGUAGES;

    /**
     * The supported vocabularies.
     */
    private static final Collection<String> SUPPORTED_VOCABULARIES;

    /**
     * This machine translator's identifier.
     */
    private static final String IDENTIFIER = "microsoft";

    /**
     * The config key for the client id.
     */
    private static final String CLIENT_ID_CFG = "translations.ms.clientId";

    /**
     * The config key for the client secret.
     */
    private static final String CLIENT_SECRET_CFG = "translations.ms.clientSecret";

    /**
     * The scope for the token service request.
     */
    private static final String SCOPE = "http://api.microsofttranslator.com/";

    /**
     * The grant type for the token service request.
     */
    private static final String GRANT_TYPE = "client_credentials";

    /**
     * The endpoint for the token request service.
     */
    private static final String TOKEN_ENDPOINT = "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13";

    /**
     * The endpoint for the translate service.
     */
    private static final String TRANSLATE_ENDPOINT = "http://api.microsofttranslator.com/V2/Http.svc/Translate";

    /**
     * The pattern for the output.
     */
    private static final Pattern OUT_PATTERN = Pattern.compile("<string.*>(.*)</string>");

    /**
     * The object mapper for deserialization.
     */
    private ObjectMapper om;

    /**
     * The configuration source.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * Our logger.
     */
    @Inject
    private Logger logger;

    /**
     * The azure client id.
     */
    private String clientId;

    /**
     * The azure client secret.
     */
    private String clientSecret;

    /**
     * The azure token.
     */
    private String token;

    /**
     * Whether this is enabled.
     */
    private boolean enabled;

    /**
     * When the token will expire.
     */
    private long tokenExpires;

    static {
        SUPPORTED_LANGUAGES = new HashSet<>();
        SUPPORTED_LANGUAGES.add("es");
        SUPPORTED_VOCABULARIES = new HashSet<>();
        SUPPORTED_VOCABULARIES.add("hpo");
    }

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();
        clientId = configuration.getProperty(CLIENT_ID_CFG, "").trim();
        clientSecret = configuration.getProperty(CLIENT_SECRET_CFG, "").trim();
        enabled = clientId != null && !"".equals(clientId) && clientSecret != null
            && !"".equals(clientSecret);
        om = new ObjectMapper();
    }

    @Override
    protected String doTranslate(String input)
    {
        if (!enabled) {
            return null;
        }
        try {
            if (hasExpired()) {
                getAccessToken();
            }
            URIBuilder builder = new URIBuilder(TRANSLATE_ENDPOINT);
            builder.addParameter("appId", "");
            builder.addParameter("text", input);
            builder.addParameter("from", "en");
            builder.addParameter("to", getLanguage());
            builder.addParameter("contentType", "text/plain");
            URI uri = builder.build();
            String result = Request.Get(uri).
                addHeader("Authorization", "Bearer " + token).
                execute().returnContent().asString();
            /* Microsoft hates documenting what it actually does, so I had to find out the hard way
             * that it returns everything wrapped in this silly <string> tag. Filter it out */
            Matcher m = OUT_PATTERN.matcher(result);
            m.find();
            if (m.matches()) {
                result = m.group(1).trim();
            }
            return result;
        } catch (URISyntaxException e) {
            logger.error("URI Syntax Exception " + e.getMessage());
            enabled = false;
            return null;
        } catch (IOException e) {
            logger.error("Error from translate API " + e.getMessage());
            enabled = false;
            return null;
        }
    }

    @Override
    public String getIdentifier()
    {
        return IDENTIFIER;
    }

    @Override
    public Collection<String> getSupportedLanguages()
    {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public Collection<String> getSupportedVocabularies()
    {
        return SUPPORTED_VOCABULARIES;
    }

    /**
     * Ask for an azure access token with which we can do our translate requests.
     */
    private void getAccessToken() throws IOException
    {
        InputStream is = Request.Post(TOKEN_ENDPOINT).
            bodyForm(new BasicNameValuePair("client_id", clientId),
                     new BasicNameValuePair("client_secret", clientSecret),
                     new BasicNameValuePair("scope", SCOPE),
                     new BasicNameValuePair("grant_type", GRANT_TYPE)).
            execute().returnContent().asStream();
        Map<String, String> json = om.readValue(is, new TypeReference<Map<String, String>>() { });
        token = json.get("access_token");
        String expiresIn = json.get("expires_in");
        tokenExpires = (System.currentTimeMillis() / 1000) + Long.parseLong(expiresIn);
    }

    /**
     * Return whether the token has expired.
     *
     * @return has the token expired.
     */
    private boolean hasExpired()
    {
        return token == null || ((System.currentTimeMillis() / 1000) >= tokenExpires);
    }
}
