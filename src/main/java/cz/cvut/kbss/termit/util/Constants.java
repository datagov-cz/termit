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
package cz.cvut.kbss.termit.util;

import cz.cvut.kbss.jopa.vocabulary.SKOS;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Application-wide constants.
 */
public class Constants {

    /**
     * URL path to the application's REST API.
     */
    public static final String REST_MAPPING_PATH = "/rest";

    /**
     * Default page size.
     * <p>
     * Implemented as maximum integer so that a default page specification corresponds to a find all query.
     *
     * @see #DEFAULT_PAGE_SPEC
     */
    public static final int DEFAULT_PAGE_SIZE = Integer.MAX_VALUE;

    /**
     * Default page specification, corresponding to a find all query with no page specification.
     * <p>
     * I.e., the request asks for the first page (number = 0) and its size is {@link Integer#MAX_VALUE}.
     */
    public static final Pageable DEFAULT_PAGE_SPEC = PageRequest.of(0, DEFAULT_PAGE_SIZE);

    /**
     * Path to directory containing queries used by the system.
     * <p>
     * The path should be relative to the classpath, so that queries from it can be loaded using {@link
     * ClassLoader#getResourceAsStream(String)}.
     */
    public static final String QUERY_DIRECTORY = "query";

    /**
     * Represents the X-Total-Count HTTP header used to convey the total number of items in paged or otherwise
     * restricted response.
     */
    public static final String X_TOTAL_COUNT_HEADER = "X-Total-Count";

    /**
     * Score threshold for term occurrence.
     */
    public static final Double SCORE_THRESHOLD = 0.49;

    /**
     * Default identifier component for {@link cz.cvut.kbss.termit.model.Model}.
     * <p>
     * This component is appended to the containing vocabulary identifier to form the model identifier.
     */
    public static final String DEFAULT_MODEL_IRI_COMPONENT = "model";

    /**
     * CRON pattern for executing scheduled actions.
     * <p>
     * Indicates that the scheduled actions should be executed at 1:10 every day.
     */
    public static final String SCHEDULING_PATTERN = "0 1 1 * * ?";

    /**
     * Instant representing the Unix epoch.
     * <p>
     * Useful as a default minimum value for timestamp-based calculations.
     */
    public static final Instant EPOCH_TIMESTAMP = Instant.EPOCH;

    /**
     * Formatter for timestamps (for example, in asset snapshot identifiers).
     * <p>
     * It represents ISO instant string without separator dashes and colons truncated to seconds at the UTC timezone.
     */
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
                                                                                 .withZone(ZoneId.of("UTC"));

    /**
     * SKOS relationships between concepts from different concept schemes (glossaries).
     */
    public static final Set<URI> SKOS_CONCEPT_MATCH_RELATIONSHIPS = Stream.of(
            SKOS.BROAD_MATCH, SKOS.NARROW_MATCH, SKOS.EXACT_MATCH, SKOS.RELATED_MATCH
    ).map(URI::create).collect(Collectors.toSet());

    private Constants() {
        throw new AssertionError();
    }

    /**
     * Constants from the RDFa vocabulary.
     */
    public static final class RDFa {

        /**
         * RDFa property attribute.
         */
        public static final String PROPERTY = "property";

        /**
         * RDFa context identifier attribute.
         */
        public static final String ABOUT = "about";

        /**
         * RDFa content attribute.
         */
        public static final String CONTENT = "content";

        /**
         * RDFa type identifier attribute.
         */
        public static final String TYPE = "typeof";

        /**
         * RDFa resource identifier.
         */
        public static final String RESOURCE = "resource";

        /**
         * RDFa prefix attribute.
         */
        public static final String PREFIX = "prefix";

        private RDFa() {
            throw new AssertionError();
        }
    }

    /**
     * Additional media types not covered by {@link org.springframework.http.MediaType}.
     */
    public static final class MediaType {
        public static final String CSV = "text/csv";
        public static final String EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        public static final String TURTLE = "text/turtle";
        public static final String RDF_XML = "application/rdf+xml";
    }

    /**
     * Useful HTTP request query parameters used by the application REST API.
     */
    public static final class QueryParams {

        /**
         * HTTP request query parameter denoting identifier namespace.
         * <p>
         * Used in connection with normalized name of an individual.
         */
        public static final String NAMESPACE = "namespace";

        /**
         * HTTP request query parameter denoting page number.
         * <p>
         * Used for paging in collections of results.
         *
         * @see #PAGE_SIZE
         */
        public static final String PAGE = "page";

        /**
         * HTTP request query parameter denoting page size.
         * <p>
         * Used for paging in collections of results.
         *
         * @see #PAGE
         */
        public static final String PAGE_SIZE = "size";

        private QueryParams() {
            throw new AssertionError();
        }
    }
}
