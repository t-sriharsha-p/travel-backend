# BharatExplore AI: Backend

Spring Boot 3 (Java 17) REST API with MySQL, JWT auth and a Gemini-powered travel planner.

## Run locally

Requires Java 17+ and Maven.

**Without MySQL (quickest):** uses an in-memory H2 database, data resets on restart.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**With MySQL:** create nothing by hand; the database is created automatically if your user may create databases.

```bash
export DB_URL="jdbc:mysql://localhost:3306/bharatexplore?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
export DB_USER=root
export DB_PASSWORD=yourpassword
export GEMINI_API_KEY=your_key   # optional
mvn spring-boot:run
```

The API starts on http://localhost:8080. Check `GET /api/health`.
On first start, 10 destinations (with places, hotels and restaurants) are seeded from `src/main/resources/seed-data.json`.

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | no | Create account, returns JWT |
| POST | `/api/auth/login` | no | Log in, returns JWT |
| GET | `/api/auth/me` | yes | Current user |
| GET | `/api/destinations?q=&region=` | no | Search destinations |
| GET | `/api/destinations/{id}` | no | Destination with places, hotels, restaurants |
| POST | `/api/planner/generate` | yes | Generate and save a day-wise itinerary |
| GET | `/api/plans` | yes | My saved plans |
| GET | `/api/plans/{id}` | yes | One saved plan |
| DELETE | `/api/plans/{id}` | yes | Delete a plan |

Send the token as `Authorization: Bearer <token>`.

Planner request body:

```json
{ "destination": "Jaipur", "days": 3, "budget": 20000,
  "interests": ["Heritage", "Food"], "foodPreference": "VEG", "travelStyle": "Balanced" }
```

Interests: Heritage, Nature, Adventure, Spiritual, Food, Beaches, Shopping, Nightlife, Wildlife, Relaxation.
Food preference: VEG, NON_VEG, ANY. Travel style: Relaxed, Balanced, Packed, Luxury, Backpacker.

If `GEMINI_API_KEY` is empty, or the Gemini call fails, the planner falls back to a rule-based itinerary built from the database (the response has `"source": "rule-based"`).

## Environment variables

See `.env.example`.

## Deploy (Render, Docker)

1. Push this repo to GitHub.
2. On Render: New > Web Service > connect the repo, runtime **Docker**.
3. Add the environment variables from `.env.example`.
4. Create a MySQL database (Railway, Aiven or PlanetScale-compatible) and use its JDBC URL as `DB_URL`.
5. After the frontend is on Vercel, set `CORS_ORIGINS` to the Vercel URL.
