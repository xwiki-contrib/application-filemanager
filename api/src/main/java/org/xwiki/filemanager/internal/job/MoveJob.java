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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.ObjectUtils;
import org.jgroups.util.UUID;
import org.xwiki.component.annotation.Component;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.FileSystem;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.MoveRequest;
import org.xwiki.filemanager.job.OverwriteQuestion;
import org.xwiki.job.AbstractJob;
import org.xwiki.model.reference.DocumentReference;

/**
 * Move files and folders to a different path, possibly renaming the target file or folder in case there is only one
 * item to move.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named(MoveJob.JOB_TYPE)
public class MoveJob extends AbstractJob<MoveRequest>
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "move";

    /**
     * The error message logged when the folder destination of a move operation doesn't exist.
     */
    private static final String ERROR_DESTINATION_NOT_FOUND = "The destination folder [{}] doesn't exist.";

    /**
     * The pseudo file system.
     */
    @Inject
    private FileSystem fileSystem;

    /**
     * Specifies whether all files with the same name are to be overwritten on not. When {@code true} all files with the
     * same name are overwritten. When {@code false} all files with the same name are skipped. If {@code null} then a
     * question is asked for each file.
     */
    private Boolean overwriteAll;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void start() throws Exception
    {
        Collection<Path> paths = getRequest().getPaths();
        Path destination = getRequest().getDestination();
        if (paths != null && destination != null) {
            if (destination.getFolderReference() != null && fileSystem.exists(destination.getFolderReference())
                && destination.getFileReference() == null) {
                move(paths, destination.getFolderReference());
            } else if (paths.size() == 1 && destination.getFileReference() != null) {
                rename(paths.iterator().next(), destination);
            }
        }
    }

    /**
     * Moves a collection of files and folders to the destination folder.
     * 
     * @param paths the paths to the files and folders to move
     * @param destination the destination folder where to move the files and folders
     */
    private void move(Collection<Path> paths, DocumentReference destination)
    {
        notifyPushLevelProgress(paths.size());

        try {
            for (Path path : paths) {
                move(path, destination);
                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Moves the specified file or folder to the destination folder.
     * 
     * @param path the path to the file or folder to move
     * @param destination the destination folder
     */
    private void move(Path path, DocumentReference destination)
    {
        if (path.getFileReference() != null) {
            moveFile(path.getFileReference(), path.getFolderReference(), destination);
        } else if (path.getFolderReference() != null) {
            moveFolder(path.getFolderReference(), destination);
        }
    }

    /**
     * Moves a folder to another folder.
     * 
     * @param folderReference the folder to move
     * @param newParentReference the destination folder
     */
    private void moveFolder(DocumentReference folderReference, DocumentReference newParentReference)
    {
        if (isDescendantOrSelf(newParentReference, folderReference)) {
            this.logger.error("Cannot move [{}] to a sub-folder of itself.", folderReference);
            return;
        }

        Folder folder = fileSystem.getFolder(folderReference);
        if (folder != null && !ObjectUtils.equals(folder.getParentReference(), newParentReference)) {
            if (fileSystem.canEdit(folderReference)) {
                Folder newParent = fileSystem.getFolder(newParentReference);
                if (newParent != null) {
                    moveFolder(folder, newParent);
                } else {
                    this.logger.error(ERROR_DESTINATION_NOT_FOUND, newParentReference);
                }
            } else {
                this.logger.error("You are not allowed to move the folder [{}].", folderReference);
            }
        }
    }

    /**
     * @param aliceReference a folder reference
     * @param bobReference a folder reference
     * @return {@code true} if the first folder is a descendant of the second, {@code false} otherwise
     */
    private boolean isDescendantOrSelf(DocumentReference aliceReference, DocumentReference bobReference)
    {
        DocumentReference parentReference = aliceReference;
        while (parentReference != null && !parentReference.equals(bobReference)) {
            Folder parent = fileSystem.getFolder(parentReference);
            if (parent == null) {
                return false;
            } else {
                parentReference = parent.getParentReference();
            }
        }
        return parentReference != null;
    }

    /**
     * Move a folder to a different folder.
     * 
     * @param folder the folder to move
     * @param newParent the destination folder
     */
    private void moveFolder(Folder folder, Folder newParent)
    {
        // Check if the new parent has a child folder with the same name.
        Folder child = getChildFolderByName(newParent, folder.getName());
        if (child != null) {
            mergeFolders(folder, child.getReference());
        } else {
            folder.setParentReference(newParent.getReference());
            fileSystem.save(folder);
        }
    }

    /**
     * Looks for a folder with the given name under the specified parent.
     * 
     * @param parent the parent folder
     * @param name the name to look for
     * @return a child folder with the given name, {@code null} if the parent doesn't have a child folder with the
     *         specified name
     */
    private Folder getChildFolderByName(Folder parent, String name)
    {
        for (DocumentReference childReference : parent.getChildFolderReferences()) {
            Folder child = fileSystem.getFolder(childReference);
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Moves the content of the source folder to the destination folder and then deletes the remaining empty source
     * folder.
     * 
     * @param source the folder whose content is moved
     * @param destination a reference to the destination folder
     */
    private void mergeFolders(Folder source, DocumentReference destination)
    {
        List<DocumentReference> childFolderReferences = source.getChildFolderReferences();
        List<DocumentReference> childFileReferences = source.getChildFileReferences();
        notifyPushLevelProgress(childFolderReferences.size() + childFileReferences.size() + 1);

        try {
            for (DocumentReference childReference : childFolderReferences) {
                moveFolder(childReference, destination);
                notifyStepPropress();
            }

            for (DocumentReference childReference : childFileReferences) {
                moveFile(childReference, source.getReference(), destination);
                notifyStepPropress();
            }

            // Delete the source folder if it's empty.
            if (source.getChildFolderReferences().isEmpty() && source.getChildFileReferences().isEmpty()) {
                fileSystem.delete(source.getReference());
            }
            notifyStepPropress();
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Moves a file to a different folder. Since a file can have multiple parent folders, the specified parent is
     * replaced with the new folder.
     * 
     * @param fileReference the file to move
     * @param oldParentReference the parent folder to replace
     * @param newParentReference the new parent folder
     */
    private void moveFile(DocumentReference fileReference, DocumentReference oldParentReference,
        DocumentReference newParentReference)
    {
        File file = fileSystem.getFile(fileReference);
        if (file != null && !ObjectUtils.equals(oldParentReference, newParentReference)) {
            if (fileSystem.canEdit(fileReference)) {
                Folder newParent = fileSystem.getFolder(newParentReference);
                if (newParent != null) {
                    moveFile(file, oldParentReference, newParent);
                } else {
                    this.logger.error(ERROR_DESTINATION_NOT_FOUND, newParentReference);
                }
            } else {
                this.logger.error("You are not allowed to move the file [{}].", fileReference);
            }
        }
    }

    /**
     * Move a file to a different parent folder.
     * 
     * @param file the file to be moved
     * @param oldParentReference the parent folder to replace
     * @param newParent the new parent folder
     */
    private void moveFile(File file, DocumentReference oldParentReference, Folder newParent)
    {
        // Check if a file with the same name already exits under the new parent folder.
        File child = getChildFileByName(newParent, file.getName());
        if (child != null) {
            if (fileSystem.canEdit(child.getReference())
                && shouldOverwrite(file.getReference(), child.getReference())) {
                deleteFile(child, newParent.getReference());
            } else {
                return;
            }
        }

        Collection<DocumentReference> parentReferences = file.getParentReferences();
        boolean save = parentReferences.remove(oldParentReference);
        save |= parentReferences.add(newParent.getReference());
        if (save) {
            fileSystem.save(file);
        }
    }

    /**
     * Looks for a file with the given name under the specified parent.
     * 
     * @param parent the parent folder
     * @param name the name to look for
     * @return a child file with the given name, {@code null} if the parent doesn't have a child file with the specified
     *         name
     */
    private File getChildFileByName(Folder parent, String name)
    {
        for (DocumentReference childReference : parent.getChildFileReferences()) {
            File child = fileSystem.getFile(childReference);
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Ask whether to overwrite or not the destination file with the source file.
     * 
     * @param source the file being moved or copied
     * @param destination a file with the same name that exists in the destination folder
     * @return {@code true} to overwrite the file, {@code false} otherwise
     */
    private boolean shouldOverwrite(DocumentReference source, DocumentReference destination)
    {
        if (getRequest().isInteractive() && getStatus() != null) {
            if (overwriteAll == null) {
                OverwriteQuestion question = new OverwriteQuestion(source, destination);
                try {
                    getStatus().ask(question);
                    if (!question.isAskAgain()) {
                        overwriteAll = question.isOverwrite();
                    }
                    return question.isOverwrite();
                } catch (InterruptedException e) {
                    this.logger.warn("Overwrite question has been interrupted.");
                }
            } else {
                return overwriteAll;
            }
        }

        return false;
    }

    /**
     * Deletes the given file from the specified parent folder. If the file has no other parent folders then it is moved
     * to the recycle bin. Otherwise it is only removed from the specified parent folder.
     * 
     * @param file the file to be deleted
     * @param parentReference the folder from where to delete the file
     */
    private void deleteFile(File file, DocumentReference parentReference)
    {
        Collection<DocumentReference> parentReferences = file.getParentReferences();
        parentReferences.remove(parentReference);
        if (parentReferences.isEmpty()) {
            fileSystem.delete(file.getReference());
        } else {
            fileSystem.save(file);
        }
    }

    /**
     * Rename a file or a folder.
     * 
     * @param oldPath the path to rename
     * @param newPath the new path
     */
    private void rename(Path oldPath, Path newPath)
    {
        if (oldPath != null) {
            if (oldPath.getFileReference() != null) {
                renameFile(oldPath, newPath);
            } else if (oldPath.getFolderReference() != null) {
                renameFolder(oldPath.getFolderReference(), newPath);
            }
        }
    }

    /**
     * Renames a folder.
     * 
     * @param oldReference the old folder reference
     * @param newPath the new folder path
     */
    private void renameFolder(DocumentReference oldReference, Path newPath)
    {
        Folder folder = fileSystem.getFolder(oldReference);
        if (folder != null) {
            if (fileSystem.canEdit(oldReference)) {
                DocumentReference newParentReference = newPath.getFolderReference();
                if (newParentReference == null) {
                    // If the new parent is not specified we assume the parent doesn't change.
                    newParentReference = folder.getParentReference();
                }
                if (ObjectUtils.equals(newParentReference, folder.getParentReference())
                    && newPath.getFileReference().equals(oldReference)) {
                    // No move (same parent) and no rename (same reference).
                    return;
                }
                if (newParentReference != null) {
                    renameFolder(folder, new Path(newParentReference, newPath.getFileReference()));
                } else {
                    // Rename an orphan folder.
                    // The file reference from the new path is actually used as the new folder reference.
                    renameFolder(folder, newPath.getFileReference());
                }
            } else {
                this.logger.error("You are not allowed to rename the folder [{}].", oldReference);
            }
        }
    }

    /**
     * Renames a folder.
     * 
     * @param folder the folder to rename
     * @param newPath the new path
     */
    private void renameFolder(Folder folder, Path newPath)
    {
        Folder newParent = fileSystem.getFolder(newPath.getFolderReference());
        Folder child = getChildFolderByName(newParent, newPath.getFileReference().getName());
        if (child == null) {
            folder.setParentReference(newParent.getReference());
            if (newPath.getFileReference().equals(folder.getReference())) {
                // No rename, just move.
                fileSystem.save(folder);
            } else {
                // The file reference from the new path is actually used as the new folder reference.
                renameFolder(folder, newPath.getFileReference());
            }
        } else {
            this.logger.error("A folder with the same name [{}] already exists under [{}]", newPath.getFileReference()
                .getName(), newParent.getReference());
        }
    }

    /**
     * Rename the given folder. The new folder reference may not be exactly the given one, in case a document with the
     * same reference already exists on the same drive (XWiki space), in which case a counter is added to the folder id.
     * 
     * @param folder the folder to rename
     * @param newReference the desired new folder reference
     */
    private void renameFolder(Folder folder, DocumentReference newReference)
    {
        List<DocumentReference> childFolderReferences = folder.getChildFolderReferences();
        List<DocumentReference> childFileReferences = folder.getChildFileReferences();

        folder.setName(newReference.getName());

        DocumentReference oldReference = folder.getReference();
        fileSystem.rename(folder, getUniqueReference(newReference));

        for (DocumentReference childFolderReference : childFolderReferences) {
            Folder childFolder = fileSystem.getFolder(childFolderReference);
            childFolder.setParentReference(folder.getReference());
            fileSystem.save(childFolder);
        }

        for (DocumentReference childFileReference : childFileReferences) {
            File childFile = fileSystem.getFile(childFileReference);
            childFile.getParentReferences().remove(oldReference);
            childFile.getParentReferences().add(folder.getReference());
            fileSystem.save(childFile);
        }
    }

    /**
     * Renames a file.
     * 
     * @param oldPath the old file path
     * @param newPath the new file path
     */
    private void renameFile(Path oldPath, Path newPath)
    {
        File file = fileSystem.getFile(oldPath.getFileReference());
        if (file != null) {
            if (fileSystem.canEdit(file.getReference())) {
                boolean save = false;
                Collection<DocumentReference> parentReferences = file.getParentReferences();
                if (newPath.getFolderReference() != null && oldPath.getFolderReference() != null
                    && !newPath.getFolderReference().equals(oldPath.getFolderReference())) {
                    save |= parentReferences.remove(oldPath.getFolderReference());
                    save |= parentReferences.add(newPath.getFolderReference());
                }

                if (!file.getReference().equals(newPath.getFileReference())) {
                    renameFile(file, newPath.getFileReference());
                } else if (save) {
                    fileSystem.save(file);
                }
            } else {
                this.logger.error("You are not allowed to rename the file [{}].", file.getReference());
            }
        }
    }

    /**
     * Renames the given file.
     * 
     * @param file the file to rename
     * @param newReference the new file reference
     */
    private void renameFile(File file, DocumentReference newReference)
    {
        if (!file.getReference().getName().equals(newReference.getName())) {
            for (DocumentReference parentReference : file.getParentReferences()) {
                Folder folder = fileSystem.getFolder(parentReference);
                if (folder != null && getChildFileByName(folder, newReference.getName()) != null) {
                    this.logger.error("A file with the same name [{}] already exists under [{}]",
                        newReference.getName(), parentReference);
                    return;
                }
            }
            file.setName(newReference.getName());
        }
        fileSystem.rename(file, getUniqueReference(newReference));
    }

    /**
     * @param documentReference a document reference
     * @return a unique document references based on the given reference (adds a counter if needed)
     */
    private DocumentReference getUniqueReference(DocumentReference documentReference)
    {
        if (!fileSystem.exists(documentReference)) {
            return documentReference;
        }

        for (int i = 1; i < 100; i++) {
            String uniqueName = documentReference.getName() + i;
            DocumentReference uniqueReference =
                new DocumentReference(uniqueName, documentReference.getSpaceReferences().get(0));
            if (!fileSystem.exists(uniqueReference)) {
                return uniqueReference;
            }
        }

        for (int i = 0; i < 5; i++) {
            int counter = 100 + (int) (Math.random() * 100000);
            String uniqueName = documentReference.getName() + counter;
            DocumentReference uniqueReference =
                new DocumentReference(uniqueName, documentReference.getSpaceReferences().get(0));
            if (!fileSystem.exists(uniqueReference)) {
                return uniqueReference;
            }
        }

        String uniqueName = documentReference.getName() + UUID.randomUUID().toString();
        return new DocumentReference(uniqueName, documentReference.getSpaceReferences().get(0));
    }
}
