## Agent skills

### Installing the skills after cloning

The skill files are committed in `.agents/skills/` and pinned by `skills-lock.json`, but Claude Code reads them from `.claude/skills/`, which is gitignored because it holds per-machine links. After cloning, run this once from the repo root (needs Node.js 22.20+):

```bash
npx skills experimental_install --agent claude-code
```

It keeps the committed skill files that still match the lock's hashes and creates the missing `.claude/skills/` links. The command is experimental in the `skills` CLI, so check `npx skills --help` if it has been renamed.

### Issue tracker

Issues are tracked in GitHub Issues on `junaid55555/my_worker_lodger` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
