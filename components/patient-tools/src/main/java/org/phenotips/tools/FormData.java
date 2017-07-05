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
package org.phenotips.tools;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 * @since 1.0M2
 */
public class FormData
{
    private DisplayMode mode;

    private Document doc;

    private String positiveFieldName;

    private String negativeFieldName;

    private String positivePropertyName;

    private String negativePropertyName;

    private Collection<String> selectedValues;

    private Collection<String> selectedNegativeValues;

    private Map<String, List<String>> customCategories;

    private Map<String, List<String>> customNegativeCategories;

    /**
     * @return the mode
     */
    public DisplayMode getMode()
    {
        return this.mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(DisplayMode mode)
    {
        this.mode = mode;
    }

    /**
     * @return the doc
     */
    public Document getDocument()
    {
        return this.doc;
    }

    /**
     * @param doc the doc to set
     */
    public void setDocument(Document doc)
    {
        this.doc = doc;
    }

    /**
     * @return the positiveFieldName
     */
    public String getPositiveFieldName()
    {
        return this.positiveFieldName;
    }

    /**
     * @param positiveFieldName the positiveFieldName to set
     */
    public void setPositiveFieldName(String positiveFieldName)
    {
        this.positiveFieldName = positiveFieldName;
    }

    /**
     * @return the negativeFieldName
     */
    public String getNegativeFieldName()
    {
        return this.negativeFieldName;
    }

    /**
     * @param negativeFieldName the negativeFieldName to set
     */
    public void setNegativeFieldName(String negativeFieldName)
    {
        this.negativeFieldName = negativeFieldName;
    }

    /**
     * @return the positivePropertyName
     */
    public String getPositivePropertyName()
    {
        return this.positivePropertyName;
    }

    /**
     * @param positivePropertyName the positivePropertyName to set
     */
    public void setPositivePropertyName(String positivePropertyName)
    {
        this.positivePropertyName = positivePropertyName;
    }

    /**
     * @return the negativePropertyName
     */
    public String getNegativePropertyName()
    {
        return this.negativePropertyName;
    }

    /**
     * @param negativePropertyName the negativePropertyName to set
     */
    public void setNegativePropertyName(String negativePropertyName)
    {
        this.negativePropertyName = negativePropertyName;
    }

    /**
     * @return the selectedValues
     */
    public Collection<String> getSelectedValues()
    {
        return this.selectedValues;
    }

    /**
     * @param selectedValues the selectedValues to set
     */
    public void setSelectedValues(Collection<String> selectedValues)
    {
        this.selectedValues = selectedValues;
    }

    /**
     * @return the selectedNegativeValues
     */
    public Collection<String> getSelectedNegativeValues()
    {
        return this.selectedNegativeValues;
    }

    /**
     * @param selectedNegativeValues the selectedNegativeValues to set
     */
    public void setSelectedNegativeValues(Collection<String> selectedNegativeValues)
    {
        this.selectedNegativeValues = selectedNegativeValues;
    }

    /**
     * @return the customCategories
     */
    public Map<String, List<String>> getCustomCategories()
    {
        return this.customCategories;
    }

    /**
     * Set the categories in which non-standard positive features have been recorded in.
     *
     * @param customCategories a map between custom terms and their categories, may be empty but not {@code null}
     */
    public void setCustomCategories(Map<String, List<String>> customCategories)
    {
        this.customCategories = customCategories;
    }

    /**
     * @return the customNegativeCategories
     */
    public Map<String, List<String>> getCustomNegativeCategories()
    {
        return this.customNegativeCategories;
    }

    /**
     * Set the categories in which non-standard negative features have been recorded in.
     *
     * @param customNegativeCategories a map between custom terms and their categories, may be empty but not
     *            {@code null}
     */
    public void setCustomNegativeCategories(Map<String, List<String>> customNegativeCategories)
    {
        this.customNegativeCategories = customNegativeCategories;
    }
}
