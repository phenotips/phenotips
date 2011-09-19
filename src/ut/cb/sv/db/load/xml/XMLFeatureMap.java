package ut.cb.sv.db.load.xml;

import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.db.load.AbstractFeatureMap;

public class XMLFeatureMap extends AbstractFeatureMap
{

    protected final static String ENTRY_END_XPATH_MARKER = "entryEndXPath:";

    private String entryEndXPath;

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
        return true;
    }

    public String getEntryEndXPath()
    {
        return this.entryEndXPath;
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
