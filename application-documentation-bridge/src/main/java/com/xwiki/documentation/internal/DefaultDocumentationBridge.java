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
package com.xwiki.documentation.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.documentation.DocumentationBridge;
import com.xwiki.documentation.DocumentationException;

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
    static final List<String> PARENT_PATH = Arrays.asList("Documentation", "Code");

    static final LocalDocumentReference SECTION_CLASS = new LocalDocumentReference(PARENT_PATH, "SectionClass");

    static final String NUMBERING_PROPERTY = "numbering";

    static final String PREVIOUS_SECTION_PROPERTY = "previousSection";

    static final String NEXT_SECTION_PROPERTY = "nextSection";

    static final String PARENT_SECTION_PROPERTY = "parentSection";

    static final String IS_INCLUDED_IN_EXPORTS_PROPERTY = "includeInExports";

    @Inject
    private Provider<XWikiContext> xContextProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

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
    public void setNumbering(DocumentReference documentReference, long numbering) throws DocumentationException
    {
        try {
            XWikiContext xContext = xContextProvider.get();
            XWiki xwiki = xContext.getWiki();
            XWikiDocument doc = xwiki.getDocument(documentReference, xContext);

            BaseObject obj = doc.getXObject(
                    new DocumentReference(SECTION_CLASS, new WikiReference(xContext.getWikiId())));
            if (obj != null) {
                obj.setLongValue(NUMBERING_PROPERTY, numbering);

                xwiki.saveDocument(doc, "Section numbering update", true, xContext);
            } else {
                throw new DocumentationException(String.format("Document [%s] is not a documentation section.",
                        documentReference));
            }
        } catch (XWikiException e) {
            throw new DocumentationException(
                    String.format("Failed to set numbering for document [%s].", documentReference));
        }
    }

    @Override
    public void setPreviousAndNextSections(DocumentReference documentReference, DocumentReference previousSection,
            DocumentReference nextSection) throws DocumentationException
    {
        updateDocumentSections(documentReference,
                new HashMap<String, DocumentReference>() {{
                    put(PREVIOUS_SECTION_PROPERTY, previousSection);
                    put(NEXT_SECTION_PROPERTY, nextSection);
                }}
        );
    }

    @Override public void setPreviousSection(DocumentReference documentReference, DocumentReference previousSection)
            throws DocumentationException
    {
        updateDocumentSections(documentReference,
                new HashMap<String, DocumentReference>() {{
                    put(PREVIOUS_SECTION_PROPERTY, previousSection);
                }}
        );
    }

    @Override public void setNextSection(DocumentReference documentReference, DocumentReference nextSection)
            throws DocumentationException
    {
        updateDocumentSections(documentReference,
                new HashMap<String, DocumentReference>() {{
                    put(NEXT_SECTION_PROPERTY, nextSection);
                }}
        );
    }

    @Override
    public void setParent(DocumentReference documentReference, DocumentReference parentSection)
            throws DocumentationException
    {
        updateDocumentSections(documentReference,
                new HashMap<String, DocumentReference>() {{
                    put(PARENT_SECTION_PROPERTY, parentSection);
                }}
        );
    }

    @Override
    public DocumentReference getParentSection(DocumentReference documentReference)
            throws DocumentationException
    {
        try {
            XWikiContext xContext = xContextProvider.get();
            XWiki xwiki = xContext.getWiki();
            XWikiDocument doc = xwiki.getDocument(documentReference, xContext);

            BaseObject obj = doc.getXObject(
                    new DocumentReference(SECTION_CLASS, new WikiReference(xContext.getWikiId())));
            String parentSection = obj.getStringValue(PARENT_SECTION_PROPERTY);

            if (parentSection != null && !StringUtils.isBlank(parentSection)) {
                return stringDocumentReferenceResolver.resolve(parentSection);
            } else {
                return null;
            }
        } catch (XWikiException e) {
            throw new DocumentationException(
                    String.format("Failed to get parent section for document [%s].", documentReference));
        }
    }

    @Override
    public boolean getIsIncludedInExports(DocumentReference documentReference, boolean withUpperSpacesCheck)
            throws DocumentationException
    {
        try {
            XWikiContext xContext = xContextProvider.get();
            XWiki xwiki = xContext.getWiki();
            XWikiDocument doc = xwiki.getDocument(documentReference, xContext);
            if (withUpperSpacesCheck) {
                // Check that all parent spaces are included
                List<SpaceReference> spaceReferences = documentReference.getSpaceReferences();
                for (SpaceReference spaceReference : spaceReferences) {
                    boolean flag = getIsIncludedInExports(new DocumentReference("WebHome", spaceReference), false);
                    if (!flag) {
                        return false;
                    }
                }
            }

            BaseObject obj = doc.getXObject(
                    new DocumentReference(SECTION_CLASS, new WikiReference(xContext.getWikiId())));
            if (obj == null) {
                return true;
            }
            return obj.getIntValue(IS_INCLUDED_IN_EXPORTS_PROPERTY, 1) == 1;
        } catch (XWikiException e) {
            throw new DocumentationException(
                    String.format("Failed to get isIncludedInExports boolean for document [%s].", documentReference));
        }
    }

    /**
     * @param documentReference a section
     * @param propertyName either "previous" or "next"
     * @return the previous or next included section
     * @throws DocumentationException in case an error occurs
     */
    public DocumentReference getPreviousOrNextIncludedSection(DocumentReference documentReference, String propertyName)
            throws DocumentationException
    {
        XWikiContext xContext = xContextProvider.get();
        XWiki wiki = xContext.getWiki();

        try {
            XWikiDocument doc = wiki.getDocument(documentReference, xContext);
            BaseObject obj = doc.getXObject(
                    new DocumentReference(SECTION_CLASS, new WikiReference(xContext.getWikiId())));

            if (obj != null) {
                String previousOrNextSection = obj.getStringValue(propertyName);
                if (StringUtils.isNotEmpty(previousOrNextSection)) {
                    DocumentReference previousOrNextSectionReference =
                            stringDocumentReferenceResolver.resolve(previousOrNextSection);
                    if (getIsIncludedInExports(previousOrNextSectionReference, true)) {
                        return previousOrNextSectionReference;
                    } else {
                        return getPreviousOrNextIncludedSection(previousOrNextSectionReference, propertyName);
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (XWikiException e) {
            throw new DocumentationException(
                    String.format("Failed to get previous / next included section for [%s].", documentReference));
        }
    }

    private void updateDocumentSections(DocumentReference documentReference, Map<String, DocumentReference> sections)
            throws DocumentationException
    {
        try {
            XWikiContext xContext = xContextProvider.get();
            XWiki xwiki = xContext.getWiki();
            XWikiDocument doc = xwiki.getDocument(documentReference, xContext);

            BaseObject obj = doc.getXObject(
                    new DocumentReference(SECTION_CLASS, new WikiReference(xContext.getWikiId())));

            for (String property : sections.keySet()) {
                setSection(obj, property, sections.getOrDefault(property, null));
            }

            xwiki.saveDocument(doc, "Section metadata update", true, xContext);
        } catch (XWikiException e) {
            throw new DocumentationException(
                    String.format("Failed to save document [%s].", documentReference));
        }
    }

    private void setSection(BaseObject object, String property, DocumentReference reference)
    {
        object.setStringValue(property,
                (reference != null)
                        ? entityReferenceSerializer.serialize(reference)
                        : StringUtils.EMPTY);
    }
}
