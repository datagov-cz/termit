package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.dto.TermDto;
import cz.cvut.kbss.termit.dto.TermInfo;
import cz.cvut.kbss.termit.dto.assignment.TermAssignments;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.assignment.FileOccurrenceTarget;
import cz.cvut.kbss.termit.model.assignment.TermDefinitionSource;
import cz.cvut.kbss.termit.model.comment.Comment;
import cz.cvut.kbss.termit.model.util.TermStatus;
import cz.cvut.kbss.termit.service.comment.CommentService;
import cz.cvut.kbss.termit.service.export.VocabularyExporters;
import cz.cvut.kbss.termit.service.export.util.TypeAwareByteArrayResource;
import cz.cvut.kbss.termit.service.repository.ChangeRecordService;
import cz.cvut.kbss.termit.service.repository.TermRepositoryService;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.CsvUtils;
import cz.cvut.kbss.termit.util.TypeAwareResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cz.cvut.kbss.termit.environment.Generator.generateTermWithId;
import static cz.cvut.kbss.termit.environment.Generator.generateVocabulary;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TermServiceTest {

    @Mock
    private VocabularyExporters exporters;

    @Mock
    private VocabularyService vocabularyService;

    @Mock
    private TermRepositoryService termRepositoryService;

    @Mock
    private TermOccurrenceService termOccurrenceService;

    @Mock
    private ChangeRecordService changeRecordService;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private TermService sut;

    private final Vocabulary vocabulary = Generator.generateVocabularyWithId();

    @Test
    void exportGlossaryGetsGlossaryExportForSpecifiedVocabularyFromExporters() {
        final TypeAwareByteArrayResource resource = new TypeAwareByteArrayResource("test".getBytes(),
                CsvUtils.MEDIA_TYPE, CsvUtils.FILE_EXTENSION);
        when(exporters.exportVocabularyGlossary(vocabulary, CsvUtils.MEDIA_TYPE)).thenReturn(Optional.of(resource));
        final Optional<TypeAwareResource> result = sut.exportGlossary(vocabulary, CsvUtils.MEDIA_TYPE);
        assertTrue(result.isPresent());
        assertEquals(resource, result.get());
        verify(exporters).exportVocabularyGlossary(vocabulary, CsvUtils.MEDIA_TYPE);
    }

    @Test
    void findVocabularyLoadsVocabularyFromRepositoryService() {
        when(vocabularyService.find(vocabulary.getUri())).thenReturn(Optional.of(vocabulary));
        final Vocabulary result = sut.findVocabularyRequired(vocabulary.getUri());
        assertEquals(vocabulary, result);
        verify(vocabularyService).find(vocabulary.getUri());
    }

    @Test
    void findVocabularyThrowsNotFoundExceptionWhenVocabularyIsNotFound() {
        when(vocabularyService.find(any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> sut.findVocabularyRequired(vocabulary.getUri()));
    }

    @Test
    void findFindsTermByIdInRepositoryService() {
        final Term t = Generator.generateTermWithId();
        when(termRepositoryService.find(t.getUri())).thenReturn(Optional.of(t));
        final Optional<Term> result = sut.find(t.getUri());
        assertTrue(result.isPresent());
        assertEquals(t, result.get());
        verify(termRepositoryService).find(t.getUri());
    }

    @Test
    void findAllRootsWithPagingRetrievesRootTermsFromVocabularyUsingRepositoryService() {
        final List<TermDto> terms = Collections.singletonList(new TermDto(Generator.generateTermWithId()));
        when(termRepositoryService.findAllRoots(eq(vocabulary), eq(Constants.DEFAULT_PAGE_SPEC), anyCollection()))
                .thenReturn(terms);
        final List<TermDto> result = sut.findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC, Collections.emptyList());
        assertEquals(terms, result);
        verify(termRepositoryService).findAllRoots(vocabulary, Constants.DEFAULT_PAGE_SPEC, Collections.emptyList());
    }

    @Test
    void findAllBySearchStringRetrievesMatchingTermsFromVocabularyUsingRepositoryService() {
        final String searchString = "test";
        final List<TermDto> terms = Collections.singletonList(new TermDto(Generator.generateTermWithId()));
        when(termRepositoryService.findAll(searchString, vocabulary)).thenReturn(terms);
        final List<TermDto> result = sut.findAll(searchString, vocabulary);
        assertEquals(terms, result);
        verify(termRepositoryService).findAll(searchString, vocabulary);
    }

    @Test
    void getAssignmentInfoRetrievesTermAssignmentInfoFromRepositoryService() {
        final Term term = generateTermWithId();
        final List<TermAssignments> assignments = Collections
                .singletonList(new TermAssignments(term.getUri(), Generator.generateUri(), "test", true));
        when(termRepositoryService.getAssignmentsInfo(term)).thenReturn(assignments);
        final List<TermAssignments> result = sut.getAssignmentInfo(term);
        assertEquals(assignments, result);
        verify(termRepositoryService).getAssignmentsInfo(term);
    }

    @Test
    void persistUsesRepositoryServiceToPersistTermIntoVocabulary() {
        final Term term = generateTermWithId();
        sut.persistRoot(term, vocabulary);
        verify(termRepositoryService).addRootTermToVocabulary(term, vocabulary);
    }

    @Test
    void persistUsesRepositoryServiceToPersistTermAsChildOfSpecifiedParentTerm() {
        final Term parent = generateTermWithId();
        final Term toPersist = generateTermWithId();
        sut.persistChild(toPersist, parent);
        verify(termRepositoryService).addChildTerm(toPersist, parent);
    }

    @Test
    void updateUsesRepositoryServiceToUpdateTerm() {
        final Term term = generateTermWithId();
        sut.update(term);
        verify(termRepositoryService).update(term);
    }

    @Test
    void findSubTermsReturnsEmptyCollectionForTermWithoutSubTerms() {
        final Term term = generateTermWithId();
        when(termRepositoryService.findSubTerms(any())).thenReturn(Collections.emptyList());
        final List<Term> result = sut.findSubTerms(term);
        assertTrue(result.isEmpty());
    }

    @Test
    void findSubTermsLoadsChildTermsOfTermUsingRepositoryService() {
        final Term parent = generateTermWithId();
        final List<Term> children = IntStream.range(0, 5).mapToObj(i -> generateTermWithId())
                .collect(Collectors.toList());
        parent.setSubTerms(children.stream().map(TermInfo::new).collect(Collectors.toSet()));
        when(termRepositoryService.findSubTerms(parent)).thenReturn(children);

        final List<Term> result = sut.findSubTerms(parent);
        assertEquals(children.size(), result.size());
        assertTrue(children.containsAll(result));
        verify(termRepositoryService).findSubTerms(parent);
    }

    @Test
    void existsInVocabularyChecksForLabelExistenceInVocabularyViaRepositoryService() {
        final String label = "test";
        when(termRepositoryService.existsInVocabulary(label, vocabulary, Constants.DEFAULT_LANGUAGE)).thenReturn(true);
        assertTrue(sut.existsInVocabulary(label, vocabulary, Constants.DEFAULT_LANGUAGE));
        verify(termRepositoryService).existsInVocabulary(label, vocabulary, Constants.DEFAULT_LANGUAGE);
    }

    @Test
    void findAllRetrievesAllTermsFromVocabularyUsingRepositoryService() {
        final List<Term> terms = Collections.singletonList(Generator.generateTermWithId());
        when(termRepositoryService.findAll(vocabulary)).thenReturn(terms);
        final List<Term> result = sut.findAll(vocabulary);
        assertEquals(terms, result);
        verify(termRepositoryService).findAll(vocabulary);
    }

    @Test
    void getReferenceRetrievesTermReferenceFromRepositoryService() {
        final Term t = Generator.generateTermWithId();
        when(termRepositoryService.getReference(t.getUri())).thenReturn(Optional.of(t));
        final Optional<Term> result = sut.getReference(t.getUri());
        assertTrue(result.isPresent());
        assertEquals(t, result.get());
        verify(termRepositoryService).getReference(t.getUri());
    }

    @Test
    void getRequiredReferenceRetrievesTermReferenceFromRepositoryService() {
        final Term t = Generator.generateTermWithId();
        when(termRepositoryService.getRequiredReference(t.getUri())).thenReturn(t);
        final Term result = sut.getRequiredReference(t.getUri());
        assertEquals(t, result);
        verify(termRepositoryService).getRequiredReference(t.getUri());
    }

    @Test
    void removeRemovesTermViaRepositoryService() {
        final Term toRemove = generateTermWithId();
        sut.remove(toRemove);
        verify(termRepositoryService).remove(toRemove);
    }

    @Test
    void getUnusedTermsReturnsUnusedTermsInVocabulary() {
        final List<URI> terms = Collections.singletonList(Generator.generateUri());
        final Vocabulary vocabulary = generateVocabulary();
        when(termRepositoryService.getUnusedTermsInVocabulary(vocabulary)).thenReturn(terms);
        final List<URI> result = sut.getUnusedTermsInVocabulary(vocabulary);
        assertEquals(terms, result);
        verify(termRepositoryService).getUnusedTermsInVocabulary(vocabulary);
    }

    @Test
    void setTermDefinitionSourceSetsTermOnDefinitionAndPersistsIt() {
        final Term term = Generator.generateTermWithId();
        final TermDefinitionSource definitionSource = new TermDefinitionSource();
        definitionSource.setTarget(new FileOccurrenceTarget(Generator.generateFileWithId("test.html")));
        when(termRepositoryService.findRequired(term.getUri())).thenReturn(term);

        sut.setTermDefinitionSource(term, definitionSource);
        assertEquals(term.getUri(), definitionSource.getTerm());
        verify(termOccurrenceService).persistOccurrence(definitionSource);
    }

    @Test
    void setTermDefinitionReplacesExistingTermDefinition() {
        final Term term = Generator.generateTermWithId();
        final TermDefinitionSource existingSource = new TermDefinitionSource(term.getUri(),
                new FileOccurrenceTarget(Generator.generateFileWithId("existing.html")));
        term.setDefinitionSource(existingSource);
        final TermDefinitionSource definitionSource = new TermDefinitionSource();
        definitionSource.setTarget(new FileOccurrenceTarget(Generator.generateFileWithId("test.html")));
        when(termRepositoryService.findRequired(term.getUri())).thenReturn(term);

        sut.setTermDefinitionSource(term, definitionSource);
        assertEquals(term.getUri(), definitionSource.getTerm());
        verify(termOccurrenceService).removeOccurrence(existingSource);
        verify(termOccurrenceService).persistOccurrence(definitionSource);
    }

    @Test
    void getChangesRetrievesChangeRecordsFromChangeRecordService() {
        final Term asset = Generator.generateTermWithId();
        sut.getChanges(asset);
        verify(changeRecordService).getChanges(asset);
    }

    @Test
    void getCommentsRetrievesCommentsForSpecifiedTerm() {
        final Term term = Generator.generateTermWithId();
        final Comment comment = new Comment();
        comment.setAsset(term.getUri());
        comment.setCreated(new Date());
        when(commentService.findAll(term)).thenReturn(Collections.singletonList(comment));

        final List<Comment> result = sut.getComments(term);
        assertEquals(Collections.singletonList(comment), result);
        verify(commentService).findAll(term);
    }

    @Test
    void addCommentAddsCommentToTermViaCommentService() {
        final Term term = Generator.generateTermWithId();
        final Comment comment = new Comment();
        comment.setContent("test comment");
        sut.addComment(comment, term);
        verify(commentService).addToAsset(comment, term);
    }

    @Test
    void setStatusToDraftSetsTermDraftFlagToTrueAndUpdatesIt() {
        final Term term = generateTermWithId();
        when(termRepositoryService.findRequired(term.getUri())).thenReturn(term);
        sut.setStatus(term, TermStatus.DRAFT);
        assertTrue(term.isDraft());
        verify(termRepositoryService).update(term);
    }

    @Test
    void setStatusToConfirmedSetsTermDraftFlagToFalseAndUpdatesIt() {
        final Term term = generateTermWithId();
        when(termRepositoryService.findRequired(term.getUri())).thenReturn(term);
        sut.setStatus(term, TermStatus.CONFIRMED);
        assertFalse(term.isDraft());
        verify(termRepositoryService).update(term);
    }

    @Test
    void findAllRootsInWorkspaceRetrievesRootTermsFromRepositoryService() {
        final List<TermDto> terms = Collections.singletonList(new TermDto(generateTermWithId()));
        when(termRepositoryService.findAllRoots(any(Pageable.class))).thenReturn(terms);
        final Pageable pageSpec = PageRequest.of(2, 117);
        final List<TermDto> result = sut.findAllRoots(pageSpec);
        assertEquals(terms, result);
        verify(termRepositoryService).findAllRoots(pageSpec);
    }

    @Test
    void findAllWithPageSpecInWorkspaceRetrievesTermsFromRepositoryService() {
        final List<TermDto> terms = Collections.singletonList(new TermDto(generateTermWithId()));
        when(termRepositoryService.findAll(any(Pageable.class))).thenReturn(terms);
        final Pageable pageSpec = PageRequest.of(1, 117);
        final List<TermDto> result = sut.findAll(pageSpec);
        assertEquals(terms, result);
        verify(termRepositoryService).findAll(pageSpec);
    }

    @Test
    void findAllWithSearchStringInWorkspaceRetrievesTermsFromRepositoryService() {
        final List<TermDto> terms = Collections.singletonList(new TermDto(generateTermWithId()));
        when(termRepositoryService.findAll(anyString())).thenReturn(terms);
        final String searchString = "search";
        final List<TermDto> result = sut.findAll(searchString);
        assertEquals(terms, result);
        verify(termRepositoryService).findAll(searchString);
    }
}
