const SESSION_KEY = 'idea-demo-session';

export class ApiError extends Error {
	constructor(status, data = {}, retryAfter = null) {
		super(
			data.message ||
				(status === 0
					? '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.'
					: '요청을 처리하지 못했습니다. 다시 시도해 주세요.'),
		);
		this.name = 'ApiError';
		this.status = status;
		this.code = data.code;
		this.retryAfter = retryAfter;
	}
}

export function createApiClient({
	fetcher = (...args) => fetch(...args),
	storage = sessionStorage,
	onAuthChange = () => {},
	onTrace = () => {},
} = {}) {
	let session = null;
	try {
		session = JSON.parse(storage.getItem(SESSION_KEY));
	} catch {
		/* A damaged demo session starts logged out. */
	}
	let refreshing = null;

	function setSession(value) {
		session = value;
		if (value) storage.setItem(SESSION_KEY, JSON.stringify(value));
		else storage.removeItem(SESSION_KEY);
		onAuthChange(value);
	}

	async function send(path, { method = 'GET', body, auth = true, signal } = {}, retry = true) {
		const sentToken = session?.accessToken;
		const headers = { 'Content-Type': 'application/json' };
		if (auth && sentToken) headers.Authorization = `Bearer ${sentToken}`;
		const started = performance.now();
		let response;
		try {
			response = await fetcher(path, {
				method,
				headers,
				body: body === undefined ? undefined : JSON.stringify(body),
				signal,
			});
		} catch (error) {
			if (error.name === 'AbortError') throw error;
			onTrace({ method, path, status: 0, duration: Math.round(performance.now() - started) });
			throw new ApiError(0);
		}
		let data;
		try {
			data = await response.json();
		} catch {
			data = {};
		}
		onTrace({
			method,
			path,
			status: response.status,
			duration: Math.round(performance.now() - started),
			data: path.startsWith('/app/user/')
				? { message: '인증 응답은 표시하지 않습니다.' }
				: data,
		});
		if (response.status === 401 && auth && retry && session?.refreshToken) {
			try {
				// A delayed 401 can arrive after another request already renewed the token.
				if (session.accessToken === sentToken) {
					if (!refreshing)
						refreshing = send(
							'/app/user/refresh-token',
							{
								method: 'POST',
								body: { refreshToken: session.refreshToken },
								auth: false,
							},
							false,
						)
							.then((result) => {
								if (!result.accessToken) throw new ApiError(401);
								setSession({ ...session, accessToken: result.accessToken });
							})
							.finally(() => {
								refreshing = null;
							});
					await refreshing;
				}
			} catch (error) {
				setSession(null);
				throw error;
			}
			return send(path, { method, body, auth, signal }, false);
		}
		if (!response.ok) {
			if (response.status === 401 && auth) setSession(null);
			throw new ApiError(response.status, data, response.headers.get('Retry-After'));
		}
		return data;
	}
	return { request: send, setSession, getSession: () => session, logout: () => setSession(null) };
}
