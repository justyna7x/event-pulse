import React, { useEffect, useState } from 'react';
import {fetchEndpoints, createEndpoint, deleteEndpoint, triggerPingNow} from './api/endpointApi';
import type { MonitoredEndpoint, CreateEndpointPayload } from './api/endpointApi';

export const App: React.FC = () => {
  const [endpoints, setEndpoints] = useState<MonitoredEndpoint[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

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
    // Poll endpoints every 10 seconds to update 'lastStatus' in real-time
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
      await loadEndpoints();
    } catch (err: any) {
      alert('Failed to delete endpoint.');
    }
  };

  return (
      <div className="container">
        <header>
          <h1>EventPulse Dashboard</h1>
          <p>Real-Time API & Webhook Monitoring</p>
        </header>

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

        {/* Endpoint List Table */}
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
                  <th>Action</th>
                  <th>Webhook Alert</th>
                </tr>
                </thead>
                <tbody>
                {endpoints.length === 0 ? (
                    <tr>
                      <td colSpan={6}>No endpoints configured yet.</td>
                    </tr>
                ) : (
                    endpoints.map((ep) => (
                        <tr key={ep.id}>
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
                            <button onClick={() => handleDelete(ep.id)} className="btn-danger">
                              Delete
                            </button>
                            <button
                                onClick={async () => {
                                  await triggerPingNow(ep.id);
                                  await loadEndpoints();
                                }}
                                className="btn-secondary"
                                style={{ marginRight: '0.5rem' }}
                            >
                              ⚡ Ping
                            </button>
                          </td>
                          <td>
                            {ep.webhookUrl ? (
                                <span className="badge webhook-active">Active</span>
                            ) : (
                                <span className="badge webhook-none">None</span>
                            )}
                          </td>
                        </tr>
                    ))
                )}
                </tbody>
              </table>
          )}
        </section>
      </div>
  );
};

export default App;