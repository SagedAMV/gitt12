#!/usr/bin/env bash
set -euo pipefail
SUPABASE_PROJECT_REF="${SUPABASE_PROJECT_REF:-puewznmoypmlouxzzoqz}"
[[ "$SUPABASE_PROJECT_REF" == "puewznmoypmlouxzzoqz" ]] || { echo 'Refusing to deploy: only the dedicated GitHub Vault project is authorized.'; exit 1; }
command -v supabase >/dev/null || { echo 'Install Supabase CLI first'; exit 1; }
echo "Target: $SUPABASE_PROJECT_REF"
read -rp 'Apply migrations and deploy functions? Type DEPLOY: ' answer
[[ "$answer" == DEPLOY ]] || exit 1
supabase link --project-ref "$SUPABASE_PROJECT_REF"
supabase db push
for fn in auth-login registration account repositories files downloads uploads admin-users admin-repositories admin-items audit; do
 supabase functions deploy "$fn" --project-ref "$SUPABASE_PROJECT_REF" --no-verify-jwt
done
echo 'Set server secrets separately via supabase secrets set --env-file <private-file>.'
echo 'Disable public Auth signup in the dashboard, then bootstrap the first admin.'
