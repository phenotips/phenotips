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
package org.phenotips;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * Frequently used values in the rest of the code, gathered in one place.
 *
 * @version $Id$
 * @since 1.0M9
 */
public interface Constants
{
    /** The name of the space where most of the PhenoTips code resides in. */
    String CODE_SPACE = "PhenoTips";

    /** Reference to the space where most of the PhenoTips code resides in. */
    EntityReference CODE_SPACE_REFERENCE = new EntityReference(CODE_SPACE, EntityType.SPACE);

    /** The name of the space where the XWiki code resides in. */
    String XWIKI_SPACE = "XWiki";

    /** The reference to the space where most of the XWiki code resides in. */
    EntityReference XWIKI_SPACE_REFERENCE = new EntityReference(XWIKI_SPACE, EntityType.SPACE);
}
