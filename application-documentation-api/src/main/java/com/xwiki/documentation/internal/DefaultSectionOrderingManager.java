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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import com.xwiki.documentation.DocumentationBridge;
import com.xwiki.documentation.DocumentationException;
import com.xwiki.documentation.SectionNumberingManager;
import com.xwiki.documentation.SectionOrderingManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * This is the default implementation for {@link SectionOrderingManager}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultSectionOrderingManager implements SectionOrderingManager
{
    private static final String WEB_HOME = "WebHome";

    private static final String SPACE_FILTER_FORMAT = "%s.%%.WebHome";

    private enum SiblingPosition
    {
        PREVIOUS,
        NEXT
    }

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
    private SectionNumberingManager sectionNumberingManager;

    @Override
    public void computePreviousAndNextSections(DocumentReference documentReference)
            throws DocumentationException
    {
        DocumentReference previousSection = computePreviousSection(documentReference);
        DocumentReference nextSection = computeNextSection(documentReference);

        documentationBridge.setPreviousAndNextSections(documentReference, previousSection, nextSection);
    }

    @Override
    public DocumentReference computePreviousSection(DocumentReference documentReference)
            throws DocumentationException
    {
        // We get the previous section in the current space
        DocumentReference previousSiblingReference = getSectionInSpace(SiblingPosition.PREVIOUS, documentReference);

        if (previousSiblingReference == null) {
            // First case, we don't have any previous sibling, this would mean that our previous section should be
            // the parent of the current section
            DocumentReference parentDocumentReference =
                    new DocumentReference(WEB_HOME, (SpaceReference) documentReference.getParent().getParent());
            if (sectionNumberingManager.isSection(parentDocumentReference)) {
                return parentDocumentReference;
            } else {
                // We're out of the documentation
                return null;
            }

        } else if (previousSiblingReference.getParent() instanceof SpaceReference) {
            // In that case, we try to get the latest subsection of the previous section
            List<Object[]> children = getDocumentChildren(previousSiblingReference);

            // The previous sibling has no children, this means that it is our previous section
            if (children.size() == 0) {
                return previousSiblingReference;
            } else {
                int referenceSRSize = previousSiblingReference.getSpaceReferences().size();
                Map<Integer, List<Pair<DocumentReference, Long>>> resultsMatrix = new HashMap<>();
                for (Object[] result : children) {
                    DocumentReference reference = documentReferenceResolver.resolve((String) result[0]);
                    int spaceDifference = reference.getSpaceReferences().size() - referenceSRSize;

                    List<Pair<DocumentReference, Long>> matrixEntry = resultsMatrix.getOrDefault(
                            spaceDifference,
                            new ArrayList<>());
                    matrixEntry.add(new ImmutablePair<>(reference, (long) result[1]));
                    resultsMatrix.put(spaceDifference, matrixEntry);
                }

                return getLastSection(1, previousSiblingReference.getSpaceReferences(), resultsMatrix);
            }
        } else {
            throw new DocumentationException(String.format(
                    "The parent of [%s] is not a space reference : [%s]",
                    previousSiblingReference, previousSiblingReference.getParent()));
        }
    }

    @Override
    public DocumentReference computeNextSection(DocumentReference documentReference)
            throws DocumentationException
    {
        // We need to get the first child.
        int referenceSRSize = documentReference.getSpaceReferences().size();


        List<Object[]> children = getDocumentChildren(documentReference);

        if (children.size() > 0) {
            for (Object[] result : children) {
                DocumentReference reference = documentReferenceResolver.resolve((String) result[0]);
                int spaceSize = reference.getSpaceReferences().size();

                if (spaceSize - referenceSRSize == 1 && (long) result[1] == 1) {
                    return reference;
                }
            }

            // If we arrive there, we have a problem
            return null;
        } else {
            DocumentReference nextSectionReference;
            DocumentReference currentReference = documentReference;
            do {
                nextSectionReference = getSectionInSpace(SiblingPosition.NEXT, currentReference);

                if (nextSectionReference == null) {
                    // Go up one level
                    currentReference =
                            new DocumentReference(WEB_HOME,
                                    (SpaceReference) currentReference.getParent().getParent());
                }
            } while (nextSectionReference == null
                    && sectionNumberingManager.isSection(currentReference)
                    && currentReference.getSpaceReferences().size() > 0);

            return nextSectionReference;
        }
    }

    private DocumentReference getSectionInSpace(SiblingPosition position, DocumentReference documentReference)
            throws DocumentationException {
        if (sectionNumberingManager.isSection(documentReference)) {
            long sectionNumbering =
                    (position == SiblingPosition.PREVIOUS)
                            ? documentationBridge.getNumbering(documentReference) - 1
                            : documentationBridge.getNumbering(documentReference) + 1;

            EntityReference parentSpace = documentReference.getParent().getParent();
            if (parentSpace instanceof SpaceReference) {
                // Compute the reference of the parent
                DocumentReference parentReference = new DocumentReference(WEB_HOME, (SpaceReference) parentSpace);

                try {
                    String hqlQuery = "select obj.name "
                            + "from BaseObject obj, LongProperty numbering, StringProperty parent "
                            + "where obj.className =  'Documentation.Code.SectionClass' "
                            + "and numbering.id.id = obj.id "
                            + "and numbering.id.name = 'numbering' "
                            + "and parent.id.id = obj.id "
                            + "and parent.id.name = 'parentSection' "
                            + "and numbering.value = :expectedNumbering "
                            + "and parent.value = :parentReference "
                            // Exclude everything from the code space
                            + "and obj.name not like 'Documentation.Code.%'";

                    List<String> results = queryManager.createQuery(hqlQuery, Query.HQL)
                            .bindValue("expectedNumbering", sectionNumbering)
                            .bindValue("parentReference", entityReferenceSerializer.serialize(parentReference))
                            .execute();

                    if (results.size() >= 1) {
                        for (String result : results) {
                            DocumentReference resultReference = documentReferenceResolver.resolve(result);
                            // Return only the first result (if we have more than one, it means that something went
                            // wrong when creating pages
                            return resultReference;
                        }

                        // In case we didn't found anything, then it means that we have no other section available.
                        return null;
                    } else {
                        return null;
                    }

                } catch (QueryException e) {
                    throw new DocumentationException(
                            String.format("Failed to get the [%s] section in space [%s]", position, parentSpace), e);
                }
            } else {
                throw new DocumentationException("Creating a documentation at the root of the wiki is currently "
                        + "not supported, please create your documentation in a space.");
            }

        } else {
            throw new DocumentationException(
                    String.format("The document [%s] has no numbering.", documentReference));
        }
    }

    private DocumentReference getLastSection(int level,
            List<SpaceReference> spaces, Map<Integer, List<Pair<DocumentReference, Long>>> matrix) {

        if (matrix.keySet().contains(level)) {
            // Consider the current level
            List<Pair<DocumentReference, Long>> levelList = matrix.get(level);

            // Get the highest numbering
            long bestNumbering = 0;
            DocumentReference bestNumberingReference = null;

            for (Pair<DocumentReference, Long> element : levelList) {
                if (element.getLeft().getSpaceReferences().containsAll(spaces) && element.getRight() > bestNumbering) {
                    bestNumberingReference = element.getLeft();
                    bestNumbering = element.getRight();
                }
            }

            // If nothing has changed : we don't have any children in there
            if (bestNumbering == 0) {
                return null;
            } else {
                DocumentReference subtreeTest = getLastSection(level + 1,
                        bestNumberingReference.getSpaceReferences(), matrix);

                if (subtreeTest == null) {
                    return bestNumberingReference;
                } else {
                    return subtreeTest;
                }
            }
        } else {
            return null;
        }
    }

    private List<Object[]> getDocumentChildren(DocumentReference documentReference) throws DocumentationException
    {
        try {
            SpaceReference spaceReference = (SpaceReference) documentReference.getParent();

            // Select every child elements of this space
            String hqlQuery = "select subSectionObject.name, subSectionNumbering.value "
                    + "from BaseObject subSectionObject, LongProperty subSectionNumbering "
                    + "where subSectionObject.className = 'Documentation.Code.SectionClass' "
                    + "and subSectionNumbering.id.id = subSectionObject.id "
                    + "and subSectionNumbering.id.name = 'numbering' "
                    + "and subSectionObject.name like :parentSpace "
                    // Exclude everything from the code space
                    + "and subSectionObject.name not like 'Documentation.Code.%'";
            return queryManager.createQuery(hqlQuery, Query.HQL)
                    .bindValue("parentSpace",
                            String.format(SPACE_FILTER_FORMAT,
                                    entityReferenceSerializer.serialize(spaceReference))).execute();
        } catch (QueryException e) {
            throw new DocumentationException(
                    String.format("Failed to get the children of the document [%s]", documentReference), e);
        }
    }
}
