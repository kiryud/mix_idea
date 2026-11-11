import { useEffect, useRef, useState } from 'react';
import GuideView from './GuideView.jsx';
import { guideTypes, savedGuide } from '../guides.js';

export default function IdeaWizard({
	api,
	categories,
	reloadCategories,
	run,
	busy,
	notify,
	onSave,
	restore,
}) {
	const [categoryId, setCategoryId] = useState('');
	const [words, setWords] = useState([]);
	const [locked, setLocked] = useState([]);
	const [direction, setDirection] = useState('');
	const [idea, setIdea] = useState(null);
	const [guide, setGuide] = useState(null);
	const [confirmed, setConfirmed] = useState(false);
	const [custom, setCustom] = useState('');
	const [preview, setPreview] = useState(null);
	const [share, setShare] = useState(null);
	const generation = useRef(null);
	const selected = categories.find((row) => row.id === categoryId);
	const guideType =
		idea?.guideType ||
		selected?.guideType ||
		((idea?.supportsRecipe ?? selected?.supportsRecipe) ? 'food' : 'general');
	const guideInfo = guideTypes[guideType] || guideTypes.general;
	const activeGuide = guide?.kind === guideType ? guide : null;
	const [customGuideType, setCustomGuideType] = useState('general');
	useEffect(() => {
		if (restore) {
			setIdea(restore.idea);
			setGuide(savedGuide(restore));
			setCategoryId(restore.idea.categoryId);
			setWords(restore.idea.words);
			setDirection(restore.idea.direction);
			setLocked([]);
			setConfirmed(true);
			setShare(null);
		}
	}, [restore]);
	useEffect(() => () => generation.current?.abort(), []);
	function resetResult() {
		setIdea(null);
		setGuide(null);
		setConfirmed(false);
		setShare(null);
	}
	function choose(id) {
		setCategoryId(id);
		setWords([]);
		setLocked([]);
		setDirection('');
		resetResult();
	}
	function toggleLock(id) {
		if (locked.includes(id)) setLocked(locked.filter((value) => value !== id));
		else if (locked.length < 2) setLocked([...locked, id]);
		else notify('단어는 최대 2개까지 고정할 수 있습니다.', 'error');
	}
	async function draw() {
		await run(
			async () => {
				const result = await api.request('/app/words/draw', {
					method: 'POST',
					body: {
						categoryId,
						lockedWordIds: locked,
						previousWordIds: words.map((word) => word.id),
						direction,
					},
				});
				setWords(result.words);
				resetResult();
			},
			'단어 3개를 골랐습니다. 마음에 드는 단어는 고정해 보세요.',
			'draw',
		);
	}
	async function generate() {
		const controller = new AbortController();
		generation.current = controller;
		await run(
			async () => {
				const result = await api.request('/app/ideas', {
					method: 'POST',
					body: { categoryId, wordIds: words.map((word) => word.id), direction },
					signal: controller.signal,
				});
				if (!controller.signal.aborted) {
					setIdea(result.idea);
					setGuide(null);
					setConfirmed(false);
					setShare(null);
					notify(
						result.cached
							? '같은 조합의 결과를 재사용했습니다.'
							: '새 아이디어가 도착했습니다.',
					);
				}
			},
			null,
			'generate',
		);
		if (generation.current === controller) generation.current = null;
	}
	async function customAction(create) {
		const submittedName = custom.trim();
		await run(
			async () => {
				const result = await api.request(
					create ? '/app/categories' : '/app/categories/preview',
					{ method: 'POST', body: { name: submittedName, guideType: customGuideType } },
				);
				if (create) {
					await reloadCategories();
					choose(result.category.id);
					setCustom('');
					setPreview(null);
				} else setPreview({ ...result, submittedName });
			},
			create ? '카테고리가 준비되었습니다.' : '연관 단어를 확인했습니다.',
		);
	}
	async function shareIdea(feed) {
		await run(
			async () => {
				const result = await api.request(feed ? '/app/posts' : '/app/shares', {
					method: 'POST',
					body: { ideaId: idea.id },
				});
				setShare(result);
				if (!feed) {
					try {
						await navigator.clipboard.writeText(result.shareUrl);
						notify('공유 링크를 복사했습니다.');
					} catch {
						notify('아래 링크를 선택해 복사할 수 있습니다.');
					}
				}
			},
			feed ? '피드에 게시했습니다. 피드 탭에서 확인해 보세요.' : null,
		);
	}
	const currentStep = !categoryId ? 1 : words.length !== 3 ? 2 : !idea ? 3 : 4;
	return (
		<>
			<div className="page-heading">
				<div>
					<span className="eyebrow">IDEA PLAYGROUND</span>
					<h1>오늘은 어떤 생각을 해볼까요?</h1>
					<p>세 단어를 연결해 나만의 아이디어를 만들어 보세요.</p>
				</div>
				<span className="round-stamp">
					작은 시작
					<br />
					<b>큰 가능성 ↗</b>
				</span>
			</div>
			<ol className="progress">
				{['주제 고르기', '단어 섞기', '아이디어 만들기', '결과와 공유'].map(
					(text, index) => (
						<li key={text} className={currentStep >= index + 1 ? 'active' : ''}>
							<span>{String(index + 1).padStart(2, '0')}</span>
							{text}
						</li>
					),
				)}
			</ol>
			<section className="card">
				<div className="section-head">
					<div>
						<span className="step-kicker">01 · CATEGORY</span>
						<h2>관심 있는 주제를 골라주세요</h2>
					</div>
					<span className="muted">{categories.length}개 주제</span>
				</div>
				<div className="category-grid">
					{categories.map((category, index) => (
						<button
							disabled={!!busy}
							aria-pressed={category.id === categoryId}
							className={`category ${categoryId === category.id ? 'selected' : ''}`}
							key={category.id}
							onClick={() => choose(category.id)}
						>
							<span className="category-icon">
								{category.kind === 'default' ? ['✦', '⌘', '✳'][index % 3] : '＋'}
							</span>
							<strong>{category.name}</strong>
							<small>{category.wordCount}개의 단어</small>
							<span className="category-check">
								{categoryId === category.id ? '✓' : '↗'}
							</span>
						</button>
					))}
				</div>
				<details className="custom-category">
					<summary>＋ 나만의 카테고리 만들기</summary>
					<div className="inline-form">
						<label>
							새 카테고리
							<input
								value={custom}
								maxLength={50}
								disabled={!!busy}
								onChange={(event) => {
									setCustom(event.target.value);
									setPreview(null);
								}}
								placeholder="예: 건강 관리, 캠핑 요리"
							/>
						</label>
						<label>
							후속 가이드 유형
							<select
								value={customGuideType}
								disabled={!!busy}
								onChange={(event) => {
									setCustomGuideType(event.target.value);
									setPreview(null);
								}}
							>
								{Object.entries(guideTypes).map(([value, type]) => (
									<option key={value} value={value}>
										{type.label}
									</option>
								))}
							</select>
						</label>
						<button
							disabled={!custom.trim() || !!busy}
							onClick={() => customAction(false)}
						>
							연관 단어 미리보기
						</button>
						<button
							disabled={!custom.trim() || !!busy}
							onClick={() => customAction(true)}
						>
							추가하고 선택
						</button>
					</div>
					{preview && (
						<div className="preview">
							<strong>{preview.matchedWordCount}개 연관 단어</strong>
							<p>{preview.words.map((word) => word.text).join(' · ')}</p>
							<p>
								후속 지원: {preview.guideLabel || guideTypes[customGuideType].label}
							</p>
							{preview.existingCategoryId && (
								<button onClick={() => choose(preview.existingCategoryId)}>
									이미 있는 카테고리 선택
								</button>
							)}
						</div>
					)}
				</details>
			</section>
			<section className="card">
				<div className="section-head">
					<div>
						<span className="step-kicker">02 · MIX YOUR WORDS</span>
						<h2>우연한 조합에서 시작해 보세요</h2>
					</div>
					<span className="tag">{selected?.name || '주제를 먼저 선택하세요'}</span>
				</div>
				<label>
					아이디어의 방향 <span className="muted">선택 · 최대 200자</span>
					<input
						value={direction}
						disabled={!!busy}
						maxLength={200}
						onChange={(event) => {
							setDirection(event.target.value);
							resetResult();
						}}
						placeholder="예: 간편하고 매콤한 캠핑 요리"
					/>
				</label>
				<div className="word-grid">
					{[0, 1, 2].map((index) => (
						<div className={`word-card ${words[index] ? 'filled' : ''}`} key={index}>
							<small>WORD 0{index + 1}</small>
							<strong>{words[index]?.text || '어떤 단어일까요?'}</strong>
							{words[index] ? (
								<button
									className="lock"
									disabled={!!busy}
									aria-pressed={locked.includes(words[index].id)}
									aria-label={`${words[index].text} ${locked.includes(words[index].id) ? '고정 해제' : '고정'}`}
									onClick={() => toggleLock(words[index].id)}
								>
									{locked.includes(words[index].id) ? '● 고정됨' : '○ 고정하기'}
								</button>
							) : (
								<span className="word-placeholder">✳</span>
							)}
						</div>
					))}
				</div>
				<div className="action-row">
					<p className="muted">최대 두 단어를 고정하고 나머지만 다시 뽑을 수 있어요.</p>
					<button disabled={!categoryId || !!busy} onClick={draw}>
						{busy === 'draw'
							? '단어를 고르는 중…'
							: words.length
								? '↻ 다시 뽑기'
								: '세 단어 뽑기'}
					</button>
					<button
						className="primary"
						disabled={words.length !== 3 || !!busy}
						onClick={generate}
					>
						아이디어 만들기 ↗
					</button>
				</div>
				{busy === 'generate' && (
					<div className="loading" role="status">
						<span className="spinner" />세 단어를 연결하고 있습니다…{' '}
						<button onClick={() => generation.current?.abort()}>대기 취소</button>
					</div>
				)}
			</section>
			{idea && (
				<section className="card result-card">
					<span className="step-kicker">03 · YOUR NEW IDEA</span>
					<div className="section-head">
						<h2>{idea.title}</h2>
						<span className="tag">
							{idea.source === 'mock' ? '시연 예시' : 'Gemini 생성'}
						</span>
					</div>
					<div className="chips">
						{idea.words.map((word) => (
							<span key={word.id}>{word.text}</span>
						))}
					</div>
					<p className="idea-content">{idea.content}</p>
					<div className="actions">
						<button
							disabled={!!busy}
							onClick={() =>
								run(
									async () => {
										const result = await api.request(
											`/app/ideas/${idea.id}/recombine`,
											{ method: 'POST' },
										);
										setIdea(result.idea);
										setGuide(null);
										setConfirmed(false);
										setShare(null);
									},
									'같은 단어로 새로운 결과를 만들었습니다.',
									'recombine',
								)
							}
						>
							같은 단어로 다시 조합
						</button>
						<button
							className="primary"
							disabled={!!busy || confirmed}
							onClick={() => {
								setConfirmed(true);
								notify(
									`아이디어를 확정했습니다. ${guideInfo.label}로 발전시키거나 공유해 보세요.`,
								);
							}}
						>
							{confirmed ? '✓ 확정한 아이디어' : '이 아이디어로 확정'}
						</button>
					</div>
					{confirmed && (
						<div className="confirmed-actions">
							<p className="muted guide-description">{guideInfo.description}</p>
							{
								<button
									disabled={!!busy}
									onClick={() =>
										run(
											async () => {
												const result = await api.request(
													`/app/ideas/${idea.id}/guide`,
													{ method: 'POST' },
												);
												setGuide(result.guide);
											},
											`${guideInfo.label}이 준비되었습니다.`,
											'recipe',
										)
									}
								>
									{busy === 'recipe'
										? `${guideInfo.label} 작성 중…`
										: activeGuide
											? `${guideInfo.label} 다시 확인`
											: `${guideInfo.label} 보기`}
								</button>
							}
							<button disabled={!!busy} onClick={() => onSave(idea, activeGuide)}>
								내 기록에 저장
							</button>
							<button disabled={!!busy} onClick={() => shareIdea(false)}>
								링크 복사
							</button>
							<button disabled={!!busy} onClick={() => shareIdea(true)}>
								피드에 올리기 ↗
							</button>
						</div>
					)}
					{activeGuide && <GuideView guide={activeGuide} />}
					{share && (
						<div className="share-result">
							<span>{share.scope === 'feed' ? '피드 게시 완료' : '공유 링크'}</span>
							<a href={share.shareUrl} target="_blank" rel="noreferrer">
								{share.shareUrl}
							</a>
						</div>
					)}
				</section>
			)}
		</>
	);
}
