# Release signing

`release.jks.gpg` is the release keystore encrypted with GPG (AES-256).
Only this encrypted file belongs in Git. Never commit the decrypted keystore
or its passwords. The key alias is `release`.

GitHub Actions secrets for `y2k/words`:

- `KEYSTORE_GPG_PASSPHRASE`: decrypts the keystore.
- `KEYSTORE_PASSWORD`: opens the keystore.
- `KEY_PASSWORD`: accesses the private key.

## Backup

The initial keystore and `credentials.json` are backed up locally in
`~/.local/share/words/signing/` (directory mode 700, files mode 600).
Move both to secure, independent backup storage. Losing the key or its
passwords can prevent signing compatible updates. GitHub Secrets cannot
be read back, so they are not a substitute for this backup.

## Verify

With GPG and a JDK installed, set the three secrets in your environment and run:

```sh
sh signing/check.sh
```

This decrypts to a temporary directory, verifies access to the private key
by generating a certificate request, and removes the temporary files.
Do not use shell tracing (`set -x`) when handling secrets.

## Future CI integration

The build workflow must decrypt the keystore to a restricted temporary
directory, use alias `release` and the two keystore passwords to sign the APK,
then remove the decrypted keystore even if the build fails. Upload only the
APK, not the keystore or temporary directory. Give signing secrets only to
trusted workflow runs; do not expose them to pull requests from forks.

Keystore preparation alone does not enable release signing: the current
`make build_apk` still builds a debug APK.
