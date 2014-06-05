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
package org.xwiki.filemanager.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filemanager.File;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Default {@link File} implementation, based on XWiki document.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultFile extends AbstractDocument implements File
{
    /**
     * The reference to the Tag class which is used to store the parent folders.
     */
    private static final EntityReference TAG_CLASS_REFERENCE = new EntityReference("TagClass", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    /**
     * The 'tags' property of {@link #TAG_CLASS_REFERENCE}.
     */
    private static final String PROPERTY_TAGS = "tags";

    /**
     * The cached collection of references to parent folders.
     */
    private Collection<DocumentReference> parentReferences;

    @Override
    public Collection<DocumentReference> getParentReferences()
    {
        if (parentReferences == null) {
            parentReferences = retrieveParentReferences();
        }
        return parentReferences;
    }

    /**
     * Updates the list of parent references on the underlying document.
     */
    void updateParentReferences()
    {
        if (parentReferences == null) {
            return;
        }

        BaseObject tagObject = getDocument().getXObject(TAG_CLASS_REFERENCE);
        if (tagObject == null) {
            tagObject = new BaseObject();
            tagObject.setXClassReference(TAG_CLASS_REFERENCE);
            getDocument().addXObject(tagObject);
        }

        List<String> tags = new ArrayList<String>();
        for (DocumentReference parentReference : parentReferences) {
            tags.add(parentReference.getName());
        }
        tagObject.setStringListValue(PROPERTY_TAGS, tags);

        parentReferences = null;
    }

    /**
     * @return the saved collection of parent folder references
     */
    private Collection<DocumentReference> retrieveParentReferences()
    {
        Collection<DocumentReference> references = new ArrayList<DocumentReference>();
        BaseObject tagObject = getDocument().getXObject(TAG_CLASS_REFERENCE);
        if (tagObject != null) {
            try {
                List<String> tags = (List<String>) ((BaseProperty) tagObject.get(PROPERTY_TAGS)).getValue();
                if (tags != null) {
                    for (String tag : tags) {
                        references.add(new DocumentReference(tag, getReference().getLastSpaceReference()));
                    }
                }
            } catch (XWikiException e) {
                logger.error("Failed to retrieve the list of tags for file [{}].", getReference(), e);
            }
        }
        return references;
    }
}
