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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filemanager.Folder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default {@link Folder} implementation, based on XWiki document.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultFolder extends AbstractDocument implements Folder
{
    /**
     * The space query parameter.
     */
    private static final String PARAMETER_SPACE = "space";

    /**
     * Used to resolve string document references.
     */
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Used to get the full name from a document reference.
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * Used to retrieve the list of child files and folders.
     */
    @Inject
    private QueryManager queryManager;

    @Override
    public DocumentReference getParentReference()
    {
        return getDocument().getParentReference();
    }

    @Override
    public void setParentReference(DocumentReference parentReference)
    {
        XWikiDocument document = getClonedDocument();
        if (parentReference != null) {
            if (parentReference.getWikiReference().equals(getReference().getWikiReference())) {
                document.setParentReference(parentReference.removeParent(parentReference.getWikiReference()));
            } else {
                document.setParentReference(parentReference.extractReference(EntityType.DOCUMENT));
            }
        } else {
            document.setParentReference((EntityReference) null);
        }
    }

    @Override
    public List<DocumentReference> getChildFolderReferences()
    {
        try {
            String statement =
                "from doc.object(FileManagerCode.FolderClass) as folder"
                    + " where doc.space = :space and doc.parent = :parent";
            Query query = queryManager.createQuery(statement, Query.XWQL);
            query.bindValue(PARAMETER_SPACE, getReference().getLastSpaceReference().getName());
            query.bindValue("parent", localEntityReferenceSerializer.serialize(getReference()));
            query.setWiki(getReference().getWikiReference().getName());
            List<DocumentReference> childFolderReferences = new LinkedList<DocumentReference>();
            for (Object result : query.execute()) {
                childFolderReferences.add(documentReferenceResolver.resolve((String) result, getReference()));
            }
            return childFolderReferences;
        } catch (QueryException e) {
            logger.error("Failed to retrieve the child folders of [{}]", getReference(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DocumentReference> getChildFileReferences()
    {
        try {
            String statement =
                "from doc.object(FileManagerCode.FileClass) as file where doc.space = :space"
                    + " and :tag member of doc.object(XWiki.TagClass).tags";
            Query query = queryManager.createQuery(statement, Query.XWQL);
            query.bindValue(PARAMETER_SPACE, getReference().getLastSpaceReference().getName());
            query.bindValue("tag", getReference().getName());
            query.setWiki(getReference().getWikiReference().getName());
            List<DocumentReference> childFolderReferences = new LinkedList<DocumentReference>();
            for (Object result : query.execute()) {
                childFolderReferences.add(documentReferenceResolver.resolve((String) result, getReference()));
            }
            return childFolderReferences;
        } catch (QueryException e) {
            logger.error("Failed to retrieve the child files of [{}]", getReference(), e);
            return Collections.emptyList();
        }
    }
}
