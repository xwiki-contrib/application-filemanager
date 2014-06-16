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
package org.xwiki.filemanager.internal.job;

import java.util.Collection;
import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.model.reference.DocumentReference;

/**
 * Copy files and folders to a different parent, possibly with a different name if the target list contains one item.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named(CopyJob.JOB_TYPE)
public class CopyJob extends MoveJob
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "copy";

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        Collection<Path> paths = getRequest().getPaths();
        Path destination = getRequest().getDestination();
        if (paths == null || destination == null || destination.getFolderReference() == null
            || !fileSystem.exists(destination.getFolderReference())) {
            return;
        }

        notifyPushLevelProgress(paths.size());

        try {
            for (Path path : paths) {
                copy(path, destination);
                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Copy the specified file or folder to the given destination.
     * 
     * @param path the file or folder to copy
     * @param destination the destination
     */
    private void copy(Path path, Path destination)
    {
        if (path.getFileReference() != null) {
            copyFile(path.getFileReference(), destination);
        } else if (path.getFolderReference() != null) {
            copyFolder(path.getFolderReference(), destination);
        }
    }

    /**
     * Copy the specified file to the given destination.
     * 
     * @param fileReference the file to be copied
     * @param destination the destination
     */
    private void copyFile(DocumentReference fileReference, Path destination)
    {
        File file = fileSystem.getFile(fileReference);
        if (file != null) {
            if (fileSystem.canView(fileReference)) {
                Collection<DocumentReference> parentReferences = file.getParentReferences();
                boolean copyToDifferentFolder = !parentReferences.contains(destination.getFolderReference());
                if (destination.getFileReference() == null && copyToDifferentFolder) {
                    // Same name but a different folder.
                    DocumentReference copyReference =
                        new DocumentReference(file.getName(), fileReference.getLastSpaceReference());
                    copyFile(file, new Path(destination.getFolderReference(), copyReference));
                } else if (destination.getFileReference() != null
                    && (!destination.getFileReference().getName().equals(file.getName()) || copyToDifferentFolder)) {
                    // Either different name or different folder.
                    copyFile(file, destination);
                }
            } else {
                this.logger.error("You are not allowed to copy the file [{}].", fileReference);
            }
        }
    }

    /**
     * Copy the given file to the specified path.
     * 
     * @param file the file to copy
     * @param destination the destination path
     */
    private void copyFile(File file, Path destination)
    {
        Folder folder = fileSystem.getFolder(destination.getFolderReference());
        File child = getChildFileByName(folder, destination.getFileReference().getName());
        if (child != null) {
            if (fileSystem.canDelete(child.getReference())
                && shouldOverwrite(file.getReference(), child.getReference())) {
                deleteFile(child, folder.getReference());
            } else {
                return;
            }
        }

        DocumentReference copyReference = getUniqueReference(destination.getFileReference());
        fileSystem.copy(file.getReference(), copyReference);
        File copy = fileSystem.getFile(copyReference);
        if (copy != null) {
            // Update the name ..
            copy.setName(destination.getFileReference().getName());
            // .. and the parent folder.
            Collection<DocumentReference> parentReferences = copy.getParentReferences();
            parentReferences.clear();
            parentReferences.add(destination.getFolderReference());
            fileSystem.save(copy);
        }
    }

    /**
     * Copy the specified folder to the given destination.
     * 
     * @param folderReference the folder to copy
     * @param destination the destination
     */
    private void copyFolder(DocumentReference folderReference, Path destination)
    {
        if (isDescendantOrSelf(destination.getFolderReference(), folderReference)) {
            this.logger.error("Cannot copy [{}] to a sub-folder of itself.", folderReference);
            return;
        }

        Folder folder = fileSystem.getFolder(folderReference);
        if (folder != null) {
            if (fileSystem.canView(folderReference)) {
                boolean copyToDifferentFolder = !destination.getFolderReference().equals(folder.getParentReference());
                if (destination.getFileReference() == null && copyToDifferentFolder) {
                    // Same name but a different folder.
                    copyFolder(folder, new Path(destination.getFolderReference(), folderReference));
                } else if (destination.getFileReference() != null
                    && (!destination.getFileReference().getName().equals(folder.getName()) || copyToDifferentFolder)) {
                    // Either different name or different folder.
                    copyFolder(folder, destination);
                }
            } else {
                this.logger.error("You are not allowed to copy the folder [{}].", folderReference);
            }
        }
    }

    /**
     * Copy the given folder to the specified destination.
     * 
     * @param folder the folder to copy
     * @param destination the destination
     */
    private void copyFolder(Folder folder, Path destination)
    {
        // Check if the new parent has a child folder with the same name.
        Folder newParent = fileSystem.getFolder(destination.getFolderReference());
        Folder child = getChildFolderByName(newParent, destination.getFileReference().getName());
        if (child != null) {
            copyContent(folder, child.getReference());
        } else {
            DocumentReference copyReference = getUniqueReference(destination.getFileReference());
            fileSystem.copy(folder.getReference(), copyReference);
            Folder copy = fileSystem.getFolder(copyReference);
            if (copy != null) {
                copy.setName(destination.getFileReference().getName());
                copy.setParentReference(destination.getFolderReference());
                fileSystem.save(copy);

                copyContent(folder, copyReference);
            }
        }
    }

    /**
     * Copy the content of the given folder to the specified destination path.
     * 
     * @param source the folder whose content is copied
     * @param destination the destination folder where to copy the content
     */
    private void copyContent(Folder source, DocumentReference destination)
    {
        Path destinationPath = new Path(destination);
        List<DocumentReference> childFolderReferences = source.getChildFolderReferences();
        List<DocumentReference> childFileReferences = source.getChildFileReferences();
        notifyPushLevelProgress(childFolderReferences.size() + childFileReferences.size());

        try {
            for (DocumentReference childFileReference : childFileReferences) {
                copyFile(childFileReference, destinationPath);
                notifyStepPropress();
            }

            for (DocumentReference childFolderReference : childFolderReferences) {
                copyFolder(childFolderReference, destinationPath);
                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }
}
