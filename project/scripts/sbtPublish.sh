#!/usr/bin/env bash
set -e

# Release command:
RELEASE_CMD="${1:?Missing publish command}"

# Make sure required environment variable are set
: "${SONATYPE_USER:?not set}"
: "${SONATYPE_PW:?not set}"
: "${PGP_PASSPHRASE:?not set}"
: "${PGP_SECRET:?is not set}"

export GPG_TTY="$(tty)"
echo "$PGP_SECRET" | gpg --batch --import

# run sbt with the supplied arg
sbt "$RELEASE_CMD"
