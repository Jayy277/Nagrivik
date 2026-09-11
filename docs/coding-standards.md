# Nagrivic Coding Standards & Conventions

---

## 1. TypeScript Standards (Mobile & Web)

Both the Expo mobile app and Next.js web portals adhere to strict TypeScript standards to ensure type safety, maintainability, and code readability.

### Strict Configuration
- `"strict": true` is enforced in all `tsconfig.json` files.
- `noImplicitAny`, `strictNullChecks`, and `noUnusedLocals` must remain enabled.
- Avoid using `any`. If a value is genuinely unknown at compile time, use `unknown` and narrow it with type guards or runtime validation (e.g., Zod).

### Naming Conventions

| Item | Convention | Example |
|---|---|---|
| **Components** | `PascalCase` | `IssueCard.tsx`, `ReportButton.tsx` |
| **Component Files** | `kebab-case.tsx` | `issue-card.tsx`, `report-button.tsx` |
| **Hooks** | `camelCase` with `use` prefix | `useIssueFeed.ts`, `useUserLocation.ts` |
| **Types / Interfaces** | `PascalCase` | `IssueDTO`, `UserProfile`, `CategoryType` |
| **Variables & Functions**| `camelCase` | `formatDate()`, `calculateDistance()`, `isOpen` |
| **Constants** | `UPPER_SNAKE_CASE` | `MAX_RADIUS_METERS`, `DEFAULT_PAGE_SIZE` |
| **Folders** | `kebab-case` | `issue-details/`, `nearby-map/` |

### Types vs. Interfaces
- Use `interface` for public API data contracts, DTOs, and component prop types that may be extended.
- Use `type` for unions, intersections, mapped types, or primitive aliases:
  ```typescript
  // Interfaces for data contracts
  export interface IssueDTO {
    id: string;
    title: string;
    description: string;
    category: CivicCategory;
    status: IssueStatus;
    location: GeoPoint;
    createdAt: string;
  }

  // Type aliases for unions and states
  export type IssueStatus = 'SUBMITTED' | 'VERIFIED' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
  export type CivicCategory = 'ROADS_POTHOLES' | 'GARBAGE' | 'STREETLIGHTS' | 'WATER' | 'DRAINAGE';
  ```

### State Management Guidelines
- Start with React built-ins: `useState`, `useReducer`, and React Context.
- Do **not** introduce external state-management libraries (such as Redux, Zustand, or MobX) prematurely during early MVP development.

### Import Organization Order
Group imports in the following order with blank lines separating groups:
1. React and React Native / Next.js core packages.
2. Third-party libraries (`expo-*`, `@expo/*`, `lucide-react-native`).
3. Internal aliases (`@/components/...`, `@/features/...`, `@/services/...`).
4. Relative imports (`./types`, `../utils`).
5. Style files (`./styles.module.css`).

---

## 2. Java & Spring Boot Standards (Backend)

The Spring Boot modular monolith follows a clean layered architecture with clear separation of concerns.

### Layered Architecture Flow

```
HTTP Request
    │
    ▼
Controller      --> Validates input, coordinates HTTP status, delegates to Service
    │
    ▼
Service         --> Executes business logic, enforces rules, manages transactions (@Transactional)
    │
    ▼
Repository      --> Handles database queries & persistence via Spring Data JPA / PostGIS
    │
    ▼
Database (PostgreSQL + PostGIS)
```

### Layer Responsibilities & Rules

1. **Controllers (`org.nagrivic.modules.<module>.api` or `controller`)**:
   - Must **not** contain business logic or SQL queries.
   - Responsible only for: request validation (`@Valid`), HTTP status codes, and mapping between HTTP requests and DTOs.
   - Always return `ResponseEntity<T>`.

2. **Services (`org.nagrivic.modules.<module>.service`)**:
   - Core domain business logic lives here.
   - Coordinate cross-entity rules, state transitions, and audit logs.
   - Define transaction boundaries with `@Transactional(readOnly = true)` on classes and `@Transactional` on mutating methods.

3. **Repositories (`org.nagrivic.modules.<module>.repository`)**:
   - Handle database persistence using Spring Data JPA.
   - Use spatial queries (`@Query("SELECT i FROM IssueEntity i WHERE ST_DWithin(...)")`).
   - Must never be injected into controllers directly; always accessed via services.

4. **DTOs vs. Entities**:
   - **Never expose JPA entities directly in API controllers**. Exposing entities causes tight coupling to database schemas, lazy loading serialization bugs, and potential security leaks.
   - Use Java `record` classes for immutable request and response DTOs:
     ```java
     public record IssueResponse(
         UUID id,
         String title,
         String description,
         String category,
         String status,
         Double latitude,
         Double longitude,
         Instant createdAt
     ) {}
     ```

5. **Validation**:
   - Use Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`) on all incoming request DTOs.
   - Annotate controller method arguments with `@Valid`.

6. **Exception Handling**:
   - Throw domain-specific exceptions (e.g., `ResourceNotFoundException`, `DuplicateReportException`, `InvalidStatusTransitionException`).
   - Handle all exceptions centrally using `@RestControllerAdvice`.
   - Never return unhandled stack traces to API clients.

7. **Logging**:
   - Use SLF4J (`private static final Logger log = LoggerFactory.getLogger(MyService.class);`).
   - Use parameterized logging (`log.info("Issue {} reported at [{}, {}]", issueId, lat, lng);`) instead of string concatenation.
   - **Never log passwords, OTPs, auth tokens, or personally identifiable citizen data**.

---

## 3. Git Workflow & Conventions

A simple, standard Git workflow ensures smooth collaboration without excessive overhead.

### Branch Naming Conventions
- `main`: Production-ready, stable codebase.
- `develop`: Integration branch for completed features.
- `feature/<feature-name>`: Feature branch (e.g., `feature/pothole-reporting`, `feature/health-api`).
- `fix/<bug-name>`: Bug fixes (e.g., `fix/location-permission`, `fix/cors-headers`).
- `docs/<topic>`: Documentation updates (e.g., `docs/api-conventions`).

### Commit Message Standards (Conventional Commits)
Write concise commit messages prefixed by category:

| Prefix | Description | Example |
|---|---|---|
| `feat:` | A new feature or capability | `feat: add GET /api/health endpoint` |
| `fix:` | A bug fix | `fix: resolve port conflict in dev server` |
| `docs:` | Documentation changes | `docs: add database and api conventions` |
| `refactor:`| Code restructuring without changing behavior | `refactor: extract CorsConfig into dedicated class` |
| `test:` | Adding or updating tests | `test: add unit test for health controller` |
| `chore:` | Build scripts, dependencies, configuration | `chore: update gradle wrapper to 9.7.1` |
