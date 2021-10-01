package cz.cvut.kbss.termit.service.business;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.persistence.dao.SearchDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cz.cvut.kbss.termit.dto.workspace.VocabularyInfo;
import cz.cvut.kbss.termit.dto.workspace.WorkspaceMetadata;
import cz.cvut.kbss.termit.environment.WorkspaceGenerator;
import cz.cvut.kbss.termit.persistence.dao.workspace.WorkspaceMetadataProvider;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchDao searchDao;

    @InjectMocks
    private SearchService sut;

    @Mock
    private WorkspaceMetadataProvider wsMetadataCache;

    @Test
    void fullTextSearchExtractsContextsForSearchingFromCurrentWorkspace() {
        final WorkspaceMetadata current = new WorkspaceMetadata(WorkspaceGenerator.generateWorkspace());
        final VocabularyInfo vocInfo = new VocabularyInfo(Generator.generateUri(), Generator.generateUri(),
                Generator.generateUri());
        current.getVocabularies().put(vocInfo.getUri(), vocInfo);
        when(wsMetadataCache.getCurrentWorkspaceMetadata()).thenReturn(current);
        sut.fullTextSearch("test");
        verify(searchDao).fullTextSearch("test", current.getVocabularyContexts());
    }
}
