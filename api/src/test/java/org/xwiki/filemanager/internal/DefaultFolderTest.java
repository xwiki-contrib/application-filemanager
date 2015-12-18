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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.filemanager.Folder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultFolder}.
 * 
 * @version $Id$
 * @since 2.0.5
 */
public class DefaultFolderTest
{
    @Rule
    public MockitoComponentMockingRule<Folder> mocker = new MockitoComponentMockingRule<Folder>(DefaultFolder.class);

    private DefaultFolder folder;

    private QueryManager queryManager;

    @Before
    public void configure() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.clone()).thenReturn(document);

        folder = (DefaultFolder) mocker.getComponentUnderTest();
        folder.setDocument(document);

        queryManager = mocker.getInstance(QueryManager.class);
    }

    @Test
    public void getChildFolderReferences() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("one", "Two", "Three");
        when(folder.getDocument().getDocumentReference()).thenReturn(documentReference);

        Query query = mock(Query.class);
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.<Object>asList("A", "B"));

        assertEquals(2, folder.getChildFolderReferences().size());

        verify(query).setWiki(documentReference.getWikiReference().getName());

        DocumentReferenceResolver<String> explicitDocumentReferenceResolver =
            mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "explicit");
        verify(explicitDocumentReferenceResolver).resolve("A", documentReference);
        verify(explicitDocumentReferenceResolver).resolve("B", documentReference);
    }
}
