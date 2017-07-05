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
package org.phenotips.vocabulary.translation;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.xliff12.LenientResourceBundleWrapper;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.LocalizationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.DisMaxParams;
import org.slf4j.Logger;

/**
 * Implements {@link VocabularyExtension} to provide translation services. Works with XLIFF translations similar to the
 * files up at <a href="https://github.com/Human-Phenotype-Ontology/HPO-translations">
 * https://github.com/Human-Phenotype-Ontology/HPO-translations</a>. By default, when indexing it adds translated fields
 * in the {@link #getTargetLocale() target language} for {@code name}, {@code def} and {@code synonym}, and when
 * querying it includes these translated fields in the query, if {@link #isCurrentLocaleTargeted() the current language
 * is supported}.
 *
 * @version $Id$
 * @since 1.3
 */
public abstract class AbstractXliffTranslatedVocabularyExtension implements VocabularyExtension
{
    /** The format of the translated property names. */
    private static final String TRANSLATED_FIELD_FORMAT = "%s_%s";

    /** The name of the property used for storing the name of a term. */
    private static final String NAME_KEY = "name";

    /** The name of the property used for storing the description of a term. */
    private static final String DESCRIPTION_KEY = "def";

    /** The name of the property used for storing the synonyms of a term. */
    private static final String SYNONYM_KEY = "synonym";

    /** A map going from the names of term properties to the names of keys in the xliff file. */
    private static final Map<String, String> KEY_MAP;

    static {
        KEY_MAP = new HashMap<>(3, 1.0f);
        KEY_MAP.put(NAME_KEY, "_label");
        KEY_MAP.put(DESCRIPTION_KEY, "_definition");
        KEY_MAP.put(SYNONYM_KEY, "_synonyms");
    }

    /** The localization context. */
    @Inject
    private LocalizationContext localizationContext;

    /** Configuration where the enabled languages are defined. */
    @Inject
    @Named("wiki")
    private ConfigurationSource config;

    /** Nicer translation API. */
    private LenientResourceBundleWrapper translator = new LenientResourceBundleWrapper();

    @Inject
    protected Logger logger;

    @Override
    public boolean isVocabularySupported(Vocabulary vocabulary)
    {
        return getTargetVocabularyId().equals(vocabulary.getIdentifier());
    }

    @Override
    public void indexingStarted(Vocabulary vocabulary)
    {
        // Nothing to do
    }

    @Override
    public void extendTerm(VocabularyInputTerm term, Vocabulary vocabulary)
    {
        if (!isTargetLocaleEnabled()) {
            return;
        }
        Locale targetLocale = getTargetLocale();
        String label = this.translator.getTranslation(vocabulary.getIdentifier(),
            term.getId().replace(':', '_') + KEY_MAP.get(NAME_KEY), targetLocale);
        String definition = this.translator.getTranslation(vocabulary.getIdentifier(),
            term.getId().replace(':', '_') + KEY_MAP.get(DESCRIPTION_KEY), targetLocale);
        String synonyms = this.translator.getTranslation(vocabulary.getIdentifier(),
            term.getId().replace(':', '_') + KEY_MAP.get(SYNONYM_KEY), targetLocale);
        if (StringUtils.isNotBlank(label)) {
            term.set(String.format(TRANSLATED_FIELD_FORMAT, NAME_KEY, targetLocale), label);
        }
        if (StringUtils.isNotBlank(definition)) {
            term.set(String.format(TRANSLATED_FIELD_FORMAT, DESCRIPTION_KEY, targetLocale), definition);
        }
        if (StringUtils.isNotBlank(synonyms)) {
            term.set(String.format(TRANSLATED_FIELD_FORMAT, SYNONYM_KEY, targetLocale), splitMultiValuedText(synonyms));
        }
    }

    @Override
    public void indexingEnded(Vocabulary vocabulary)
    {
        // Nothing to do
    }

    @Override
    public void extendQuery(SolrQuery query, Vocabulary vocabulary)
    {
        if (!isCurrentLocaleTargeted()) {
            return;
        }
        Locale currentLocale = this.localizationContext.getCurrentLocale();
        if (currentLocale != null && isLocaleSupported(currentLocale)) {
            Locale targetLocale = getTargetLocale();
            if (StringUtils.isNotBlank(query.get(DisMaxParams.PF))) {
                try (Formatter f = new Formatter()) {
                    f.out().append(query.get(DisMaxParams.PF));
                    f.format(" name_%1$s^60 synonym_%1$s^45 def_%1$s^12 ", targetLocale);
                    query.set(DisMaxParams.PF, f.toString());
                } catch (IOException ex) {
                    // Shouldn't happen
                    this.logger.warn(
                        "Unexpected exception while formatting SolrQuery PF for vocabulary [{}] and locale [{}]: {}",
                        vocabulary.getIdentifier(), targetLocale, ex.getMessage());
                }
            }
            if (StringUtils.isNotBlank(query.get(DisMaxParams.QF))) {
                try (Formatter f = new Formatter()) {
                    f.out().append(query.get(DisMaxParams.QF));
                    f.format(" name_%1$s^30 synonym_%1$s^21 def_%1$s^6 ", targetLocale);
                    query.set(DisMaxParams.QF, f.toString());
                } catch (IOException ex) {
                    // Shouldn't happen
                    this.logger.warn(
                        "Unexpected exception while formatting SolrQuery QF for vocabulary [{}] and locale [{}]: {}",
                        vocabulary.getIdentifier(), targetLocale, ex.getMessage());
                }
            }
        }
    }

    /**
     * Check if the {@link #getTargetLocale() locale targeted by this translation} is in the list of supported locales
     * by this instance.
     *
     * @return {@code true} if the current language is targeted, or if the instance is set as multilingual and the
     *         targeted language is included in the list of supported locales, {@code false} otherwise
     */
    protected boolean isTargetLocaleEnabled()
    {
        Locale currentLocale = this.localizationContext.getCurrentLocale();
        if (isLocaleSupported(currentLocale)) {
            return true;
        }
        if (!this.config.getProperty("multilingual", Boolean.FALSE)) {
            return false;
        }
        Set<String> defaultValue = new HashSet<>();
        defaultValue.add(currentLocale.getLanguage());
        Set<String> enabledLanguages = this.config.getProperty("languages", defaultValue);
        for (String lang : enabledLanguages) {
            if (StringUtils.isBlank(lang)) {
                continue;
            }
            String trimmed = lang.trim();
            if (isLocaleSupported(Locale.forLanguageTag(trimmed))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the current context is set to a locale {@link #isLocaleSupported(Locale) supported} by this translation.
     *
     * @return {@code true} if the current locale is supported
     */
    protected boolean isCurrentLocaleTargeted()
    {
        return isLocaleSupported(this.localizationContext.getCurrentLocale());
    }

    /**
     * Check if the specified locale is supported by this translation, specifically if the {@link #getTargetLocale()
     * targeted locale} is a superset of it.
     *
     * @param locale the locale to check
     * @return {@code true} if the specified locale is supported
     */
    protected boolean isLocaleSupported(Locale locale)
    {
        return locale.toLanguageTag().startsWith(getTargetLocale().toLanguageTag());
    }

    /**
     * The HPO translations store multi-valued fields, such as synonyms, as a single-line text with {@code #} separating
     * each value. The methods splits such fields into individual values.
     *
     * @param text the joined text to split
     * @return the split text as a list of values
     */
    protected List<String> splitMultiValuedText(String text)
    {
        return Arrays.asList(text.split("\\s*+#\\s*+"));
    }

    /**
     * Specifies the vocabulary targeted by this translation.
     *
     * @return a valid {@link Vocabulary#getIdentifier() vocabulary identifier}
     */
    protected abstract String getTargetVocabularyId();

    /**
     * Specifies the locale targeted by this translation.
     *
     * @return a valid locale
     */
    protected abstract Locale getTargetLocale();
}
