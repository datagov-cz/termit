/**
 * TermIt Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.ResourceExistsException;
import cz.cvut.kbss.termit.exception.ValidationException;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.Model;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.model.assignment.FileOccurrenceTarget;
import cz.cvut.kbss.termit.model.assignment.TermFileOccurrence;
import cz.cvut.kbss.termit.model.assignment.TermOccurrence;
import cz.cvut.kbss.termit.model.resource.Document;
import cz.cvut.kbss.termit.model.resource.File;
import cz.cvut.kbss.termit.model.resource.Resource;
import cz.cvut.kbss.termit.model.selector.Selector;
import cz.cvut.kbss.termit.model.selector.TextQuoteSelector;
import cz.cvut.kbss.termit.persistence.context.DescriptorFactory;
import cz.cvut.kbss.termit.service.BaseServiceTestRunner;
import cz.cvut.kbss.termit.util.Vocabulary;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceRepositoryServiceTest extends BaseServiceTestRunner {

    @Autowired
    private DescriptorFactory descriptorFactory;

    @Autowired
    private EntityManager em;

    @Autowired
    private ResourceRepositoryService sut;

    private User user;

    @BeforeEach
    void setUp() {
        this.user = Generator.generateUserWithId();
        transactional(() -> em.persist(user));
        Environment.setCurrentUser(user);
    }

    private Term generateTermWithUriAndPersist() {
        final Term t = Generator.generateTerm();
        t.setUri(Generator.generateUri());
        transactional(() -> em.persist(t));
        return t;
    }

    @Test
    void persistThrowsValidationExceptionWhenResourceLabelIsMissing() {
        final Resource resource = Generator.generateResourceWithId();
        resource.setLabel(null);
        assertThrows(ValidationException.class, () -> sut.persist(resource));
    }

    @Test
    void removeDeletesOccurrenceTargetsAndTermOccurrencesAssociatedWithResource() {
        enableRdfsInference(em);
        final File file = Generator.generateFileWithId("test.txt");
        transactional(() -> em.persist(file));
        final Term tOne = generateTermWithUriAndPersist();
        final FileOccurrenceTarget target = new FileOccurrenceTarget(file);
        final Selector selector = new TextQuoteSelector("test");
        target.setSelectors(Collections.singleton(selector));
        final TermOccurrence occurrence = new TermFileOccurrence(tOne.getUri(), target);
        transactional(() -> {
            final Descriptor descriptor = new EntityDescriptor(occurrence.resolveContext());
            em.persist(target, descriptor);
            em.persist(occurrence, descriptor);
        });

        sut.remove(file);
        assertNull(em.find(File.class, file.getUri()));
        verifyInstancesDoNotExist(Vocabulary.s_c_vyskyt_termu, em);
        verifyInstancesDoNotExist(Vocabulary.s_c_cil_vyskytu, em);
        verifyInstancesDoNotExist(Vocabulary.s_c_selektor_text_quote, em);
    }

    @Test
    void removeDeletesTermOccurrencesAndAllTargetsAssociatedWithResource() {
        enableRdfsInference(em);
        final File file = Generator.generateFileWithId("test.txt");
        transactional(() -> em.persist(file));
        final Term tOne = generateTermWithUriAndPersist();
        final FileOccurrenceTarget occurrenceTarget = new FileOccurrenceTarget(file);
        final Selector selector = new TextQuoteSelector("test");
        occurrenceTarget.setSelectors(Collections.singleton(selector));
        final TermOccurrence occurrence = new TermFileOccurrence(tOne.getUri(), occurrenceTarget);
        transactional(() -> {
            final Descriptor descriptor = new EntityDescriptor(occurrence.resolveContext());
            em.persist(occurrenceTarget, descriptor);
            em.persist(occurrence, descriptor);
        });

        sut.remove(file);
        verifyInstancesDoNotExist(Vocabulary.s_c_vyskyt_termu, em);
        verifyInstancesDoNotExist(Vocabulary.s_c_cil_vyskytu, em);
        verifyInstancesDoNotExist(Vocabulary.s_c_selektor_text_quote, em);
    }

    @Test
    void updateSupportsSubclassesOfResource() {
        final Document doc = Generator.generateDocumentWithId();
        final File fileOne = Generator.generateFileWithId("test.html");
        doc.addFile(fileOne);
        final File fileTwo = Generator.generateFileWithId("testTwo.html");
        transactional(() -> {
            // Ensure correct RDFS class hierarchy interpretation
            final Repository repository = em.unwrap(Repository.class);
            try (final RepositoryConnection conn = repository.getConnection()) {
                final ValueFactory vf = conn.getValueFactory();
                conn.add(vf.createIRI(Vocabulary.s_c_dokument), RDFS.SUBCLASSOF, vf.createIRI(Vocabulary.s_c_zdroj));
            }
            em.persist(doc);
            em.persist(fileOne);
            em.persist(fileTwo);
        });

        final String newName = "Updated name";
        doc.setLabel(newName);
        final String newDescription = "Document description.";
        doc.setDescription(newDescription);
        doc.addFile(fileTwo);
        sut.update(doc);
        final Document result = em.find(Document.class, doc.getUri());
        assertEquals(newName, result.getLabel());
        assertEquals(newDescription, result.getDescription());
        assertEquals(2, result.getFiles().size());
        assertTrue(result.getFiles().contains(fileTwo));
    }

    @Test
    void persistGeneratesResourceIdentifierWhenItIsNotSet() {
        final Resource resource = Generator.generateResource();
        assertNull(resource.getUri());
        transactional(() -> sut.persist(resource));
        assertNotNull(resource.getUri());
        final Resource result = em.find(Resource.class, resource.getUri());
        assertEquals(resource, result);
    }

    @Test
    void persistThrowsResourceExistsExceptionWhenResourceIdentifierAlreadyExists() {
        final Resource existing = Generator.generateResourceWithId();
        transactional(() -> em.persist(existing));

        final Resource toPersist = Generator.generateResource();
        toPersist.setUri(existing.getUri());
        assertThrows(ResourceExistsException.class, () -> sut.persist(toPersist));
    }

    @Test
    void removeDeletesReferenceFromParentDocumentToRemovedFile() {
        final File file = Generator.generateFileWithId("test.html");
        final Document parent = Generator.generateDocumentWithId();
        parent.addFile(file);
        file.setDocument(parent);   // Manually set the inferred attribute
        transactional(() -> {
            em.persist(file);
            em.persist(parent);
        });

        transactional(() -> sut.remove(file));

        assertFalse(sut.exists(file.getUri()));
        final Document result = em.find(Document.class, parent.getUri());
        assertThat(result.getFiles(), anyOf(nullValue(), empty()));
    }

    @Test
    void persistWithVocabularyPersistsInstanceIntoVocabularyContext() {
        final Document document = Generator.generateDocumentWithId();
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabularyWithId();

        sut.persist(document, vocabulary);

        final Document result = em
                .find(Document.class, document.getUri(), descriptorFactory.documentDescriptor(vocabulary));
        assertNotNull(result);
        assertEquals(document, result);
    }

    @Test
    void updateDocumentWithVocabularyUpdatesInstanceInVocabularyContext() {
        enableRdfsInference(em);
        final Document document = Generator.generateDocumentWithId();
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = Generator.generateVocabularyWithId();
        document.setVocabulary(vocabulary.getUri());
        transactional(() -> em.persist(document, descriptorFactory.documentDescriptor(vocabulary)));

        final String newLabel = "Updated document";
        document.setLabel(newLabel);

        sut.update(document);

        final Document result = em
                .find(Document.class, document.getUri(), descriptorFactory.documentDescriptor(vocabulary));
        assertNotNull(result);
        assertEquals(newLabel, result.getLabel());
    }

    @Test
    void removeFileUpdatesParentDocumentInVocabularyContext() {
        final Document document = Generator.generateDocumentWithId();
        final cz.cvut.kbss.termit.model.Vocabulary vocabulary = new cz.cvut.kbss.termit.model.Vocabulary();
        vocabulary.setUri(Generator.generateUri());
        vocabulary.setLabel("Vocabulary");
        vocabulary.setGlossary(new Glossary());
        vocabulary.setModel(new Model());
        vocabulary.setDocument(document);
        document.setVocabulary(vocabulary.getUri());
        final File file = new File();
        file.setLabel("test.html");
        file.setUri(Generator.generateUri());
        file.setDocument(document);
        final File fileTwo = new File();
        fileTwo.setLabel("test-two.html");
        fileTwo.setUri(Generator.generateUri());
        fileTwo.setDocument(document);
        document.addFile(fileTwo);
        transactional(() -> {
            em.persist(vocabulary, descriptorFactory.vocabularyDescriptor(vocabulary));
            em.persist(document, descriptorFactory.documentDescriptor(vocabulary));
            em.persist(file, descriptorFactory.fileDescriptor(vocabulary));
            em.persist(fileTwo, descriptorFactory.fileDescriptor(vocabulary));
        });

        transactional(() -> {
            final Resource toRemove = sut.getRequiredReference(file.getUri());
            sut.remove(toRemove);
        });

        final cz.cvut.kbss.termit.model.Vocabulary
                result = em.find(cz.cvut.kbss.termit.model.Vocabulary.class, vocabulary.getUri(),
                descriptorFactory.vocabularyDescriptor(vocabulary));
        assertEquals(1, result.getDocument().getFiles().size());
        assertTrue(result.getDocument().getFiles().contains(fileTwo));
    }

    @Test
    void getLastModifiedReturnsInitializedValue() {
        final long result = sut.getLastModified();
        assertThat(result, greaterThan(0L));
        assertThat(result, lessThanOrEqualTo(System.currentTimeMillis()));
    }
}
