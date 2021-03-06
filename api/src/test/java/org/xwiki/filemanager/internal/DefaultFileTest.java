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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.filemanager.File;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultFile}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultFileTest
{
    @Rule
    public MockitoComponentMockingRule<File> mocker = new MockitoComponentMockingRule<File>(DefaultFile.class);

    private DefaultFile file;

    @Before
    public void configure() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.clone()).thenReturn(document);

        file = (DefaultFile) mocker.getComponentUnderTest();
        file.setDocument(document);
    }

    @Test
    public void getName()
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getFilename()).thenReturn("logo.png");
        when(file.getDocument().getAttachmentList()).thenReturn(Collections.singletonList(attachment));

        assertEquals(attachment.getFilename(), file.getName());
    }

    @Test
    public void getNameWithoutAttachment()
    {
        when(file.getDocument().getAttachmentList()).thenReturn(Collections.<XWikiAttachment>emptyList());
        when(file.getDocument().getRenderedTitle(eq(Syntax.PLAIN_1_0), any(XWikiContext.class)))
            .thenReturn("style.css");

        assertEquals("style.css", file.getName());
    }

    @Test
    public void setName() throws Exception
    {
        InputStream content = mock(InputStream.class);

        XWikiAttachment attachment = mock(XWikiAttachment.class);
        // We need to specify the previous name because the setter checks if the new name is different.
        when(attachment.getFilename()).thenReturn("old.html");
        when(attachment.getContentInputStream(any(XWikiContext.class))).thenReturn(content);
        when(file.getDocument().getAttachmentList()).thenReturn(Collections.singletonList(attachment));

        file.setName("index.html");

        verify(file.getDocument()).clone();
        verify(file.getDocument()).setTitle("index.html");
        verify(file.getDocument()).addAttachment(eq("index.html"), same(content), any(XWikiContext.class));
        verify(file.getDocument()).removeAttachment(attachment, false);
    }

    @Test
    public void setNameWithoutAttachment()
    {
        when(file.getDocument().getAttachmentList()).thenReturn(Collections.<XWikiAttachment>emptyList());
        // We need to specify the previous name because the setter checks if the new name is different.
        when(file.getDocument().getRenderedTitle(eq(Syntax.PLAIN_1_0), any(XWikiContext.class))).thenReturn("old.ttf");

        file.setName("font.ttf");

        verify(file.getDocument()).clone();
        verify(file.getDocument()).setTitle("font.ttf");
    }

    @Test
    public void getParentReferences() throws Exception
    {
        BaseObject tagObject = mock(BaseObject.class);
        when(file.getDocument().getXObject(DefaultFile.TAG_CLASS_REFERENCE)).thenReturn(tagObject);

        BaseProperty tagsProperty = mock(BaseProperty.class, withSettings().extraInterfaces(PropertyInterface.class));
        when(tagObject.get("tags")).thenReturn((PropertyInterface) tagsProperty);
        when(tagsProperty.getValue()).thenReturn(Arrays.asList("Concerto", "Resilience"));

        DocumentReference fileReference = new DocumentReference("tech", "FileSystem", "status.xml");
        when(file.getDocument().getDocumentReference()).thenReturn(fileReference);

        assertEquals(Arrays.asList(new DocumentReference("tech", "FileSystem", "Concerto"), new DocumentReference(
            "tech", "FileSystem", "Resilience")), file.getParentReferences());
    }

    @Test
    public void updateParentReferences()
    {
        BaseObject tagObject = mock(BaseObject.class);
        when(file.getDocument().getXObject(DefaultFile.TAG_CLASS_REFERENCE)).thenReturn(null, tagObject);

        when(file.getDocument().getDocumentReference()).thenReturn(
            new DocumentReference("chess", "FileSystem", "Carol"));
        DocumentReference firstParent = new DocumentReference("chess", "FileSystem", "Alice");
        DocumentReference secondParent = new DocumentReference("math", "FileSystem", "Bob");
        Collection<DocumentReference> parentReferences = file.getParentReferences();
        parentReferences.add(firstParent);
        parentReferences.add(secondParent);

        file.updateParentReferences();

        verify(tagObject).setStringListValue(DefaultFile.PROPERTY_TAGS,
            Arrays.asList(firstParent.getName(), secondParent.getName()));
        verify(file.getDocument()).clone();
        verify(file.getDocument()).setParentReference(firstParent.removeParent(firstParent.getWikiReference()));
    }

    @Test
    public void clearParentReferences()
    {
        BaseObject tagObject = mock(BaseObject.class);
        when(file.getDocument().getXObject(DefaultFile.TAG_CLASS_REFERENCE)).thenReturn(null, tagObject);

        // Initialize the parent references. Should be empty.
        Collection<DocumentReference> parentReferences = file.getParentReferences();

        file.updateParentReferences();

        verify(tagObject).setStringListValue(DefaultFile.PROPERTY_TAGS, Collections.emptyList());
        verify(file.getDocument()).clone();
        verify(file.getDocument()).setParentReference((EntityReference) null);
    }

    @Test
    public void getContent() throws Exception
    {
        ByteArrayInputStream content = new ByteArrayInputStream(new byte[] {});

        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getContentInputStream(any(XWikiContext.class))).thenReturn(content);
        when(file.getDocument().getAttachmentList()).thenReturn(Collections.singletonList(attachment));

        assertSame(content, file.getContent());
    }

    @Test
    public void getContentWithoutAttachment() throws Exception
    {
        when(file.getDocument().getAttachmentList()).thenReturn(Collections.<XWikiAttachment>emptyList());

        assertTrue(IOUtils.contentEquals(file.getContent(), new ByteArrayInputStream(new byte[] {})));
    }

    @Test
    public void getContentWithNoAttachmentContent() throws Exception
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getContentInputStream(any(XWikiContext.class))).thenThrow(XWikiException.class);
        when(file.getDocument().getAttachmentList()).thenReturn(Collections.singletonList(attachment));

        assertTrue(IOUtils.contentEquals(file.getContent(), new ByteArrayInputStream(new byte[] {})));
    }
}
