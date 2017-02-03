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

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.translation.MachineTranslator;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.LocalizationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

/**
 * Implements {@link VocabularyExtension} to provide translation services. Works with the xml files up at
 * https://github.com/Human-Phenotype-Ontology/HPO-translations
 *
 * @version $Id$
 */
@Component
@Singleton
public class MachineTranslatedVocabularyExtension implements VocabularyExtension
{
    /** The format to add a language to a solr field. */
    private static final String TRANSLATED_FIELD_FORMAT = "%s_%s";

    /** The name of the Solr field used for storing the name of a term. */
    private static final String NAME_KEY = "name";

    /** The name of the Solr field used for storing the description of a term. */
    private static final String DESCRIPTION_KEY = "def";

    /** The name of the Solr field used for storing the synonyms of a term. */
    private static final String SYNONYM_KEY = "synonym";

    /** A map going from the names of properties in the solr index to the names of properties in the xliff file. */
    private static final Map<String, String> KEY_MAP;

    static {
        KEY_MAP = new HashMap<>(3, 1.0f);
        KEY_MAP.put(NAME_KEY, "_label");
        KEY_MAP.put(DESCRIPTION_KEY, "_definition");
        KEY_MAP.put(SYNONYM_KEY, "_synonyms");
    }

    /**
     * The supported languages. Will be set when we start indexing so the component supports dynamic switching.
     */
    private Set<Locale> locales;

    @Inject
    @Named("wiki")
    private ConfigurationSource config;

    /** The machine translator. */
    @Inject
    private MachineTranslator translator;

    /**
     * The localization context.
     */
    @Inject
    private LocalizationContext localizationContext;

    @Override
    public boolean isVocabularySupported(Vocabulary vocabulary)
    {
        return "hpo".equals(vocabulary.getIdentifier());
    }

    @Override
    public void indexingStarted(Vocabulary vocabulary)
    {
        this.locales = getLocales();
    }

    @Override
    public void indexingEnded(Vocabulary vocabulary)
    {
        // Nothing to do
    }

    @Override
    public void extendTerm(VocabularyInputTerm term, Vocabulary vocabulary)
    {
        String originalName = term.getName();
        String originalDescription = term.getDescription();
        @SuppressWarnings("unchecked")
        List<String> synonyms = (List<String>) term.get("synonym");
        for (Locale locale : this.locales) {
            String translatedName = this.translator.translate(originalName, locale);
            if (StringUtils.isNotBlank(translatedName)) {
                term.set(String.format(TRANSLATED_FIELD_FORMAT, NAME_KEY, locale), translatedName);
            }
            String translatedDescription = this.translator.translate(originalDescription, locale);
            if (StringUtils.isNotBlank(translatedDescription)) {
                term.set(String.format(TRANSLATED_FIELD_FORMAT, DESCRIPTION_KEY, locale), translatedDescription);
            }
            if (synonyms != null) {
                List<String> translatedSynonyms = new ArrayList<>(synonyms.size());
                for (String synonym : synonyms) {
                    translatedSynonyms.add(this.translator.translate(synonym, locale));
                }
                if (!translatedSynonyms.isEmpty()) {
                    term.set(String.format(TRANSLATED_FIELD_FORMAT, SYNONYM_KEY, locale), translatedSynonyms);
                }
            }
        }
    }

    /**
     * Get the supported languages as a set.
     *
     * @return the languages
     */
    private Set<Locale> getLocales()
    {
        Set<String> defaultValue = new HashSet<>();
        defaultValue.add(this.localizationContext.getCurrentLocale().getLanguage());
        Set<String> enabledLanguages = this.config.getProperty("languages", defaultValue);
        Set<Locale> retval = new HashSet<>(enabledLanguages.size());
        for (String lang : enabledLanguages) {
            if (StringUtils.isBlank(lang)) {
                continue;
            }
            String trimmed = lang.trim();
            if (!"en".equals(trimmed)) {
                Locale locale = Locale.forLanguageTag(trimmed);
                if (this.translator.isLocaleSupported(locale)) {
                    retval.add(locale);
                }
            }
        }
        return retval;
    }

    @Override
    public void extendQuery(SolrQuery query, Vocabulary vocabulary)
    {
        // TODO Auto-generated method stub
    }
}
