import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App.jsx';
import Feed from './components/Feed.jsx';

const words = [
	{ id: 'w1', text: '감자' },
	{ id: 'w2', text: '캠핑' },
	{ id: 'w3', text: '치즈' },
];
const idea = {
	id: 'i1',
	categoryId: 'c1',
	words,
	direction: '',
	title: '캠핑 감자 치즈',
	content: '세 단어가 만난 새 요리입니다.',
	source: 'mock',
	hasRecipe: false,
	supportsRecipe: true,
};
const recipe = {
	ideaId: 'i1',
	kind: 'food',
	feasible: true,
	summary: '간편하게 만들 수 있습니다.',
	ingredients: [{ name: '감자', amount: '2개' }],
	steps: [{ order: 1, title: '재료 준비', description: '감자를 씻습니다.' }],
	source: 'mock',
};
const json = (data, status = 200) => new Response(JSON.stringify(data), { status });
function server(overrides = {}) {
	return vi.fn(async (path, options) => {
		if (overrides[path]) return overrides[path](options);
		if (path === '/common/health') return json({ status: 'ok', aiMode: 'mock' });
		if (path === '/app/user/login')
			return json({ accessToken: 'access', refreshToken: 'refresh' });
		if (path === '/app/user/profile') return json({ uid: 'student', name: '학생1' });
		if (path === '/app/user/sign-up') return json({ uid: 'student' });
		if (path === '/app/categories')
			return json({
				items: [{ id: 'c1', name: '음식 개발', kind: 'default', wordCount: 149 }],
			});
		if (path === '/app/words/draw') return json({ words, source: 'random' });
		if (path === '/app/ideas') return json({ idea, cached: false }, 201);
		if (path === '/app/ideas/i1/guide')
			return json(
				{
					guide: {
						...recipe,
						label: '만드는 법',
						requirementsLabel: '재료',
						requirements: recipe.ingredients.map((item) => ({
							name: item.name,
							detail: item.amount,
						})),
					},
					cached: false,
				},
				201,
			);
		if (path.startsWith('/app/posts?')) return json({ items: [], nextCursor: null });
		return json({ message: `Unhandled test route ${path}` }, 404);
	});
}
async function login(user) {
	await user.type(screen.getByLabelText('아이디'), 'student');
	await user.type(screen.getByLabelText('비밀번호'), 'password');
	await user.click(screen.getByRole('button', { name: '로그인', exact: true }));
	return screen.findByRole('button', { name: /음식 개발/ });
}
async function chooseAndDraw(user) {
	await user.click(await login(user));
	await user.click(screen.getByRole('button', { name: '세 단어 뽑기' }));
	await screen.findByRole('button', { name: '감자 고정' });
}
describe('student demo interactions', () => {
	it('shows backend readiness and signup fields before login', async () => {
		const user = userEvent.setup();
		render(<App fetcher={server()} />);
		expect(await screen.findByText('백엔드 연결됨')).toBeInTheDocument();
		await user.click(screen.getByRole('button', { name: /처음인가요/ }));
		expect(screen.getByLabelText('이메일')).toHaveAttribute('type', 'email');
		expect(screen.getByLabelText('생년월일')).toHaveAttribute('type', 'date');
	});
	it('shows a login error without entering the workshop', async () => {
		const user = userEvent.setup();
		render(
			<App
				fetcher={server({
					'/app/user/login': () => json({ message: '비밀번호가 틀립니다.' }, 400),
				})}
			/>,
		);
		await user.type(screen.getByLabelText('아이디'), 'student');
		await user.type(screen.getByLabelText('비밀번호'), 'wrong');
		await user.click(screen.getByRole('button', { name: '로그인', exact: true }));
		expect(await screen.findByRole('alert')).toHaveTextContent('비밀번호가 틀립니다.');
		expect(screen.queryByRole('button', { name: '세 단어 뽑기' })).not.toBeInTheDocument();
	});
	it('requires three words and limits fixed words to two', async () => {
		const user = userEvent.setup();
		render(<App fetcher={server()} />);
		await login(user);
		expect(screen.getByRole('button', { name: '아이디어 만들기 ↗' })).toBeDisabled();
		await user.click(screen.getByRole('button', { name: /음식 개발/ }));
		await user.click(screen.getByRole('button', { name: '세 단어 뽑기' }));
		await user.click(await screen.findByRole('button', { name: '감자 고정' }));
		await user.click(screen.getByRole('button', { name: '캠핑 고정' }));
		await user.click(screen.getByRole('button', { name: '치즈 고정' }));
		expect(await screen.findByRole('alert')).toHaveTextContent('최대 2개');
		expect(screen.getByRole('button', { name: '감자 고정 해제' })).toHaveAttribute(
			'aria-pressed',
			'true',
		);
	});
	it('creates confirms requests a recipe and saves an offline record', async () => {
		const user = userEvent.setup();
		const fetcher = server();
		render(<App fetcher={fetcher} />);
		await chooseAndDraw(user);
		await user.click(screen.getByRole('button', { name: '아이디어 만들기 ↗' }));
		expect(await screen.findByRole('heading', { name: idea.title })).toBeInTheDocument();
		expect(screen.queryByRole('button', { name: '만드는 법 보기' })).not.toBeInTheDocument();
		await user.click(screen.getByRole('button', { name: '이 아이디어로 확정' }));
		await user.click(screen.getByRole('button', { name: '만드는 법 보기' }));
		expect(await screen.findByText('감자를 씻습니다.')).toBeInTheDocument();
		await user.click(screen.getByRole('button', { name: '내 기록에 저장' }));
		await user.click(
			within(screen.getByRole('navigation', { name: '주 메뉴' })).getByRole('button', {
				name: /내 기록/,
			}),
		);
		expect(screen.getByText('1개 저장됨')).toBeInTheDocument();
		expect(JSON.parse(localStorage.getItem('idea-demo-records'))[0].recipe.summary).toBe(
			recipe.summary,
		);
		const call = fetcher.mock.calls.find(([path]) => path === '/app/ideas');
		expect(JSON.parse(call[1].body).wordIds).toEqual(['w1', 'w2', 'w3']);
	});
	it('retains selected words when generation fails', async () => {
		const user = userEvent.setup();
		render(
			<App
				fetcher={server({
					'/app/ideas': () => json({ message: 'AI 응답 시간이 초과되었습니다.' }, 504),
				})}
			/>,
		);
		await chooseAndDraw(user);
		await user.click(screen.getByRole('button', { name: '아이디어 만들기 ↗' }));
		expect(await screen.findByRole('alert')).toHaveTextContent('시간이 초과');
		expect(screen.getByRole('button', { name: '감자 고정' })).toBeInTheDocument();
		expect(screen.getByRole('button', { name: '아이디어 만들기 ↗' })).toBeEnabled();
	});
	it('disables reactions on the current member post', async () => {
		const api = {
			request: vi.fn().mockResolvedValue({
				items: [
					{
						id: 'p1',
						ideaId: 'i1',
						title: idea.title,
						content: '설명',
						words: ['감자'],
						authorName: '학생',
						isMine: true,
						myReaction: null,
						reactions: { like: 0, meh: 0, dislike: 0 },
						createdAt: '2026-10-27T00:00:00Z',
					},
				],
				nextCursor: null,
			}),
		};
		render(
			<Feed
				api={api}
				active
				userUid="student"
				run={(action) => action()}
				busy=""
				onOpen={() => {}}
			/>,
		);
		expect(await screen.findByRole('button', { name: `${idea.title} 좋아요` })).toBeDisabled();
	});
	it('clicking an active reaction sends null to cancel it', async () => {
		const user = userEvent.setup();
		const post = {
			id: 'p1',
			ideaId: 'i1',
			title: idea.title,
			content: '설명',
			words: [],
			authorName: '다른 학생',
			isMine: false,
			myReaction: 'like',
			reactions: { like: 1, meh: 0, dislike: 0 },
			createdAt: '2026-10-27T00:00:00Z',
		};
		const api = {
			request: vi
				.fn()
				.mockResolvedValueOnce({ items: [post], nextCursor: null })
				.mockResolvedValueOnce({
					myReaction: null,
					reactions: { like: 0, meh: 0, dislike: 0 },
				}),
		};
		render(
			<Feed
				api={api}
				active
				userUid="student"
				run={(action) => action()}
				busy=""
				onOpen={() => {}}
			/>,
		);
		await user.click(await screen.findByRole('button', { name: `${idea.title} 좋아요` }));
		expect(api.request).toHaveBeenLastCalledWith('/app/posts/p1/reaction', {
			method: 'PUT',
			body: { kind: null },
		});
		await waitFor(() =>
			expect(screen.getByRole('button', { name: `${idea.title} 좋아요` })).toHaveAttribute(
				'aria-pressed',
				'false',
			),
		);
	});
});
