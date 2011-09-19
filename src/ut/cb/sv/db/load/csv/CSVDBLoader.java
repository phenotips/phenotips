package ut.cb.sv.db.load.csv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ut.cb.sv.db.CSVDatabaseFormatter;
import ut.cb.sv.db.load.AbstractDBLoader;
import ut.cb.sv.db.load.FeatureMap;

public class CSVDBLoader extends AbstractDBLoader
{
    public CSVDBLoader(FeatureMap featureMap)
    {
        super(featureMap);
    }

    public CSVDBLoader(String mappingFileName)
    {
        super(mappingFileName);
    }

    @Override
    public CSVFeatureMap getFeatureMap()
    {
        return (CSVFeatureMap) super.getFeatureMap();
    }

    @Override
    public FeatureMap getFeatureMapInstance(String mappingFileName)
    {
        return new CSVFeatureMap(mappingFileName);
    }

    @Override
    public void parseDocument(String filename)
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));

            String line;
            String separator = CSVDatabaseFormatter.DEFAULT_SEPARATOR;
            boolean isLineValid = true;
            line = in.readLine();
            String originalFeatureNames[] = line.split(separator);
            List<String> featureNames = new ArrayList<String>();
            for (String name : originalFeatureNames) {
                featureNames.add(this.getFeatureMap().getFeatureNameForOriginalName(name));
            }
            String featureValues[];
            while ((line = in.readLine()) != null) {
                isLineValid = true;
                featureValues = line.split(separator);
                if (featureValues.length != originalFeatureNames.length) {
                    // invalid line
                    continue;
                }
                for (int i = 0; i < featureValues.length; ++i) {
                    if (!this.getFeatureMap().accepts(originalFeatureNames[i], featureValues[i])) {
                        isLineValid = false;
                        break;
                    }
                    if (featureNames.get(i) != null) {
                        this.loadFeatureValueToCrtDBEntry(featureNames.get(i), featureValues[i]);
                    }
                }
                if (isLineValid) {
                    this.saveCrtDBEntry();
                }
            }
            in.close();
        } catch (FileNotFoundException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }
}
