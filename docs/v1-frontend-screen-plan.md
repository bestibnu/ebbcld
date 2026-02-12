# V1 Frontend Screen Plan

## Goal
Deliver a functional operational UI first, then iterate toward 3D city visualization.

## Screen 1: Project Overview Dashboard
Purpose:
- Give instant operational state for one project.

Widgets:
1. Header
- Project name, description
- Budget quick edit button

2. Budget and Cost Card
- Current total (`/cost/delta`)
- Delta from snapshot
- Budget status badge (`NOT_CONFIGURED | OK | WARNING | EXCEEDED`)
- Used percent progress bar

3. Topology Summary Card
- Total nodes/edges (`/graph/summary`)
- Top 3 cost types
- Top 3 cost regions

4. Graph Health Card
- Orphan count
- Misconfigured count
- Expandable issue list (`/graph/health`)

5. Pipeline Gate Card
- Form: projectedMonthlyDelta + strictMode toggle
- Run check button (`/pipeline/check`)
- Pass/fail + reason + recommended action

## Screen 2: Discovery Operations
Purpose:
- Run discovery and monitor progress.

Flow:
1. Create discovery
2. Execute discovery
3. Poll status endpoint every 2-3s until terminal state
4. Refresh graph summary/health after completion

## Screen 3: Terraform Guardrail Flow
Purpose:
- Controlled path from plan to apply.

Flow:
1. Plan (`/terraform/plan`)
- show status + parsed plan summary

2. Approval action (`/terraform/{id}/approve`)
- Approve or reject with reason

3. Apply (`/terraform/{id}/apply`)
- Enabled only if status is `APPROVED`

4. State refresh (`/terraform/export/{id}`)
- Poll while applying

## UX Rules for V1
- Always show server-returned reason text for blocked actions.
- Use status-to-color mapping consistently:
  - success: `OK`, `APPLIED`
  - warning: `WARNING`, `PENDING_APPROVAL`
  - danger: `EXCEEDED`, `REJECTED`, `FAILED`
- Keep graph view initially as list/table + summary chips; reserve full 3D for V1.1.

## Suggested Frontend Delivery Slices
1. Dashboard shell + data hooks (`project`, `cost/delta`, `graph/summary`, `graph/health`).
2. Pipeline check panel.
3. Discovery panel.
4. Terraform plan/approve/apply panel.
5. Polishing: loading states, empty states, retry, error banners.

## Acceptance Criteria for UI V1
- A user can see budget status and topology summary in one view.
- A user can run pipeline check and understand pass/fail reason.
- A user can execute discovery and observe status progression.
- A user can complete plan -> approve -> apply with status visibility.
- No critical action depends on hidden backend assumptions.
