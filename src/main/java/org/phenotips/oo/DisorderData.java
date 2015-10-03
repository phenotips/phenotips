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

import java.util.HashMap;
import java.util.Map;

public class DisorderData
{
    private String id;

    private String name;

    private Map<String, Double> symptoms = new HashMap<String, Double>();

    private Map<String, Double> negativePhenotypes = new HashMap<String, Double>();

    private RecordData meta;

    private double prelevance = 1;

    protected DisorderData(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    protected Double addSymptom(String id, String prelevance)
    {
        return this.symptoms.put(id, interpretPrelevance(prelevance));
    }

    protected Double addNegativePhenotype(String id, String prelevance)
    {
        return this.negativePhenotypes.put(id, interpretPrelevance(prelevance));
    }

    public double getPrelevance()
    {
        return this.prelevance;
    }

    protected void setPrelevance(double prelevance)
    {
        this.prelevance = prelevance;
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    protected void setName(String name)
    {
        this.name = name;
    }

    public Map<String, Double> getSymptoms()
    {
        return this.symptoms;
    }

    public Map<String, Double> getNegativePhenotypes()
    {
        return this.negativePhenotypes;
    }

    private double interpretPrelevance(String text)
    {
        double result = 0.5; // just average
        String t = text.trim();
        // as percentage, e.g. 95.00 %
        if (t.endsWith("%")) {
            t = t.replaceAll("\\s*%", "");
            // as percentage interval, e.g. 1-2 %; take the mean
            if (t.indexOf('-') > 0) {
                String pieces[] = t.split("\\s*-\\s*");
                try {
                    result = (Double.parseDouble(pieces[0]) + Double.parseDouble(pieces[1])) / 2.0 * 0.01;
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            } else {
                // as simple percentage
                try {
                    result = Double.parseDouble(t);
                    result *= 0.01;
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        }
        // as ratio, e.g. "24/25" or "11 of 29"
        t = t.replaceAll("\\s*of\\s*", "/");
        if (t.indexOf('/') > 0) {
            String pieces[] = t.split("\\s*/\\s*");
            try {
                result = (Double.parseDouble(pieces[0]) / Double.parseDouble(pieces[1]));
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        // as fuzzy values (with typos, unfortunately)
        // known fuzzy values:
        /*
         * 155 common 26 Common 1 Freqeunt 144 frequent 13 Frequent 12984 hallmark 6 Hallmark 9 obligate 1 occaisonal
         * 9404 occasional 99 Occasional 1221 rare 73 Rare 9907 typical 7 Typical 1 UNCOMMON 26 variable 9 very rare 2
         * Very rare
         */

        if (t.equalsIgnoreCase("occasional") || t.equalsIgnoreCase("occaisonal")) {
            result = 0.25;
        } else if (t.equalsIgnoreCase("frequent") || t.equalsIgnoreCase("freqeunt")) {
            result = 0.75;
        } else if (t.equalsIgnoreCase("common") || t.equalsIgnoreCase("variable") || t.equals("")) {
            result = 0.5;
        } else if (t.equalsIgnoreCase("hallmark") || t.equalsIgnoreCase("typical")) {
            result = .90;
        } else if (t.equalsIgnoreCase("obligate")) {
            result = 1.0;
        } else if (t.equalsIgnoreCase("rare") || t.equalsIgnoreCase("uncommon") || t.equalsIgnoreCase("very rare")) {
            result = 0.1;
        }

        // "round" the result
        result = Math.round(result * 20);
        return result;
    }

    public RecordData getMeta()
    {
        return this.meta;
    }

    public void setMeta(RecordData meta)
    {
        this.meta = meta;
    }
}
