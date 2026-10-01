# MediLinkPro — Backend Spring Boot

Backend REST de **MediLinkPro**, plateforme intelligente de suivi medical et de
localisation des specialistes. Genere a partir de la modelisation systeme du projet
(diagramme de classes, cas d'utilisation, exigences fonctionnelles).

## Perimetre de ce backend (MVP)

Coeur metier complet :

- **Authentification** multi-role (JWT) : Patient, Medecin, Admin, Directeur, Secretaire
- **Module 1 — Dossier Medical Electronique (DME)** : dossier auto-cree a l'inscription,
  consultations, resultats d'analyses, ordonnances numeriques (QR simule)
- **Module 2 — Geolocalisation & Rendez-vous** : recherche de specialistes par specialite
  et proximite, prise de rendez-vous avec verification de creneau, gestion du statut
- **Module 4 — Etablissements de sante** : CRUD des hopitaux/cliniques geolocalises

**Non inclus dans ce MVP** (a ajouter dans une iteration suivante) :
OTP/SMS reels, chiffrement applicatif AES-256 des donnees, WebRTC (teleconsultation
video), envoi reel d'emails/SMS (les rappels sont prevus dans le modele mais pas
branches a un fournisseur externe).

## Stack technique

| Composant       | Choix                                  |
|------------------|-----------------------------------------|
| Langage / JDK    | Java 21                                |
| Framework        | Spring Boot 3.3.4                      |
| Securite         | Spring Security 6 + JWT (JJWT 0.12.6)  |
| Persistance      | Spring Data JPA / Hibernate            |
| Base de donnees  | PostgreSQL 16                          |
| Mapping DTO      | MapStruct 1.6.0                        |
| Documentation API| springdoc-openapi (Swagger UI) 2.6.0   |
| Build            | Maven                                  |
| Conteneurisation | Docker / Docker Compose                |

## Lancer le projet (Docker Compose — recommande)

Pre-requis : Docker et Docker Compose installes.

```bash
cd medilinkpro
docker-compose up --build
```

Cela demarre deux conteneurs :
- `medilinkpro-postgres` : PostgreSQL 16 sur le port `5432`
- `medilinkpro-backend` : l'API Spring Boot sur le port `8080`

Hibernate cree automatiquement le schema (`ddl-auto: update`) au premier demarrage.

## Tester sur Swagger

Une fois les conteneurs lances, ouvrez :

```
http://localhost:8080/swagger-ui.html
```

### Etapes de test recommandees

1. **Creer un compte** via `POST /api/auth/register` (choisir `role: PATIENT` ou
   `role: MEDECIN`, etc.). La reponse contient un `token` JWT.
2. Cliquer sur le bouton **Authorize** en haut de Swagger UI, coller le token
   (sans le prefixe `Bearer`), valider.
3. Tous les endpoints proteges deviennent accessibles depuis l'interface.
4. Pour tester la prise de rendez-vous : creer un patient et un medecin, puis
   `POST /api/rendez-vous` avec leurs ids.
5. Pour tester le DME : `GET /api/dossiers-medicaux/patient/{patientId}` (cree
   automatiquement a l'inscription du patient).

### Exemple de corps de requete — Inscription patient

```json
{
  "nom": "Mballa",
  "prenom": "Aline",
  "email": "aline.mballa@example.com",
  "motDePasse": "motdepasse123",
  "role": "PATIENT",
  "telephone": "+237600000000",
  "dateNaissance": "1995-04-12",
  "groupeSanguin": "O_POSITIF"
}
```

### Exemple de corps de requete — Inscription medecin

```json
{
  "nom": "Ngono",
  "prenom": "Paul",
  "email": "dr.ngono@example.com",
  "motDePasse": "motdepasse123",
  "role": "MEDECIN",
  "telephone": "+237677000000",
  "specialite": "Cardiologie",
  "numeroOrdre": "CM-12345",
  "latitude": 4.0511,
  "longitude": 9.7679,
  "tarif": 15000
}
```

## Lancer sans Docker (PostgreSQL local)

1. Creer la base et l'utilisateur :
   ```sql
   CREATE DATABASE medilinkpro;
   CREATE USER medilinkpro_user WITH PASSWORD 'medilinkpro_pass';
   GRANT ALL PRIVILEGES ON DATABASE medilinkpro TO medilinkpro_user;
   ```
2. Lancer l'application :
   ```bash
   mvn spring-boot:run
   ```
   Les variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
   peuvent surcharger `application.yml` si besoin (voir ce fichier).

## Securite par role (apercu)

| Endpoint                          | Roles autorises                              |
|------------------------------------|-----------------------------------------------|
| `/api/auth/**`                    | Public                                        |
| `/api/admin/**`                   | ADMIN                                         |
| `/api/dashboard/**`               | DIRECTEUR, ADMIN                              |
| `/api/dossiers-medicaux/**`       | PATIENT, MEDECIN, SECRETAIRE, ADMIN           |
| `/api/consultations/**`           | MEDECIN, PATIENT, SECRETAIRE, ADMIN           |
| `/api/ordonnances/**`             | MEDECIN, PATIENT, SECRETAIRE, ADMIN           |
| `/api/rendez-vous/**`             | PATIENT, MEDECIN, SECRETAIRE, ADMIN           |
| `/api/medecins/**`                | MEDECIN, SECRETAIRE, ADMIN, DIRECTEUR         |

Ajustable dans `SecurityConfig.java`.

## Important — compilation non verifiee dans cet environnement

Le code a ete genere et relu attentivement (coherence des entites, DTOs, mappers,
annotations Lombok/JPA, accolades equilibrees sur les 80 fichiers), mais **n'a pas
pu etre compile dans cet environnement** : l'acces a Maven Central
(`repo.maven.apache.org`) n'est pas autorise depuis ce sandbox. La toute premiere
chose a faire chez vous est donc :

```bash
docker-compose up --build
```

ou, en local avec acces internet :

```bash
mvn clean compile
```

Si une erreur de compilation apparait, copiez-la moi et je la corrige immediatement.

## Structure du projet

```
src/main/java/com/medilinkpro/backend/
├── entity/          # Entites JPA (Utilisateur + heritage Patient/Medecin/Admin/...)
├── enums/           # Role, StatutRendezVous, TypeConsultation, GroupeSanguin, StatutDossier
├── repository/      # Interfaces Spring Data JPA
├── service/         # Logique metier
├── controller/      # Endpoints REST
├── dto/request/      # DTOs entrants (avec validation Jakarta)
├── dto/response/     # DTOs sortants
├── dto/mapper/       # Mappers MapStruct Entity <-> DTO
├── security/         # CustomUserDetailsService
├── security/jwt/     # JwtService, JwtAuthFilter
├── config/           # SecurityConfig, OpenApiConfig
└── exception/        # Exceptions metier + GlobalExceptionHandler
```

## Identification d'urgence par reconnaissance faciale

A l'inscription, chaque patient envoie obligatoirement une photo de son visage
(`POST /api/auth/register` en `multipart/form-data` : parties `donnees`, `photo`, `descripteur`).
Le navigateur calcule l'empreinte faciale (128 reels, face-api) ; le backend stocke la photo dans
un dossier **prive** (`medilinkpro.upload.private-dir`, jamais servi statiquement) et l'empreinte en base.

| Endpoint | Acces | Contenu |
|---|---|---|
| `POST /api/reconnaissance-faciale/recherche` | tout utilisateur connecte | au plus 3 correspondances **probables** (distance < 0.6), score de confiance, photo de reference, prenom, groupe sanguin, allergies, contact d'un proche (+ nom et date de naissance pour le personnel de sante) |
| `GET /api/reconnaissance-faciale/patients/{id}/carnet` | Medecin, Infirmier, Secretaire au compte approuve | carnet complet en lecture seule |
| `GET/PUT /api/reconnaissance-faciale/moi/photo` | Patient | consulter / remplacer sa photo |

Chaque recherche et chaque consultation de carnet est journalisee dans `acces_urgence_logs`.

## Soins a domicile geolocalises

- L'application de l'infirmiere partage sa position GPS en continu (STOMP `/app/infirmiers/position`,
  ou `PUT /api/alertes/infirmiers/moi/position`) tant que sa page d'alertes est ouverte.
- A l'envoi d'une alerte avec la position du patient, seules les **5 infirmieres disponibles les plus
  proches** (rayon 20 km, position de moins de 15 min, sans intervention en cours) sont notifiees en prive
  (`/user/queue/alertes`), avec leur distance au patient. Sans reponse sous 2 min, la vague suivante est
  notifiee ; quand il n'y a plus personne a proximite, l'alerte passe en diffusion generale (`/topic/alertes`).
  Sans position du patient, la diffusion est generale d'emblee.
- Des qu'une infirmiere accepte, le patient recoit son telephone et suit sa position en temps reel
  (`/user/queue/suivi`, etat initial via `GET /api/alertes/{id}/suivi`). Le partage s'arrete a la fin de l'intervention.
- Reglages : constantes `TAILLE_VAGUE`, `RAYON_MAX_KM`, `FRAICHEUR_POSITION_MINUTES`, `DELAI_ELARGISSEMENT_SECONDES` dans `AlerteService`.

## Acces au carnet medical

| Qui | Lecture | Ecriture |
|---|---|---|
| Patient | son propre carnet uniquement ; pour les autres, seulement les donnees d'urgence via la reconnaissance faciale | sa fiche (identite, contact, informations d'urgence) |
| Medecin (compte approuve) | tous les carnets | seulement si le patient l'a autorise (`/api/carnets/autorisations`) ou s'il est son ancien patient (consultation, rendez-vous confirme/termine) ; uniquement les donnees medicales de la fiche |
| Tout medecin | — | declaration de deces (`POST /api/patients/{id}/deces`) : compte desactive, alertes annulees, proche informe par SMS |
| Admin | tout | annulation d'une declaration de deces erronee |

Regles centralisees dans `CarnetAccesService`. Seul le patient peut prendre rendez-vous pour lui-meme
(un medecin ne peut pas s'attribuer un patient).

**SMS** : aucun fournisseur n'est branche. Les messages sont enregistres dans `notifications_sms`
avec le statut `NON_ENVOYE_AUCUN_FOURNISSEUR`. Pour envoyer reellement, declarer un bean implementant
`service.sms.FournisseurSms` (Twilio, Africa's Talking, operateur local...).

**Role Secretaire supprime** : au demarrage, `SuppressionSecretairesMigration` supprime les comptes
secretaires existants et la table `secretaires`.

## Nouvelles fonctionnalites

| Fonctionnalite | Endpoints | Qui |
|---|---|---|
| Validation des adhesions medecin -> etablissement | `GET /api/demandes-integration/en-attente`, `PATCH /api/demandes-integration/{id}/repondre` | Directeur (ses etablissements), Admin (tous) |
| Calendrier de disponibilite | `GET /api/disponibilites/medecins/{id}`, `GET .../creneaux?du=&au=`, `PUT /api/disponibilites/moi`, `POST/DELETE /api/disponibilites/moi/absences` | Lecture : tout connecte ; ecriture : le medecin |
| Documents medicaux (antecedents, anciens carnets scannes) | `POST/GET /api/documents-medicaux/patients/{patientId}`, `GET /api/documents-medicaux/{id}/pages/{n}`, `DELETE /api/documents-medicaux/{id}` | Le patient ; medecin autorise a ecrire ; lecture selon les regles du carnet |
| Teleconsultation video (WebRTC) | `GET /api/teleconsultations/{rdvId}` + STOMP `/app/teleconsultation/{rdvId}/signal` -> `/user/queue/teleconsultation` | Le medecin et le patient du rendez-vous |

- Un rendez-vous n'est accepte que sur un creneau du calendrier du medecin. Sans semaine definie,
  heures ouvrables par defaut : lundi-vendredi 08:00-12:00 / 14:00-17:00, creneaux de 30 min.
- Les documents sont stockes dans `private-uploads/documents-medicaux` (non expose publiquement), 10 Mo max par fichier, 20 fichiers max par document.
- La salle de teleconsultation ouvre 15 min avant le rendez-vous et ferme 2 h apres
  (`TELECONSULTATION_OUVERTURE_MINUTES`, `TELECONSULTATION_DUREE_MAX_MINUTES`). La video circule en pair-a-pair :
  la camera exige HTTPS (ou localhost), et un serveur TURN est recommande en production
  (cote frontend : `VITE_ICE_SERVERS='[{"urls":"turn:...","username":"...","credential":"..."}]'`).

### Infirmieres et navigation integree

| Fonctionnalite | Endpoints | Qui |
|---|---|---|
| Photo de profil obligatoire (inscription multipart, ou ajout depuis le profil) | `PUT /api/infirmiers/moi/photo` | L'infirmiere ; sans photo elle ne peut pas accepter d'alerte |
| Profil et photo presentes au patient | `GET /api/infirmiers/{id}/profil`, `GET /api/infirmiers/{id}/photo` | L'infirmiere, l'admin, les directeurs, et les patients qu'elle a pris en charge |
| Adhesion d'une infirmiere a un etablissement | `POST /api/infirmiers/moi/demander-integration/{etablissementId}`, `GET /api/infirmiers/moi/demandes-integration` ; validation via `/api/demandes-integration/...` | L'infirmiere demande ; le directeur de l'etablissement (ou l'admin) valide |
| Infirmieres d'un etablissement | `GET /api/etablissements/{id}/infirmiers` | Directeur de l'etablissement, admin |

- Au demarrage, `ContraintesEnumMigration` rend `demandes_integration.medecin_id` facultatif et retire la
  contrainte CHECK obsolete sur `initiateur` (nouvelle valeur `INFIRMIER`).
- Navigation de l'infirmiere vers le patient : entierement dans l'application (carte, trace, consignes
  vocales en francais), sans redirection vers Google Maps. Le calcul d'itineraire utilise par defaut le serveur
  OSRM public de demonstration ; en production, heberger une instance OSRM et la configurer cote frontend :
  `VITE_ROUTAGE_URL=https://osrm.mondomaine.cm`. Les fonds de carte sont configurables avec `VITE_TUILES_URL`.

## Securite et configuration (production)

Le serveur **refuse de demarrer hors developpement** si l'une de ces variables manque ou est faible :

| Variable | Contenu | Generer |
|---|---|---|
| `JWT_SECRET` | cle HS256, 256 bits min., Base64 | `openssl rand -base64 48` |
| `CLE_CHIFFREMENT` | cle AES-256 (32 octets, Base64) des donnees de sante | `openssl rand -base64 32` |
| `SUPER_ADMIN_EMAIL` / `SUPER_ADMIN_PASSWORD` | compte admin cree au 1er demarrage (12 caracteres min.) | |
| `CORS_ORIGINES` | URL du frontend, separees par des virgules (sans `*`) | |

- **`CLE_CHIFFREMENT` doit etre sauvegardee en lieu sur** : sans elle, les champs medicaux chiffres
  (allergies, antecedents, diagnostics, comptes rendus, ordonnances...) et les fichiers prives (photos,
  documents scannes) deviennent illisibles. Ne jamais la changer sans re-chiffrer les donnees.
- `mvn spring-boot:run` active le profil `dev` (valeurs de developpement publiques, `application-dev.yml`).
  Le jar / l'image Docker n'active aucun profil : copier `.env.example` en `.env` pour docker-compose.
- Au demarrage, `MigrationChiffrement` chiffre les donnees enregistrees avant l'activation du chiffrement (idempotent).
- Connexion bloquee 15 min apres 5 echecs (par email) ou 20 (par adresse IP). Mot de passe oublie : code a 6 chiffres
  par SMS (`/api/auth/mot-de-passe-oublie`, `/api/auth/reinitialiser-mot-de-passe`). Sans fournisseur SMS configure,
  le code n'est visible que dans les journaux du profil dev.

## Rendez-vous, notifications et tableau de bord

- Cycle : demande du patient (`EN_ATTENTE`) -> le medecin **accepte**, **refuse** (motif) ou **reporte** sur un creneau libre
  (`/api/rendez-vous/{id}/accepter|refuser|reporter`) -> `TERMINE` / `NO_SHOW`. Le patient est prevenu a chaque etape.
- Notifications : toujours dans l'application (`/api/notifications`, temps reel sur `/user/queue/notifications`),
  plus push si l'appareil est abonne et SMS pour les evenements importants.
- Rappels automatiques des rendez-vous confirmes : la veille et une heure avant (`RappelsRendezVousPlanificateur`).
- Avis patients sur les medecins apres un rendez-vous effectue (`POST /api/rendez-vous/{id}/avis`), moderation admin.
- Fin de teleconsultation : compte rendu + ordonnance en une action (`POST /api/teleconsultations/{id}/cloture`).
- Etablissements geolocalises (`latitude` / `longitude`) pour la carte dans l'application.
- Statistiques des 30 derniers jours : `GET /api/dashboard/statistiques` (directeur : ses etablissements ; admin : tout).
