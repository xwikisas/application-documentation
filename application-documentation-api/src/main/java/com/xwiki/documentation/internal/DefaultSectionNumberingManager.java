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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import com.xwiki.documentation.DocumentationBridge;
import com.xwiki.documentation.DocumentationException;
import com.xwiki.documentation.SectionManager;
import com.xwiki.documentation.SectionNumberingManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
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

    private static final String PARENT_DOCUMENT = "parentDocument";

    private static final String SELECT_OBJ_NAME_NUMBERING_VALUE = "select obj.name, numbering.value ";

    private static final String FROM_OBJ_PARENT_NUMBERING = "from BaseObject obj, "
            + "StringProperty parent, LongProperty numbering ";

    private static final String WHERE_OBJ_CLASSNAME_SECTIONCLASS = "where obj.className = "
            + "'Documentation.Code.SectionClass' ";

    private static final String OBJ_ID_PARENT_EQUALS = "and obj.id = parent.id.id ";

    private static final String PARENT_NAME_EQUALS = "and parent.id.name = 'parentSection' ";

    private static final String OBJ_ID_NUMBERING_EQUALS = "and obj.id = numbering.id.id ";

    private static final String NUMBERING_NAME_EQUALS = "and numbering.id.name = 'numbering' ";

    private static final String PARENT_VALUE_EQUALS = "and parent.value = :" + PARENT_DOCUMENT + " ";

    private static final String ORDER_BY_NUMBERING_ASC = "order by numbering.value asc";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentationBridge documentationBridge;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private SectionManager sectionManager;

    @Override
    public String getFullNumbering(DocumentReference documentReference) throws DocumentationException
    {
        // Check fist if the current document has a numbering, else it would be inefficient to load other docs
        // in memory.
        if (sectionManager.isSection(documentReference)) {
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
        } else if (sectionManager.isSection(previousSiblingReference)) {
            return documentationBridge.getNumbering(previousSiblingReference) + 1;
        } else {
            throw new DocumentationException(
                    String.format("The given sibling [%s] is not a section.", previousSiblingReference));
        }
    }

    @Override
    public void recomputeNumberingsFromSection(DocumentReference documentReference)
            throws DocumentationException
    {
        // We first need need to get the numbering of the current document
        long numbering = documentationBridge.getNumbering(documentReference);

        // Compute the parent reference
        EntityReference parentSpaceReference = documentReference.getParent().getParent();

        if (parentSpaceReference instanceof SpaceReference) {
            DocumentReference parentReference = new DocumentReference(WEB_HOME,
                    (SpaceReference) parentSpaceReference);

            try {
                // Get every document in the space
                Query query = queryManager.createQuery(SELECT_OBJ_NAME_NUMBERING_VALUE
                        + FROM_OBJ_PARENT_NUMBERING
                        + WHERE_OBJ_CLASSNAME_SECTIONCLASS
                        + OBJ_ID_PARENT_EQUALS
                        + PARENT_NAME_EQUALS
                        + OBJ_ID_NUMBERING_EQUALS
                        + NUMBERING_NAME_EQUALS
                        + "and numbering.value >= :numbering "
                        + "and obj.name <> :currentDocument "
                        + PARENT_VALUE_EQUALS
                        + ORDER_BY_NUMBERING_ASC, Query.HQL);

                query.bindValue("numbering", numbering);
                query.bindValue("currentDocument", entityReferenceSerializer.serialize(documentReference));
                query.bindValue(PARENT_DOCUMENT, entityReferenceSerializer.serialize(parentReference));

                // So, we get a list of sections
                List<Object[]> results = query.execute();

                // The first next section that we should encounter should have the numbering of the current section + 1
                updateNumbering(results, numbering + 1);
            } catch (QueryException e) {
                throw new DocumentationException(
                        String.format("Failed to fetch the list of siblings from [%s], which are children of [%s]",
                                documentReference, parentReference), e);
            }
        }
    }

    @Override
    public void recomputeNumberingsFromParentSection(DocumentReference documentReference)
            throws DocumentationException
    {
        try {
            // Get every document in the space
            Query query = queryManager.createQuery(SELECT_OBJ_NAME_NUMBERING_VALUE
                    + FROM_OBJ_PARENT_NUMBERING
                    + WHERE_OBJ_CLASSNAME_SECTIONCLASS
                    + OBJ_ID_PARENT_EQUALS
                    + PARENT_NAME_EQUALS
                    + OBJ_ID_NUMBERING_EQUALS
                    + NUMBERING_NAME_EQUALS
                    + PARENT_VALUE_EQUALS
                    + ORDER_BY_NUMBERING_ASC, Query.HQL);

            query.bindValue(PARENT_DOCUMENT, entityReferenceSerializer.serialize(documentReference));

            List<Object[]> results = query.execute();

            updateNumbering(results, 1L);
        } catch (QueryException e) {
            throw new DocumentationException(
                    String.format("Failed to fetch the list of children of [%s]", documentReference), e);
        }
    }

    private void updateNumbering(List<Object[]> results, long baseNumbering) throws DocumentationException
    {
        Long bn = baseNumbering;

        for (Object[] result : results) {
            if (!bn.equals(result[1])) {
                documentationBridge.setNumbering(
                        documentReferenceResolver.resolve((String) result[0]), bn);
            }

            bn++;
        }
    }
}
