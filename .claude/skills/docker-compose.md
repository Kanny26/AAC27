# Docker Compose - Entorno de Desarrollo Local
- Versión mínima: `3.8`
- Servicios obligatorios: `db` (MySQL 8), `redis`, `backend`, `frontend` (Nginx o dev server)
- Volúmenes: `db_data` persistente, `./backend` y `./frontend` montados para hot-reload
- Redes: `backend_net`, `frontend_net` (aislamiento lógico)
- Variables de entorno: usar `.env` y `.env.example`. Nunca credenciales hardcodeadas.
- Comandos útiles: `docker compose up -d`, `docker compose logs -f backend`, `docker compose down -v`
- Healthchecks: MySQL listo antes de iniciar backend. Backend listo antes de frontend.
- Puerto mapeo: 8080 (API), 5173 (Vite dev), 3306 (DB), 6379 (Redis)