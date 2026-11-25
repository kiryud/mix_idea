import { useEffect, useState } from 'react';

export default function Feed({ api, active, userUid, run, busy, onOpen }) {
	const [sort, setSort] = useState('latest');
	const [items, setItems] = useState([]);
	const [cursor, setCursor] = useState(null);
	async function load(append = false, order = sort) {
		await run(
			async () => {
				const query = new URLSearchParams({ sort: order, limit: '10' });
				if (append && cursor) query.set('cursor', cursor);
				const result = await api.request(`/app/posts?${query}`);
				setItems((previous) =>
					append
						? [
								...new Map(
									[...previous, ...result.items].map((item) => [item.id, item]),
								).values(),
							]
						: result.items,
				);
				setCursor(result.nextCursor);
			},
			null,
			'feed',
		);
	}
	useEffect(() => {
		if (active) {
			setCursor(null);
			load();
		}
	}, [active, userUid]);
	async function react(post, kind) {
		await run(async () => {
			const result = await api.request(`/app/posts/${post.id}/reaction`, {
				method: 'PUT',
				body: { kind: post.myReaction === kind ? null : kind },
			});
			setItems((previous) =>
				previous.map((item) =>
					item.id === post.id
						? { ...item, myReaction: result.myReaction, reactions: result.reactions }
						: item,
				),
			);
		}, '반응을 반영했습니다.');
	}
	return (
		<>
			<div className="page-heading">
				<div>
					<span className="eyebrow">SHARE THE SPARK</span>
					<h1>생각이 모이는 피드</h1>
					<p>다른 사람의 조합을 보고 생각을 나눠 보세요.</p>
				</div>
				<button disabled={!!busy} onClick={() => load()}>
					↻ 새로고침
				</button>
			</div>
			<div className="feed-toolbar">
				<div className="segmented">
					{[
						['latest', '최신순'],
						['popular', '인기순'],
					].map(([key, title]) => (
						<button
							aria-pressed={sort === key}
							disabled={!!busy}
							key={key}
							onClick={() => {
								setSort(key);
								setCursor(null);
								load(false, key);
							}}
						>
							{title}
						</button>
					))}
				</div>
				<p className="muted">내 글에는 반응할 수 없습니다.</p>
			</div>
			{!items.length && (
				<div className="card empty-state">
					<span>✦</span>
					<h2>
						{busy === 'feed' ? '아이디어를 가져오고 있어요' : '첫 생각을 공유해 주세요'}
					</h2>
					<p>아이디어를 만든 뒤 ‘피드에 올리기’를 누르면 여기에 나타납니다.</p>
				</div>
			)}
			<div className="feed-grid">
				{items.map((post) => (
					<article className="card feed-card" key={post.id}>
						<div className="post-author">
							<span className="avatar">{post.authorName.slice(0, 1)}</span>
							<strong>{post.authorName}</strong>
							{post.isMine && <span className="tag">내 글</span>}
							<time>{new Date(post.createdAt).toLocaleDateString('ko-KR')}</time>
						</div>
						<h2>{post.title}</h2>
						<div className="chips">
							{post.words.map((word, index) => (
								<span key={index}>{word}</span>
							))}
						</div>
						<p className="feed-content">{post.content}</p>
						<button
							className="text-button"
							disabled={!!busy}
							onClick={() => onOpen(post.ideaId)}
						>
							아이디어 자세히 보기 ↗
						</button>
						<div className="reactions">
							{[
								['like', '좋아요'],
								['meh', '그저 그래요'],
								['dislike', '별로예요'],
							].map(([kind, label]) => (
								<button
									aria-label={`${post.title} ${label}`}
									aria-pressed={post.myReaction === kind}
									disabled={post.isMine || !!busy}
									key={kind}
									onClick={() => react(post, kind)}
								>
									{label} <b>{post.reactions[kind]}</b>
								</button>
							))}
						</div>
					</article>
				))}
			</div>
			{cursor && (
				<button className="load-more" disabled={!!busy} onClick={() => load(true)}>
					아이디어 더 보기 ↓
				</button>
			)}
		</>
	);
}
