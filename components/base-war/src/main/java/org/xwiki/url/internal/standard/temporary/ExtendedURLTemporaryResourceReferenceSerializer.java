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
package org.xwiki.url.internal.standard.temporary;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Resolver that generates {@link ExtendedURL} out of {@link org.xwiki.resource.temporary.TemporaryResourceReference}.
 * The generated format corresponds to {@code http://(server)/xwiki/temp/(space)/(page)/(module name)/(resource name)},
 * where:
 * <ul>
 * <li>(space): the space owning the temporary resource (used to check permissions when accessing the resource later on)
 * </li>
 * <li>(page): the page owning the temporary resource (used to check permissions when accessing the resource later on)
 * </li>
 * <li>(module name): a free name (used as a namespace) allowing several components to generate temporary resources for
 * the same page</li>
 * <li>(resource name): the name of the resource (usually the filename on disk, for example {@code image1.png})</li>
 * </ul>
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("standard/tmp")
@Singleton
public class ExtendedURLTemporaryResourceReferenceSerializer
    implements ResourceReferenceSerializer<TemporaryResourceReference, ExtendedURL>
{
    @Inject
    @Named("contextpath+actionservletpath")
    private URLNormalizer<ExtendedURL> extendedURLNormalizer;

    @Override
    public ExtendedURL serialize(TemporaryResourceReference resource)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        DocumentReference owningReference = (DocumentReference) resource.getOwningEntityReference();
        List<String> segments = new LinkedList<>();
        segments.add("temp");
        segments.add(owningReference.getLastSpaceReference().getName());
        segments.add(owningReference.getName());
        segments.add(resource.getModuleId());
        segments.add(resource.getResourceName());
        ExtendedURL result = new ExtendedURL(segments, new HashMap<String, List<String>>());
        return this.extendedURLNormalizer.normalize(result);
    }
}
