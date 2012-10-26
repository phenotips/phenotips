/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.cidb.tools;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.xml.XMLUtils;

public class FormSection extends FormGroup {
	private final String propertyName;
	private final List<String> categories;
	private FormGroup customElements = new FormGroup("");

	FormSection(String title, String propertyName, Collection<String> categories) {
		super(title);
		this.propertyName = propertyName;
		this.categories = new LinkedList<String>();
		this.categories.addAll(categories);
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	protected boolean addCustomElement(FormElement e) {
		return this.customElements.addElement(e);
	}

	@Override
	public String display(DisplayMode mode, String fieldNames[]) {
		String displayedElements = super.display(mode, fieldNames);
		String customValueDisplay = this.customElements.display(mode,
				fieldNames);
		if (!DisplayMode.Edit.equals(mode)
				&& StringUtils.isBlank(displayedElements)
				&& StringUtils.isBlank(customValueDisplay)) {
			return "";
		}
		return "<div class='"
				+ this.getPropertyName()
				+ "-group'><h3 id='H"
				+ this.title.replaceAll("[^a-zA-Z0-9]+", "-")
				+ "'><span>"
				+ XMLUtils.escapeElementContent(this.title)
				+ "</span></h3><div class='"
				+ this.getPropertyName()
				+ "-main predefined-entries'>"
				+ displayedElements
				+ "</div>"
				+ "<div class='"
				+ this.getPropertyName()
				+ "-other custom-entries'>"
				+ (!StringUtils.isBlank(customValueDisplay) ? ("<div class=\"custom-display-data\">"
						+ customValueDisplay + "</div>")
						: "") + generateSuggestionsField(mode, fieldNames)
				+ "</div></div>";
	}

	private String generateSuggestionsField(DisplayMode mode,
			String fieldNames[]) {
		if (!DisplayMode.Edit.equals(mode)) {
			return "";
		}
		String result = "";
		String id = fieldNames[YES] + "_" + Math.random();
		String displayedLabel = "Other";
		result += "<label for='" + id + "' class='label-other label-other-"
				+ fieldNames[YES] + "'>" + displayedLabel + "</label>";
		result += "<p class='hint'>(enter free text and choose among suggested ontology terms)</p>";

		result += "<input type='text' name='"
				+ fieldNames[YES]
				+ "' class='suggested multi suggest-hpo "
				+ (fieldNames[NO] == null ? "generateCheckboxes"
						: "generateYesNo")
				+ " accept-value' value='' size='16' id='" + id + "'/>";
		result += "<input type='hidden' value='"
				+ this.categories.toString().replaceAll("[\\[\\]\\s]", "")
				+ "' name='_category'/>";
		return result;
	}

	public FormGroup getCustomElements() {
		return this.customElements;
	}

	public void setCustomElements(FormGroup customElements) {
		this.customElements = customElements;
	}

	public List<String> getCategories() {
		return this.categories;
	}

}
