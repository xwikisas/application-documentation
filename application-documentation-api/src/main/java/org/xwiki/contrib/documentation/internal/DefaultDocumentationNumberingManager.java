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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.documentation.DocumentationBridge;
import org.xwiki.contrib.documentation.DocumentationException;
import org.xwiki.contrib.documentation.DocumentationNumberingManager;
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
 * This is the default implementation of the {@link DocumentationNumberingManager}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultDocumentationNumberingManager implements DocumentationNumberingManager
{
    private static final String WEB_HOME = "WebHome";

    private static final String NUMBERING_FORMAT = ".%s";

    private static final String SPACE_FILTER_FORMAT = "%s.%%.WebHome";

    private enum SectionPosition {
        PREVIOUS,
        NEXT
    }

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentationBridge documentationBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public boolean isSection(DocumentReference documentReference) throws DocumentationException
    {
        try {
            String hqlQuery = "select object.name"
                    + "from BaseObject subSectionObject where subSectionObject.className = 'SCORCode.SectionClass' "
                    + "and subSectionObject.name = :docName";
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
            List<EntityReference> referenceChain = documentReference.getReversedReferenceChain();


            StringBuilder sb = new StringBuilder();
            for (EntityReference element : referenceChain) {
                if (element instanceof DocumentReference
                        && isSection((DocumentReference) element)) {
                    sb.append(String.format(NUMBERING_FORMAT,
                                            documentationBridge.getNumbering((DocumentReference) element)));
                } else if (element instanceof SpaceReference) {
                    // Build the parent document reference
                    DocumentReference reference = new DocumentReference(WEB_HOME, (SpaceReference) element);

                    if (isSection(reference)) {
                        sb.append(String.format(NUMBERING_FORMAT,
                                                documentationBridge.getNumbering(reference)));
                    }
                }
                // In any other case, we skip
            }

            return sb.reverse().toString();
        } else {
            return StringUtils.EMPTY;
        }

    }

    @Override
    public void computeSiblings(DocumentReference documentReference) throws DocumentationException
    {
        DocumentReference previousSibling = getPreviousSibling(documentReference);
        DocumentReference nextSibling = getNextSibling(documentReference);

        documentationBridge.setSiblings(documentReference, previousSibling, nextSibling);
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
                    + "where subSectionObject.className = 'SCORCode.SectionClass' "
                    + "and subSectionNumbering.id.id = subSectionObject.id "
                    + "and subSectionNumbering.id.name = 'numbering' "
                    + "and subSectionObject.name like :parentSpace";
            return queryManager.createQuery(hqlQuery, Query.HQL)
                    .bindValue("parentSpace",
                            String.format(SPACE_FILTER_FORMAT,
                                    entityReferenceSerializer.serialize(spaceReference))).execute();
        } catch (QueryException e) {
            throw new DocumentationException(
                    String.format("Failed to get the children of the document [%s]", documentReference), e);
        }
    }

    private DocumentReference getPreviousSibling(DocumentReference documentReference) throws DocumentationException
    {

        // We get the previous section
        DocumentReference previousSectionReference = getSectionInSpace(SectionPosition.PREVIOUS, documentReference);

        if (previousSectionReference == null) {
            // Our previous sibling is our parent
            // TODO check if the parent is a space
            return new DocumentReference(WEB_HOME, (SpaceReference) documentReference.getParent());
        } else if (previousSectionReference.getParent() instanceof SpaceReference) {
            int referenceSRSize = previousSectionReference.getSpaceReferences().size();
            List<Object[]> children = getDocumentChildren(previousSectionReference);

            Map<Integer, List<Pair<DocumentReference, Long>>> resultsMatrix = new HashMap<>();
            for (Object[] result : children) {
                DocumentReference reference = documentReferenceResolver.resolve((String) result[0]);
                int spaceSize = reference.getSpaceReferences().size();

                List<Pair<DocumentReference, Long>> matrixEntry = resultsMatrix.getOrDefault(
                        referenceSRSize - spaceSize,
                        new ArrayList<>());
                matrixEntry.add(new ImmutablePair<>(reference, (long) result[1]));
            }

            return getLastSection(1, previousSectionReference.getSpaceReferences(), resultsMatrix);
        } else {
            throw new DocumentationException(String.format(
                    "The parent of [%s] is not a space reference : [%s]",
                    previousSectionReference, previousSectionReference.getParent()));
        }
    }

    private DocumentReference getNextSibling(DocumentReference documentReference) throws DocumentationException
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
                nextSectionReference = getSectionInSpace(SectionPosition.NEXT, currentReference);

                if (nextSectionReference == null) {
                    // Go up one level
                    currentReference =
                            new DocumentReference(WEB_HOME,
                                    (SpaceReference) documentReference.getLastSpaceReference().getParent());
                }
            } while (nextSectionReference == null || currentReference.getSpaceReferences().size() > 0);

            return nextSectionReference;
        }
    }

    private DocumentReference getSectionInSpace(SectionPosition position, DocumentReference documentReference)
            throws DocumentationException {
        if (isSection(documentReference)) {
            long sectionNumbering =
                    (position == SectionPosition.PREVIOUS)
                            ? documentationBridge.getNumbering(documentReference) - 1
                            : documentationBridge.getNumbering(documentReference) + 1;

            /* For computing the name of the space of the current doc that we'll use here,
               we call EntityReference#parent twice as we are working with nested pages
               One #parent() will remove .WebHome
               The other one will define the space
             */
            String currentSpace = entityReferenceSerializer.serialize(documentReference.getParent().getParent());

            try {
                String hqlQuery = "select sectionObject.name "
                        + "from BaseObject sectionObject, LongProperty sectionNumbering "
                        + "where sectionObject.className = 'SCORCode.SectionClass' "
                        + "and sectionNumbering.id.id = sectionObject.id "
                        + "and sectionNumbering.id.name = 'numbering' "
                        + "and sectionNumbering.value = :nextNumbering "
                        + "and sectionObject.name like :currentSpace";

                List<String> results = queryManager.createQuery(hqlQuery, Query.HQL)
                        .bindValue("nextNumbering", sectionNumbering)
                        .bindValue("currentSpace", String.format(SPACE_FILTER_FORMAT, currentSpace))
                        .setLimit(1).execute();

                if (results.size() >= 1) {
                    return documentReferenceResolver.resolve(results.get(0));
                } else {
                    return null;
                }

            } catch (QueryException e) {
                throw new DocumentationException(
                        String.format("Failed to get the [%s] section in space [%s]", position, currentSpace), e);
            }
        } else {
            throw new DocumentationException(
                    String.format("The document [%s] has no numbering.", documentReference));
        }
    }
}
