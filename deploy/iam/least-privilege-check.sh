#!/bin/sh
# Runs inside a one-off Fargate task with the backend task role (see docs/REFCARD05.md).
B="${PHOTO_BUCKET:?set PHOTO_BUCKET}"
t(){ desc="$1"; shift; if out=$("$@" 2>&1); then echo "ALLOWED | $desc"; else echo "DENIED  | $desc | $(echo "$out" | grep -o 'AccessDenied\|Forbidden\|An error occurred ([A-Za-z]*)' | head -1)"; fi; }
echo probe > /tmp/p.txt
echo "=== Least-privilege check as $(aws sts get-caller-identity --query Arn --output text | sed 's#/[^/]*$##')"
t "PutObject    employee-photos/iam-check.txt" aws s3api put-object --bucket $B --key employee-photos/iam-check.txt --body /tmp/p.txt
t "GetObject    employee-photos/iam-check.txt" aws s3api get-object --bucket $B --key employee-photos/iam-check.txt /tmp/o.txt
t "ListBucket   prefix employee-photos/" aws s3api list-objects-v2 --bucket $B --prefix employee-photos/ --max-items 1
t "PutObject    other-prefix/iam-check.txt (outside prefix)" aws s3api put-object --bucket $B --key other-prefix/iam-check.txt --body /tmp/p.txt
t "GetBucketPolicy" aws s3api get-bucket-policy --bucket $B
t "DeleteBucketPolicy" aws s3api delete-bucket-policy --bucket $B
t "DeleteObject employee-photos/iam-check.txt" aws s3api delete-object --bucket $B --key employee-photos/iam-check.txt
echo "=== done"
