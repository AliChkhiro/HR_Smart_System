package com.apprh.backend.ia.application;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.api.ChatRequest;
import com.apprh.backend.ia.api.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Assistant RH - service applicatif.
 * <p>
 * STEP 2 : reponse statique, aucun appel LLM. L'identite provient toujours du JWT
 * (UserPrincipal) et jamais du corps de la requete.
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    public ChatResponse ask(UserPrincipal principal, ChatRequest request) {
        // On trace l'utilisateur et la taille du message, jamais son contenu.
        log.info("chat - utilisateur {} role {} - message de {} caracteres",
                principal.id(), principal.role(), request.message().length());
        return new ChatResponse(replyFor(principal), false);
    }

    private String replyFor(UserPrincipal principal) {
        return "Bonjour " + principal.firstName() + ", le service IA n'est pas encore branche. "
                + "Votre message a bien ete recu par le serveur : la chaine complete "
                + "(widget, authentification, controleur) fonctionne deja. "
                + "La redaction par un modele de langage arrivera a l'etape suivante.";
    }
}
