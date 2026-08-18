import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1/endpoints';

export interface MonitoredEndpoint {
    id: number;
    name: string;
    url: string;
    httpMethod: string;
    expectedStatusCode: number;
    checkIntervalSeconds: number;
    active: boolean;
    lastCheckedAt?: string;
    lastStatus?: 'UP' | 'DOWN';
}

export interface CreateEndpointPayload {
    name: string;
    url: string;
    httpMethod?: string;
    expectedStatusCode?: number;
    checkIntervalSeconds?: number;
}

export const fetchEndpoints = async (): Promise<MonitoredEndpoint[]> => {
    const response = await axios.get(API_BASE_URL);
    return response.data;
};

export const createEndpoint = async (data: CreateEndpointPayload): Promise<MonitoredEndpoint> => {
    const response = await axios.post(API_BASE_URL, data);
    return response.data;
};

export const deleteEndpoint = async (id: number): Promise<void> => {
    await axios.delete(`${API_BASE_URL}/${id}`);
};