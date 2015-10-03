package org.phenotips.oo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tab
 *
 * @author marta
 */
public class TSVParser
{
    private static final int IDX__MIM_NUMBER = 0;

    private static final int IDX__TYPE = 1;

    private static final int IDX__GENE_ID = 2;

    private static final int IDX__GENE_SYMBOL = 3;

    private static final int IDX__COUNT = 4;

    Map<String, String> data;

    String tmpKey;

    String tmpValue;

    String tmpText;

    private String sourceFileName;

    public TSVParser(String sourceFileName)
    {
        this.sourceFileName = sourceFileName;
        this.data = new HashMap<String, String>();
        parseDocument();
    }

    private void parseDocument()
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader(this.sourceFileName));
            String line;
            while ((line = in.readLine()) != null) {
                String pieces[] = line.split("\t");
                if (pieces.length < IDX__COUNT) {
                    continue;
                }
                if (pieces[IDX__TYPE].indexOf("gene") < 0) {
                    continue;
                }
                this.data.put(pieces[IDX__MIM_NUMBER],
                    pieces[IDX__GENE_ID].trim() + " " + pieces[IDX__GENE_SYMBOL].trim());
            }
            in.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String get(String key)
    {
        return this.data.get(key);
    }

    public Set<String> keySet()
    {
        return this.data.keySet();
    }

    public Set<Entry<String, String>> entrySet()
    {
        return this.data.entrySet();
    }

    public Map<String, String> getData()
    {
        return this.data;
    }

    public void printData()
    {
        // System.out.println(bookL.size());
        for (String k : this.data.keySet()) {
            System.out.println(k + "\t" + this.data.get(k));
        }
    }
}
