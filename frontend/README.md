# AMANAH Frontend

A dependency-free banking dashboard for the AMANAH Spring Boot API.

## Run locally

1. Start the backend from the repository root:

```powershell
docker compose up -d
```

2. Start the frontend from this directory. Python 3 is required:

```powershell
python -m http.server 5500
```

3. Open <http://localhost:5500>.

The UI calls the backend at `http://localhost:8080` and supports login, account creation, deposits, withdrawals, balances, and recent transaction activity.
