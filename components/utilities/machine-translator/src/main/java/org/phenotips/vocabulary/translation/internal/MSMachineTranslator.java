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
package org.phenotips.vocabulary.translation.internal;

import org.phenotips.vocabulary.translation.AbstractMachineTranslator;
import org.phenotips.vocabulary.translation.MachineTranslator;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;

/**
 * Implements the {@link MachineTranslator} interface to provide translation services through microsoft translate.
 *
 * @version $Id$
 */
@Component
@Singleton
public class MSMachineTranslator extends AbstractMachineTranslator
{
    /** This machine translator's identifier. */
    private static final String IDENTIFIER = "microsoft";

    /** The configuration key for the client secret. */
    private static final String CLIENT_SECRET_CFG = "translations.ms.clientKey";

    /** The endpoint for the token request service. */
    private static final String TOKEN_ENDPOINT = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";

    /** The endpoint for the translate service. */
    private static final String TRANSLATE_ENDPOINT = "http://api.microsofttranslator.com/V2/Http.svc/Translate";

    /** The pattern for the output. */
    private static final Pattern OUT_PATTERN = Pattern.compile("<string.*>(.*)</string>");

    /** The configuration source. */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The azure client secret. */
    private String clientSecret;

    /** The current ephemeral azure token. */
    private String token;

    /** Whether this is enabled. If the required configuration is missing, then this translator is disabled. */
    private boolean enabled;

    /** When the current token will expire. */
    private long tokenExpires;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();
        this.clientSecret = this.configuration.getProperty(CLIENT_SECRET_CFG, "").trim();
        this.enabled = StringUtils.isNotBlank(this.clientSecret);
    }

    @Override
    public String getIdentifier()
    {
        return IDENTIFIER;
    }

    @Override
    public Collection<Locale> getSupportedLocales()
    {
        Collection<Locale> result = new HashSet<>();
        result.add(Locale.ENGLISH);
        result.add(Locale.FRENCH);
        result.add(Locale.GERMAN);
        result.add(Locale.CHINESE);
        result.add(Locale.SIMPLIFIED_CHINESE);
        for (String tag : Arrays.asList("af", "ar", "bg", "ca", "hr", "cs", "da", "nl", "et", "fj", "fil", "fi", "el",
            "ht", "he", "hi", "hu", "id", "it", "ja", "sw", "ko", "lv", "lt", "mg", "ms", "mt", "nb", "fa", "pl", "pt",
            "ro", "ru", "sm", "sr", "sk", "sl", "es", "sv", "ty", "th", "tr", "uk", "ur", "vi", "cy")) {
            result.add(Locale.forLanguageTag(tag));
        }
        return result;
    }

    @Override
    public boolean isLocaleSupported(Locale locale)
    {
        return getSupportedLocales().contains(locale);
    }

    @Override
    protected String doTranslate(String inputText, Locale inputLocale, Locale targetLocale)
    {
        if (!this.enabled) {
            return null;
        }
        try {
            if (hasExpired()) {
                getAccessToken();
                if (hasExpired()) {
                    // Problem renewing the token, something is wrong...
                    this.logger.error("Failed to get a valid token, terminating the MS translator");
                    this.enabled = false;
                    return null;
                }
            }
            URIBuilder builder = new URIBuilder(TRANSLATE_ENDPOINT);
            builder.addParameter("appId", "");
            builder.addParameter("text", inputText);
            builder.addParameter("from", inputLocale == null ? "en" : inputLocale.toString());
            builder.addParameter("to", targetLocale.toString());
            builder.addParameter("contentType", "text/plain");
            URI uri = builder.build();
            Response response = Request.Get(uri).addHeader("Authorization", "Bearer " + this.token).execute();
            if (response.returnResponse().getStatusLine().getStatusCode() == 200) {
                String result = response.returnContent().asString();
                /*
                 * Microsoft hates documenting what it actually does, so I had to find out the hard way that it returns
                 * everything wrapped in this silly <string> tag. Filter it out
                 */
                Matcher m = OUT_PATTERN.matcher(result);
                m.find();
                if (m.matches()) {
                    result = m.group(1).trim();
                }
                return result;
            } else {
                // Probably the token expired, request a new one and try again
                this.token = null;
                return doTranslate(inputText, inputLocale, targetLocale);
            }
        } catch (URISyntaxException ex) {
            this.logger.error("URI Syntax Exception " + ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("Error from translate API " + ex.getMessage());
        }
        this.enabled = false;
        return null;
    }

    /** Ask for an azure access token with which we can do our translate requests. */
    private void getAccessToken() throws IOException
    {
        Response response = Request.Post(TOKEN_ENDPOINT).addHeader("Ocp-Apim-Subscription-Key", this.clientSecret)
            .execute();
        if (response.returnResponse().getStatusLine().getStatusCode() == 200) {
            this.token = response.returnContent().asString();
            // A token is valid for ten minutes
            this.tokenExpires = System.currentTimeMillis() + 600 * 1000;
        } else {
            this.logger.error("Failed to get a Microsoft Translation token: {}", response.returnContent().asString());
            this.enabled = false;
        }
    }

    /**
     * Return whether the token has expired.
     *
     * @return has the token expired.
     */
    private boolean hasExpired()
    {
        return this.token == null || (System.currentTimeMillis() >= this.tokenExpires);
    }
}
