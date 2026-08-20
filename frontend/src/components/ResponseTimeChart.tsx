import React, { useEffect, useState } from 'react';
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { fetchEndpointLogs } from '../api/endpointApi';
import type { CheckLog } from '../api/endpointApi';

interface Props {
    endpointId: number;
    endpointName: string;
}

export const ResponseTimeChart: React.FC<Props> = ({ endpointId, endpointName }) => {
    const [logs, setLogs] = useState<CheckLog[]>([]);
    const [loading, setLoading] = useState<boolean>(true);

    useEffect(() => {
        const loadLogs = async () => {
            try {
                const data = await fetchEndpointLogs(endpointId, 20);
                // Reverse so oldest is on the left, newest on the right
                setLogs(data.reverse());
            } catch (err) {
                console.error('Failed to load logs', err);
            } finally {
                setLoading(false);
            }
        };

        loadLogs();
        const interval = setInterval(loadLogs, 10000); // refresh with background ping
        return () => clearInterval(interval);
    }, [endpointId]);

    const formattedData = logs.map((log) => ({
        time: new Date(log.checkedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
        latency: log.responseTimeMs,
        success: log.success
    }));

    if (loading) return <p>Loading latency metrics...</p>;
    if (logs.length === 0) return <p>No metrics collected yet.</p>;

    return (
        <div style={{ width: '100%', height: 250, marginTop: '1rem' }}>
            <h4>Response Time (ms) - {endpointName}</h4>
            <ResponsiveContainer width="100%" height="80%">
                <AreaChart data={formattedData}>
                    <defs>
                        <linearGradient id="colorLatency" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#2563eb" stopOpacity={0.8}/>
                            <stop offset="95%" stopColor="#2563eb" stopOpacity={0}/>
                        </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="time" tick={{ fontSize: 12 }} />
                    <YAxis unit=" ms" tick={{ fontSize: 12 }} />
                    <Tooltip
                        formatter={(value: any) => [`${value} ms`, 'Latency']}
                        labelFormatter={(label: any) => `Time: ${label}`}
                    />
                    <Area
                        type="monotone"
                        dataKey="latency"
                        stroke="#2563eb"
                        fillOpacity={1}
                        fill="url(#colorLatency)"
                    />
                </AreaChart>
            </ResponsiveContainer>
        </div>
    );
};