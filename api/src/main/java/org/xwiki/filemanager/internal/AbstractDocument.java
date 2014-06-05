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

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.filemanager.Document;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default {@link Document} implementation, based on {@link XWikiDocument}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractDocument implements Document
{
    /**
     * Used to log messages.
     */
    @Inject
    protected Logger logger;

    /**
     * Provides the XWiki context.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * The underlying {@link XWikiDocument} that defines this file system document.
     */
    private XWikiDocument document;

    @Override
    public DocumentReference getReference()
    {
        return document.getDocumentReference();
    }

    @Override
    public String getName()
    {
        return document.getRenderedTitle(Syntax.PLAIN_1_0, getContext());
    }

    @Override
    public void setName(String name)
    {
        document.setTitle(name);
    }

    /**
     * @return the underlying {@link XWikiDocument} that defines this file system document
     */
    XWikiDocument getDocument()
    {
        return document;
    }

    /**
     * Sets the underlying {@link XWikiDocument} that defines this file system document.
     * 
     * @param document the underlying document
     */
    void setDocument(XWikiDocument document)
    {
        this.document = document;
    }

    /**
     * @return the XWiki context
     */
    protected XWikiContext getContext()
    {
        return xcontextProvider.get();
    }
}
