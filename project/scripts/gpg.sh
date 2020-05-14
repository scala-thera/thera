#!/bin/sh
# See https://github.com/sbt/sbt-pgp/issues/173#issuecomment-579288675
gpg --pinentry-mode loopback $@
