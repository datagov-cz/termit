package cz.cvut.kbss.termit.service.repository;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.business.TermService;
import cz.cvut.kbss.termit.service.business.WorkspaceService;
import cz.cvut.kbss.termit.util.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabularyRepositoryServiceTextAnalysisTest {

    @Mock
    TermService termService;

    // Used just to prevent NPX in SUT initialization
    @Mock
    Configuration configuration;

    @InjectMocks
    private VocabularyRepositoryService sut;

    @Mock
    private VocabularyDao vocabularyDao;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WorkspaceService workspaceService;

    @Test
    void runTextAnalysisOnAllTermsInvokesTextAnalysisOnAllTermsInVocabulary() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final Term termOne = Generator.generateTermWithId();
        final Term termTwo = Generator.generateTermWithId();
        List<Term> terms = Arrays.asList(termOne, termTwo);
        when(termService.findAll(vocabulary)).thenReturn(terms);
        when(workspaceService.getCurrentWorkspaceWithMetadata().getVocabularies()).thenReturn(Collections.singleton(vocabulary.getUri()));
        sut.runTextAnalysisOnAllTerms(vocabulary);
        verify(termService).analyzeTermDefinition(termOne, vocabulary.getUri());
        verify(termService).analyzeTermDefinition(termTwo, vocabulary.getUri());
    }

    @Test
    void runTextAnalysisOnAllTermsInvokesTextAnalysisOnAllVocabularies() {
        final List<Vocabulary> vocabularies = Collections.singletonList(Generator.generateVocabularyWithId());
        final Term term = Generator.generateTermWithId();
        when(vocabularyDao.findAll()).thenReturn(vocabularies);
        when(termService.findAll(vocabularies.get(0))).thenReturn(Collections.singletonList(term));
        sut.runTextAnalysisOnAllVocabularies();
        verify(termService).analyzeTermDefinition(term, vocabularies.get(0).getUri());
    }
}
