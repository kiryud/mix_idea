export default function Records({ records, onOpen, onRemove }) {
	return (
		<>
			<div className="page-heading">
				<div>
					<span className="eyebrow">MY COLLECTION</span>
					<h1>다시 꺼내보고 싶은 생각</h1>
					<p>
						이 브라우저에 저장한 기록입니다. 서버 결과가 사라져도 내용을 읽을 수 있어요.
					</p>
				</div>
				<span className="tag">{records.length}개 저장됨</span>
			</div>
			{!records.length && (
				<div className="card empty-state">
					<span>↗</span>
					<h2>아직 저장한 기록이 없어요</h2>
					<p>아이디어를 확정한 뒤 ‘내 기록에 저장’을 눌러 주세요.</p>
				</div>
			)}
			<div className="feed-grid">
				{records.map((row) => (
					<article className="card" key={row.idea.id}>
						<span className="step-kicker">
							{new Date(row.savedAt).toLocaleString('ko-KR')}
						</span>
						<h2>{row.idea.title}</h2>
						<p className="feed-content">{row.idea.content}</p>
						<div className="actions">
							<button onClick={() => onOpen(row)}>기록 열기</button>
							<button className="text-button" onClick={() => onRemove(row.idea.id)}>
								기록 삭제
							</button>
						</div>
					</article>
				))}
			</div>
		</>
	);
}
