import { expect, it } from 'vitest';
import { proxyPaths } from '../proxy-paths.js';

it('proxies API and shared pages while serving React source locally', () => {
	const prefixes = proxyPaths;
	expect(prefixes.some((prefix) => '/s/share-code'.startsWith(prefix))).toBe(true);
	expect(prefixes.some((prefix) => '/app/categories'.startsWith(prefix))).toBe(true);
	expect(prefixes.some((prefix) => '/src/main.jsx'.startsWith(prefix))).toBe(false);
});
