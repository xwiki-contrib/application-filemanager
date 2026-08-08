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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.Folder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultFileSystem}.
 *
 * @version $Id$
 * @since 2.0M1
 */
@ComponentTest
class DefaultFileSystemTest
{
    @InjectMockComponents
    private DefaultFileSystem fileSystem;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    /**
     * {@link DefaultFileSystem} looks up the {@link File} and {@link Folder} implementations from the component
     * manager, so we register them here.
     */
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private XWikiContext xcontext;

    private XWiki wiki;

    @BeforeEach
    void configure()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);

        this.wiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.wiki);
        when(this.xcontext.getUserReference()).thenReturn(new DocumentReference("wiki", "Users", "mflorea"));
    }

    @Test
    void getFolder() throws Exception
    {
        DocumentReference folderReference = new DocumentReference("wiki", "Drive", "Folder");
        XWikiDocument folderDocument = mock(XWikiDocument.class);
        when(this.wiki.getDocument(folderReference, this.xcontext)).thenReturn(folderDocument);
        when(folderDocument.isNew()).thenReturn(false);

        DefaultFolder expectedFolder = spy(new DefaultFolder());
        this.componentManager.registerComponent(Folder.class, expectedFolder);

        Folder actualFolder = this.fileSystem.getFolder(folderReference);

        assertSame(expectedFolder, actualFolder);
        verify(expectedFolder).setDocument(folderDocument);
    }

    @Test
    void getFile() throws Exception
    {
        DocumentReference fileReference = new DocumentReference("wiki", "Drive", "file.txt");
        XWikiDocument fileDocument = mock(XWikiDocument.class);
        when(this.wiki.getDocument(fileReference, this.xcontext)).thenReturn(fileDocument);
        when(fileDocument.isNew()).thenReturn(false);

        DefaultFile expectedFile = spy(new DefaultFile());
        this.componentManager.registerComponent(File.class, expectedFile);

        File actualFile = this.fileSystem.getFile(fileReference);

        assertSame(expectedFile, actualFile);
        verify(expectedFile).setDocument(fileDocument);
    }

    @Test
    void saveFile() throws Exception
    {
        XWikiDocument xdoc = mock(XWikiDocument.class);
        when(xdoc.clone()).thenReturn(xdoc);
        when(xdoc.isContentDirty()).thenReturn(false);
        when(xdoc.isMetaDataDirty()).thenReturn(true);

        DefaultFile file = spy(new DefaultFile());
        file.setDocument(xdoc);

        this.fileSystem.save(file);

        verify(file).updateParentReferences();
        verify(xdoc).setAuthorReference(this.xcontext.getUserReference());
        verify(this.wiki).saveDocument(xdoc, "", false, this.xcontext);
    }

    /**
     * @see "FILEMAN-105: Files from File manager disappear after renaming the folder"
     */
    @Test
    void rename() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "Space", "OldPage");
        DocumentReference newReference = new DocumentReference("wiki", "Space", "NewPage");

        XWikiDocument oldDocument = mock(XWikiDocument.class, "old");
        when(this.wiki.getDocument(oldReference, this.xcontext)).thenReturn(oldDocument);

        XWikiDocument clonedDocument = mock(XWikiDocument.class, "cloned");
        when(oldDocument.clone()).thenReturn(clonedDocument);

        this.fileSystem.rename(oldReference, newReference);

        verify(clonedDocument).rename(newReference, this.xcontext);
    }

    @Test
    void delete() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "Drive", "File");

        XWikiDocument cachedDocument = mock(XWikiDocument.class, "cached");
        when(this.wiki.getDocument(reference, this.xcontext)).thenReturn(cachedDocument);

        XWikiDocument clonedDocument = mock(XWikiDocument.class, "cloned");
        when(cachedDocument.clone()).thenReturn(clonedDocument);

        this.fileSystem.delete(reference);

        verify(this.wiki).deleteDocument(clonedDocument, this.xcontext);
    }

    @Test
    void copy() throws Exception
    {
        DocumentReference source = new DocumentReference("wiki", "Source", "Page");
        DocumentReference target = new DocumentReference("wiki", "Target", "Page");

        this.fileSystem.copy(source, target);

        verify(this.wiki).copyDocument(source, target, null, false, true, true, this.xcontext);
    }
}
