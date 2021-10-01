package cz.cvut.kbss.termit.dto;

import cz.cvut.kbss.jopa.model.annotations.*;
import cz.cvut.kbss.jopa.vocabulary.DC;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.io.Serializable;
import java.net.URI;

/**
 * Represents configuration data provided by the server to client.
 */
@OWLClass(iri = Vocabulary.s_c_konfigurace)
public class ConfigurationDto implements Serializable {

    @Id
    private URI id;

    @OWLAnnotationProperty(iri = DC.Terms.LANGUAGE)
    private String language;

    @OWLDataProperty(iri = Vocabulary.s_p_ma_maximalni_velikost_souboru)
    private String maxFileUploadSize;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public String getMaxFileUploadSize() {
        return maxFileUploadSize;
    }

    public void setMaxFileUploadSize(String maxFileUploadSize) {
        this.maxFileUploadSize = maxFileUploadSize;
    }
}
