import { guideTypes } from '../guides.js';

export default function GuideView({ guide }) {
	const type = guideTypes[guide.kind];
	if (!type) return null;
	return (
		<section className="recipe">
			<div className="section-head">
				<h3>{type.label}</h3>
				<span className="tag">{guide.feasible ? '실행 가능' : '조건 확인 필요'}</span>
			</div>
			<p>{guide.summary}</p>
			{guide.requirements.length > 0 && (
				<>
					<h4>{type.requirementsLabel}</h4>
					<ul>
						{guide.requirements.map((item, index) => (
							<li key={index}>
								{item.name} <span className="muted">{item.detail}</span>
							</li>
						))}
					</ul>
				</>
			)}
			<ol className="recipe-steps">
				{guide.steps.map((step) => (
					<li key={step.order}>
						<strong>{step.title}</strong>
						<p>{step.description}</p>
					</li>
				))}
			</ol>
		</section>
	);
}
