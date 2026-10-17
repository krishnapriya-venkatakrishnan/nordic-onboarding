import { describe, expect, it } from 'vitest';
import config from '../../ngsw-config.json';

/**
 * The service worker caches the app so it starts offline and can be installed. It must never
 * answer for the API: a cached market config could show a market the kill switch turned off.
 */
describe('service worker config', () => {
  it('never caches API answers', () => {
    expect('dataGroups' in config).toBe(false);
    const files = config.assetGroups.flatMap((g) => g.resources.files);
    expect(files.some((f) => f.startsWith('/api') || f.startsWith('/**/api'))).toBe(false);
  });

  it('lets the API and health check through, instead of answering with the app page', () => {
    expect(config.navigationUrls).toContain('!/api/**');
    expect(config.navigationUrls).toContain('!/actuator/**');
  });
});
