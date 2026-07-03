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
