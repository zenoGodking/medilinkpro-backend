package com.medilinkpro.backend.service.push;

import com.medilinkpro.backend.entity.ParametreSysteme;
import com.medilinkpro.backend.repository.ParametreSystemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

/**
 * Cles VAPID identifiant ce serveur aupres des services push des navigateurs. Priorite aux
 * variables d'environnement (VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY) ; sinon une paire est generee
 * une seule fois et conservee en base. Elles doivent rester stables : en changer invalide tous
 * les abonnements existants.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleVapidService {

    static final String CLE_PUBLIQUE = "vapid.cle-publique";
    static final String CLE_PRIVEE = "vapid.cle-privee";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final ParametreSystemeRepository parametreRepository;

    @Value("${medilinkpro.push.vapid-public:}")
    private String publiqueConfiguree;

    @Value("${medilinkpro.push.vapid-private:}")
    private String priveeConfiguree;

    private volatile String[] cles;

    /** [publique, privee] en base64url. */
    @Transactional
    public String[] cles() {
        if (cles == null) {
            synchronized (this) {
                if (cles == null) {
                    cles = charger();
                }
            }
        }
        return cles;
    }

    public String clePublique() {
        return cles()[0];
    }

    private String[] charger() {
        if (!publiqueConfiguree.isBlank() && !priveeConfiguree.isBlank()) {
            return new String[]{publiqueConfiguree, priveeConfiguree};
        }
        var publique = parametreRepository.findById(CLE_PUBLIQUE);
        var privee = parametreRepository.findById(CLE_PRIVEE);
        if (publique.isPresent() && privee.isPresent()) {
            return new String[]{publique.get().getValeur(), privee.get().getValeur()};
        }
        try {
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            generateur.initialize(ECNamedCurveTable.getParameterSpec("prime256v1"));
            KeyPair paire = generateur.generateKeyPair();
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            String pub = b64.encodeToString(Utils.encode((ECPublicKey) paire.getPublic()));
            String priv = b64.encodeToString(Utils.encode((ECPrivateKey) paire.getPrivate()));
            parametreRepository.save(new ParametreSysteme(CLE_PUBLIQUE, pub));
            parametreRepository.save(new ParametreSysteme(CLE_PRIVEE, priv));
            log.info("Clés VAPID générées et enregistrées (notifications push)");
            return new String[]{pub, priv};
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de générer les clés VAPID", e);
        }
    }
}
