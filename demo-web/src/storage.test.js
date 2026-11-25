import { describe, it, expect } from 'vitest';
import { loadRecords, saveRecord, removeRecord } from './storage.js';

describe('local idea records', () => {
	const idea = {
		id: 'idea-1',
		title: '감자 캠핑',
		content: '서버가 없어도 읽을 설명',
		words: [{ id: 'w1', text: '감자' }],
	};
	it('stores a complete offline snapshot including the recipe', () => {
		saveRecord(idea, { summary: '만드는 법', steps: [] });
		expect(loadRecords()[0]).toMatchObject({ idea, recipe: { summary: '만드는 법' } });
	});
	it('updates an existing snapshot without duplicate IDs', () => {
		saveRecord(idea, null);
		saveRecord({ ...idea, title: '수정' }, null);
		expect(loadRecords()).toHaveLength(1);
		expect(loadRecords()[0].idea.title).toBe('수정');
	});
	it('keeps at most fifty recent records', () => {
		for (let i = 0; i < 55; i++) saveRecord({ ...idea, id: `idea-${i}` }, null);
		expect(loadRecords()).toHaveLength(50);
		expect(loadRecords()[0].idea.id).toBe('idea-54');
	});
	it('recovers from malformed storage', () => {
		localStorage.setItem('idea-demo-records', '{bad');
		expect(loadRecords()).toEqual([]);
	});
	it('removes only the selected record', () => {
		saveRecord(idea, null);
		saveRecord({ ...idea, id: 'other' }, null);
		expect(removeRecord(idea.id).map((row) => row.idea.id)).toEqual(['other']);
	});
});
