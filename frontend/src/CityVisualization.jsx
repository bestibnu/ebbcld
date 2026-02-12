import { useMemo, useState } from "react";

const ZONE_RULES = [
  { key: "COMPUTE", title: "Compute", types: ["EC2"] },
  { key: "DATABASE", title: "Database", types: ["RDS"] },
  { key: "STORAGE", title: "Storage", types: ["S3"] },
  { key: "NETWORK", title: "Networking", types: ["VPC", "SUBNET", "ELB"] },
  { key: "SECURITY", title: "Security", types: ["SG"] }
];

function buildCityModel(graph) {
  const nodes = graph?.nodes || [];
  const regionsMap = new Map();

  for (const node of nodes) {
    const region = node.region && node.region.trim() ? node.region.trim() : "unknown";
    if (!regionsMap.has(region)) {
      regionsMap.set(region, []);
    }
    regionsMap.get(region).push(node);
  }

  const regions = [...regionsMap.keys()].sort((a, b) => a.localeCompare(b)).map((regionName) => {
    const regionNodes = regionsMap.get(regionName);
    const zones = ZONE_RULES.map((zoneRule) => {
      const grouped = new Map();
      for (const node of regionNodes) {
        if (!zoneRule.types.includes(node.type)) continue;
        const key = node.type;
        if (!grouped.has(key)) {
          grouped.set(key, {
            key,
            label: key,
            region: regionName,
            type: key,
            count: 0,
            cost: 0,
            nodeNames: []
          });
        }
        const group = grouped.get(key);
        group.count += 1;
        group.cost += Number(node.costEstimate || 0);
        group.nodeNames.push(node.name);
      }

      const buildings = [...grouped.values()].sort((a, b) => a.type.localeCompare(b.type));
      return {
        ...zoneRule,
        buildings
      };
    });

    return {
      region: regionName,
      zones
    };
  });

  const allBuildings = [];
  for (const region of regions) {
    for (const zone of region.zones) {
      for (const building of zone.buildings) {
        allBuildings.push(building);
      }
    }
  }

  const costs = allBuildings.map((b) => b.cost).filter((v) => v > 0).sort((a, b) => a - b);
  function percentile(cost) {
    if (!costs.length || cost <= 0) return 0;
    let idx = costs.findIndex((v) => v >= cost);
    if (idx === -1) idx = costs.length - 1;
    return (idx + 1) / costs.length;
  }

  return { regions, percentile };
}

function buildingHeight(building) {
  if (building.cost > 0) {
    return Math.max(12, Math.log10(building.cost + 1) * 34);
  }
  return Math.max(10, building.count * 10);
}

function fmtCurrency(value) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2
  }).format(Number(value || 0));
}

export default function CityVisualization({ graph, graphHealth, costDelta }) {
  const [selected, setSelected] = useState(null);
  const model = useMemo(() => buildCityModel(graph), [graph]);

  const districtWidth = 300;
  const districtHeight = 230;
  const regionGap = 18;
  const canvasWidth = Math.max(1100, model.regions.length * (districtWidth + regionGap) + 28);
  const canvasHeight = 300;

  return (
    <section className="card card-wide">
      <h2>Cloud City Preview (2D Deterministic MVP)</h2>
      {!model.regions.length && <p className="muted">No graph data available for visualization.</p>}
      {!!model.regions.length && (
        <div className="city-layout">
          <div className="city-canvas-wrap">
            <svg viewBox={`0 0 ${canvasWidth} ${canvasHeight}`} className="city-canvas" role="img">
              <rect x="0" y="0" width={canvasWidth} height={canvasHeight} className="city-ground" />
              {model.regions.map((region, regionIndex) => {
                const x = 16 + regionIndex * (districtWidth + regionGap);
                const y = 22;
                const zoneWidth = (districtWidth - 24) / 3;
                const zoneHeight = (districtHeight - 32) / 2;
                return (
                  <g key={region.region}>
                    <rect x={x} y={y} width={districtWidth} height={districtHeight} className="district-frame" />
                    <text x={x + 10} y={y + 16} className="district-label">
                      {region.region}
                    </text>
                    {region.zones.map((zone, zoneIdx) => {
                      const col = zoneIdx % 3;
                      const row = Math.floor(zoneIdx / 3);
                      const zx = x + 8 + col * zoneWidth;
                      const zy = y + 24 + row * zoneHeight;
                      return (
                        <g key={`${region.region}-${zone.key}`}>
                          <rect x={zx} y={zy} width={zoneWidth - 5} height={zoneHeight - 5} className="zone-box" />
                          <text x={zx + 6} y={zy + 12} className="zone-label">
                            {zone.title}
                          </text>
                          {zone.buildings.map((building, buildingIdx) => {
                            const slots = zone.buildings.length + 1;
                            const slotWidth = (zoneWidth - 18) / slots;
                            const bx = zx + 6 + slotWidth * (buildingIdx + 1) - slotWidth * 0.35;
                            const height = buildingHeight(building);
                            const by = zy + zoneHeight - 12 - height;
                            const intensity = model.percentile(building.cost);
                            const fill = `hsl(${20 + intensity * 8}deg ${65 + intensity * 20}% ${62 - intensity * 24}%)`;
                            const selectedNow =
                              selected &&
                              selected.region === building.region &&
                              selected.type === building.type;
                            return (
                              <g key={`${region.region}-${zone.key}-${building.type}`}>
                                <rect
                                  x={bx}
                                  y={by}
                                  width={Math.max(9, slotWidth * 0.65)}
                                  height={height}
                                  fill={fill}
                                  className={`building ${selectedNow ? "selected" : ""}`}
                                  onClick={() => setSelected(building)}
                                />
                              </g>
                            );
                          })}
                        </g>
                      );
                    })}
                  </g>
                );
              })}
            </svg>
          </div>

          <aside className="city-panel">
            {!selected && <p className="muted">Click a building to inspect grouped resource details.</p>}
            {selected && (
              <>
                <h3>{selected.type}</h3>
                <div className="kv">
                  <span>Region</span>
                  <span>{selected.region}</span>
                </div>
                <div className="kv">
                  <span>Instance Count</span>
                  <strong>{selected.count}</strong>
                </div>
                <div className="kv">
                  <span>Monthly Cost</span>
                  <strong>{fmtCurrency(selected.cost)}</strong>
                </div>
                <div className="kv">
                  <span>Budget Status</span>
                  <span>{costDelta?.budgetStatus || "-"}</span>
                </div>
                <h4>Sample Resources</h4>
                <ul>
                  {selected.nodeNames.slice(0, 6).map((name, idx) => (
                    <li key={`${name}-${idx}`}>{name}</li>
                  ))}
                </ul>
                <h4>Health Signals</h4>
                <ul>
                  {(graphHealth?.misconfiguredNodes || [])
                    .filter((issue) => issue.nodeType === selected.type)
                    .slice(0, 4)
                    .map((issue) => (
                      <li key={issue.nodeId}>{issue.issue}</li>
                    ))}
                </ul>
              </>
            )}
          </aside>
        </div>
      )}
    </section>
  );
}
