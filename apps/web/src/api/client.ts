const BASE_URL = import.meta.env.VITE_CORE_API_URL;

let token: string | null = null;
export const setAuthToken = (t: string | null) => (token = t);

export async function api<T>(
    path: string,
    init: RequestInit & { ifMatch?: string } = {}
): Promise<{ ok: boolean; data?: T; etag?: string; status: number }> {
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...(init.headers as Record<string, string>),
    };
    if (token) headers.Authorization = `Bearer ${token}`;
    if (init.ifMatch) headers['If-Match'] = init.ifMatch;

    const res = await fetch(`${BASE_URL}${path}`, { ...init, headers });
    const etag = res.headers.get('ETag') ?? undefined;
    const body = res.headers.get('content-type')?.includes('application/json')
        ? await res.json()
        : await res.text();

    return { ok: res.ok, data: body as T, etag, status: res.status };
}
