import React, { useEffect, useState } from 'react';
import { fetchEndpoints, createEndpoint, deleteEndpoint, triggerPingNow } from './api/endpointApi';
import type { MonitoredEndpoint, CreateEndpointPayload } from './api/endpointApi';
import { ResponseTimeChart } from './components/ResponseTimeChart'; // Adjust path if needed

export const App: React.FC = () => {
  const [endpoints, setEndpoints] = useState<MonitoredEndpoint[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Inspection & Manual Ping state
  const [selectedEndpoint, setSelectedEndpoint] = useState<MonitoredEndpoint | null>(null);
  const [pingingId, setPingingId] = useState<number | null>(null);

  // Aggregate metrics
  const totalEndpoints = endpoints.length;
  const upCount = endpoints.filter((e) => e.lastStatus === 'UP').length;
  const downCount = endpoints.filter((e) => e.lastStatus === 'DOWN').length;
  const overallHealth = totalEndpoints > 0 ? Math.round((upCount / totalEndpoints) * 100) : 100;

  // Form state
  const [formData, setFormData] = useState<CreateEndpointPayload>({
    name: '',
    url: '',
    webhookUrl: '',
    httpMethod: 'GET',
    expectedStatusCode: 200,
    checkIntervalSeconds: 60,
  });

  const loadEndpoints = async () => {
    try {
      setLoading(true);
      const data = await fetchEndpoints();
      setEndpoints(data);
      setError(null);
    } catch (err: any) {
      setError('Failed to fetch endpoints. Is the backend running?');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEndpoints();
    const interval = setInterval(loadEndpoints, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'expectedStatusCode' || name === 'checkIntervalSeconds' ? Number(value) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createEndpoint(formData);
      setFormData({
        name: '',
        url: '',
        webhookUrl: '',
        httpMethod: 'GET',
        expectedStatusCode: 200,
        checkIntervalSeconds: 60,
      });
      await loadEndpoints();
    } catch (err: any) {
      alert('Error creating endpoint. Check your inputs.');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteEndpoint(id);
      if (selectedEndpoint?.id === id) {
        setSelectedEndpoint(null);
      }
      await loadEndpoints();
    } catch (err: any) {
      alert('Failed to delete endpoint.');
    }
  };

  const handleManualPing = async (id: number) => {
    try {
      setPingingId(id);
      await triggerPingNow(id);
      await loadEndpoints();
    } catch (err: any) {
      alert('Failed to trigger manual ping.');
    } finally {
      setPingingId(null);
    }
  };

  return (
      <div className="container">
        <header>
          <h1>EventPulse Dashboard</h1>
          <p>Real-Time API & Webhook Monitoring</p>
        </header>

        {/* Summary Metrics Banner */}
        <section className="stats-grid">
          <div className="stat-card">
            <span className="stat-label">Total Endpoints</span>
            <span className="stat-value">{totalEndpoints}</span>
          </div>

          <div className="stat-card">
            <span className="stat-label">System Health</span>
            <span className={`stat-value ${overallHealth === 100 ? 'healthy' : 'degraded'}`}>
            {overallHealth}%
          </span>
          </div>

          <div className="stat-card">
            <span className="stat-label">Services UP</span>
            <span className="stat-value up-text">{upCount}</span>
          </div>

          <div className="stat-card">
            <span className="stat-label">Services DOWN</span>
            <span className={`stat-value ${downCount > 0 ? 'down-text' : ''}`}>
            {downCount}
          </span>
          </div>
        </section>

        {/* Add New Endpoint Form */}
        <section className="card">
          <h2>Add Monitored Endpoint</h2>
          <form onSubmit={handleSubmit} className="form-grid">
            <div>
              <label>Name</label>
              <input
                  type="text"
                  name="name"
                  placeholder="e.g. Payments Service Health"
                  value={formData.name}
                  onChange={handleInputChange}
                  required
              />
            </div>

            <div>
              <label>Target URL</label>
              <input
                  type="url"
                  name="url"
                  placeholder="https://httpbin.org/status/200"
                  value={formData.url}
                  onChange={handleInputChange}
                  required
              />
            </div>

            <div>
              <label>Webhook Alert URL (Optional)</label>
              <input
                  type="url"
                  name="webhookUrl"
                  placeholder="https://discord.com/api/webhooks/..."
                  value={formData.webhookUrl}
                  onChange={handleInputChange}
              />
            </div>

            <div>
              <label>HTTP Method</label>
              <select name="httpMethod" value={formData.httpMethod} onChange={handleInputChange}>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="HEAD">HEAD</option>
              </select>
            </div>

            <div>
              <label>Expected Status</label>
              <input
                  type="number"
                  name="expectedStatusCode"
                  value={formData.expectedStatusCode}
                  onChange={handleInputChange}
                  required
              />
            </div>

            <div>
              <label>Interval (s)</label>
              <input
                  type="number"
                  name="checkIntervalSeconds"
                  value={formData.checkIntervalSeconds}
                  onChange={handleInputChange}
                  min={5}
                  required
              />
            </div>

            <button type="submit" className="btn-primary">Add Endpoint</button>
          </form>
        </section>

        {/* Active Endpoints Table */}
        <section className="card">
          <h2>Active Endpoints ({endpoints.length})</h2>
          {error && <div className="alert error">{error}</div>}
          {loading && endpoints.length === 0 ? (
              <p>Loading endpoints...</p>
          ) : (
              <table className="data-table">
                <thead>
                <tr>
                  <th>Status</th>
                  <th>Name</th>
                  <th>URL</th>
                  <th>Method</th>
                  <th>Last Checked</th>
                  <th>Webhook Alert</th>
                  <th>Action</th>
                </tr>
                </thead>
                <tbody>
                {endpoints.length === 0 ? (
                    <tr>
                      <td colSpan={7}>No endpoints configured yet.</td>
                    </tr>
                ) : (
                    endpoints.map((ep) => (
                        <tr
                            key={ep.id}
                            className={`table-row ${selectedEndpoint?.id === ep.id ? 'active-row' : ''}`}
                            onClick={() => setSelectedEndpoint(selectedEndpoint?.id === ep.id ? null : ep)}
                            style={{ cursor: 'pointer' }}
                        >
                          <td>
                      <span className={`badge ${ep.lastStatus ? ep.lastStatus.toLowerCase() : 'pending'}`}>
                        {ep.lastStatus || 'PENDING'}
                      </span>
                          </td>
                          <td><strong>{ep.name}</strong></td>
                          <td><code>{ep.url}</code></td>
                          <td><span className="method-tag">{ep.httpMethod}</span></td>
                          <td>{ep.lastCheckedAt ? new Date(ep.lastCheckedAt).toLocaleTimeString() : 'Never'}</td>
                          <td>
                            {ep.webhookUrl ? (
                                <span className="badge webhook-active">Active</span>
                            ) : (
                                <span className="badge webhook-none">None</span>
                            )}
                          </td>
                          <td onClick={(e) => e.stopPropagation()}>
                            <button
                                onClick={() => handleManualPing(ep.id)}
                                disabled={pingingId === ep.id}
                                className="btn-secondary"
                                style={{ marginRight: '0.5rem' }}
                            >
                              {pingingId === ep.id ? 'Pinging...' : '⚡ Ping Now'}
                            </button>
                            <button onClick={() => handleDelete(ep.id)} className="btn-danger">
                              Delete
                            </button>
                          </td>
                        </tr>
                    ))
                )}
                </tbody>
              </table>
          )}
        </section>

        {/* Detailed Metrics Panel */}
        {selectedEndpoint && (
            <section className="card detail-panel">
              <div className="detail-header">
                <div>
                  <h3>Inspecting: {selectedEndpoint.name}</h3>
                  <p className="subtitle"><code>{selectedEndpoint.url}</code></p>
                </div>
                <button className="btn-close" onClick={() => setSelectedEndpoint(null)}>✕ Close</button>
              </div>

              <div className="detail-grid">
                <div className="detail-item">
                  <span className="label">HTTP Method</span>
                  <span className="value">{selectedEndpoint.httpMethod}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Expected Status</span>
                  <span className="value">{selectedEndpoint.expectedStatusCode}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Check Interval</span>
                  <span className="value">{selectedEndpoint.checkIntervalSeconds}s</span>
                </div>
                <div className="detail-item">
                  <span className="label">Webhook Target</span>
                  <span className="value">{selectedEndpoint.webhookUrl || 'Not Configured'}</span>
                </div>
              </div>

              <ResponseTimeChart
                  endpointId={selectedEndpoint.id}
                  endpointName={selectedEndpoint.name}
              />
            </section>
        )}
      </div>
  );
};

export default App;