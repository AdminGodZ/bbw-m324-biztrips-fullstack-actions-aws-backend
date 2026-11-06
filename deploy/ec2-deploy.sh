#!/usr/bin/env bash
set -euo pipefail
: "${EC2_HOST:?Missing EC2_HOST}" "${EC2_USER:?Missing EC2_USER}"
: "${EC2_SSH_KEY:?Missing EC2_SSH_KEY}" "${EC2_KNOWN_HOSTS:?Missing pinned EC2_KNOWN_HOSTS}"
key_dir=$(mktemp -d)
runner_cidr=$(curl -fsS https://checkip.amazonaws.com)/32
cleanup() {
  rm -rf "$key_dir"
  aws ec2 revoke-security-group-ingress --group-id "$EC2_SECURITY_GROUP" --protocol tcp --port 22 --cidr "$runner_cidr" >/dev/null 2>&1 || true
}
trap cleanup EXIT
aws ec2 authorize-security-group-ingress --group-id "$EC2_SECURITY_GROUP" --protocol tcp --port 22 --cidr "$runner_cidr" >/dev/null

printf '%s\n' "$EC2_SSH_KEY" > "$key_dir/key"
printf '%s\n' "$EC2_KNOWN_HOSTS" > "$key_dir/known_hosts"
chmod 600 "$key_dir/"*
export RSYNC_RSH="ssh -i $key_dir/key -o UserKnownHostsFile=$key_dir/known_hosts -o StrictHostKeyChecking=yes"
rsync -az -e "$RSYNC_RSH" target/*.jar "$EC2_USER@$EC2_HOST:/opt/businesstrips/app.jar.next"
$RSYNC_RSH "$EC2_USER@$EC2_HOST" 'bash -s' <<'REMOTE'
set -euo pipefail
cd /opt/businesstrips
if test -f app.jar; then cp app.jar app.jar.previous; fi
mv app.jar.next app.jar
sudo systemctl restart businesstrips
for attempt in $(seq 1 60); do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health; then exit 0; fi
  sleep 2
done
sudo journalctl -u businesstrips -n 60 --no-pager
if test -f app.jar.previous; then mv app.jar.previous app.jar; sudo systemctl restart businesstrips; fi
exit 1
REMOTE
curl --fail --retry 5 --retry-delay 3 "http://$EC2_HOST/actuator/health"
