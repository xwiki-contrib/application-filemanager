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

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;

/**
 * Resolves the temporary resource that holds the ZIP archive produced by a pack job.
 *
 * @version $Id$
 * @since 2.1
 */
@Component(roles = PackFileResolver.class)
@Singleton
public class PackFileResolver
{
    /**
     * The module id used as a name space for the temporary resources created by this application.
     */
    private static final String MODULE_ID = "filemanager";

    /**
     * Used to access the temporary files.
     */
    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    /**
     * The document that owns the given attachment reference becomes the entity that owns the temporary resource, which
     * means only the users that can view that document are allowed to download the packed file.
     *
     * @param outputFileReference the reference to the ZIP archive produced by a pack job
     * @return the reference to the temporary resource that holds the specified ZIP archive
     */
    public TemporaryResourceReference getTemporaryResourceReference(AttachmentReference outputFileReference)
    {
        return new TemporaryResourceReference(MODULE_ID, outputFileReference.getName(),
            outputFileReference.getDocumentReference());
    }

    /**
     * @param outputFileReference the reference to the ZIP archive produced by a pack job
     * @return the temporary file where the specified ZIP archive has to be written, its parent folders being created
     *         if needed
     * @throws IOException if the temporary file can't be resolved or if its parent folders can't be created
     */
    public File getTemporaryFile(AttachmentReference outputFileReference) throws IOException
    {
        File file = this.temporaryResourceStore.getTemporaryFile(getTemporaryResourceReference(outputFileReference));
        // The temporary resource store creates the parent folders only when it also writes the file content, but we
        // need to write the ZIP archive ourselves, incrementally.
        FileUtils.forceMkdirParent(file);
        return file;
    }
}
