# Pull request classification

Every pull request selects exactly one fork class:

- `upstreamable`: a standalone generally useful fix suitable for the original
  CoolReader project;
- `temporary-delta`: a compatibility bridge with a concrete removal condition;
- `fork-only`: an intentional downstream product, release or policy choice.

The PR also declares either `no-delta-change` or `delta-updated`. The governance
check compares that declaration with the actual presence of `FORK_DELTA.md` in
the PR diff. Reviewers remain responsible for deciding whether a new permanent
behavior needs a ledger row.

The check runs on `pull_request_target`, checks out the trusted base revision
and only fetches the untrusted PR tree for a path-only diff. It never checks out
or executes code from the PR. Configure branch protection to require
`Validate PR classification` together with the normal build checks.
