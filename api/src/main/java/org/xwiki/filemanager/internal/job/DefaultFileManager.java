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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jgroups.util.UUID;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.BatchPathRequest;
import org.xwiki.filemanager.job.FileManager;
import org.xwiki.filemanager.job.MoveRequest;
import org.xwiki.job.JobException;
import org.xwiki.job.JobManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.observation.EventListener;

/**
 * Default {@link FileManager} implementation.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Singleton
public class DefaultFileManager implements FileManager
{
    /**
     * The key under which we store the reference to the current user in the job request (in order to know the user that
     * triggered the job and to verify access rights when performing the job actions).
     */
    private static final String PROPERTY_USER_REFERENCE = "user.reference";

    /**
     * The key under which we store the job type in the job request. This is useful when different jobs use the same
     * type of request and we need to know the job type.
     */
    private static final String PROPERTY_JOB_TYPE = "job.type";

    /**
     * Used to access the current user reference.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Handles the execution of the file system jobs.
     */
    @Inject
    private JobManager jobManager;

    /**
     * The queue of active (unfinished) jobs.
     */
    @Inject
    @Named("ActiveFileSystemJobQueue")
    private EventListener activeJobQueue;

    @Override
    public String move(Collection<Path> paths, Path destination) throws JobException
    {
        MoveRequest moveRequest = createMoveRequest(paths, destination, MoveJob.JOB_TYPE);

        this.jobManager.addJob(MoveJob.JOB_TYPE, moveRequest);
        return addToQueue(moveRequest);
    }

    @Override
    public String copy(Collection<Path> paths, Path destination) throws JobException
    {
        MoveRequest moveRequest = createMoveRequest(paths, destination, CopyJob.JOB_TYPE);

        this.jobManager.addJob(CopyJob.JOB_TYPE, moveRequest);
        return addToQueue(moveRequest);
    }

    @Override
    public String delete(Collection<Path> paths) throws JobException
    {
        BatchPathRequest deleteRequest = initBatchPathRequest(new BatchPathRequest(), paths, DeleteJob.JOB_TYPE);

        this.jobManager.addJob(DeleteJob.JOB_TYPE, deleteRequest);
        return addToQueue(deleteRequest);
    }

    @Override
    public JobStatus getJobStatus(String jobId)
    {
        return this.jobManager.getJobStatus(getJobStatusId(jobId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getActiveJobs()
    {
        return new ArrayList<String>((Queue<String>) activeJobQueue);
    }

    /**
     * Initializes the given request.
     * 
     * @param request the request to initialize
     * @param paths the collection of files and folders targeted by the request
     * @param jobType the job type
     * @param <T> the request type
     * @return the initialized request
     */
    private <T extends BatchPathRequest> T initBatchPathRequest(T request, Collection<Path> paths, String jobType)
    {
        request.setId(getJobStatusId(UUID.randomUUID().toString()));
        // Copy the collection of paths to make sure it isn't modified while we iterate it.
        request.setPaths(new LinkedList<Path>(paths));
        request.setProperty(PROPERTY_USER_REFERENCE, this.documentAccessBridge.getCurrentUserReference());
        request.setProperty(PROPERTY_JOB_TYPE, jobType);
        return request;
    }

    /**
     * @param paths the files and folders to move or copy
     * @param destination where to move or copy the specified files and folders
     * @param jobType either move or copy
     * @return the move/copy request
     */
    private MoveRequest createMoveRequest(Collection<Path> paths, Path destination, String jobType)
    {
        MoveRequest moveRequest = initBatchPathRequest(new MoveRequest(), paths, jobType);
        moveRequest.setDestination(destination);
        moveRequest.setInteractive(true);
        return moveRequest;
    }

    /**
     * @param jobId the job id
     * @return the id used to retrieve the status of the job with the given id
     */
    private List<String> getJobStatusId(String jobId)
    {
        return Arrays.asList(JOB_ID_PREFIX, jobId);
    }

    /**
     * @param request the request to add to the queue
     * @return the id of the job that will perform the request; use this id to get the job status
     */
    @SuppressWarnings("unchecked")
    private String addToQueue(BatchPathRequest request)
    {
        String jobId = request.getId().get(1);
        ((Queue<String>) activeJobQueue).offer(jobId);
        return jobId;
    }
}
