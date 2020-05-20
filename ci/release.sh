#!/usr/bin/env bash
set -eux

echo $PGP_SECRET | base64 --decode | gpg --import --no-tty --batch --yes

./mill thera.publish \
  --sonatypeCreds $SONATYPE_USER:$SONATYPE_PW \
  --gpgArgs --passphrase=$PGP_PASSPHRASE,--batch,--yes,-a,-b,--no-tty \
  --readTimeout 600000 \
  --release true \
  --signed true
