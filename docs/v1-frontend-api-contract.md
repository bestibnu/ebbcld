# V1 Frontend API Contract

## Scope
This contract defines the minimum backend interfaces the V1 frontend should rely on.

## Base
- Base path: `/api/v1`
- Auth for local test mode currently runs with filters disabled in tests; frontend should still send auth headers when auth is enabled.

## Core Project Context
1. Get project
- `GET /projects/{projectId}`
- Used by: project header, budget panel
- Required fields:
  - `id`
  - `name`
  - `description`
  - `monthlyBudget`
  - `budgetWarningThreshold`

2. Update project budget settings
- `PATCH /projects/{projectId}`
- Payload fields used by frontend:
  - `monthlyBudget`
  - `budgetWarningThreshold`

## Discovery
1. Create discovery run
- `POST /projects/{projectId}/discoveries`

2. Execute discovery run
- `POST /projects/{projectId}/discoveries/{discoveryId}/execute`

3. Poll discovery status
- `GET /projects/{projectId}/discoveries/{discoveryId}/status`
- Required fields:
  - `status`
  - `progress`
  - `finishedAt`

## Graph
1. Full graph
- `GET /projects/{projectId}/graph`
- Used by: topology table/list and future 3D adapter

2. Graph summary (dashboard cards)
- `GET /projects/{projectId}/graph/summary`
- Required fields:
  - `totalNodes`
  - `totalEdges`
  - `nodeCountByType`
  - `nodeCountByRegion`
  - `totalEstimatedCost`
  - `estimatedCostByType`
  - `estimatedCostByRegion`
  - `topCostTypes`
  - `topCostRegions`

3. Graph health (quality panel)
- `GET /projects/{projectId}/graph/health`
- Required fields:
  - `orphanNodeCount`
  - `misconfiguredNodeCount`
  - `orphanNodes[]`
  - `misconfiguredNodes[]`

## Cost and Guardrails
1. Cost snapshot
- `GET /projects/{projectId}/cost`

2. Cost delta + budget status
- `GET /projects/{projectId}/cost/delta`
- Required fields:
  - `previousTotal`
  - `currentTotal`
  - `delta`
  - `currency`
  - `budgetStatus`
  - `monthlyBudget`
  - `budgetUsedPercent`
  - `budgetWarningThreshold`

3. Policy check (manual or automation)
- `POST /projects/{projectId}/cost/policy-check`
- Request:
  - `projectedMonthlyDelta` (optional)
- Response:
  - `allowed`
  - `budgetStatus`
  - `reason`
  - `projectedTotal`

## Terraform Execution Flow
1. Plan
- `POST /projects/{projectId}/terraform/plan`
- Required response fields:
  - `id`
  - `status`
  - `summaryJson`
  - `artifactPath`

2. Approve or reject
- `POST /projects/{projectId}/terraform/{exportId}/approve`
- Request:
  - `approved` (boolean)
  - `reason` (optional)

3. Apply
- `POST /projects/{projectId}/terraform/{exportId}/apply`

4. Fetch current state
- `GET /projects/{projectId}/terraform/export/{exportId}`

Expected statuses used by frontend:
- `PENDING_APPROVAL`
- `APPROVED`
- `REJECTED`
- `APPLYING`
- `APPLIED`
- `FAILED`
- Legacy still possible: `READY`

## CI/CD Pipeline Gate
1. Unified pipeline check
- `POST /projects/{projectId}/pipeline/check`
- Request:
  - `projectedMonthlyDelta` (optional)
  - `strictMode` (optional)
- Response fields:
  - `pass`
  - `budgetStatus`
  - `requiredApproval`
  - `terraformPlanEligible`
  - `reason`
  - `recommendedAction`
  - `strictMode`

## Frontend Error Handling Contract
- `400`: validation/business rule failure (show actionable message)
- `404`: missing project/resource
- `409`: state-transition conflict (for approve/apply flow)
- `5xx`: generic failure, allow retry

## V1 Stability Rules
- Frontend should treat `summaryJson` as opaque JSON text and parse defensively.
- Frontend should not infer status transitions; always use server status.
- Missing optional budget fields should render as "Not configured".
