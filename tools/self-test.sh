#!/usr/bin/env bash
# The server self-test (dyedresin.DevSelfTest): every recipe through the real recipe manager, placing and breaking a
# clump. Output in run/selftest.out; prints the failed checks and the count, exits non-zero on a failure.
set -u
cd "$(dirname "$0")/.."
if [ "${OS:-}" = Windows_NT ]; then
  export MSYS_NO_PATHCONV=1 JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot' TEMP='C:\jtmp' TMP='C:\jtmp'
fi
mkdir -p run
echo "eula=true" > run/eula.txt
grep -q '^online-mode' run/server.properties 2>/dev/null || printf 'online-mode=false\nserver-port=25596\n' >> run/server.properties
./gradlew runServer -PselfTest > run/selftest.out 2>&1
grep "SELFTEST FAIL" run/selftest.out
grep "SELFTEST done" run/selftest.out || { echo "self-test did not finish, see run/selftest.out"; exit 1; }
! grep -q "SELFTEST FAIL" run/selftest.out
