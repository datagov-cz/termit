package cz.cvut.kbss.termit.service.security;

import cz.cvut.kbss.termit.exception.SnapshotNotEditableException;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.UserAccount;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.model.util.SupportsSnapshots;
import cz.cvut.kbss.termit.workspace.EditableVocabularies;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Provides authorization to various actions in TermIt.
 * <p>
 * This class provides custom authorization logic that cannot be (at least not easily) done using SpEL. Instead, methods
 * of this class should be invoked by authorization mechanisms such as {@link org.springframework.security.access.prepost.PreAuthorize}.
 */
@Service
public class AuthorizationService {

    private final EditableVocabularies editableVocabularies;

    private final SecurityUtils securityUtils;

    public AuthorizationService(EditableVocabularies editableVocabularies, SecurityUtils securityUtils) {
        this.editableVocabularies = editableVocabularies;
        this.securityUtils = securityUtils;
    }

    /**
     * Checks whether the specified vocabulary can be edited by the current user.
     * <p>
     * This also checks whether it is editable in the current context (e.g., w.r.t. workspace).
     *
     * @param vocabulary Vocabulary to be edited
     * @return {@code true} if the specified vocabulary can be edited by the current user, {@code false} otherwise
     */
    public boolean canEdit(Vocabulary vocabulary) {
        Objects.requireNonNull(vocabulary);
        // Currently just check workspace. in the future, this will also be checking ACL of the vocabulary w.r.t. the
        // current user
        return isUserAtLeastEditor() && editableVocabularies.isEditable(vocabulary);
    }

    private boolean isUserAtLeastEditor() {
        final UserAccount user = securityUtils.getCurrentUser();
        return user.isAdmin() || user.hasType(cz.cvut.kbss.termit.util.Vocabulary.s_c_plny_uzivatel_termitu);
    }

    /**
     * Checks whether the specified term can be edited by the current user.
     * <p>
     * This in essence means whether the current user can edit the vocabulary to which the specified term belongs.
     *
     * @param term Term to be edited
     * @return {@code true} if the specified term can be edited by the current user, {@code false} otherwise
     * @see #canEdit(Vocabulary)
     */
    public boolean canEdit(Term term) {
        Objects.requireNonNull(term);
        Objects.requireNonNull(term.getVocabulary());
        return isUserAtLeastEditor() && editableVocabularies.isEditable(term.getVocabulary());
    }

    /**
     * Checks that the specified asset being modified is not a snapshot.
     * <p>
     * If it is, a {@link SnapshotNotEditableException} is thrown.
     *
     * @param asset Asset to check for being a snapshot
     * @throws SnapshotNotEditableException If the specified asset is a snapshot
     */
    public static void verifySnapshotNotModified(SupportsSnapshots asset) {
        if (asset.isSnapshot()) {
            throw SnapshotNotEditableException.create(asset);
        }
    }
}
