package cz.cvut.kbss.termit.dto;

import cz.cvut.kbss.jopa.model.MultilingualString;
import cz.cvut.kbss.jopa.model.annotations.FetchType;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jopa.vocabulary.SKOS;
import cz.cvut.kbss.jsonld.annotation.JsonLdAttributeOrder;
import cz.cvut.kbss.termit.model.AbstractTerm;
import cz.cvut.kbss.termit.model.Term;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO for term listing.
 * <p>
 * Contains less data than a regular {@link cz.cvut.kbss.termit.model.Term}.
 */
@OWLClass(iri = SKOS.CONCEPT)
@JsonLdAttributeOrder({"uri", "label", "subTerms"})
public class TermDto extends AbstractTerm {

    @OWLObjectProperty(iri = SKOS.BROADER, fetch = FetchType.EAGER)
    private Set<TermDto> parentTerms;

    public TermDto() {
    }

    public TermDto(Term other) {
        // This copy constructor is used mainly in tests to allow seamless transformation between the two classes
        Objects.requireNonNull(other);
        setUri(other.getUri());
        if (other.getLabel() != null) {
            setLabel(new MultilingualString(other.getLabel().getValue()));
        }
        if (other.getDefinition() != null) {
            setDefinition(new MultilingualString(other.getDefinition().getValue()));
        }
        setDraft(other.isDraft());
        setGlossary(other.getGlossary());
        if (other.getParentTerms() != null) {
            setParentTerms(other.getParentTerms().stream().map(TermDto::new).collect(Collectors.toSet()));
        }
        if (other.getSubTerms() != null) {
            setSubTerms(new LinkedHashSet<>(other.getSubTerms()));
        }
    }

    public Set<TermDto> getParentTerms() {
        return parentTerms;
    }

    public void setParentTerms(Set<TermDto> parentTerms) {
        this.parentTerms = parentTerms;
    }

    public void addParentTerms(Collection<TermDto> parents) {
        Objects.requireNonNull(parents);
        if (parentTerms == null) {
            this.parentTerms = new LinkedHashSet<>();
        }
        parentTerms.addAll(parents);
    }
}
