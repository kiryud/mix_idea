import { describe, it, expect, vi } from 'vitest';
import { createApiClient, ApiError } from './api.js';

const response = (status, body) =>
	new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
describe('API client', () => {
	it('sends JSON and the current access token', async () => {
		const fetcher = vi.fn().mockResolvedValue(response(200, { items: [] }));
		const api = createApiClient({ fetcher });
		api.setSession({ accessToken: 'access', refreshToken: 'refresh' });
		await api.request('/app/categories');
		expect(fetcher.mock.calls[0][1].headers.Authorization).toBe('Bearer access');
		expect(fetcher.mock.calls[0][1].headers['Content-Type']).toBe('application/json');
	});
	it('refreshes a 401 once and retries with the new token', async () => {
		const fetcher = vi
			.fn()
			.mockResolvedValueOnce(response(401, {}))
			.mockResolvedValueOnce(response(200, { accessToken: 'new' }))
			.mockResolvedValueOnce(response(200, { ok: true }));
		const api = createApiClient({ fetcher });
		api.setSession({ accessToken: 'old', refreshToken: 'refresh' });
		expect(await api.request('/app/categories')).toEqual({ ok: true });
		expect(fetcher).toHaveBeenCalledTimes(3);
		expect(fetcher.mock.calls[2][1].headers.Authorization).toBe('Bearer new');
		expect(api.getSession().refreshToken).toBe('refresh');
	});
	it('does not retry a second unauthorized result', async () => {
		const fetcher = vi
			.fn()
			.mockResolvedValueOnce(response(401, {}))
			.mockResolvedValueOnce(response(200, { accessToken: 'new' }))
			.mockResolvedValueOnce(response(401, { message: '다시 로그인해 주세요' }));
		const api = createApiClient({ fetcher });
		api.setSession({ accessToken: 'old', refreshToken: 'refresh' });
		await expect(api.request('/app/categories')).rejects.toMatchObject({ status: 401 });
		expect(fetcher).toHaveBeenCalledTimes(3);
		expect(api.getSession()).toBeNull();
	});
	it('clears the session when refresh fails', async () => {
		const fetcher = vi
			.fn()
			.mockResolvedValueOnce(response(401, {}))
			.mockResolvedValueOnce(response(401, {}));
		const api = createApiClient({ fetcher });
		api.setSession({ accessToken: 'old', refreshToken: 'refresh' });
		await expect(api.request('/app/categories')).rejects.toBeInstanceOf(ApiError);
		expect(api.getSession()).toBeNull();
	});
	it('never refreshes signup or login failures', async () => {
		const fetcher = vi
			.fn()
			.mockResolvedValue(response(400, { code: 6, message: '비밀번호 확인' }));
		const api = createApiClient({ fetcher });
		await expect(
			api.request('/app/user/login', {
				method: 'POST',
				body: { id: 'student', pw: 'password' },
				auth: false,
			}),
		).rejects.toMatchObject({ code: 6 });
		expect(fetcher).toHaveBeenCalledTimes(1);
	});
	it('preserves domain message and Retry-After', async () => {
		const fetcher = vi
			.fn()
			.mockResolvedValue(
				new Response(JSON.stringify({ code: 1107, message: '잠시 기다려 주세요' }), {
					status: 429,
					headers: { 'Retry-After': '30' },
				}),
			);
		await expect(createApiClient({ fetcher }).request('/app/ideas')).rejects.toMatchObject({
			status: 429,
			code: 1107,
			retryAfter: '30',
			message: '잠시 기다려 주세요',
		});
	});
	it('never writes auth credentials to request traces', async () => {
		const onTrace = vi.fn();
		const api = createApiClient({
			fetcher: vi
				.fn()
				.mockResolvedValue(
					response(200, { accessToken: 'SECRET_ACCESS', refreshToken: 'SECRET_REFRESH' }),
				),
			onTrace,
		});
		await api.request('/app/user/login', {
			method: 'POST',
			body: { pw: 'SECRET_PASSWORD' },
			auth: false,
		});
		expect(JSON.stringify(onTrace.mock.calls)).not.toMatch(/SECRET_/);
	});
	it('reports a helpful error for an unreachable server', async () => {
		const api = createApiClient({
			fetcher: vi.fn().mockRejectedValue(new TypeError('Failed to fetch')),
		});
		await expect(api.request('/common/health', { auth: false })).rejects.toMatchObject({
			status: 0,
		});
	});
	it('shares a refresh between simultaneous unauthorized requests', async () => {
		let refreshCalls = 0;
		const fetcher = vi.fn(async (path, options) => {
			if (path.includes('refresh-token')) {
				refreshCalls++;
				await new Promise((resolve) => setTimeout(resolve, 5));
				return response(200, { accessToken: 'new' });
			}
			return options.headers.Authorization === 'Bearer new'
				? response(200, { items: [] })
				: response(401, {});
		});
		const api = createApiClient({ fetcher });
		api.setSession({ accessToken: 'old', refreshToken: 'refresh' });
		await Promise.all([api.request('/app/categories'), api.request('/app/posts')]);
		expect(refreshCalls).toBe(1);
	});
	it('logout removes session tokens', () => {
		const api = createApiClient();
		api.setSession({ accessToken: 'a', refreshToken: 'r' });
		api.logout();
		expect(sessionStorage.getItem('idea-demo-session')).toBeNull();
	});
});
