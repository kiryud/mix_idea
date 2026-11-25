import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Node 26 exposes an experimental, undefined global localStorage unless it is
// started with --localstorage-file. That shadows jsdom's browser storage during
// Vitest runs, so provide the small Storage surface this demo uses.
if (!globalThis.localStorage) {
	const values = new Map();
	Object.defineProperty(globalThis, 'localStorage', {
		configurable: true,
		value: {
			getItem: (key) => values.get(String(key)) ?? null,
			setItem: (key, value) => values.set(String(key), String(value)),
			removeItem: (key) => values.delete(String(key)),
			clear: () => values.clear(),
		},
	});
}

afterEach(() => {
	cleanup();
	sessionStorage.clear();
	localStorage.clear();
});
