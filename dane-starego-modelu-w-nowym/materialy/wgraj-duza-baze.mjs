#!/usr/bin/env node
// Wgrywa dużą bazę campsite-power (duza-baza.sql.gz) do działającej bazy. Node 22, bez paczek.
// Najpierw raz uruchom aplikację (zakłada tabele), potem:
//
//   node wgraj-duza-baze.mjs                 # docker compose (serwis postgres), uruchom w katalogu repo
//   node wgraj-duza-baze.mjs --podman        # kontener campsite-power-pg z Przygotowania
//   node wgraj-duza-baze.mjs --psql          # lokalny PostgreSQL (localhost:5432, albo CAMPSITE_DB_PORT)
//   node wgraj-duza-baze.mjs --kontener <nazwa> [--docker|--podman]
//
// Plik może leżeć obok skryptu albo wskaż go: --plik <ścieżka>. Wgrywa się ponownie bez szkody (najpierw sprząta swoje wiersze).
import { spawn } from 'node:child_process';
import { createReadStream, existsSync } from 'node:fs';
import { createGunzip } from 'node:zlib';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const a = process.argv.slice(2);
const opt = n => { const i = a.indexOf('--' + n); return i >= 0 ? (a[i + 1] && !a[i + 1].startsWith('--') ? a[i + 1] : true) : null; };
const plik = opt('plik') || path.join(path.dirname(fileURLToPath(import.meta.url)), 'duza-baza.sql.gz');
if (!existsSync(plik)) { console.error(`Nie ma pliku ${plik}. Pobierz duza-baza.sql.gz ze strony zadania obok tego skryptu.`); process.exit(1); }

const psql = ['psql', '-q', '-v', 'ON_ERROR_STOP=1', '-U', 'campsite', 'campsite_power'];
let cmd, args, env = process.env;
if (opt('psql')) {
  cmd = 'psql'; args = ['-q', '-v', 'ON_ERROR_STOP=1', '-h', process.env.CAMPSITE_DB_HOST || 'localhost', '-p', process.env.CAMPSITE_DB_PORT || '5432', '-U', 'campsite', 'campsite_power'];
  env = { ...process.env, PGPASSWORD: process.env.CAMPSITE_DB_PASSWORD || 'campsite' };
} else if (opt('kontener') || opt('podman')) {
  cmd = opt('podman') && !opt('docker') ? 'podman' : 'docker';
  args = ['exec', '-i', typeof opt('kontener') === 'string' ? opt('kontener') : 'campsite-power-pg', ...psql];
} else {
  cmd = 'docker'; args = ['compose', 'exec', '-T', 'postgres', ...psql];
}

console.log(`wgrywam ${path.basename(plik)} przez: ${cmd} ${args.join(' ')}`);
const start = Date.now();
const p = spawn(cmd, args, { env, stdio: ['pipe', 'inherit', 'inherit'] });
p.on('error', e => { console.error(`Nie mogę uruchomić „${cmd}”: ${e.message}`); process.exit(1); });
createReadStream(plik).pipe(createGunzip()).pipe(p.stdin);
p.on('close', code => {
  if (code) { console.error(`\nNie wyszło (kod ${code}). Czy baza działa i czy aplikacja choć raz wystartowała (tabele)?`); process.exit(code); }
  console.log(`gotowe w ${Math.round((Date.now() - start) / 1000)} s. Uruchom aplikację ponownie.`);
});
