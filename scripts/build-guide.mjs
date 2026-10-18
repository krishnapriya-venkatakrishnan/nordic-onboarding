#!/usr/bin/env node
// Prints docs/guide/guide.html to docs/nordic-onboarding-guide.pdf with headless Chrome, after
// its diagrams and fonts have loaded. Needs Google Chrome and an internet connection (the fonts and
// Mermaid come from a CDN). Usage: node scripts/build-guide.mjs [in.html out.pdf]
import { spawn } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const root = resolve(import.meta.dirname, '..');
const input = resolve(process.argv[2] ?? join(root, 'docs/guide/guide.html'));
const output = resolve(process.argv[3] ?? join(root, 'docs/nordic-onboarding-guide.pdf'));
const chromePath = process.env.CHROME ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const port = 9340 + Math.floor(Math.random() * 50);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const chrome = spawn(
  chromePath,
  ['--headless=new', `--remote-debugging-port=${port}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'guide-'))}`, 'about:blank'],
  { stdio: 'ignore' },
);
try {
  let targets;
  for (let i = 0; i < 50 && !targets; i++) {
    try {
      targets = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
    } catch {
      await sleep(200);
    }
  }
  if (!targets) throw new Error(`Chrome didn't start (${chromePath}); set CHROME to its path`);
  const ws = new WebSocket(targets.find((t) => t.type === 'page').webSocketDebuggerUrl);
  await new Promise((r) => ws.addEventListener('open', r, { once: true }));
  let id = 0;
  const pending = new Map();
  ws.addEventListener('message', (e) => {
    const m = JSON.parse(e.data);
    pending.get(m.id)?.(m);
    pending.delete(m.id);
  });
  const send = (method, params = {}) =>
    new Promise((r) => {
      pending.set(++id, r);
      ws.send(JSON.stringify({ id, method, params }));
    });

  await send('Page.enable');
  await send('Page.navigate', { url: pathToFileURL(input).href });
  const evaluate = async (expression) =>
    (await send('Runtime.evaluate', { expression, returnByValue: true })).result?.result?.value;
  // The page sets window.guideReady once every diagram is drawn and the fonts are in.
  let state;
  for (let i = 0; i < 100 && !state; i++) {
    await sleep(200);
    state = await evaluate('window.guideError ? "error: " + window.guideError : window.guideReady ? "ready" : ""');
  }
  if (state !== 'ready') throw new Error(`The guide didn't render: ${state || 'timed out'}`);

  // Each page is a fixed A4 sheet, so content that doesn't fit would be cut off silently.
  const problems = await evaluate(`(() => {
    const out = [];
    if (![...document.fonts].some((f) => f.family === 'Inter' && f.status === 'loaded')) out.push('Inter did not load');
    document.querySelectorAll('a[href^="#"]').forEach((a) => {
      if (!document.getElementById(a.getAttribute('href').slice(1))) out.push('broken link: ' + a.getAttribute('href'));
    });
    document.querySelectorAll('img').forEach((img) => {
      if (!img.naturalWidth) out.push('missing image: ' + img.getAttribute('src'));
    });
    document.querySelectorAll('.sheet').forEach((sheet, i) => {
      const limit = sheet.getBoundingClientRect().bottom - parseFloat(getComputedStyle(sheet).paddingBottom);
      const bottom = Math.max(...[...sheet.children].filter((c) => !c.classList.contains('folio'))
        .map((c) => c.getBoundingClientRect().bottom + parseFloat(getComputedStyle(c).marginBottom)));
      if (bottom > limit + 1) out.push('page ' + (i + 1) + ' is ' + Math.ceil(bottom - limit) + 'px too long');
    });
    return out;
  })()`);
  if (problems.length) {
    const report = `Fix the guide before printing:\n  ${problems.join('\n  ')}`;
    // DRAFT=1 prints anyway, to look at the pages while fixing them.
    if (!process.env.DRAFT) throw new Error(report);
    console.warn(report);
  }

  // The outline gives PDF viewers a sidebar of chapters; tagging helps screen readers.
  const pdf = await send('Page.printToPDF', {
    printBackground: true,
    preferCSSPageSize: true,
    generateDocumentOutline: true,
    generateTaggedPDF: true,
  });
  if (!pdf.result?.data) throw new Error(`Printing failed: ${JSON.stringify(pdf.error ?? pdf)}`);
  writeFileSync(output, Buffer.from(pdf.result.data, 'base64'));
  console.log(`wrote ${output}`);
  ws.close();
} finally {
  chrome.kill();
}
