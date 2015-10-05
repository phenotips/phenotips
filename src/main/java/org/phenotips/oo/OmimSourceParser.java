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
package org.phenotips.oo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;

public class OmimSourceParser
{
    private static final String RECORD_MARKER = "*RECORD*";

    private static final String FIELD_MARKER = "*FIELD* ";

    private static final String FIELD_MIM_NUMBER = "NO";

    private static final String FIELD_TITLE = "TI";

    private static final String FIELD_TEXT = "TX";

    private static final String END_MARKER = "*THEEND*";

    private SolrInputDocument crtTerm;

    private Map<String, SolrInputDocument> data = new HashMap<>();

    public OmimSourceParser(String path)
    {
        try (BufferedReader in =
            new BufferedReader(new InputStreamReader(new CompressorStreamFactory().createCompressorInputStream(
                new URL(path).openConnection().getInputStream()), "UTF-8"))) {
            transform(in);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (CompressorException ex) {
            ex.printStackTrace();
        }
    }

    public Map<String, SolrInputDocument> getData()
    {
        return this.data;
    }

    private Map<String, SolrInputDocument> transform(BufferedReader in) throws IOException
    {
        String line;
        StringBuilder fieldValue = new StringBuilder();
        String fieldName = null;
        while ((line = in.readLine()) != null) {
            if (RECORD_MARKER.equalsIgnoreCase(line) || END_MARKER.equalsIgnoreCase(line)) {
                if (this.crtTerm != null) {
                    loadField(fieldName, fieldValue.toString().trim());
                    storeCrtTerm();
                } else {
                    this.crtTerm = new SolrInputDocument();
                }
            } else if (line.startsWith(FIELD_MARKER)) {
                loadField(fieldName, fieldValue.toString().trim());
                fieldValue.setLength(0);
                fieldName = line.substring(FIELD_MARKER.length());
            } else {
                fieldValue.append(line.trim()).append(" ");
            }
        }

        return this.data;
    }

    private void storeCrtTerm()
    {
        this.data.put(String.valueOf(this.crtTerm.get("id").getFirstValue()), this.crtTerm);
        this.crtTerm = new SolrInputDocument();
    }

    private void loadField(String name, String value)
    {
        if (StringUtils.isAnyBlank(name, value)) {
            return;
        }
        switch (name) {
            case FIELD_MIM_NUMBER:
                this.crtTerm.addField("id", value);
                break;
            case FIELD_TITLE:
                String title = StringUtils.substringBefore(value, ";;").trim();
                String[] synonyms = StringUtils.split(StringUtils.substringAfter(value, ";;"), ";;");
                this.crtTerm.addField("name", title);
                for (String synonym : synonyms) {
                    this.crtTerm.addField("synonym", synonym.trim());
                }
                break;
            case FIELD_TEXT:
                this.crtTerm.addField("def", value);
                break;
            default:
                return;
        }
    }
}
