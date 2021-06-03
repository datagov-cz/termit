package cz.cvut.kbss.termit.persistence.dao.skos;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.termit.exception.DataImportException;
import cz.cvut.kbss.termit.exception.UnsupportedImportMediaTypeException;
import cz.cvut.kbss.termit.model.Glossary;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.persistence.dao.TermDao;
import cz.cvut.kbss.termit.persistence.dao.VocabularyDao;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The tool to import plain SKOS thesauri.
 * <p>
 * It takes the thesauri as a TermIt glossary and 1) creates the necessary metadata (vocabulary, model) 2) generates the
 * necessary hasTopConcept relationships based on the broader/narrower hierarchy.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SKOSImporter {

    private static final Logger LOG = LoggerFactory.getLogger(SKOSImporter.class);

    private final Configuration config;
    private final VocabularyDao vocabularyDao;
    private final TermDao termDao;
    private final IdentifierResolver resolver;

    private final Repository repository;
    private final ValueFactory vf;

    private final Model model = new LinkedHashModel();

    private IRI glossaryIri;

    @Autowired
    public SKOSImporter(Configuration config,
                        VocabularyDao vocabularyDao,
                        TermDao termDao,
                        IdentifierResolver resolver,
                        EntityManager em) {
        this.config = config;
        this.vocabularyDao = vocabularyDao;
        this.termDao = termDao;
        this.resolver = resolver;
        this.repository = em.unwrap(org.eclipse.rdf4j.repository.Repository.class);
        vf = repository.getValueFactory();
    }

    public Vocabulary importVocabulary(URI vocabularyIri, String mediaType,
                                       InputStream... inputStreams) {
        Objects.requireNonNull(vocabularyIri);
        if (inputStreams.length == 0) {
            throw new IllegalArgumentException("No input provided for importing vocabulary.");
        }
        LOG.debug("Vocabulary import started.");
        parseDataFromStreams(mediaType, inputStreams);
        glossaryIri = resolveGlossaryIriFromImportedData(model);
        LOG.trace("Importing glossary {}.", glossaryIri);
        insertTopConceptAssertions();
        Vocabulary vocabulary = resolveVocabulary(vocabularyIri);
        vocabularyDao.persist(vocabulary);
        addDataIntoRepository(vocabularyIri);
        LOG.debug("Vocabulary import successfully finished.");
        return vocabulary;
    }

    private void parseDataFromStreams(String mediaType, InputStream... inputStreams) {
        final RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mediaType).orElseThrow(
            () -> new UnsupportedImportMediaTypeException(
                "Media type" + mediaType + "not supported."));
        final RDFParser p = Rio.createParser(rdfFormat);
        final StatementCollector collector = new StatementCollector(model);
        p.setRDFHandler(collector);
        for (InputStream is : inputStreams) {
            try {
                p.parse(is, "");
            } catch (IOException e) {
                throw new DataImportException("Unable to parse data for import.", e);
            }
        }
    }

    private boolean canBeImportedIntoVocabulary(final Resource r, final URI vocabularyIri) {
        return resolver
            .buildNamespace(
                vocabularyIri.toString(),
                config.get(ConfigParam.TERM_NAMESPACE_SEPARATOR)
            ).equals(IdentifierResolver.extractIdentifierNamespace(URI.create(r.stringValue())));
    }

    private void addDataIntoRepository(URI vocabularyIri) {
        final Set<Resource> unmappedConcepts = model.filter(null, RDF.TYPE, SKOS.CONCEPT).stream()
            .map(s -> s.getSubject())
            .filter(s -> !canBeImportedIntoVocabulary(s, vocabularyIri))
            .collect(Collectors.toSet());

        LOG.warn("Cannot import concepts {}, removing", unmappedConcepts);
        unmappedConcepts.forEach( a -> model.remove(a, null, null));

        try (final RepositoryConnection conn = repository.getConnection()) {
            conn.begin();
            final IRI targetContext = vf.createIRI(vocabularyIri.toString());
            LOG.debug("Importing vocabulary into context <{}>.", targetContext);
            conn.add(model, targetContext);
            conn.commit();
        }
    }

    private IRI resolveGlossaryIriFromImportedData(final Model model) {
        final Model glossaryRes = model.filter(null, RDF.TYPE, SKOS.CONCEPT_SCHEME);
        if (glossaryRes.size() == 1) {
            final Resource glossary = glossaryRes.iterator().next().getSubject();
            if (glossary.isIRI()) {
                return (IRI) glossary;
            } else {
                throw new IllegalArgumentException(
                        "Blank node skos:ConceptScheme not supported.");
            }
        } else {
            throw new IllegalArgumentException(
                    "No unique skos:ConceptScheme found in the provided data.");
        }
    }

    private Vocabulary resolveVocabulary(final URI vocabularyIri) {
        final Vocabulary vocabulary;
        if ( vocabularyIri != null ) {
            vocabulary = vocabularyDao.find(vocabularyIri).get();
        } else {
            throw new IllegalArgumentException("Cannot import glossaries without vocabulary IRI being specified, yet.");
        }

        // check if the glossary is the same;
        final URI newUri = URI.create(glossaryIri.stringValue());
        final Glossary glossary;
        if ( !vocabulary.getGlossary().getUri().equals( newUri ) ) {
            glossary = new Glossary();
            glossary.setUri(newUri);
            vocabulary.setGlossary(glossary);
        } else {
            glossary = vocabulary.getGlossary();
        }

        // remove old terms
        termDao.findAll(vocabulary).forEach(term -> {
            termDao.remove(term);
        });

        final Set<Statement> labels = model.filter(glossaryIri, DCTERMS.TITLE, null);
        labels.stream().filter(s -> {
            assert s.getObject() instanceof Literal;
            return Objects.equals(config.get(ConfigParam.LANGUAGE),
                    ((Literal) s.getObject()).getLanguage().orElse(config.get(ConfigParam.LANGUAGE)));
        }).findAny().ifPresent(s -> vocabulary.setLabel(s.getObject().stringValue()));
        return vocabulary;
    }

    private void insertTopConceptAssertions() {
        LOG.trace("Generating top concept assertions.");
        final List<Resource> terms = model.filter(null, RDF.TYPE, SKOS.CONCEPT).stream().map
                (Statement::getSubject)
                .collect(Collectors.toList());
        terms.forEach(t -> {
            final List<Value> broader = model.filter(t, SKOS.BROADER, null).stream().map
                    (Statement::getObject)
                    .collect(Collectors.toList());
            final boolean hasBroader = broader.stream()
                    .anyMatch(p -> model.contains((Resource) p, RDF
                            .TYPE, SKOS.CONCEPT));
            final List<Value> narrower = model.filter(null, SKOS.NARROWER, t).stream().map
                    (Statement::getObject)
                    .collect(Collectors.toList());
            final boolean isNarrower = narrower.stream()
                    .anyMatch(p -> model.contains((Resource) p, RDF
                            .TYPE, SKOS.CONCEPT));
            if (!hasBroader && !isNarrower) {
                model.add(glossaryIri, SKOS.HAS_TOP_CONCEPT, t);
            }
        });
    }
}
