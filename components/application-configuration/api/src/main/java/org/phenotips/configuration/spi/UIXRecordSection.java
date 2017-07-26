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
package org.phenotips.configuration.spi;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;

import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * An implementation for {@link RecordSection} which reads the configuration from an {@code UIExtension} xobject.
 *
 * @version $Id$
 * @since 1.4
 */
public class UIXRecordSection implements RecordSection
{
    private static final String TITLE_LABEL = "title";

    private static final String ORDER_LABEL = "order";

    private static final String FALSE_LABEL = "false";

    private static final String TRUE_LABEL = "true";

    private static final String ENABLED_LABEL = "enabled";

    private static final String EXPANDED_BY_DEFAULT_LABEL = "expanded_by_default";

    /** @see #getExtension() */
    protected final UIExtension extension;

    /** Lists the contained fields. */
    protected final UIExtensionManager uixManager;

    /** Sorts fields by their declared order. */
    protected final UIExtensionFilter orderFilter;

    protected boolean enabled = true;

    protected boolean expanded;

    protected List<RecordElement> elements;

    /**
     * Simple constructor, taking the UI {@code extension}, the {@code uixManager UI extension manager}, and the
     * {@code orderFilter UI extension filter} as parameters.
     *
     * @param extension the {@link UIExtension} object defining this element
     * @param uixManager the {@link UIExtensionManager} object
     * @param orderFilter the {@link UIExtensionFilter} object for ordering sections and elements
     * @throws IllegalArgumentException if {@code extension} or {@code uixManager} or {@code orderFilter} are null
     */
    public UIXRecordSection(UIExtension extension, UIExtensionManager uixManager, UIExtensionFilter orderFilter)
    {
        if (extension == null || uixManager == null || orderFilter == null) {
            throw new IllegalArgumentException("DefaultRecordSection constructor parameters must not be null");
        }
        this.extension = extension;
        this.uixManager = uixManager;
        this.orderFilter = orderFilter;
        this.enabled = !StringUtils.equals(FALSE_LABEL, extension.getParameters().get(ENABLED_LABEL));
        this.expanded = StringUtils.equals(TRUE_LABEL, this.extension.getParameters().get(EXPANDED_BY_DEFAULT_LABEL));
    }

    @Override
    public UIExtension getExtension()
    {
        return this.extension;
    }

    @Override
    public String getName()
    {
        String result = this.extension.getParameters().get(TITLE_LABEL);
        if (StringUtils.isBlank(result)) {
            result = StringUtils.capitalize(StringUtils.replaceChars(
                StringUtils.substringAfterLast(this.extension.getId(), "."), "_-", "  "));
        }
        return result;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public boolean isExpandedByDefault()
    {
        return this.expanded;
    }

    @Override
    public List<RecordElement> getAllElements()
    {
        if (this.elements == null) {
            this.elements = collectElements();
        }

        return Collections.unmodifiableList(this.elements);
    }

    /**
     * Returns a list of {@link RecordElement} from a list of {@link UIExtension section elements}.
     *
     * @return a list of {@link RecordElement} objects
     */
    private List<RecordElement> collectElements()
    {
        List<RecordElement> result = new LinkedList<>();
        List<UIExtension> fields = this.uixManager.get(this.extension.getId());
        fields = this.orderFilter.filter(fields, ORDER_LABEL);
        for (UIExtension field : fields) {
            result.add(new UIXRecordElement(field, this));
        }
        return result;
    }

    @Override
    public List<RecordElement> getEnabledElements()
    {
        List<RecordElement> result = new LinkedList<>();
        for (RecordElement element : getAllElements()) {
            if (element.isEnabled()) {
                result.add(element);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder(getName());
        result.append(" [");
        result.append(StringUtils.join(getEnabledElements(), ", "));
        result.append(']');
        return result.toString();
    }
}
