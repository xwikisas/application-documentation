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
package org.xwiki.contrib.documentation.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.documentation.DocumentationBridge;
import org.xwiki.contrib.documentation.DocumentationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for the {@link DocumentationBridge}.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
@Singleton
public class DefaultDocumentationBridge implements DocumentationBridge
{
    private static final List<String> PARENT_PATH = Arrays.asList("DocumentationApp", "Code");

    private static final LocalDocumentReference SECTION_CLASS = new LocalDocumentReference(PARENT_PATH, "SectionClass");

    private static final String NUMBERING_PROPERTY = "numbering";

    private static final String PREVIOUS_SECTION_PROPERTY = "previousSection";

    private static final String NEXT_SECTION_PROPERTY = "nextSection";


    @Inject
    private Provider<XWikiContext> xContextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public long getNumbering(DocumentReference documentReference) throws DocumentationException
    {
        try {
            XWikiContext xContext = xContextProvider.get();
            XWiki xwiki = xContext.getWiki();
            XWikiDocument doc = xwiki.getDocument(documentReference, xContext);

            BaseObject obj = doc.getXObject(
                    new DocumentReference(SECTION_CLASS, new WikiReference(xContext.getWikiId())));
            return obj.getLongValue(NUMBERING_PROPERTY);
        } catch (XWikiException e) {
            throw new DocumentationException(
                    String.format("Failed to get numbering for document [%s].", documentReference));
        }
    }

    @Override
    public void setSiblings(DocumentReference documentReference, DocumentReference previousSibling,
            DocumentReference nextSibling) throws DocumentationException
    {
        try {
            XWikiContext xContext = xContextProvider.get();
            XWiki xwiki = xContext.getWiki();
            XWikiDocument doc = xwiki.getDocument(documentReference, xContext);

            BaseObject obj = doc.getXObject(
                    new DocumentReference(SECTION_CLASS, new WikiReference(xContext.getWikiId())));

            obj.setStringValue(PREVIOUS_SECTION_PROPERTY,
                    (previousSibling == null)
                            ? entityReferenceSerializer.serialize(previousSibling)
                            : StringUtils.EMPTY);

            obj.setStringValue(NEXT_SECTION_PROPERTY,
                    (nextSibling == null)
                            ? entityReferenceSerializer.serialize(nextSibling)
                            : StringUtils.EMPTY);

            xwiki.saveDocument(doc, "Sibling update", true, xContext);
        } catch (XWikiException e) {
            throw new DocumentationException(
                    String.format("Failed to save siblings for document [%s].", documentReference));
        }
    }
}
