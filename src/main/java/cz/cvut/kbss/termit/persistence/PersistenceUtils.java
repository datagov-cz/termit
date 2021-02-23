package cz.cvut.kbss.termit.persistence;

import cz.cvut.kbss.jopa.model.EntityManagerFactory;
import cz.cvut.kbss.jopa.model.metamodel.Metamodel;
import cz.cvut.kbss.termit.model.Workspace;
import cz.cvut.kbss.termit.persistence.dao.workspace.WorkspaceMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

@Component
public class PersistenceUtils {

    private final WorkspaceMetadataProvider workspaceMetadataProvider;

    private final EntityManagerFactory emf;

    @Autowired
    public PersistenceUtils(WorkspaceMetadataProvider workspaceMetadataProvider, EntityManagerFactory emf) {
        this.workspaceMetadataProvider = workspaceMetadataProvider;
        this.emf = emf;
    }

    /**
     * Gets the current workspace identifier.
     *
     * @return Current workspace IRI
     */
    public Workspace getCurrentWorkspace() {
        return workspaceMetadataProvider.getCurrentWorkspace();
    }

    /**
     * Determines the identifier of the repository context (named graph) in which vocabulary with the specified
     * identifier is stored.
     *
     * @param vocabularyUri Vocabulary identifier
     * @return Repository context identifier
     */
    public URI resolveVocabularyContext(URI vocabularyUri) {
        Objects.requireNonNull(vocabularyUri);
        return workspaceMetadataProvider.getCurrentWorkspaceMetadata().getVocabularyInfo(vocabularyUri).getContext();
    }

    /**
     * Determines the identifier of the repository context (named graph) in which vocabulary with the specified
     * identifier is stored in the specified workspace.
     *
     * @param workspace     Workspace containing the vocabulary
     * @param vocabularyUri Vocabulary identifier
     * @return Repository context identifier
     */
    public URI resolveVocabularyContext(Workspace workspace, URI vocabularyUri) {
        Objects.requireNonNull(workspace);
        Objects.requireNonNull(vocabularyUri);
        return workspaceMetadataProvider.getWorkspaceMetadata(workspace.getUri()).getVocabularyInfo(vocabularyUri)
                                        .getContext();
    }

    /**
     * Gets identifiers of contexts in which vocabularies available in the current workspace are stored.
     *
     * @return Set of context identifiers
     */
    public Set<URI> getCurrentWorkspaceVocabularyContexts() {
        return workspaceMetadataProvider.getCurrentWorkspaceMetadata().getVocabularyContexts();
    }

    /**
     * Gets identifiers of contexts in which vocabularies available in the specified workspace are stored.
     *
     * @param workspace Workspace to get contexts for
     * @return Set of context identifiers
     */
    public Set<URI> getWorkspaceVocabularyContexts(Workspace workspace) {
        return workspaceMetadataProvider.getWorkspaceMetadata(workspace.getUri()).getVocabularyContexts();
    }

    /**
     * Gets identifiers of change tracking contexts in the currently loaded workspace.
     *
     * @return Set of context identifiers
     */
    public Set<URI> getCurrentWorkspaceChangeTrackingContexts() {
        return workspaceMetadataProvider.getCurrentWorkspaceMetadata().getChangeTrackingContexts();
    }

    /**
     * Gets JOPA metamodel.
     *
     * @return Metamodel of the persistence unit
     */
    public Metamodel getMetamodel() {
        return emf.getMetamodel();
    }
}
