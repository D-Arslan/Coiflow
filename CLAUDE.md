# Coiflow - CLAUDE.md

## Projet
SaaS de gestion de salon de coiffure : rendez-vous, caisse, commissions, multi-tenant.

## Repository
- **GitHub** : https://github.com/D-Arslan/Coiflow
- **Branches** : `main` (stable), `dev` (développement actif)
- Workflow : développer sur `dev`, merger dans `main` quand stable

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
- **Flyway** pour les migrations (`src/main/resources/db/migration/`, V1 à V5)
- **JWT RS256** (clés RSA dans `src/main/resources/keys/`) + **HttpOnly Cookies**
- **MapStruct** + **Lombok** (`@Slf4j`, `@RequiredArgsConstructor`) pour DTO mapping
- **SpringDoc OpenAPI** → Swagger UI sur `/swagger-ui.html`
- Package : `com.coiflow`

### Frontend
- **React 18** + TypeScript + Vite
- **Tailwind CSS** (via `@tailwindcss/vite`)
- **TanStack Query v5** (server state) + **AuthContext** (auth state, pas de Zustand)
- **React Router DOM v6** (routing par rôle, future flags v7 activés)
- **react-toastify** pour les notifications
- **Axios** avec intercepteurs (auto-refresh 401, `withCredentials: true`)
- Path alias : `@/` → `./src/`
- Proxy dev : `/api` → `http://localhost:8085`

## Architecture backend — Hybride layer + sous-modules
```
com.coiflow/
├── config/              → CORS, OpenAPI
├── security/            → SecurityConfig, jwt/, TenantContextHolder, LoginRateLimiter
├── controller/{module}/ → auth, salon, staff, catalog, client, appointment, transaction, commission, dashboard
├── service/{module}/    → 1 service par module
├── model/{module}/      → entités JPA groupées par domaine
├── model/enums/         → Role, AppointmentStatus, PaymentMethod, TransactionStatus
├── repository/{module}/ → Spring Data JPA repos
├── dto/{module}/        → Request/Response DTOs séparés
├── mapper/              → MapStruct mappers (SalonMapper)
├── exception/           → GlobalExceptionHandler (@Slf4j) + BusinessException + ResourceNotFoundException
└── util/
```

## Architecture frontend — Feature-based
```
src/
├── config/api.ts                     → endpoints centralisés
├── router/AppRouter.tsx              → routes + ProtectedRoute
├── features/auth/pages/              → LoginPage (redirect auto si déjà connecté)
├── features/admin/pages/             → AdminLayout, SalonsPage
├── features/admin/hooks/             → useSalons
├── features/manager/pages/           → ManagerLayout, ManagerDashboard, AppointmentsPage, TransactionsPage, CommissionsPage, StaffPage, ServicesPage, ClientsPage
├── features/manager/hooks/           → useAppointments, useTransactions, useCommissions, useDashboard, useStaff, useServices, useClients
├── features/barber/pages/            → BarberLayout, MySchedule, MyCommissions
├── shared/api/axiosClient.ts         → Axios + intercepteurs (refresh 401, guard /auth/)
├── shared/context/AuthContext.tsx     → user, role, isAuthenticated, authReady
├── shared/components/                → ProtectedRoute, DataTable, Modal, MainLayout, WeekCalendar
├── shared/services/                  → AuthService, SalonService, StaffService, ServiceCatalogService, ClientService, AppointmentService, TransactionService, CommissionService, DashboardService
├── shared/types/                     → auth, salon, staff, service, client, appointment, transaction, commission, dashboard
└── shared/utils/                     → dateHelpers, formatters (DZD), errorMessage, useDebounce
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
- `/api/auth/me` : public (permitAll), retourne 204 si pas de session (évite bruit console)
- @PreAuthorize sur les méthodes de service (`@EnableMethodSecurity` activé)
- LoginRateLimiter : blocage après 3 échecs / 10 min
- TenantContextHolder (ThreadLocal) pour isolation multi-tenant

## Commandes

### Backend
```bash
cd D:/Coiflow/coiflow-backend
mvn compile                    # compiler
mvn clean spring-boot:run      # lancer (port 8085)
mvn test                       # tests
```

### Frontend
```bash
cd D:/Coiflow/coiflow-web
npm run dev                    # dev server (port 3001)
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
- **Devise** : DZD (Dinar algérien), `currencyDisplay: 'code'` pour rendu stable
- **Timezone** : `Africa/Algiers` pour les calculs "du jour" côté dashboard

## Pièges connus
- **Hibernate STI proxy** : `appointment.getBarber()` retourne un proxy `Utilisateur`, pas `Barber`. Utiliser `Hibernate.unproxy()` avant le cast.
- **Admin sans salonId** : les endpoints tenant-scoped retournent 409 pour un admin (pas de salon). C'est voulu.

## Conventions
- Backend : snake_case (BDD), camelCase (Java/JSON)
- Frontend : camelCase partout, PascalCase pour composants
- IDs : UUID v4 (String "36 chars")
- Pas de sessionStorage/localStorage pour l'auth (HttpOnly cookies uniquement)
- Auth bootstrap : GET /api/auth/me au boot de l'app (204 = pas de session)
- Clés RSA dev commitées pour commodité, clés prod injectées via env

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

### Sprint 2 — Gestion du salon ✅
- [x] CRUD Salon (admin) — SalonController, SalonService, SalonMapper
- [x] CRUD Staff / Coiffeurs (manager) — StaffController, StaffService
- [x] CRUD Services / Prestations — ServiceController, ServiceCatalogService
- [x] CRUD Clients — ClientController, ClientService
- [x] Composants partagés : DataTable, Modal, MainLayout
- [x] Pages frontend complètes avec recherche et modales

### Sprint 3 — Coeur métier ✅
- [x] Module Rendez-vous (CRUD + statuts + anti double-booking)
- [x] Planning calendrier WeekCalendar (vue semaine, 8h-20h, slots 30min)
- [x] Module Transactions (encaissement + paiement mixte + validation stricte)
- [x] Calcul auto commissions (TransactionService → Commission)
- [x] Pages : AppointmentsPage, TransactionsPage, CommissionsPage
- [x] Barber : MySchedule (lecture seule)
- [x] Migrations V3 (contraintes uniques), V4 (refresh token), V5 (indexes)

### Sprint 4 — Dashboard & Polish ✅
- [x] Dashboard manager : stats temps réel (CA jour, RDV, coiffeurs actifs) + tableau CA 7 jours
- [x] Backend dashboard : DashboardController/Service avec @PreAuthorize
- [x] Page barber "Mes commissions" avec filtres date et total
- [x] Devise changée EUR → DZD (currencyDisplay: 'code')
- [x] GlobalExceptionHandler production-ready (@Slf4j, message générique)
- [x] Fix console : /api/auth/me retourne 204 (pas 403/401), React Router future flags
- [x] LoginPage : redirect auto si déjà authentifié

### Sprint 5 — À planifier
- [ ] Tests (unitaires + intégration)
- [ ] Configuration PostgreSQL production
- [ ] Déploiement (Docker, CI/CD)
- [ ] Fonctionnalités supplémentaires à définir
