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
package org.phenotips.xliff12;

import org.phenotips.xliff12.model.Bpt;
import org.phenotips.xliff12.model.Ept;
import org.phenotips.xliff12.model.File;
import org.phenotips.xliff12.model.G;
import org.phenotips.xliff12.model.It;
import org.phenotips.xliff12.model.Mrk;
import org.phenotips.xliff12.model.Ph;
import org.phenotips.xliff12.model.Sub;
import org.phenotips.xliff12.model.TransUnit;
import org.phenotips.xliff12.model.Xliff;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around the auto-generated {@link Xliff} class, offering a nicer API for accessing translations.
 *
 * @version $Id$
 * @since 1.3
 */
public final class XLIFFResourceBundleControl extends ResourceBundle.Control
{
    /** The singleton instance for this resource bundle loading controller. */
    public static final XLIFFResourceBundleControl INSTANCE = new XLIFFResourceBundleControl();

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XLIFFResourceBundleControl.class);

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("xlf", "xliff");

    private JAXBContext jaxbContext;

    /**
     * Hidden default constructor, to prevent initialization. Use {@link #INSTANCE} to access an instance of this
     * resouce bundle loading controller.
     */
    private XLIFFResourceBundleControl()
    {
        try {
            this.jaxbContext = JAXBContext.newInstance(Xliff.class);
        } catch (JAXBException ex) {
            LOGGER.error("Failed to initialize JAXB: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public List<String> getFormats(String baseName)
    {
        if (baseName == null) {
            throw new NullPointerException();
        }
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public Locale getFallbackLocale(String baseName, Locale locale)
    {
        return null;
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
        throws IllegalAccessException, InstantiationException, IOException
    {
        if (baseName == null || locale == null
            || format == null || loader == null) {
            throw new NullPointerException();
        }
        ResourceBundle bundle = null;
        if (SUPPORTED_EXTENSIONS.contains(format.toLowerCase(Locale.ROOT))) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, format);
            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream != null) {
                    try {
                        Unmarshaller jaxbUnmarshaller = this.jaxbContext.createUnmarshaller();
                        Xliff xliff = (Xliff) jaxbUnmarshaller.unmarshal(stream);
                        bundle = new XLIFFResourceBundle(xliff);
                    } catch (JAXBException e) {
                        LOGGER.error("Invalid XLIFF file: {}", resourceName);
                    }
                }
            }
        }
        return bundle;
    }

    private static class XLIFFResourceBundle extends ListResourceBundle
    {
        /**
         * The file node.
         */
        private final Xliff file;

        /**
         * Simple constructor for wrapping an {@link Xliff} object.
         *
         * @param file the Xliff object to wrap
         */
        XLIFFResourceBundle(Xliff file)
        {
            this.file = file;
        }

        @Override
        protected Object[][] getContents()
        {
            Map<String, String> data = new HashMap<>();

            for (File f : this.file.getFiles()) {
                for (Object possibleTransUnit : f.getBody().getGroupsAndTransUnitsAndBinUnits()) {
                    if (possibleTransUnit instanceof TransUnit) {
                        TransUnit transUnit = (TransUnit) possibleTransUnit;
                        if (transUnit.getTarget() == null) {
                            continue;
                        }
                        String key = StringUtils.defaultString(transUnit.getResname(), transUnit.getId());
                        StringBuilder result = new StringBuilder();
                        getContent(transUnit.getTarget().getContent(), result);
                        if (result.length() > 0) {
                            data.put(key, result.toString());
                        }
                    }
                }
            }
            Object[][] result = new Object[data.size()][];
            int i = 0;
            for (Map.Entry<String, String> item : data.entrySet()) {
                result[i] = new Object[2];
                result[i][0] = item.getKey();
                result[i][1] = item.getValue();
                ++i;
            }
            return result;
        }
    }

    private static void getContent(List<Object> objects, StringBuilder result)
    {
        for (Object element : objects) {
            if (element instanceof String) {
                result.append(String.valueOf(element));
            } else if (element instanceof Mrk) {
                getContent(((Mrk) element).getContent(), result);
            } else if (element instanceof G) {
                getContent(((G) element).getContent(), result);
            } else if (element instanceof Ph) {
                getContent(((Ph) element).getContent(), result);
            } else if (element instanceof Bpt) {
                getContent(((Bpt) element).getContent(), result);
            } else if (element instanceof Ept) {
                getContent(((Ept) element).getContent(), result);
            } else if (element instanceof It) {
                getContent(((It) element).getContent(), result);
            } else if (element instanceof Sub) {
                getContent(((Sub) element).getContent(), result);
            }
        }
    }
}
