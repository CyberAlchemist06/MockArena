# Identity Service

Local port: `8082`. It owns users, roles, password hashes, and JWT issuance in the `identity` PostgreSQL schema.

## Local JWT keys

Generate an RSA key pair outside the repository, then set paths to the PKCS#8 private and X.509 public PEM files:

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out $env:TEMP\mockarena-identity-private.pem
openssl pkey -in $env:TEMP\mockarena-identity-private.pem -pubout -out $env:TEMP\mockarena-identity-public.pem
$env:IDENTITY_JWT_PRIVATE_KEY_PATH = "$env:TEMP\mockarena-identity-private.pem"
$env:IDENTITY_JWT_PUBLIC_KEY_PATH = "$env:TEMP\mockarena-identity-public.pem"
.\mvnw.cmd spring-boot:run
```

Never commit private keys. A development admin is disabled by default. It requires the `dev` profile plus `IDENTITY_DEV_ADMIN_BOOTSTRAP_ENABLED=true`, `IDENTITY_DEV_ADMIN_BOOTSTRAP_EMAIL`, and `IDENTITY_DEV_ADMIN_BOOTSTRAP_PASSWORD`.
