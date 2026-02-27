# Coiflow - CLAUDE.md

## Projet
SaaS de gestion de salon de coiffure : rendez-vous, caisse, commissions, multi-tenant.
Inspiré du projet FIELDZ (D:\FIELDZ) — SaaS de réservation sportive.

## Structure
```
D:\Coiflow/
├── coiflow-backend/    → Spring Boot 3.4.1, Java 17, Maven
├── coiflow-web/        → React 18, TypeScript, Vite, Tailwind CSS
└── CLAUDE.md
```

## Stack technique

### Backend
- **Java 17** + Spring Boot 3.4.1 + Maven
- **H2** (dev, file-based dans `./data/`) / **PostgreSQL 16** (prod)
- **Flyway** pour les migrations (`src/main/resources/db/migration/`)
- **JWT RS256** (clés RSA dans `src/main/resources/keys/`) + **HttpOnly Cookies**
- **MapStruct** + **Lombok** pour DTO mapping
- **SpringDoc OpenAPI** → Swagger UI sur `/swagger-ui.html`
- Package : `com.coiflow`

### Frontend
- **React 18** + TypeScript + Vite
- **Tailwind CSS** (via `@tailwindcss/vite`)
- **TanStack Query** (server state) + **AuthContext** (auth state, pas de Zustand)
- **React Router DOM v6** (routing par rôle)
- **Axios** avec intercepteurs (auto-refresh 401, `withCredentials: true`)
- Path alias : `@/` → `./src/`
- Proxy dev : `/api` → `http://localhost:8080`

## Architecture backend — Hybride layer + sous-modules
```
com.coiflow/
├── config/              → CORS, OpenAPI
├── security/            → SecurityConfig, jwt/, TenantContextHolder, LoginRateLimiter
├── controller/{module}/ → 1 controller par module
├── service/{module}/    → 1 service par module
├── model/{module}/      → entités JPA groupées par domaine
├── model/enums/         → Role, AppointmentStatus, PaymentMethod, TransactionStatus
├── repository/{module}/ → Spring Data JPA repos
├── dto/{module}/        → Request/Response DTOs séparés
├── mapper/              → MapStruct mappers
├── exception/           → GlobalExceptionHandler + custom exceptions
└── util/
```

## Architecture frontend — Feature-based
```
src/
├── config/api.ts              → endpoints centralisés
├── router/AppRouter.tsx       → routes + ProtectedRoute
├── features/{role}/pages/     → pages par rôle (auth, admin, manager, barber)
├── features/{role}/hooks/     → TanStack Query hooks par feature
├── shared/api/axiosClient.ts  → Axios + intercepteurs
├── shared/context/AuthContext  → user, role, isAuthenticated, authReady
├── shared/components/         → ProtectedRoute, composants partagés
├── shared/services/           → API services (AuthService, etc.)
├── shared/types/              → interfaces TypeScript
└── shared/utils/              → formatage dates, montants
```

## Modèle de données

### Multi-tenancy
Discriminator column `salon_id` sur toutes les tables tenant-scoped.

### Utilisateurs — Single-Table Inheritance
Table `utilisateur` avec `dtype` discriminator : ADMIN, MANAGER, BARBER.
- ADMIN : pas de salon_id (plateforme)
- MANAGER : salon_id obligatoire
- BARBER : salon_id + commission_rate

### Tables (10)
salons, utilisateur, clients, services, appointments, appointment_services,
transactions, payments, commissions, refresh_tokens

### ENUMs
- Role : ADMIN, MANAGER, BARBER
- AppointmentStatus : SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW
- PaymentMethod : CASH, CARD, CHECK, TRANSFER, OTHER
- TransactionStatus : COMPLETED, VOIDED

### Relations clés
- 1 Transaction → N Payments (paiement mixte)
- 1 Appointment → N Services (via appointment_services avec price_applied)
- 1 Transaction → 1 Commission (auto-calculée)

## Sécurité
- JWT RS256 (access 15min + refresh 7j) en HttpOnly cookies
- 2 filter chains : API (JWT stateless) + Public (Swagger, H2)
- @PreAuthorize sur les méthodes de service
- LoginRateLimiter : blocage après 3 échecs / 10 min
- TenantContextHolder (ThreadLocal) pour isolation multi-tenant

## Commandes

### Backend
```bash
cd D:/Coiflow/coiflow-backend
mvn compile                    # compiler
mvn spring-boot:run            # lancer (port 8080)
mvn test                       # tests
```

### Frontend
```bash
cd D:/Coiflow/coiflow-web
npm run dev                    # dev server (port 5173)
npm run build                  # build prod
npx tsc --noEmit               # type check
```

## Comptes de test
- Admin : `admin@coiflow.com` / `admin123`

## Règles métier importantes
- **Anti double-booking** : validation overlap dans AppointmentService + index DB
- **Paiement mixte** : sum(payments) == sum(services), validation stricte
- **Prix historisé** : price_applied figé dans appointment_services
- **Soft delete** : champ `active` (boolean), jamais de DELETE physique
- **Optimistic locking** : @Version sur Transaction et Appointment
- **TransactionStatus** : COMPLETED/VOIDED (pas de DELETE, traçabilité)

## Conventions
- Backend : snake_case (BDD), camelCase (Java/JSON)
- Frontend : camelCase partout, PascalCase pour composants
- IDs : UUID v4 (String "36 chars")
- Pas de sessionStorage/localStorage pour l'auth (HttpOnly cookies uniquement)
- Auth bootstrap : GET /api/auth/me au boot de l'app

## État d'avancement

### Sprint 1 — Fondations ✅
- [x] Setup backend (Spring Boot, Maven, structure packages)
- [x] Setup frontend (Vite, React 18, Tailwind, TanStack Query)
- [x] Schema BDD complet (V1 + V2 seed admin)
- [x] 12 entités JPA + 4 enums
- [x] JWT RS256 + HttpOnly Cookies + refresh token rotation
- [x] SecurityConfig (2 chains) + LoginRateLimiter
- [x] AuthController/Service (login, logout, refresh, /me)
- [x] Frontend : AuthContext, axiosClient, ProtectedRoute, LoginPage
- [x] 3 pages placeholder (Admin, Manager, Barber)
- [x] Backend compile et démarre OK
- [x] Frontend build OK (0 erreurs TS)

### Sprint 2 — Gestion du salon (à faire)
- [ ] CRUD Salon (admin)
- [ ] CRUD Staff / Coiffeurs (manager)
- [ ] CRUD Services / Prestations
- [ ] CRUD Clients
- [ ] Pages frontend avec DataTable

### Sprint 3 — Coeur métier (à faire)
- [ ] Module Rendez-vous (CRUD + statuts + anti double-booking)
- [ ] Planning calendrier (vue semaine)
- [ ] Module Transactions (encaissement + paiement mixte)
- [ ] Calcul auto commissions
- [ ] Page Point de Vente

### Sprint 4 — Dashboard & Polish (à faire)
- [ ] Dashboard manager (stats, graphiques)
- [ ] Pages coiffeur (planning, commissions)
- [ ] Tests (unitaires + intégration Testcontainers)

## Plan détaillé
Voir : C:\Users\hp\.claude\plans\fuzzy-hugging-sutherland.md
