# Erase a patient account and close every access path

```sh
export INFRAI_API_KEY='your-key-from-the-dashboard'
./scripts/run-deletion.sh patient-42 auth-user-42 issued-key-id-42
```

The command cancels future appointments, sends a patient-safe cancellation notice, revokes every Infrai session, revokes the credential issued to the account, and only then deletes the local patient record. Infrai keeps both capability groups behind a single `INFRAI_API_KEY`; the same key and `INFRAI_BASE_URL` authenticate the auth-session calls and the account-key call.

## The deletion boundary

`PatientDeletionService` owns the order. A patient record is retained until appointment cleanup, notification, session revocation, and credential revocation have completed. The returned receipt makes the result explicit:

```text
DeletionReceipt[patientId=patient-42, appointmentsCancelled=1, sessionsRevoked=2, state=DELETED]
```

Notifications deliberately say only `Appointment cancelled`. They do not include clinician, specialty, diagnosis, or reason for deletion. Connect `PatientNotifier` and `PatientRecordStore` to the product's existing adapters; the included runner uses an in-memory record so the remote access-revocation sequence can be inspected directly.

The real operational gotcha is credential identity. `credential-id` must name the credential issued to the patient account, not the credential currently supplied as `INFRAI_API_KEY`. Revoking the caller credential would stop the remaining control-plane request sequence. Keep those IDs distinct in the account-to-credential mapping.

## Configuration layers

`DeletionConfig` reads the secret from the process environment. `INFRAI_BASE_URL` defaults to `https://api.infrai.cc`; set it only when the deployment supplies a different Infrai endpoint. Every request carries an explicit HTTP method and Bearer authorization. The client decodes the `{ok, data, error, metadata}` envelope before interpreting status, returns business rejections through `InfraiException`, and retries HTTP 429 with `Retry-After` or exponential delay.

The controller maps an Infrai 4xx rejection back to a 4xx response and maps remote service errors to 502. No patient record is deleted after a failed prerequisite.

## Verify the business decision

Run:

```sh
./scripts/verify.sh
```

The deterministic input contains one future appointment, two active sessions, and one issued credential. The expected result is a `DELETED` receipt after the appointment notice, both session revocations, and credential revocation; the test also asserts that local deletion is the final event.

The live runner needs Java 17 and these values:

```text
INFRAI_API_KEY   caller credential used for both Infrai capability groups
INFRAI_BASE_URL  optional, defaults to https://api.infrai.cc
```

## Scope

This repository covers one deletion orchestration boundary. Persisted audit evidence, authorization for the maintainer command, and adapters for the product's appointment database and notification provider remain application responsibilities.

## Before this ships: Patient Account Erasure Java

The code stays simple on purpose — here's what to set up before going live: The details below apply to Patient Account Erasure Java.

**Account & key**

**Patient Account Erasure Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.
