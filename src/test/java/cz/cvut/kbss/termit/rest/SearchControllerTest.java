/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.cvut.kbss.termit.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.termit.dto.FullTextSearchResult;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.service.business.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/search";

    @Mock
    private SearchService searchServiceMock;

    @InjectMocks
    private SearchController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        super.setUp(sut);
    }

    @Test
    void fullTextSearchExecutesSearchOnService() throws Exception {
        final List<FullTextSearchResult> expected = Collections
            .singletonList(
                new FullTextSearchResult(Generator.generateUri(), "test", null, SKOS.CONCEPT,
                    "test", "test", 1.0));
        when(searchServiceMock.fullTextSearch(any())).thenReturn(expected);
        final String searchString = "test";
        final MvcResult mvcResult = mockMvc.perform(get(PATH + "/fts").param("searchString", searchString))
                                           .andExpect(status().isOk()).andReturn();
        final List<FullTextSearchResult> result = readValue(mvcResult, new TypeReference<List<FullTextSearchResult>>() {
        });
        assertEquals(expected.size(), result.size());
        assertEquals(expected.get(0).getUri(), result.get(0).getUri());
        assertEquals(expected.get(0).getLabel(), result.get(0).getLabel());
        assertEquals(expected.get(0).getTypes(), result.get(0).getTypes());
        verify(searchServiceMock).fullTextSearch(searchString);
    }

    @Test
    void fullTextSearchOfTermsWithoutVocabularySpecificationExecutesSearchOnService() throws Exception {
        final URI vocabularyIri = URI.create("https://test.org/vocabulary");
        final List<FullTextSearchResult> expected = Collections
                .singletonList(new FullTextSearchResult(Generator.generateUri(), "test", vocabularyIri, Vocabulary.s_c_term, "test", "test", 1.0));
        when(searchServiceMock.fullTextSearchOfTerms(any(), any())).thenReturn(expected);
        final String searchString = "test";
        mockMvc.perform(get(PATH + "/fts/terms")
                       .param("searchString", searchString)
                       .param("vocabulary", vocabularyIri.toString()))
               .andExpect(status().isOk()).andReturn();
        verify(searchServiceMock).fullTextSearchOfTerms(searchString, Collections.singleton(vocabularyIri));
    }
}
