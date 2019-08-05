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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.documentation.DocumentationBridge;
import org.xwiki.contrib.documentation.DocumentationException;
import org.xwiki.contrib.documentation.SectionNumberingManager;
import org.xwiki.contrib.documentation.SectionOrderingManager;
import org.xwiki.contrib.documentation.SectionParentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Will listen for deleted or refactored document to update the section metadata associated with the documents.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(SectionEventListener.LISTENER_NAME)
@Singleton
public class SectionEventListener implements EventListener
{
    /**
     * The name of the listener.
     */
    public static final String LISTENER_NAME = "DocumentationSection";

    @Inject
    private Logger logger;

    @Inject
    private SectionNumberingManager sectionNumberingManager;

    @Inject
    private SectionParentManager sectionParentManager;

    @Inject
    private SectionOrderingManager sectionOrderingManager;

    @Inject
    private DocumentationBridge documentationBridge;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String getName()
    {
        return LISTENER_NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.asList(new DocumentDeletedEvent(), new DocumentCreatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentDeletedEvent || event instanceof DocumentCreatedEvent) {
            XWikiDocument document = (XWikiDocument) source;
            XWikiContext xContext = (XWikiContext) data;

            DocumentReference classReference =
                    new DocumentReference(
                            DefaultDocumentationBridge.SECTION_CLASS,
                            new WikiReference(xContext.getWikiId()));

            if (document.getXObjects(classReference).size() > 0) {
                if (event instanceof DocumentDeletedEvent) {
                    updateNextAndPreviousSections(document.getOriginalDocument(), classReference);
                    updateSpaceNumbering(document.getOriginalDocument());
                } else if (event instanceof  DocumentCreatedEvent) {
                    setupNewDocument(document, classReference, xContext);
                }
            }
        }
    }

    private void setupNewDocument(XWikiDocument document, DocumentReference classReference, XWikiContext xWikiContext) {
        try {
            // Compute the parent
            sectionParentManager.computeParentSection(document.getDocumentReference());
            // Refresh the numbering of the space
            sectionNumberingManager.recomputeNumberingsFromSection(document.getDocumentReference());
            // Compute the previous and next sections
            sectionOrderingManager.computePreviousAndNextSections(document.getDocumentReference());

            // Reload the document
            XWikiDocument updatedDocument = xWikiContext.getWiki()
                    .getDocument(document.getDocumentReference(), xWikiContext);
            // Update the previous and next sections attributes to match on the current document
            String previousSectionFullName = updatedDocument.getStringValue(classReference,
                    DefaultDocumentationBridge.PREVIOUS_SECTION_PROPERTY);
            String nextSectionFullName = updatedDocument.getStringValue(classReference,
                    DefaultDocumentationBridge.NEXT_SECTION_PROPERTY);

            // Test if we're not at the start of the documentation
            if (!StringUtils.isBlank(previousSectionFullName)) {
                DocumentReference previousSection = documentReferenceResolver.resolve(previousSectionFullName);

                documentationBridge.setNextSection(previousSection, updatedDocument.getDocumentReference());
            }

            // Test if we're not at the end of the documentation
            if (!StringUtils.isBlank(nextSectionFullName)) {
                DocumentReference nextSection = documentReferenceResolver.resolve(nextSectionFullName);

                documentationBridge.setPreviousSection(nextSection, updatedDocument.getDocumentReference());
            }

        } catch (DocumentationException | XWikiException e) {
            logger.error("Failed to set up section document [{}] : [{}]", document,
                    getRootCauseMessage(e));
        }
    }

    private void updateNextAndPreviousSections(XWikiDocument document, DocumentReference classReference) {
        try {
            // Get the next document reference
            String previousSectionFullName = document.getStringValue(classReference,
                    DefaultDocumentationBridge.PREVIOUS_SECTION_PROPERTY);
            String nextSectionFullName = document.getStringValue(classReference,
                    DefaultDocumentationBridge.NEXT_SECTION_PROPERTY);

            DocumentReference previousSection = (StringUtils.isBlank(previousSectionFullName))
                    ? null : documentReferenceResolver.resolve(previousSectionFullName);
            DocumentReference nextSection = (StringUtils.isBlank(nextSectionFullName))
                    ? null : documentReferenceResolver.resolve(nextSectionFullName);

            if (previousSection != null) {
                documentationBridge.setNextSection(previousSection, nextSection);
            }

            if (nextSection != null) {
                documentationBridge.setPreviousSection(nextSection, previousSection);
            }

        } catch (Exception e) {
            logger.error("Failed to update previous and next sections for document [{}] : [{}]", document,
                    getRootCauseMessage(e));
        }
    }

    private void updateSpaceNumbering(XWikiDocument document) {
        try {
            // Compute the reference of the parent document.
            EntityReference parentSpaceReference = document.getDocumentReference().getParent().getParent();

            if (parentSpaceReference instanceof SpaceReference) {
                sectionNumberingManager.recomputeNumberingsFromParentSection(
                        new DocumentReference("WebHome", (SpaceReference) parentSpaceReference));
            }
        } catch (Exception e) {
            logger.error("Failed to update the numbering of the other sections in the space (siblings of [{}]) : [{}]",
                    document, getRootCauseMessage(e));
        }
    }
}
