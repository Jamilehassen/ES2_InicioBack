-- ########## CORREÇÃO 1: Criar o Schema ##########
-- Cria o schema que a aplicação vai usar (definido no .env)
CREATE SCHEMA IF NOT EXISTS schema_es2;

-- ########## CORREÇÃO 2: O 'administrador' já foi criado pelo Docker. ##########
-- Vamos apenas criar o utilizador secundário 'dev_readonly'
CREATE ROLE dev_readonly LOGIN PASSWORD 'dev2025';

-- Remove permissões públicas do schema
REVOKE CREATE ON SCHEMA schema_es2 FROM PUBLIC;
REVOKE USAGE ON SCHEMA schema_es2 FROM PUBLIC;

-- Concede permissões ao 'administrador' (que é o dono)
-- O 'administrador' precisa de USAGE e CREATE para o Hibernate (ddl-auto) funcionar
GRANT USAGE ON SCHEMA schema_es2 TO administrador;
GRANT CREATE ON SCHEMA schema_es2 TO administrador;

-- Concede permissões ao 'dev_readonly' (apenas leitura)
GRANT USAGE ON SCHEMA schema_es2 TO dev_readonly;

-- Define permissões para o 'administrador' em TODAS as tabelas (atuais e futuras)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA schema_es2 TO administrador;
ALTER DEFAULT PRIVILEGES IN SCHEMA schema_es2 FOR ROLE administrador GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO administrador;
ALTER DEFAULT PRIVILEGES IN SCHEMA schema_es2 FOR ROLE administrador GRANT USAGE, SELECT ON SEQUENCES TO administrador;

-- Define permissões para o 'dev_readonly' em TODAS as tabelas (atuais e futuras)
GRANT SELECT ON ALL TABLES IN SCHEMA schema_es2 TO dev_readonly;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA schema_es2 TO dev_readonly;

ALTER DEFAULT PRIVILEGES IN SCHEMA schema_es2 FOR ROLE administrador GRANT SELECT ON TABLES TO dev_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA schema_es2 FOR ROLE administrador GRANT USAGE, SELECT ON SEQUENCES TO dev_readonly;

-- Revoga permissões específicas da tabela de password reset para o 'dev_readonly'
-- Nota: Esta tabela precisa de existir primeiro. Se o ddl-auto=create,
-- este comando pode falhar na primeira execução, mas as permissões default acima
-- já previnem o acesso se esta tabela for criada depois.
-- Para garantir, vamos fazer o 'administrador' ser o dono do schema.
ALTER SCHEMA schema_es2 OWNER TO administrador;

-- Tentativa de revogar privilégios. Se a tabela não existir, não faz mal.
-- (Melhoria: Usar um DO $$ BEGIN ... END; $$ para verificar se a tabela existe)
REVOKE ALL PRIVILEGES ON TABLE schema_es2.password_reset_code FROM dev_readonly;