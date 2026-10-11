#!/bin/sh
set -eu
umask 077

build_tools="$ANDROID_SDK_ROOT/build-tools/36.0.0"
directory=$(mktemp -d)
trap 'rm -rf "$directory"' EXIT
trap 'exit 1' HUP INT TERM

gpg --homedir "$directory" --batch --pinentry-mode loopback \
  --passphrase-file /run/secrets/KEYSTORE_GPG_PASSPHRASE \
  --output "$directory/release.jks" --decrypt /signing/release.jks.gpg
"$build_tools/zipalign" -f -P 16 4 \
  /output_apk/app-release-unsigned.apk "$directory/aligned.apk"
"$build_tools/apksigner" sign \
  --ks "$directory/release.jks" --ks-key-alias release \
  --ks-pass file:/run/secrets/KEYSTORE_PASSWORD \
  --key-pass file:/run/secrets/KEY_PASSWORD \
  --out /output_apk/app-release.apk "$directory/aligned.apk"
"$build_tools/apksigner" verify --verbose /output_apk/app-release.apk
