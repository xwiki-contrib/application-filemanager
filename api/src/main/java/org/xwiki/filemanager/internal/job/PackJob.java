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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.filemanager.FileSystem;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.internal.PackFileResolver;
import org.xwiki.filemanager.job.PackJobStatus;
import org.xwiki.filemanager.job.PackRequest;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;

/**
 * Packs multiple files and folders (including the child files and sub-folders) in a single ZIP archive.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Named(PackJob.JOB_TYPE + "/actual")
public class PackJob extends AbstractJob<PackRequest, DefaultJobStatus<PackRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "fileManager/pack";

    /**
     * The pseudo file system.
     */
    @Inject
    private FileSystem fileSystem;

    /**
     * Used to access the temporary file where the output ZIP archive is written.
     */
    @Inject
    private PackFileResolver packFileResolver;

    /**
     * Wraps the {@link DefaultJobStatus} created by this job and adds custom data such as the number of bytes written
     * and the size of the output file. We wrap the status instead of extending {@link DefaultJobStatus} in order to not
     * depend on its constructor signature.
     */
    private PackJobStatus packJobStatus;

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

        File outputFile = this.packFileResolver.getTemporaryFile(getRequest().getOutputFileReference());
        // TODO: Use java.util.zip.ZipOutputStream when moving to Java 7.
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4244499
        ZipArchiveOutputStream zip = new ZipArchiveOutputStream(outputFile);
        String pathPrefix = "";

        this.progressManager.pushLevelProgress(paths.size(), this);

        try {
            for (Path path : paths) {
                pack(path, zip, pathPrefix);
                this.progressManager.stepPropress(this);
            }
        } finally {
            IOUtils.closeQuietly(zip);
            getPackStatus().setOutputFileSize(outputFile.length());
            this.progressManager.popLevelProgress(this);
        }
    }

    /**
     * Packs a file or a folder.
     * 
     * @param path the file or folder to add to the ZIP archive
     * @param zip the ZIP archive to add the file or folder to
     * @param pathPrefix the current path prefix, used to ensure the folder hierarchy is preserved in the ZIP file
     */
    private void pack(Path path, ZipArchiveOutputStream zip, String pathPrefix)
    {
        if (path.getFileReference() != null) {
            packFile(path.getFileReference(), zip, pathPrefix);
        } else if (path.getFolderReference() != null) {
            packFolder(path.getFolderReference(), zip, pathPrefix);
        }
    }

    /**
     * Packs a file.
     * 
     * @param fileReference the file to add to the ZIP archive
     * @param zip the ZIP archive to add the file to
     * @param pathPrefix the file path
     */
    private void packFile(DocumentReference fileReference, ZipArchiveOutputStream zip, String pathPrefix)
    {
        org.xwiki.filemanager.File file = fileSystem.getFile(fileReference);
        if (file != null && fileSystem.canView(fileReference)) {
            try {
                String path = pathPrefix + file.getName();
                this.logger.info("Packing file [{}]", path);
                zip.putArchiveEntry(new ZipArchiveEntry(path));
                IOUtils.copy(file.getContent(), zip);
                zip.closeArchiveEntry();
                getPackStatus().setBytesWritten(zip.getBytesWritten());
            } catch (IOException e) {
                this.logger.warn("Failed to pack file [{}].", fileReference, e);
            }
        }
    }

    /**
     * Packs a folder.
     * 
     * @param folderReference the folder to add to the ZIP archive
     * @param zip the ZIP archive to add the folder to
     * @param pathPrefix the folder path
     */
    private void packFolder(DocumentReference folderReference, ZipArchiveOutputStream zip, String pathPrefix)
    {
        Folder folder = fileSystem.getFolder(folderReference);
        if (folder != null && fileSystem.canView(folderReference)) {
            List<DocumentReference> childFolderReferences = folder.getChildFolderReferences();
            List<DocumentReference> childFileReferences = folder.getChildFileReferences();
            this.progressManager.pushLevelProgress(childFolderReferences.size() + childFileReferences.size() + 1, this);

            try {
                String path = pathPrefix + folder.getName() + '/';
                this.logger.info("Packing folder [{}]", path);
                zip.putArchiveEntry(new ZipArchiveEntry(path));
                zip.closeArchiveEntry();
                this.progressManager.stepPropress(this);

                for (DocumentReference childFolderReference : childFolderReferences) {
                    packFolder(childFolderReference, zip, path);
                    this.progressManager.stepPropress(this);
                }

                for (DocumentReference childFileReference : childFileReferences) {
                    packFile(childFileReference, zip, path);
                    this.progressManager.stepPropress(this);
                }
            } catch (IOException e) {
                this.logger.warn("Failed to pack folder [{}].", folderReference, e);
            } finally {
                this.progressManager.popLevelProgress(this);
            }
        }
    }

    /**
     * @return the extended job status
     */
    public PackJobStatus getPackStatus()
    {
        // The internal AbstractJob and AbstractJobStatus classes have been refactored in XWiki 7.4M1 by XCOMMONS-880
        // which seems to have broken the runtime compatibility in the sense that some protected fields and some public
        // methods are not accessible anymore after they have been moved higher in the class hierarchy (and in a
        // different package). The workaround I found is to cast 'this' to the interface/class that provides the public
        // method I want to access.
        JobStatus defaultJobStatus = ((Job) this).getStatus();
        if (this.packJobStatus == null && defaultJobStatus != null) {
            this.packJobStatus = new PackJobStatus(defaultJobStatus);
        }
        return this.packJobStatus;
    }
}
