import { api, setAuthToken } from './client';

export type Role = 'MANAGER' | 'EMPLOYEE' | 'COWORKER';
export interface Session { token: string; role: Role; employeeId: string }

export async function login(email: string, password: string): Promise<Session> {
    const res = await api<{ token: string; role: Role; employeeId: string }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
    });
    if (!res.ok) throw new Error('Login failed');
    const s: Session = { token: res.data!.token, role: res.data!.role, employeeId: String(res.data!.employeeId) };
    setAuthToken(s.token);
    return s;
}
