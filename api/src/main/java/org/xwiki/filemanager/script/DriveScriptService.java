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
package org.xwiki.filemanager.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.BatchPathRequest;
import org.xwiki.filemanager.job.FileManager;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;

/**
 * Exposes simplified APIs to execute batch jobs on a file system drive.
 * <p>
 * All jobs work with file system paths. A path is uniquely determined either by an existing folder or by a (parent,
 * child) pair. A file cannot specify the path alone because files can have multiple parent folders (so a file can have
 * multiple paths). Also, a path to a new file or new folder needs to specify the parent folder. In other words, a list
 * with a single item represents a path to a folder, while a list with two items represents a path to either a file or a
 * new sub-folder.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("drive")
public class DriveScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.drive.error";

    /**
     * The pattern used to split a path.
     */
    private static final Pattern PATH_PATTERN = Pattern.compile("/");

    /**
     * Used to schedule file system jobs.
     */
    @Inject
    private FileManager fileManager;

    /**
     * Provides access to the current execution context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to access the current document.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Schedules a job to move the specified files and folders to the given destination.
     * 
     * @param paths the files and folders to move
     * @param destination where to move the specified files and folders
     * @return the id of the move job that has been scheduled
     */
    public String move(Collection<String> paths, String destination)
    {
        setError(null);

        try {
            return fileManager.move(asPath(paths), asPath(destination));
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Schedules a job to copy the specified files and folders to the given destination.
     * 
     * @param paths the files and folders to copy
     * @param destination where to copy the specified files and folders
     * @return the id of the copy job that has been scheduled
     */
    public String copy(Collection<String> paths, String destination)
    {
        setError(null);

        try {
            return fileManager.copy(asPath(paths), asPath(destination));
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Schedules a job to delete the specified files and folders.
     * 
     * @param paths the files and folders to delete
     * @return the id of the delete job that has been scheduled
     */
    public String delete(Collection<String> paths)
    {
        setError(null);

        try {
            return fileManager.delete(asPath(paths));
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    /**
     * Schedules a job to pack the specified files and folders into a single ZIP archive.
     * <p>
     * The {@link org.xwiki.model.reference.DocumentReference} part of the given {@link AttachmentReference} represents
     * the document that is going to be used to access the output ZIP file. This means that only users with view right
     * on this document can access the output file. The {@code name} property of the given {@link AttachmentReference}
     * will be used as the name of the output ZIP file.
     * <p>
     * The output file is a temporary file (deleted automatically when the server is stopped) that can be accessed
     * through the 'temp' action, e.g.: {@code /xwiki/temp/Space/Page/filemanager/file.zip} .
     * 
     * @param paths the files and folders to be packed
     * @param outputFileReference the reference to the output ZIP file
     * @return the id of the pack job that has been scheduled
     * @since 2.0M2
     */
    public String pack(Collection<String> paths, AttachmentReference outputFileReference)
    {
        setError(null);

        try {
            return fileManager.pack(asPath(paths), outputFileReference);
        } catch (JobException e) {
            setError(e);
            return null;
        }
    }

    /**
     * @param jobId the job whose status to return
     * @return the status of the specified job
     */
    public JobStatus getJobStatus(String jobId)
    {
        return fileManager.getJobStatus(jobId);
    }

    /**
     * @return the list of file system jobs that are running or that are pending for execution on the current drive
     */
    public List<String> getActiveJobs()
    {
        List<String> activeJobs = new ArrayList<String>();
        for (String jobId : fileManager.getActiveJobs()) {
            if (jobTargetsDrive(jobId)) {
                activeJobs.add(jobId);
            }
        }
        return activeJobs;
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }

    /**
     * @return the reference to the current drive
     */
    private SpaceReference getCurrentDriveReference()
    {
        return documentAccessBridge.getCurrentDocumentReference().getLastSpaceReference();
    }

    /**
     * Converts a serialized path into a {@link Path} object. The serialized path has the format {@code parent/child}.
     * If there's no separator (slash) then the path points to a folder. Otherwise the path points either to a file (in
     * the specified parent folder) or to a new folder (in the specified parent folder). If the parent is missing (e.g.
     * {@code /child}) then the path points to a file, targeting all occurrences of that file in the current drive
     * (because a file can have multiple parent folders). This is useful for instance if you want to delete a file from
     * all its parent folders.
     * 
     * @param pathElements the path elements
     * @return the {@link Path} object
     */
    private Path asPath(String serializedPath)
    {
        DocumentReference parentReference = null;
        DocumentReference childReference = null;
        String[] pathElements = PATH_PATTERN.split(serializedPath, 2);
        String parentId = pathElements[0];
        if (!StringUtils.isEmpty(parentId)) {
            parentReference = new DocumentReference(parentId, getCurrentDriveReference());
        }
        if (pathElements.length > 1) {
            String childId = pathElements[1];
            if (!StringUtils.isEmpty(childId)) {
                childReference = new DocumentReference(childId, getCurrentDriveReference());
            }
        }
        return new Path(parentReference, childReference);
    }

    /**
     * Converts a collection of serialized paths into a collection of {@link Path} objects.
     * 
     * @param serializedPaths the collection of serialized paths
     * @return the collection of {@link Path} objects
     * @see #asPath(List)
     */
    private Collection<Path> asPath(Collection<String> serializedPaths)
    {
        Collection<Path> paths = new LinkedList<Path>();
        for (String serializedPath : serializedPaths) {
            paths.add(asPath(serializedPath));
        }
        return paths;
    }

    /**
     * @param jobId specifies a file system job
     * @return {@code true} if the specified job targets the current drive, {@code false} otherwise
     */
    private boolean jobTargetsDrive(String jobId)
    {
        JobStatus jobStatus = getJobStatus(jobId);
        SpaceReference currentDriveReference = getCurrentDriveReference();
        if (jobStatus != null) {
            Collection<Path> paths = ((BatchPathRequest) jobStatus.getRequest()).getPaths();
            if (paths.size() > 0) {
                Path firstPath = paths.iterator().next();
                DocumentReference reference =
                    firstPath.getFileReference() != null ? firstPath.getFileReference() : firstPath
                        .getFolderReference();
                return reference != null && reference.getLastSpaceReference().equals(currentDriveReference);
            }
        }
        return false;
    }
}
