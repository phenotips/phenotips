package ut.cb.sv.db.load.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ut.cb.sv.db.load.AbstractDBLoader;
import ut.cb.sv.db.load.FeatureMap;

public class XMLDBLoader extends AbstractDBLoader
{
    private class XMLDBSAXParser extends DefaultHandler
    {
        private static final String XPATH_ELEMENT_PREFIX = "/";

        private static final String XPATH_ELEMENT_SUFFIX = "";

        private static final String XPATH_ATTRIBUTE_PREFIX = "[@";

        private static final String XPATH_ATTRIBUTE_SUFFIX = "]";

        private String crtElementFeatureName = null;

        private String crtIgnoredElement = null;

        private StringBuffer crtXPath = new StringBuffer();

        private StringBuffer crtCharData = new StringBuffer();

        private String prevChrData = "";

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            this.addElementNameToXPath(qName);
            if (this.crtIgnoredElement != null) {
                return;
            }
            if (hasReachedMaxNumberOfEntries()) {
                return;
            }
            this.crtElementFeatureName = getFeatureMap().getFeatureNameForXPath(this.getCrtXPath());
            for (int i = 0; i < attributes.getLength(); ++i) {
                String attrName = attributes.getQName(i);
                this.addAttrNameToXPath(attrName.toString());
                if (getFeatureMap().accepts(this.getCrtXPath(), attributes.getValue(attrName))) {
                    String crtAttrFeatureName = getFeatureMap().getFeatureNameForXPath(this.getCrtXPath());
                    if (crtAttrFeatureName != null) {
                        loadFeatureValueToCrtDBEntry(crtAttrFeatureName, attributes.getValue(attrName));
                    }
                    this.removeLastAttrNameFromXPath();
                } else {
                    removeLastAttrNameFromXPath();
                    this.crtIgnoredElement = getCrtXPath();
                    this.cleanupTmpData();
                    break;
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            if (hasReachedMaxNumberOfEntries()) {
                return;
            }
            if (this.crtElementFeatureName != null) {
                this.crtCharData.append((new String(ch).substring(start, start + length)).trim());
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if (hasReachedMaxNumberOfEntries()) {
                return;
            }
            if (this.crtElementFeatureName != null) {
                loadFeatureValueToCrtDBEntry(this.crtElementFeatureName, this.crtCharData.toString());
                this.cleanupTmpData();
            }
            if (this.getCrtXPath().equals(getFeatureMap().getEntryEndXPath()) && this.crtIgnoredElement == null) {
                saveCrtDBEntry();
            }
            if (this.getCrtXPath().equals(getFeatureMap().getDataPropagationEndXPath())) {
                clearCrtDBEntry();
            }
            if (this.getCrtXPath().equals(this.crtIgnoredElement)) {
                this.crtIgnoredElement = null;
            }
            this.removeLastElementNameFromXPath();
            this.cleanupTmpData();
        }

        protected void addItemNameToXPath(String name, String prefix, String suffix)
        {
            this.crtXPath.append(prefix).append(name).append(suffix);
        }

        protected void addElementNameToXPath(String elementName)
        {
            this.addItemNameToXPath(elementName, XPATH_ELEMENT_PREFIX, XPATH_ELEMENT_SUFFIX);
        }

        protected void addAttrNameToXPath(String attrName)
        {
            this.addItemNameToXPath(attrName, XPATH_ATTRIBUTE_PREFIX, XPATH_ATTRIBUTE_SUFFIX);
        }

        protected void removeLastItemFromXPath(String separator)
        {
            int position = this.crtXPath.lastIndexOf(separator);
            if (position < 0) {
                return;
            }
            this.crtXPath.delete(position, this.crtXPath.length());

        }

        protected void removeLastElementNameFromXPath()
        {
            this.removeLastItemFromXPath(XPATH_ELEMENT_PREFIX);
        }

        protected void removeLastAttrNameFromXPath()
        {
            this.removeLastItemFromXPath(XPATH_ATTRIBUTE_PREFIX);
        }

        /**
         * @return the crtXPath
         */
        protected String getCrtXPath()
        {
            return this.crtXPath.toString();
        }

        protected void cleanupTmpData()
        {
            if (!"".equals(this.crtCharData.toString())) {
                this.prevChrData = this.crtElementFeatureName + this.crtCharData.toString();
            }
            this.crtElementFeatureName = null;
            this.crtCharData.delete(0, this.crtCharData.length());
        }
    }

    public XMLDBLoader(FeatureMap featureMap)
    {
        super(featureMap);
    }

    public XMLDBLoader(String mappingFileName)
    {
        super(mappingFileName);
    }

    @Override
    public XMLFeatureMap getFeatureMap()
    {
        return (XMLFeatureMap) super.getFeatureMap();
    }

    @Override
    public FeatureMap getFeatureMapInstance(String mappingFileName)
    {
        return new XMLFeatureMap(mappingFileName);
    }

    @Override
    public void parseDocument(String filename)
    {
        // get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        XMLDBSAXParser parser = new XMLDBSAXParser();
        try {

            // get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            // parse the file and also register this class for call backs
            sp.parse(filename, parser);

        } catch (SAXException se) {
            System.err.println(parser.getCrtXPath());
            System.err.println(parser.prevChrData);
            System.err.println(this.getDatabase().size());
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            System.err.println(parser.getCrtXPath());
            System.err.println(parser.prevChrData);
            System.err.println(this.getDatabase().size());
            pce.printStackTrace();
        } catch (IOException ie) {
            System.err.println(parser.getCrtXPath());
            System.err.println(parser.prevChrData);
            System.err.println(this.getDatabase().size());
            ie.printStackTrace();
        } catch (Exception e) {
            System.err.println(parser.getCrtXPath());
            System.err.println(parser.prevChrData);
            System.err.println(this.getDatabase().size());
            e.printStackTrace();
        }
    }
}
