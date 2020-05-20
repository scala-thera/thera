#!/usr/bin/env bash
set -eux

echo $PGP_SECRET > gpg_key
gpg --import gpg_key
rm gpg_key

./mill thera.publish \
  --sonatypeCreds $SONATYPE_USER:$SONATYPE_PASSWORD \
  --gpgArgs --passphrase=$PGP_PASSPHRASE,--batch,--yes,-a,-b \
  --readTimeout 600000 \
  --release true \
  --signed true
