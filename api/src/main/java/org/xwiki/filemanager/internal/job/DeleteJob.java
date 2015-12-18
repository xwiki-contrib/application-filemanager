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

import org.xwiki.component.annotation.Component;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.FileSystem;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.BatchPathRequest;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.model.reference.DocumentReference;

/**
 * Deletes files and folders.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named(DeleteJob.JOB_TYPE)
public class DeleteJob extends AbstractJob<BatchPathRequest, DefaultJobStatus<BatchPathRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "fileManager/delete";

    /**
     * The pseudo file system.
     */
    @Inject
    private FileSystem fileSystem;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        Collection<Path> paths = getRequest().getPaths();
        if (paths == null) {
            return;
        }

        notifyPushLevelProgress(paths.size());

        try {
            for (Path path : paths) {
                delete(path);
                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Deletes the specified file or folder.
     * 
     * @param path the path to delete
     */
    private void delete(Path path)
    {
        if (path.getFileReference() != null) {
            deleteFile(path.getFileReference(), path.getFolderReference());
        } else if (path.getFolderReference() != null) {
            deleteFolder(path.getFolderReference());
        }
    }

    /**
     * Deletes a file from one of its parent folders. If the given parent folder reference is {@code null} then the file
     * is deleted from all of its parent folders.
     * 
     * @param fileReference the file to delete
     * @param parentReference the folder the file should be deleted from, {@code null} if the file should be delete from
     *            all parents
     */
    private void deleteFile(DocumentReference fileReference, DocumentReference parentReference)
    {
        File file = fileSystem.getFile(fileReference);
        if (file != null) {
            Collection<DocumentReference> parentReferences = file.getParentReferences();
            boolean save = parentReferences.remove(parentReference);
            if (parentReferences.isEmpty() || parentReference == null) {
                if (fileSystem.canDelete(fileReference)) {
                    fileSystem.delete(fileReference);
                } else {
                    this.logger.error("You are not allowed to delete the file [{}].", fileReference);
                }
            } else if (save) {
                if (fileSystem.canEdit(fileReference)) {
                    fileSystem.save(file);
                } else {
                    this.logger.error("You are not allowed to edit the file [{}].", fileReference);
                }
            }
        }
    }

    /**
     * Deletes the folder with the given reference.
     * 
     * @param folderReference the reference to the folder to delete
     */
    private void deleteFolder(DocumentReference folderReference)
    {
        if (fileSystem.canDelete(folderReference)) {
            Folder folder = fileSystem.getFolder(folderReference);
            if (folder == null) {
                return;
            }

            List<DocumentReference> childFolderReferences = folder.getChildFolderReferences();
            List<DocumentReference> childFileReferences = folder.getChildFileReferences();
            notifyPushLevelProgress(childFolderReferences.size() + childFileReferences.size() + 1);

            try {
                for (DocumentReference childFolderReference : childFolderReferences) {
                    deleteFolder(childFolderReference);
                    notifyStepPropress();
                }

                for (DocumentReference childFileReference : childFileReferences) {
                    deleteFile(childFileReference, folderReference);
                    notifyStepPropress();
                }

                // Delete the folder if it's empty.
                if (folder.getChildFolderReferences().isEmpty() && folder.getChildFileReferences().isEmpty()) {
                    fileSystem.delete(folderReference);
                }
                notifyStepPropress();
            } finally {
                notifyPopLevelProgress();
            }
        } else {
            this.logger.error("You are not allowed to delete the folder [{}].", folderReference);
        }
    }
}
