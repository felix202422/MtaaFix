# Contributing to MtaaFix

## How We Work
- Work in phases; Phase 0 must be fully done before Phase 1.
- Use conventional commits.
- Every PR references an issue.
- Keep commits small and reviewable.
- Run the relevant checks locally before pushing.

## Development Flow
1. `git checkout -b phase/<phase>/<feature>`
2. Implement, test, document.
3. Open a PR; CI must pass.
4. Address review feedback.
5. Squash/merge after approval.

## Coding
- Follow the coding standards in `docs/standards/CODING_STANDARDS.md`.
- Do not commit secrets.
- Use DTOs; never expose entities via the API.
