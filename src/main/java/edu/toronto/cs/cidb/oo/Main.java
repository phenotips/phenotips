package edu.toronto.cs.cidb.oo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        OOSaxParser omimMappingDataSource = new OOSaxParser(args[0]);
        OOSaxParser prelevanceDataSource = new OOSaxParser(args[1]);

        Map<String, Double> omimPrelevanceData = new HashMap<String, Double>();

        for (String k : omimMappingDataSource.keySet()) {
            String omimID = omimMappingDataSource.get(k);
            String prelevance = prelevanceDataSource.get(k);
            if (omimID != null && prelevance != null) {
                omimPrelevanceData.put(omimID, interpretPrelevance(prelevance));
            }
        }
        new DisorderDataBuilder(args[2], args[3], omimPrelevanceData).generate(new File(args[4]));
    }

    private static double interpretPrelevance(String text)
    {
        double result = 0.00005; // just average
        String t = text.trim();
        // as ratio, e.g. "1/1000"
        t = t.replaceAll("\\s*+of\\s*+", "/");
        t = t.replaceAll("\\s++", "");
        if (t.indexOf('/') > 0) {
            String pieces[] = t.split("/");
            if (pieces[0].indexOf('-') > 0) {
                String iPieces[] = pieces[0].split("\\s*-\\s*");
                try {
                    result = (Double.parseDouble(iPieces[0]) + Double.parseDouble(iPieces[1])) / 2.0;
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            } else {
                try {
                    result = Double.parseDouble(pieces[0]);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
            try {
                result = (result / Double.parseDouble(pieces[1]));
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        result = 6.0 + (Math.round(Math.log10(result) * 5.0 / 3.0) / 2.0);
        return result;
    }
}
