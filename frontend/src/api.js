const DEFAULT_BASE_URL = "/api/v1";
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || DEFAULT_BASE_URL;

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  if (!response.ok) {
    let message = `HTTP ${response.status}`;
    try {
      const text = await response.text();
      if (text) {
        message = text;
      }
    } catch (e) {
      // Keep fallback message on parse failure.
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export function getProject(projectId) {
  return request(`/projects/${projectId}`);
}

export function patchProject(projectId, payload) {
  return request(`/projects/${projectId}`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function getCostDelta(projectId) {
  return request(`/projects/${projectId}/cost/delta`);
}

export function getGraphSummary(projectId) {
  return request(`/projects/${projectId}/graph/summary`);
}

export function getGraph(projectId) {
  return request(`/projects/${projectId}/graph`);
}

export function getGraphHealth(projectId) {
  return request(`/projects/${projectId}/graph/health`);
}

export function runPipelineCheck(projectId, payload) {
  return request(`/projects/${projectId}/pipeline/check`, {
    method: "POST",
    body: JSON.stringify(payload || {})
  });
}

export function createDiscovery(projectId, payload) {
  return request(`/projects/${projectId}/discoveries`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function executeDiscovery(projectId, discoveryId) {
  return request(`/projects/${projectId}/discoveries/${discoveryId}/execute`, {
    method: "POST"
  });
}

export function getDiscoveryStatus(projectId, discoveryId) {
  return request(`/projects/${projectId}/discoveries/${discoveryId}/status`);
}

export function createTerraformPlan(projectId) {
  return request(`/projects/${projectId}/terraform/plan`, {
    method: "POST"
  });
}

export function approveTerraformPlan(projectId, exportId, approved, reason) {
  return request(`/projects/${projectId}/terraform/${exportId}/approve`, {
    method: "POST",
    body: JSON.stringify({ approved, reason })
  });
}

export function applyTerraformPlan(projectId, exportId) {
  return request(`/projects/${projectId}/terraform/${exportId}/apply`, {
    method: "POST"
  });
}

export function getTerraformExport(projectId, exportId) {
  return request(`/projects/${projectId}/terraform/export/${exportId}`);
}
