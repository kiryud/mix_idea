import { useCallback, useEffect, useRef, useState } from 'react';
import { createApiClient } from './api.js';
import { loadRecords, saveRecord, removeRecord } from './storage.js';
import AuthPanel from './components/AuthPanel.jsx';
import IdeaWizard from './components/IdeaWizard.jsx';
import Feed from './components/Feed.jsx';
import Records from './components/Records.jsx';

export default function App({ fetcher }) {
	const [view, setView] = useState('create');
	const [user, setUser] = useState(null);
	const [categories, setCategories] = useState([]);
	const [health, setHealth] = useState(null);
	const [busy, setBusy] = useState('');
	const [notice, setNotice] = useState(null);
	const [traces, setTraces] = useState([]);
	const [records, setRecords] = useState(loadRecords);
	const [restore, setRestore] = useState(null);
	const taskLock = useRef(false);
	const client = useRef(null);
	if (!client.current)
		client.current = createApiClient({
			fetcher,
			onAuthChange: (session) => {
				if (!session) setUser(null);
			},
			onTrace: (trace) => setTraces((previous) => [trace, ...previous].slice(0, 15)),
		});
	const api = client.current;
	const notify = useCallback((message, type = 'success') => setNotice({ message, type }), []);
	const run = useCallback(
		async (action, message, label = 'request') => {
			if (taskLock.current) return;
			taskLock.current = true;
			setBusy(label);
			setNotice(null);
			try {
				await action();
				if (message) notify(message);
			} catch (error) {
				if (error.name !== 'AbortError')
					notify(
						`${error.message}${error.retryAfter ? ` (${error.retryAfter}초 후 다시 시도)` : ''}`,
						'error',
					);
				else notify('화면의 생성 대기를 중단했습니다.');
			} finally {
				taskLock.current = false;
				setBusy('');
			}
		},
		[notify],
	);
	const reloadCategories = useCallback(async () => {
		const result = await api.request('/app/categories');
		setCategories(result.items);
	}, [api]);
	useEffect(() => {
		let alive = true;
		api.request('/common/health', { auth: false })
			.then((result) => {
				if (alive) setHealth(result);
			})
			.catch(() => {
				if (alive) setHealth({ status: 'offline' });
			});
		if (api.getSession())
			api.request('/app/user/profile')
				.then((profile) => {
					if (alive) setUser(profile);
				})
				.catch(() => {
					if (alive) api.logout();
				});
		return () => {
			alive = false;
		};
	}, [api]);
	useEffect(() => {
		if (user) reloadCategories().catch((error) => notify(error.message, 'error'));
	}, [user, reloadCategories, notify]);
	function openRecord(row) {
		setRestore({ ...row });
		setView('create');
	}
	async function openIdea(id) {
		await run(async () => {
			const result = await api.request(`/app/ideas/${id}`);
			openRecord({ idea: result.idea, recipe: null });
		});
	}
	function store(idea, guide) {
		try {
			setRecords(saveRecord(idea, guide));
			notify('이 브라우저의 내 기록에 저장했습니다.');
		} catch {
			notify('브라우저 저장 공간이 부족합니다. 기존 기록을 정리해 주세요.', 'error');
		}
	}
	const online = health?.status === 'ok';
	return (
		<div className="app-shell">
			<header className="topbar">
				<a
					className="brand"
					href="#"
					onClick={(event) => {
						event.preventDefault();
						setView('create');
					}}
				>
					<span className="brand-symbol">✳</span>아이디어 작업실
					<small>STUDENT LAB</small>
				</a>
				<nav aria-label="주 메뉴">
					{[
						['create', '아이디어 만들기'],
						['feed', '아이디어 피드'],
						['records', '내 기록'],
					].map(([key, label]) => (
						<button
							key={key}
							aria-current={view === key ? 'page' : undefined}
							disabled={!!busy}
							onClick={() => setView(key)}
						>
							{label}
							{key === 'records' && records.length > 0 && (
								<span className="nav-count">{records.length}</span>
							)}
						</button>
					))}
				</nav>
				<div className="account">
					{user ? (
						<>
							<span>{user.name || '학생'} 님</span>
							<button
								className="text-button"
								disabled={!!busy}
								onClick={() => {
									api.logout();
									setView('create');
									setCategories([]);
									setRestore(null);
									notify('로그아웃했습니다.');
								}}
							>
								로그아웃
							</button>
						</>
					) : (
						<span className="muted">함께 만드는 새로운 생각</span>
					)}
				</div>
			</header>
			<div className="workspace">
				<main>
					{notice && (
						<div
							role={notice.type === 'error' ? 'alert' : 'status'}
							className={`notice ${notice.type}`}
						>
							<span>{notice.type === 'error' ? '!' : '✓'}</span>
							{notice.message}
							<button aria-label="알림 닫기" onClick={() => setNotice(null)}>
								×
							</button>
						</div>
					)}
					{!user && view !== 'records' && (
						<AuthPanel api={api} run={run} busy={busy} onLogin={setUser} />
					)}
					{user && (
						<>
							<div hidden={view !== 'create'}>
								<IdeaWizard
									key={user.uid}
									api={api}
									categories={categories}
									reloadCategories={reloadCategories}
									run={run}
									busy={busy}
									notify={notify}
									onSave={store}
									restore={restore}
								/>
							</div>
							<div hidden={view !== 'feed'}>
								<Feed
									api={api}
									active={view === 'feed'}
									userUid={user.uid}
									run={run}
									busy={busy}
									onOpen={openIdea}
								/>
							</div>
						</>
					)}
					{view === 'records' && (
						<Records
							records={records}
							onOpen={(row) => {
								if (user) openRecord(row);
								else notify('기록을 작업실에서 열려면 로그인해 주세요.', 'error');
							}}
							onRemove={(id) => {
								setRecords(removeRecord(id));
								notify('이 브라우저에서 기록을 삭제했습니다.');
							}}
						/>
					)}
					<footer>
						작게 시도하고, 더 크게 생각하기. <span>IDEA SUPPORT · 학생 실습</span>
					</footer>
				</main>
				<aside className="lab-panel">
					<div className="lab-title">
						<span className="live-dot" />
						LIVE LAB<span>실시간</span>
					</div>
					<h2>화면 뒤의 흐름</h2>
					<p>버튼을 누르면 어떤 요청이 오가는지 확인해 보세요.</p>
					<div className="server-status">
						<span className={`status-dot ${online ? 'online' : ''}`} />
						<strong>
							{online
								? '백엔드 연결됨'
								: health?.status === 'offline'
									? '백엔드 연결 확인 필요'
									: '서버 상태 확인 중'}
						</strong>
						{health?.aiMode && (
							<small>
								{health.aiMode === 'gemini' ? 'Gemini 실제 생성' : 'Mock 시연 모드'}
							</small>
						)}
					</div>
					<div className="lab-note">
						<strong>사용하는 순서</strong>
						<ol>
							<li>가입 또는 로그인</li>
							<li>주제와 세 단어 선택</li>
							<li>AI 아이디어 생성·확정</li>
							<li>분야별 가이드·저장·공유</li>
							<li>다른 계정으로 피드 반응</li>
						</ol>
					</div>
					<div className="section-head">
						<h3>요청 기록</h3>
						<span>{traces.length}</span>
					</div>
					<div className="trace-list">
						{traces.length ? (
							traces.map((trace, index) => (
								<details key={`${trace.path}-${index}`} className="trace">
									<summary>
										<span className={`method method-${trace.method}`}>
											{trace.method}
										</span>
										<code>{trace.path}</code>
										<span
											className={
												trace.status >= 400 || trace.status === 0
													? 'bad-code'
													: 'good-code'
											}
										>
											{trace.status || 'OFF'}
										</span>
									</summary>
									<small>{trace.duration} ms</small>
									{trace.data && <pre>{JSON.stringify(trace.data, null, 2)}</pre>}
								</details>
							))
						) : (
							<p className="muted">첫 요청을 기다리고 있습니다.</p>
						)}
					</div>
					<p className="lab-disclaimer">
						비밀번호와 인증 토큰은 요청 기록에 표시하지 않습니다.
					</p>
					<a
						className="text-link"
						href="http://localhost:8080/swagger-ui/index.html"
						target="_blank"
						rel="noreferrer"
					>
						전체 API 명세 보기 ↗
					</a>
				</aside>
			</div>
		</div>
	);
}
