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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.documentation.DocumentationBridge;
import org.xwiki.contrib.documentation.DocumentationException;
import org.xwiki.contrib.documentation.SectionNumberingManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.text.StringUtils;

/**
 * This is the default implementation of the {@link SectionNumberingManager}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultSectionNumberingManager implements SectionNumberingManager
{
    private static final String WEB_HOME = "WebHome";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentationBridge documentationBridge;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public boolean isSection(DocumentReference documentReference) throws DocumentationException
    {
        try {
            String hqlQuery = "select sectionObject.name from BaseObject sectionObject "
                    + "where sectionObject.className = 'Documentation.Code.SectionClass' "
                    + "and sectionObject.name = :docName";
            return queryManager.createQuery(hqlQuery, Query.HQL)
                    .bindValue("docName", entityReferenceSerializer.serialize(documentReference))
                    .execute().size() > 0;
        } catch (QueryException e) {
            throw new DocumentationException(
                    String.format("Failed to verify if document [%s] is a section.", documentReference), e);
        }
    }

    @Override
    public String getFullNumbering(DocumentReference documentReference) throws DocumentationException
    {
        // Check fist if the current document has a numbering, else it would be inefficient to load other docs
        // in memory.
        if (isSection(documentReference)) {
            // Get the current numbering
            long currentDocumentNumbering = documentationBridge.getNumbering(documentReference);

            // Compute the parent document of the current Document.
            // We call getParent() twice as we are using nested pages
            EntityReference parentSpaceReference = documentReference.getParent().getParent();

            if (parentSpaceReference instanceof SpaceReference) {
                String parentFullNumbering =
                        getFullNumbering(new DocumentReference(WEB_HOME, (SpaceReference) parentSpaceReference));

                return formatNumbering(parentFullNumbering, currentDocumentNumbering);
            } else {
                // We are no top of the wiki, having WikiReferences instead of SpaceReferences.
                return StringUtils.EMPTY;
            }
        } else {
            return StringUtils.EMPTY;
        }

    }

    @Override
    public String getFullNumbering(DocumentReference previousSiblingReference, DocumentReference parentSectionReference)
            throws DocumentationException
    {
        String parentFullNumbering = getFullNumbering(parentSectionReference);
        long extrapolatedNumbering = getNumberingFromSibling(previousSiblingReference);
        return formatNumbering(parentFullNumbering, extrapolatedNumbering);

    }

    private String formatNumbering(String parentFullNumbering, long currentNumbering)
    {
        if (parentFullNumbering.isEmpty()) {
            return String.format("%s.", currentNumbering);
        } else {
            return String.format("%s%s.", parentFullNumbering, currentNumbering);
        }
    }

    @Override
    public long getNumberingFromSibling(DocumentReference previousSiblingReference)
            throws DocumentationException
    {
        if (previousSiblingReference == null) {
            return 1;
        } else if (isSection(previousSiblingReference)) {
            return documentationBridge.getNumbering(previousSiblingReference) + 1;
        } else {
            throw new DocumentationException(
                    String.format("The given sibling [%s] is not a section.", previousSiblingReference));
        }
    }
}
