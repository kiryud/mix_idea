const KEY = 'idea-demo-records';
export function loadRecords(storage = localStorage) {
	try {
		const value = JSON.parse(storage.getItem(KEY) || '[]');
		return Array.isArray(value) ? value.filter((row) => row?.idea?.id) : [];
	} catch {
		return [];
	}
}
export function saveRecord(idea, recipe, storage = localStorage) {
	const next = [
		{
			idea,
			guide: recipe,
			recipe: recipe?.kind === 'food' || !recipe?.kind ? recipe : null,
			savedAt: new Date().toISOString(),
		},
		...loadRecords(storage).filter((row) => row.idea.id !== idea.id),
	].slice(0, 50);
	storage.setItem(KEY, JSON.stringify(next));
	return next;
}
export function removeRecord(id, storage = localStorage) {
	const next = loadRecords(storage).filter((row) => row.idea.id !== id);
	storage.setItem(KEY, JSON.stringify(next));
	return next;
}
