package com.medilinkpro.backend.enums;

/** Nature d'un acces aux donnees medicales d'un patient, montre au patient dans son journal. */
public enum TypeAccesCarnet {
    /** Un medecin (ou l'admin) a consulte le carnet. */
    LECTURE,
    /** Un medecin a ajoute ou modifie des informations du carnet. */
    ECRITURE,
    /** Le patient est apparu parmi les correspondances d'un scan facial (donnees d'urgence montrees). */
    RECONNAISSANCE_FACIALE,
    /** Carnet complet consulte en urgence par un soignant apres un scan facial. */
    CARNET_URGENCE,
    /** Carte d'urgence (QR code) scannee. */
    CARTE_URGENCE,
    /** Ordonnance consultee ou delivree par une pharmacie. */
    PHARMACIE,
    /** Deces declare. */
    DECLARATION_DECES
}
