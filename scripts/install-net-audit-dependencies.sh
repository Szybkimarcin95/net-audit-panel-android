#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

APP_DIR="${NET_AUDIT_DIR:-$HOME/net-audit-panel}"
VENV_DIR="$APP_DIR/.venv"
LOG_DIR="$APP_DIR/logs"
LOG_FILE="$LOG_DIR/dependencies-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "$LOG_DIR"
exec > >(tee -a "$LOG_FILE") 2>&1

ok(){ printf '[OK] %s\n' "$*"; }
warn(){ printf '[UWAGA] %s\n' "$*"; }
fail(){ printf '[BŁĄD] %s\n' "$*" >&2; exit 1; }

command -v pkg >/dev/null 2>&1 || fail "Uruchom plik w oficjalnym środowisku Termux."

printf '\nNET AUDIT PANEL — INSTALATOR ZALEŻNOŚCI\n'
printf 'Katalog aplikacji: %s\n' "$APP_DIR"
printf 'Log: %s\n\n' "$LOG_FILE"

printf '[1/6] Aktualizacja indeksów pakietów\n'
pkg update -y

printf '[2/6] Instalacja pakietów wykonawczych\n'
pkg install -y \
  python \
  git \
  nmap \
  termux-api \
  iproute2 \
  jq \
  curl \
  openssl \
  procps \
  dnsutils \
  net-tools \
  traceroute

printf '[3/6] Tworzenie izolowanego środowiska Python\n'
mkdir -p "$APP_DIR"
if [[ ! -x "$VENV_DIR/bin/python" ]]; then
  python -m venv "$VENV_DIR"
fi
"$VENV_DIR/bin/python" -m pip install --upgrade pip setuptools wheel
"$VENV_DIR/bin/python" -m pip install \
  'Flask>=3.0,<4' \
  'requests>=2.31,<3' \
  'waitress>=3,<4'

printf '[4/6] Kontrola poleceń\n'
required=(python git nmap ip jq curl openssl)
optional=(termux-wifi-connectioninfo termux-wifi-scaninfo termux-location)
missing=0
for cmd in "${required[@]}"; do
  if command -v "$cmd" >/dev/null 2>&1; then
    ok "$cmd -> $(command -v "$cmd")"
  else
    warn "brak wymaganego polecenia: $cmd"
    missing=1
  fi
done
for cmd in "${optional[@]}"; do
  if command -v "$cmd" >/dev/null 2>&1; then
    ok "$cmd -> dostępne"
  else
    warn "$cmd -> niedostępne"
  fi
done

printf '[5/6] Kontrola bibliotek Python\n'
"$VENV_DIR/bin/python" - <<'PY'
import flask, requests, waitress
print('[OK] Flask', flask.__version__ if hasattr(flask, '__version__') else 'installed')
print('[OK] requests', requests.__version__)
print('[OK] waitress', waitress.__version__ if hasattr(waitress, '__version__') else 'installed')
PY

printf '[6/6] Informacja o Bluetooth Bridge\n'
cat <<'TXT'
[INFO] Adapter Bluetooth Androida NIE jest udostępniany Termuxowi przez BlueZ,
PyBluez ani bleak. Te pakiety nie są instalowane, ponieważ dawałyby fałszywe
wrażenie obsługi sprzętu.

Rzeczywisty skan BLE i Bluetooth Classic będzie wykonywała aplikacja APK
Net Audit Bluetooth Bridge. Termux połączy się z nią lokalnie przez:
http://127.0.0.1:8766

Android poprosi osobno o uprawnienie „Urządzenia w pobliżu”.
TXT

printf '\nWERSJE\n'
python --version
nmap --version | sed -n '1p'
curl --version | sed -n '1p'
printf 'Termux prefix: %s\n' "${PREFIX:-UNKNOWN}"

if [[ "$missing" -ne 0 ]]; then
  printf '\nSTATUS: PASS WITH WARNINGS\n'
  exit 2
fi

printf '\nSTATUS: PASS\n'
printf 'Zależności zainstalowane. Następny etap: instalacja APK Bluetooth Bridge.\n'
