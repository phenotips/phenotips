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
package org.phenotips.obo2solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrUpdateGenerator
{
    private static final String TERM_MARKER = "[Term]";

    /** Not all entities are terms prompted by the presence of a {@link #TERM_MARKER} */
    private static final String ENTITY_SEPARATION_REGEX = "^\\[[a-zA-Z]+\\]$";

    private static final String FIELD_NAME_VALUE_SEPARATOR = "\\s*:\\s+";

    private int counter;

    private TermData crtTerm = new TermData();

    private Map<String, TermData> data = new LinkedHashMap<>();

    private Map<String, Double> fieldSelection;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public Map<String, TermData> transform(String ontologyUrl, Map<String, Double> fieldSelection)
    {
        URL url;
        try {
            url = new URL(ontologyUrl);
        } catch (MalformedURLException ex) {
            return null;
        }
        return transform(url, fieldSelection);
    }

    public Map<String, TermData> transform(URL input, Map<String, Double> fieldSelection)
    {
        this.fieldSelection = fieldSelection;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(input.openConnection().getInputStream()));
            String line;
            this.counter = 0;

            /*
             * When encountering a separator that is not a term separator, all data should be skipped until a term
             * separator is encountered again
             */
            boolean skip = false;
            while ((line = in.readLine()) != null) {
                if (line.trim().matches(ENTITY_SEPARATION_REGEX)) {
                    if (this.counter > 0) {
                        storeCrtTerm();
                    }
                    // Overridden below
                    skip = true;
                }
                if (line.trim().equalsIgnoreCase(TERM_MARKER)) {
                    ++this.counter;
                    skip = false;
                    continue;
                }
                if (!skip) {
                    String[] pieces = line.split(FIELD_NAME_VALUE_SEPARATOR, 2);
                    if (pieces.length != 2) {
                        continue;
                    }
                    if (pieces[0].trim().equals("data-version")) {
                        this.crtTerm.addTo("version", pieces[1]);
                        this.crtTerm.addTo(TermData.ID_FIELD_NAME, "HEADER_INFO");
                        this.counter++;
                    }
                    loadField(pieces[0], pieces[1]);
                }
            }
            if (this.counter > 0) {
                storeCrtTerm();
            }
            if (isFieldSelected(TermData.TERM_CATEGORY_FIELD_NAME)) {
                propagateAncestors();
            }
        } catch (NullPointerException ex) {
            this.logger.error("NullPointer: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException: {}", ex.getMessage());
        } finally {
            this.fieldSelection = null;
        }
        return this.data;
    }

    private void storeCrtTerm()
    {
        if (this.crtTerm.getId() != null) {
            this.data.put(this.crtTerm.getId(), this.crtTerm);
        }
        this.crtTerm = new TermData();
    }

    private boolean isFieldSelected(String name)
    {
        return this.fieldSelection.isEmpty() || this.fieldSelection.containsKey(name);
    }

    private void loadField(String name, String value)
    {
        if (!(isFieldSelected(name))) {
            return;
        }
        this.crtTerm.addTo(name, value.replaceFirst("^\"(.+)\"\\s*?(?:[A-Z]+|\\[).*", "$1").replace("\\\"", "\""));
    }

    private void propagateAncestors()
    {
        for (String id : this.data.keySet()) {
            TermData term = this.data.get(id);
            term.expandTermCategories(this.data);
        }
    }
}
