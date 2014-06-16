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
package org.xwiki.filemanager;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Represents the file system.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Role
@Unstable
public interface FileSystem
{
    /**
     * @param folderReference a folder reference
     * @return the corresponding folder, {@code null} if it doesn't exist
     */
    Folder getFolder(DocumentReference folderReference);

    /**
     * @param fileReference a file reference
     * @return the corresponding file, {@code null} if it doesn't exist
     */
    File getFile(DocumentReference fileReference);

    /**
     * @param reference a reference to a file or folder
     * @return {@code true} if the referenced entity exists, {@code false} otherwise
     */
    boolean exists(DocumentReference reference);

    /**
     * @param reference a reference to a file or folder
     * @return {@code true} if the referenced entity can be viewed by the current user
     */
    boolean canView(DocumentReference reference);

    /**
     * @param reference a reference to a file or folder
     * @return {@code true} if the referenced entity can be edited by the current user
     */
    boolean canEdit(DocumentReference reference);

    /**
     * @param reference a reference to a file or folder
     * @return {@code true} if the referenced entity can be deleted by the current user
     */
    boolean canDelete(DocumentReference reference);

    /**
     * Save a file or a folder.
     * 
     * @param document a file or a folder
     */
    void save(Document document);

    /**
     * Delete a file or a folder.
     * 
     * @param reference a reference to a file or a folder
     */
    void delete(DocumentReference reference);

    /**
     * Renames the given file or folder.
     * 
     * @param document the document to rename
     * @param newReference the new reference
     */
    void rename(Document document, DocumentReference newReference);

    /**
     * Copy a file or a folder.
     * 
     * @param source the reference to the file or folder to copy
     * @param target the reference to the file or folder to create
     */
    void copy(DocumentReference source, DocumentReference target);
}
