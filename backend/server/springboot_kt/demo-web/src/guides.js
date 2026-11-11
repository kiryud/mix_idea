export const guideTypes = {
	food: {
		label: '만드는 법',
		requirementsLabel: '재료',
		description: '재료와 구체적인 조리 순서를 확인합니다.',
	},
	software: {
		label: '구현 가이드',
		requirementsLabel: '개발 준비',
		description: '핵심 기능, 구현 순서와 테스트 방법을 확인합니다.',
	},
	contest: {
		label: '공모전 기획안',
		requirementsLabel: '기획 준비',
		description: '문제 정의, 차별점과 검증 계획으로 아이디어를 발전시킵니다.',
	},
	general: {
		label: '실행 계획',
		requirementsLabel: '필요한 것',
		description: '작은 첫 실험, 실행 순서와 성공 기준을 정합니다.',
	},
};
export function savedGuide(row) {
	if (row?.guide && guideTypes[row.guide.kind]) return row.guide;
	const recipe = row?.recipe;
	if (!recipe || !guideTypes[recipe.kind]) return null;
	const type = guideTypes[recipe.kind];
	return {
		...recipe,
		...type,
		requirements:
			recipe.requirements ||
			(recipe.ingredients || []).map((item) => ({ name: item.name, detail: item.amount })),
	};
}
