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
package org.xwiki.filemanager.job;

import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.stability.Unstable;

/**
 * Request used by {@link org.xwiki.filemanager.internal.job.PackJob} to pack multiple files and folders (including the
 * child files and sub-folders) in a single ZIP archive.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Unstable
public class PackRequest extends BatchPathRequest
{
    /**
     * @see #getOutputFileReference()
     */
    public static final String PROPERTY_OUTPUT_FILE_REFERENCE = "output.fileReference";

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @return the reference to the output ZIP file that contains the files and folders specified by {@link #getPaths()}
     */
    public AttachmentReference getOutputFileReference()
    {
        return getProperty(PROPERTY_OUTPUT_FILE_REFERENCE);
    }

    /**
     * Sets the reference to the output ZIP file that will pack the files and folders specified by {@link #getPaths()}.
     * <p>
     * The {@link org.xwiki.model.reference.DocumentReference} part of the given {@link AttachmentReference} represents
     * the document that is going to be used to access the output ZIP file. This means that only users with view right
     * on this document can access the output file. The {@code name} property of the given {@link AttachmentReference}
     * will be used as the name of the output ZIP file.
     * <p>
     * The output file is a temporary file (deleted automatically when the server is stopped) that can be accessed
     * through the 'temp' action, e.g.: {@code /xwiki/temp/Space/Page/filemanager/file.zip} .
     * 
     * @param outputFileReference the reference to the output ZIP file
     */
    public void setOutputFileReference(AttachmentReference outputFileReference)
    {
        setProperty(PROPERTY_OUTPUT_FILE_REFERENCE, outputFileReference);
    }
}
