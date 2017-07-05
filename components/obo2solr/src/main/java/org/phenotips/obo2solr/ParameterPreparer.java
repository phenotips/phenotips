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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterPreparer
{
    public static final String TMP_INPUT_LOCATION = getResourceFilePath("tmp.obo");

    public static final String FIELD_SEP = "\\s*,\\s*";

    public static final String FIELD_BOOST_SEP = ":";

    public static final Double DEFAULT_BOOST = 1.0;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public File getInputFileHandler(String inputLocation)
    {
        try {
            File result = new File(inputLocation);
            if (!result.exists()) {
                // File does not exist locally, assume it's an URL and fetch it
                result = new File(TMP_INPUT_LOCATION);
                result.createNewFile();
                result.deleteOnExit();

                BufferedInputStream in = new BufferedInputStream((new URL(inputLocation)).openStream());
                OutputStream out = new FileOutputStream(result);

                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
                out.close();
            }
            return result;
        } catch (IOException ex) {
            this.logger.warn("Failed to save OBO file from [{}] to [{}]: {}", inputLocation, TMP_INPUT_LOCATION,
                ex.getMessage());
            return null;
        }
    }

    public File getOutputFileHandler(String outputLocation)
    {
        return new File(outputLocation);
    }

    public Map<String, Double> getFieldSelection(String fieldSelection)
    {
        Map<String, Double> result = new HashMap<>();
        String[] fields = fieldSelection.split(FIELD_SEP);
        for (String field : fields) {
            if (!"".equals(field)) {
                int boostPosition = field.indexOf(FIELD_BOOST_SEP);
                if (boostPosition >= 0) {
                    String[] parts = field.split(FIELD_BOOST_SEP, 2);
                    String fieldName = parts[0];
                    double boost = DEFAULT_BOOST;
                    try {
                        boost = Double.parseDouble(parts[1]);
                    } catch (NumberFormatException ex) {
                        // Shouldn't happen, just default to the default boost
                    }
                    result.put(fieldName, boost);
                } else {
                    result.put(field.trim(), DEFAULT_BOOST);
                }
            }
        }
        return result;
    }

    private static String getResourceFilePath(String name)
    {
        return (new File("")).getAbsolutePath() + File.separator + "res" + File.separator + name;
    }
}
