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
package org.phenotips.variant;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import net.sf.json.JSONObject;

/**
 * Provides access to the available variant tools and their terms to public scripts.
 *
 * @version $Id$
 * @since 1.2M4
 */
@Unstable
@Role
public interface VariantManager
{
    /**
     * Validate the string representation of the variant.
     *
     * @param id variant id to validate
     * @return json respond in a form of {"valid": <Boolean>, "messages": []}
     */
    JSONObject validateVariant(String id);
}
