# Repository Instructions

- Do not open, read, quote, or inspect `.env` files in this repository.
- Do not open, read, quote, or inspect local secret key files such as Firebase credential JSON files.
- If configuration values are needed, ask the user to provide the non-sensitive values explicitly or work from application defaults and checked-in config files.
- For API work, follow RFC 9457 / Problem Details principles for error responses by default.
- Do not introduce new ad hoc string error bodies or bespoke error DTOs for APIs unless backward compatibility explicitly requires a temporary exception.
- When changing existing APIs incrementally, prefer a documented migration path toward Problem Details rather than creating another response shape.
- During code review, treat new or changed API error responses that diverge from RFC 9457 / Problem Details as review findings unless a documented backward compatibility exception exists.
- During code review, also evaluate whether changes respect DDD + hexagonal architecture boundaries and whether naming is precise and intention-revealing.
- Architecture and package guidance live under `docs/architecture/`.
- Standards and engineering policies live under `docs/standards/`.
- Product and feature design docs live under `docs/product-specs/`.
- Frontend/mobile handoff docs live under `docs/handoffs/`.
- Operational procedures and incident response runbooks live under `docs/runbooks/`.
- Incident retrospectives live under `docs/incident-retrospectives/`.
- `docs/superpowers/` contains local-only working docs and is not part of the official documentation system.
