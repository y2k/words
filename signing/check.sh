#!/bin/sh
set -eu
umask 077

: "${KEYSTORE_GPG_PASSPHRASE:?Set KEYSTORE_GPG_PASSPHRASE}"
: "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD}"
: "${KEY_PASSWORD:?Set KEY_PASSWORD}"
export KEYSTORE_PASSWORD KEY_PASSWORD

directory=$(mktemp -d)
trap 'rm -rf "$directory"' EXIT
trap 'exit 1' HUP INT TERM

printf '%s' "$KEYSTORE_GPG_PASSPHRASE" |
  gpg --batch --pinentry-mode loopback --passphrase-fd 0 \
    --output "$directory/release.jks" --decrypt \
    "$(dirname "$0")/release.jks.gpg"

keytool -certreq -alias release -keystore "$directory/release.jks" \
  -storepass:env KEYSTORE_PASSWORD -keypass:env KEY_PASSWORD \
  -file "$directory/request.csr"

printf '%s\n' 'PASS: keystore decrypted and signing key accessible'
