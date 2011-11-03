package ut.cb.sv.db.load.xml;

import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.db.load.AbstractFeatureMap;

public class XMLFeatureMap extends AbstractFeatureMap
{

    protected final static String ENTRY_END_XPATH_MARKER = "entryEndXPath:";

    protected final static String DATA_PROPAGATION_XPATH_MARKER = "dataPropagationEndXPath:";

    private String entryEndXPath;

    private String dataPropagationEndXPath;

    public XMLFeatureMap(String mappingFileName)
    {
        super(mappingFileName);
    }

    @Override
    public boolean onProcessLineStart(String line)
    {
        if (line.startsWith(ENTRY_END_XPATH_MARKER)) {
            this.entryEndXPath = line.substring(ENTRY_END_XPATH_MARKER.length()).trim();
            return false;
        }
        if (line.startsWith(DATA_PROPAGATION_XPATH_MARKER)) {
            this.dataPropagationEndXPath = line.substring(DATA_PROPAGATION_XPATH_MARKER.length()).trim();
            return false;
        }
        return true;
    }

    public String getEntryEndXPath()
    {
        return this.entryEndXPath;
    }

    public String getDataPropagationEndXPath()
    {
        return this.dataPropagationEndXPath;
    }

    public String getFeatureNameForXPath(String xPath)
    {
        return super.getFeatureNameForOriginalName(xPath);
    }

    public Feature getFeatureForXPath(String xPath)
    {
        return super.getFeatureForOriginalName(xPath);
    }
}
