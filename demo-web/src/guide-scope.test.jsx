import { expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IdeaWizard from './components/IdeaWizard.jsx';

it('sends the chosen follow-up type when creating a custom category', async () => {
	const user = userEvent.setup();
	const input = props('general', '실행 계획');
	input.restore = null;
	input.categories = [];
	input.api.request.mockResolvedValue({ category: { id: 'custom' } });
	render(<IdeaWizard {...input} />);
	await user.click(screen.getByText('＋ 나만의 카테고리 만들기'));
	await user.type(screen.getByLabelText('새 카테고리'), '건강 관리');
	await user.selectOptions(screen.getByLabelText('후속 가이드 유형'), 'software');
	await user.click(screen.getByRole('button', { name: '추가하고 선택' }));
	expect(input.api.request).toHaveBeenCalledWith('/app/categories', {
		method: 'POST',
		body: { name: '건강 관리', guideType: 'software' },
	});
});

function props(kind, label) {
	return {
		api: {
			request: vi
				.fn()
				.mockResolvedValue({
					guide: {
						kind,
						label,
						requirementsLabel: '준비',
						feasible: true,
						summary: '이 아이디어의 구체적인 후속 내용',
						requirements: [],
						steps: [
							{
								order: 1,
								title: '첫 단계',
								description: '작은 검증부터 시작합니다.',
							},
						],
					},
					cached: false,
				}),
		},
		categories: [{ id: 'c', name: '선택한 분야', guideType: kind, guideLabel: label }],
		reloadCategories: vi.fn(),
		run: (action) => action(),
		busy: '',
		notify: vi.fn(),
		onSave: vi.fn(),
		restore: {
			idea: {
				id: 'i',
				categoryId: 'c',
				title: '아이디어',
				content: '내용',
				words: [],
				direction: '',
				guideType: kind,
				guideLabel: label,
			},
			recipe: { kind: 'plan', summary: '예전 잘못된 계획', ingredients: [], steps: [] },
		},
	};
}
it.each([
	['food', '만드는 법'],
	['software', '구현 가이드'],
	['contest', '공모전 기획안'],
	['general', '실행 계획'],
])('offers %s guidance instead of hiding the action', async (kind, label) => {
	const user = userEvent.setup();
	const input = props(kind, label);
	render(<IdeaWizard {...input} />);
	await user.click(await screen.findByRole('button', { name: `${label} 보기`, exact: true }));
	expect(input.api.request).toHaveBeenCalledWith('/app/ideas/i/guide', { method: 'POST' });
	expect(await screen.findByText('이 아이디어의 구체적인 후속 내용')).toBeInTheDocument();
	expect(screen.queryByText('예전 잘못된 계획')).not.toBeInTheDocument();
	await user.click(screen.getByRole('button', { name: '내 기록에 저장' }));
	expect(input.onSave.mock.calls[0][1].kind).toBe(kind);
});
