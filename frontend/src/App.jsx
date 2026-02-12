import { useEffect, useMemo, useRef, useState } from "react";
import {
  applyTerraformPlan,
  approveTerraformPlan,
  createDiscovery,
  createTerraformPlan,
  getTerraformExport,
  executeDiscovery,
  getDiscoveryStatus,
  getCostDelta,
  getGraph,
  getGraphHealth,
  getGraphSummary,
  getProject,
  patchProject,
  runPipelineCheck
} from "./api";
import CityVisualization from "./CityVisualization";

function fmtCurrency(value) {
  if (value === null || value === undefined) {
    return "-";
  }
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2
  }).format(Number(value));
}

function fmtPercent(value) {
  if (value === null || value === undefined) {
    return "-";
  }
  return `${Number(value).toFixed(2)}%`;
}

function badgeClass(status) {
  if (!status) return "badge neutral";
  if (status === "OK" || status === "APPLIED") return "badge good";
  if (status === "WARNING" || status === "PENDING_APPROVAL") return "badge warn";
  if (status === "EXCEEDED" || status === "FAILED" || status === "REJECTED") return "badge bad";
  return "badge neutral";
}

export default function App() {
  const [projectIdInput, setProjectIdInput] = useState("");
  const [projectId, setProjectId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [project, setProject] = useState(null);
  const [costDelta, setCostDelta] = useState(null);
  const [graphSummary, setGraphSummary] = useState(null);
  const [graph, setGraph] = useState(null);
  const [graphHealth, setGraphHealth] = useState(null);
  const [pipelineResult, setPipelineResult] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [discoveryRun, setDiscoveryRun] = useState(null);
  const [discoveryStatus, setDiscoveryStatus] = useState(null);
  const [terraformRun, setTerraformRun] = useState(null);
  const [terraformActionReason, setTerraformActionReason] = useState("");

  const [budgetForm, setBudgetForm] = useState({
    monthlyBudget: "",
    budgetWarningThreshold: ""
  });
  const [pipelineForm, setPipelineForm] = useState({
    projectedMonthlyDelta: "0",
    strictMode: false
  });
  const [discoveryForm, setDiscoveryForm] = useState({
    accountId: "",
    roleArn: "",
    externalId: "",
    regions: "us-east-1"
  });
  const pollTimerRef = useRef(null);

  function clearDiscoveryPoller() {
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  }

  async function loadDashboard(activeProjectId) {
    if (!activeProjectId) return;
    setLoading(true);
    setError("");
    try {
      const [projectResp, costResp, summaryResp, healthResp, graphResp] = await Promise.all([
        getProject(activeProjectId),
        getCostDelta(activeProjectId),
        getGraphSummary(activeProjectId),
        getGraphHealth(activeProjectId),
        getGraph(activeProjectId)
      ]);
      setProject(projectResp);
      setCostDelta(costResp);
      setGraphSummary(summaryResp);
      setGraphHealth(healthResp);
      setGraph(graphResp);
      setBudgetForm({
        monthlyBudget: projectResp.monthlyBudget ?? "",
        budgetWarningThreshold: projectResp.budgetWarningThreshold ?? ""
      });
      setLastUpdated(new Date());
    } catch (e) {
      setError(e.message || "Failed to load project dashboard.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (projectId) {
      loadDashboard(projectId);
    }
    return () => clearDiscoveryPoller();
  }, [projectId]);

  const budgetUsageWidth = useMemo(() => {
    const percent = costDelta?.budgetUsedPercent;
    if (percent === null || percent === undefined) return "0%";
    return `${Math.min(Number(percent), 100)}%`;
  }, [costDelta]);

  async function onProjectSubmit(e) {
    e.preventDefault();
    const nextId = projectIdInput.trim();
    if (!nextId) return;
    setProjectId(nextId);
  }

  async function onBudgetSave(e) {
    e.preventDefault();
    if (!projectId) return;
    setError("");
    try {
      await patchProject(projectId, {
        monthlyBudget:
          budgetForm.monthlyBudget === "" ? null : Number(budgetForm.monthlyBudget),
        budgetWarningThreshold:
          budgetForm.budgetWarningThreshold === ""
            ? null
            : Number(budgetForm.budgetWarningThreshold)
      });
      await loadDashboard(projectId);
    } catch (e2) {
      setError(e2.message || "Failed to update budget settings.");
    }
  }

  async function onPipelineCheck(e) {
    e.preventDefault();
    if (!projectId) return;
    setError("");
    try {
      const result = await runPipelineCheck(projectId, {
        projectedMonthlyDelta: Number(pipelineForm.projectedMonthlyDelta || 0),
        strictMode: Boolean(pipelineForm.strictMode)
      });
      setPipelineResult(result);
    } catch (e2) {
      setError(e2.message || "Pipeline check failed.");
    }
  }

  async function onCreateDiscovery(e) {
    e.preventDefault();
    if (!projectId) return;
    setError("");
    clearDiscoveryPoller();
    try {
      const payload = {
        provider: "AWS",
        accountId: discoveryForm.accountId || null,
        roleArn: discoveryForm.roleArn || null,
        externalId: discoveryForm.externalId || null,
        regions: discoveryForm.regions
          .split(",")
          .map((v) => v.trim())
          .filter(Boolean)
      };
      const run = await createDiscovery(projectId, payload);
      setDiscoveryRun(run);
      setDiscoveryStatus({ status: run.status, progress: 0, finishedAt: run.finishedAt, id: run.id });
    } catch (e2) {
      setError(e2.message || "Failed to create discovery.");
    }
  }

  async function pollDiscoveryStatus(discoveryId) {
    if (!projectId || !discoveryId) return;
    try {
      const statusResp = await getDiscoveryStatus(projectId, discoveryId);
      setDiscoveryStatus(statusResp);
      if (["COMPLETED", "FAILED"].includes(statusResp.status)) {
        clearDiscoveryPoller();
        await loadDashboard(projectId);
        return;
      }
      pollTimerRef.current = setTimeout(() => pollDiscoveryStatus(discoveryId), 2000);
    } catch (e) {
      setError(e.message || "Failed to poll discovery status.");
      clearDiscoveryPoller();
    }
  }

  async function onExecuteDiscovery() {
    if (!projectId || !discoveryRun?.id) return;
    setError("");
    clearDiscoveryPoller();
    try {
      const run = await executeDiscovery(projectId, discoveryRun.id);
      setDiscoveryRun(run);
      await pollDiscoveryStatus(discoveryRun.id);
    } catch (e) {
      setError(e.message || "Failed to execute discovery.");
    }
  }

  async function onCreateTerraformPlan() {
    if (!projectId) return;
    setError("");
    try {
      const run = await createTerraformPlan(projectId);
      setTerraformRun(run);
    } catch (e) {
      setError(e.message || "Failed to create terraform plan.");
    }
  }

  async function refreshTerraformRun() {
    if (!projectId || !terraformRun?.id) return;
    setError("");
    try {
      const run = await getTerraformExport(projectId, terraformRun.id);
      setTerraformRun(run);
    } catch (e) {
      setError(e.message || "Failed to refresh terraform run.");
    }
  }

  async function onApproveTerraformPlan(approved) {
    if (!projectId || !terraformRun?.id) return;
    setError("");
    try {
      const run = await approveTerraformPlan(
        projectId,
        terraformRun.id,
        approved,
        terraformActionReason || null
      );
      setTerraformRun(run);
      setTerraformActionReason("");
    } catch (e) {
      setError(e.message || "Failed to update terraform approval.");
    }
  }

  async function onApplyTerraformPlan() {
    if (!projectId || !terraformRun?.id) return;
    setError("");
    try {
      const run = await applyTerraformPlan(projectId, terraformRun.id);
      setTerraformRun(run);
      await loadDashboard(projectId);
    } catch (e) {
      setError(e.message || "Failed to apply terraform plan.");
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Cloud City Platform</p>
          <h1>V1 Operations Console</h1>
        </div>
        <form onSubmit={onProjectSubmit} className="project-form">
          <input
            type="text"
            placeholder="Project UUID"
            value={projectIdInput}
            onChange={(e) => setProjectIdInput(e.target.value)}
          />
          <button type="submit">Load Project</button>
        </form>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <main className="grid">
        <CityVisualization graph={graph} graphHealth={graphHealth} costDelta={costDelta} />

        <section className="card">
          <h2>Project And Budget</h2>
          {!project && <p className="muted">Load a project to view details.</p>}
          {project && (
            <>
              <p className="title">{project.name}</p>
              <p className="muted">{project.description || "No description"}</p>
              <div className="kv">
                <span>Status</span>
                <span className={badgeClass(costDelta?.budgetStatus)}>{costDelta?.budgetStatus || "-"}</span>
              </div>
              <div className="kv">
                <span>Current Total</span>
                <strong>{fmtCurrency(costDelta?.currentTotal)}</strong>
              </div>
              <div className="kv">
                <span>Delta</span>
                <strong>{fmtCurrency(costDelta?.delta)}</strong>
              </div>
              <div className="meter">
                <div className="meter-fill" style={{ width: budgetUsageWidth }} />
              </div>
              <div className="kv">
                <span>Used</span>
                <span>{fmtPercent(costDelta?.budgetUsedPercent)}</span>
              </div>
              <form onSubmit={onBudgetSave} className="inline-form">
                <label>
                  Monthly Budget
                  <input
                    type="number"
                    step="0.01"
                    value={budgetForm.monthlyBudget}
                    onChange={(e) =>
                      setBudgetForm((prev) => ({ ...prev, monthlyBudget: e.target.value }))
                    }
                  />
                </label>
                <label>
                  Warning %
                  <input
                    type="number"
                    step="0.01"
                    value={budgetForm.budgetWarningThreshold}
                    onChange={(e) =>
                      setBudgetForm((prev) => ({
                        ...prev,
                        budgetWarningThreshold: e.target.value
                      }))
                    }
                  />
                </label>
                <button type="submit">Save Budget</button>
              </form>
            </>
          )}
        </section>

        <section className="card">
          <h2>Topology Summary</h2>
          {!graphSummary && <p className="muted">No graph summary loaded.</p>}
          {graphSummary && (
            <>
              <div className="stat-row">
                <div>
                  <p className="stat-label">Nodes</p>
                  <p className="stat-value">{graphSummary.totalNodes}</p>
                </div>
                <div>
                  <p className="stat-label">Edges</p>
                  <p className="stat-value">{graphSummary.totalEdges}</p>
                </div>
                <div>
                  <p className="stat-label">Estimated Cost</p>
                  <p className="stat-value">{fmtCurrency(graphSummary.totalEstimatedCost)}</p>
                </div>
              </div>
              <h3>Top Cost Types</h3>
              <ul>
                {(graphSummary.topCostTypes || []).map((item) => (
                  <li key={item.key}>
                    <span>{item.key}</span>
                    <strong>{fmtCurrency(item.estimatedCost)}</strong>
                  </li>
                ))}
              </ul>
              <h3>Top Cost Regions</h3>
              <ul>
                {(graphSummary.topCostRegions || []).map((item) => (
                  <li key={item.key}>
                    <span>{item.key}</span>
                    <strong>{fmtCurrency(item.estimatedCost)}</strong>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>

        <section className="card">
          <h2>Graph Health</h2>
          {!graphHealth && <p className="muted">No graph health loaded.</p>}
          {graphHealth && (
            <>
              <div className="stat-row">
                <div>
                  <p className="stat-label">Orphans</p>
                  <p className="stat-value">{graphHealth.orphanNodeCount}</p>
                </div>
                <div>
                  <p className="stat-label">Misconfigured</p>
                  <p className="stat-value">{graphHealth.misconfiguredNodeCount}</p>
                </div>
              </div>
              <h3>Misconfigured Nodes</h3>
              <ul>
                {(graphHealth.misconfiguredNodes || []).slice(0, 6).map((issue) => (
                  <li key={issue.nodeId}>
                    <span>{issue.nodeType} - {issue.nodeName}</span>
                    <small>{issue.issue}</small>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>

        <section className="card">
          <h2>Pipeline Gate</h2>
          <form onSubmit={onPipelineCheck} className="inline-form">
            <label>
              Projected Monthly Delta
              <input
                type="number"
                step="0.01"
                value={pipelineForm.projectedMonthlyDelta}
                onChange={(e) =>
                  setPipelineForm((prev) => ({
                    ...prev,
                    projectedMonthlyDelta: e.target.value
                  }))
                }
              />
            </label>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={pipelineForm.strictMode}
                onChange={(e) =>
                  setPipelineForm((prev) => ({ ...prev, strictMode: e.target.checked }))
                }
              />
              Strict Mode
            </label>
            <button type="submit">Run Check</button>
          </form>

          {!pipelineResult && <p className="muted">No pipeline check run yet.</p>}
          {pipelineResult && (
            <div className="pipeline-result">
              <div className="kv">
                <span>Result</span>
                <span className={pipelineResult.pass ? "badge good" : "badge bad"}>
                  {pipelineResult.pass ? "PASS" : "FAIL"}
                </span>
              </div>
              <div className="kv">
                <span>Budget Status</span>
                <span className={badgeClass(pipelineResult.budgetStatus)}>
                  {pipelineResult.budgetStatus}
                </span>
              </div>
              <p>{pipelineResult.reason}</p>
              <p className="muted">{pipelineResult.recommendedAction}</p>
            </div>
          )}
        </section>

        <section className="card card-wide">
          <h2>Discovery Operations</h2>
          <form onSubmit={onCreateDiscovery} className="inline-form discovery-form">
            <label>
              Account Id
              <input
                type="text"
                value={discoveryForm.accountId}
                onChange={(e) =>
                  setDiscoveryForm((prev) => ({ ...prev, accountId: e.target.value }))
                }
              />
            </label>
            <label>
              Role Arn
              <input
                type="text"
                value={discoveryForm.roleArn}
                onChange={(e) =>
                  setDiscoveryForm((prev) => ({ ...prev, roleArn: e.target.value }))
                }
              />
            </label>
            <label>
              External Id
              <input
                type="text"
                value={discoveryForm.externalId}
                onChange={(e) =>
                  setDiscoveryForm((prev) => ({ ...prev, externalId: e.target.value }))
                }
              />
            </label>
            <label>
              Regions (comma separated)
              <input
                type="text"
                value={discoveryForm.regions}
                onChange={(e) =>
                  setDiscoveryForm((prev) => ({ ...prev, regions: e.target.value }))
                }
              />
            </label>
            <button type="submit">Create Discovery Run</button>
          </form>

          <div className="discovery-actions">
            <div className="kv">
              <span>Run Id</span>
              <span className="mono">{discoveryRun?.id || "-"}</span>
            </div>
            <div className="kv">
              <span>Status</span>
              <span className={badgeClass(discoveryStatus?.status || discoveryRun?.status)}>
                {discoveryStatus?.status || discoveryRun?.status || "-"}
              </span>
            </div>
            <div className="meter">
              <div
                className="meter-fill"
                style={{ width: `${Math.min(Number(discoveryStatus?.progress || 0), 100)}%` }}
              />
            </div>
            <div className="kv">
              <span>Progress</span>
              <span>{discoveryStatus?.progress ?? 0}%</span>
            </div>
            <button
              type="button"
              onClick={onExecuteDiscovery}
              disabled={!discoveryRun?.id}
            >
              Execute Discovery
            </button>
          </div>
        </section>

        <section className="card card-wide">
          <h2>Terraform Execution Guardrails</h2>
          <div className="terraform-actions">
            <button type="button" onClick={onCreateTerraformPlan} disabled={!projectId}>
              Create Plan
            </button>
            <button type="button" onClick={refreshTerraformRun} disabled={!terraformRun?.id}>
              Refresh Plan Status
            </button>
          </div>

          <div className="kv">
            <span>Run Id</span>
            <span className="mono">{terraformRun?.id || "-"}</span>
          </div>
          <div className="kv">
            <span>Status</span>
            <span className={badgeClass(terraformRun?.status)}>{terraformRun?.status || "-"}</span>
          </div>
          <div className="kv">
            <span>Artifact Path</span>
            <span className="mono">{terraformRun?.artifactPath || "-"}</span>
          </div>
          <div className="summary-box">
            <p className="stat-label">Summary JSON</p>
            <pre>{terraformRun?.summaryJson || "-"}</pre>
          </div>

          <div className="inline-form terraform-form">
            <label>
              Approval Reason
              <input
                type="text"
                value={terraformActionReason}
                onChange={(e) => setTerraformActionReason(e.target.value)}
                placeholder="Optional reason"
              />
            </label>
            <div className="terraform-cta-row">
              <button
                type="button"
                onClick={() => onApproveTerraformPlan(true)}
                disabled={terraformRun?.status !== "PENDING_APPROVAL"}
              >
                Approve
              </button>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => onApproveTerraformPlan(false)}
                disabled={terraformRun?.status !== "PENDING_APPROVAL"}
              >
                Reject
              </button>
              <button
                type="button"
                onClick={onApplyTerraformPlan}
                disabled={terraformRun?.status !== "APPROVED"}
              >
                Apply
              </button>
            </div>
          </div>
        </section>
      </main>

      <footer className="footer">
        <button type="button" onClick={() => loadDashboard(projectId)} disabled={!projectId || loading}>
          {loading ? "Refreshing..." : "Refresh Dashboard"}
        </button>
        <span>{lastUpdated ? `Updated ${lastUpdated.toLocaleTimeString()}` : "Not loaded"}</span>
      </footer>
    </div>
  );
}

