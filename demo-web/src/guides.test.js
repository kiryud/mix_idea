import { expect, it } from 'vitest';
import { savedGuide } from './guides.js';

it('restores typed guides without losing the selected domain', () => {
	const guide = { kind: 'contest', summary: '기획안', requirements: [], steps: [] };
	expect(savedGuide({ guide })).toBe(guide);
});
it('converts saved food recipes to the new guide format', () => {
	const result = savedGuide({
		recipe: { kind: 'food', ingredients: [{ name: '감자', amount: '2개' }], steps: [] },
	});
	expect(result.label).toBe('만드는 법');
	expect(result.requirements).toEqual([{ name: '감자', detail: '2개' }]);
});
it('does not present untyped legacy plans as the new idea-specific guide', () => {
	expect(savedGuide({ recipe: { kind: 'plan' } })).toBeNull();
});
it('empty records have no saved guide', () => expect(savedGuide({})).toBeNull());
