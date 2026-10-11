from app.rag.corpus import load_corpus
from app.rag.retriever import HrRetriever, tokenize


def test_corpus_charges_les_documents():
    chunks = load_corpus()
    assert chunks, "le corpus RH ne doit pas être vide"
    titres = {chunk.doc_title for chunk in chunks}
    assert "Télétravail" in titres
    assert "Congés et absences" in titres


def test_tokenize_retire_accents_et_mots_vides():
    tokens = tokenize("Combien de congés approuvés ai-je ?")
    assert "combien" not in tokens
    assert "conges" in tokens
    assert "approuves" in tokens


def test_recherche_teletravail():
    results = HrRetriever().retrieve("Combien de jours de télétravail par semaine ?")
    assert results, "une question sur le télétravail doit retourner un extrait"
    assert results[0].chunk.doc_title == "Télétravail"


def test_recherche_notes_de_frais():
    results = HrRetriever().retrieve("Quel est le plafond des notes de frais pour un repas ?")
    titres = {item.chunk.doc_title for item in results}
    assert "Notes de frais" in titres


def test_recherche_pertinence_par_role():
    results = HrRetriever().retrieve("Quels sont mes droits d'accès selon mon rôle ?")
    titres = {item.chunk.doc_title for item in results}
    assert "Sécurité et confidentialité des données" in titres


def test_recherche_sans_correspondance():
    assert HrRetriever().retrieve("xyzzy blorp qwerty") == []


def test_liste_documents():
    documents = HrRetriever().documents()
    assert "Télétravail" in documents
    assert len(documents) == 5
