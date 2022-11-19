package cz.cvut.kbss.termit.service.mail;

import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.Constants;
import cz.cvut.kbss.termit.util.FrontendPaths;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class AssetLinkTest {

    private static final String BASE_URL = "http://localhost/termit/";

    @Test
    void createLinkWithTermReplacesVariablesInUrlPatternWithTermValues() {
        final Term term = Generator.generateTermWithId(Generator.generateUri());
        final String result = new AssetLink(BASE_URL).createLink(term, Collections.emptyMap());
        assertThat(result, startsWith(BASE_URL));
        assertThat(result, containsString(FrontendPaths.TERM_PATH.replace("{vocabularyName}",
                                                                          IdentifierResolver.extractIdentifierFragment(
                                                                                  term.getVocabulary()))
                                                                 .replace("{termName}",
                                                                          IdentifierResolver.extractIdentifierFragment(
                                                                                  term.getUri()))));
        assertThat(result, containsString(
                Constants.QueryParams.NAMESPACE + "=" + IdentifierResolver.extractIdentifierNamespace(
                        term.getVocabulary())));

    }

    @Test
    void createLinkWithVocabularyReplacesVariablesInUrlPatternWithTermValues() {
        final Vocabulary vocabulary = Generator.generateVocabularyWithId();
        final String result = new AssetLink(BASE_URL).createLink(vocabulary, Collections.emptyMap());
        assertThat(result, startsWith(BASE_URL));
        assertThat(result, containsString(FrontendPaths.VOCABULARY_PATH.replace("{vocabularyName}",
                                                                                IdentifierResolver.extractIdentifierFragment(
                                                                                        vocabulary.getUri()))));
        assertThat(result, containsString(
                Constants.QueryParams.NAMESPACE + "=" + IdentifierResolver.extractIdentifierNamespace(
                        vocabulary.getUri())));
    }

    @Test
    void createLinkAddsQueryParamsFromSpecifiedMapToResultUrl() {
        final Term term = Generator.generateTermWithId(Generator.generateUri());
        final Map<String, Collection<String>> params = Collections.singletonMap(FrontendPaths.ACTIVE_TAB_PARAM,
                                                                                Collections.singleton(
                                                                                        FrontendPaths.COMMENTS_TAB));
        final String result = new AssetLink(BASE_URL).createLink(term, params);
        assertThat(result, containsString(FrontendPaths.ACTIVE_TAB_PARAM + "=" + FrontendPaths.COMMENTS_TAB));
    }

    @Test
    void createLinkAddsQueryParamsAfterAssetPathFragment() {
        final Term term = Generator.generateTermWithId(Generator.generateUri());
        final Map<String, Collection<String>> params = Collections.singletonMap(FrontendPaths.ACTIVE_TAB_PARAM,
                                                                                Collections.singleton(
                                                                                        FrontendPaths.COMMENTS_TAB));
        final String result = new AssetLink(BASE_URL).createLink(term, params);
        final int fragmentIndex = result.indexOf(FrontendPaths.TERM_PATH.replace("{vocabularyName}",
                                                                                 IdentifierResolver.extractIdentifierFragment(
                                                                                         term.getVocabulary()))
                                                                        .replace("{termName}",
                                                                                 IdentifierResolver.extractIdentifierFragment(
                                                                                         term.getUri())));
        final int namespaceIndex = result.indexOf(
                Constants.QueryParams.NAMESPACE + "=" + IdentifierResolver.extractIdentifierNamespace(
                        term.getVocabulary()));
        final int tabIndex = result.indexOf(FrontendPaths.ACTIVE_TAB_PARAM + "=" + FrontendPaths.COMMENTS_TAB);
        assertThat(fragmentIndex, lessThan(namespaceIndex));
        assertThat(fragmentIndex, lessThan(tabIndex));
    }
}
