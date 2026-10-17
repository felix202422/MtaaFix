# MtaaFix Design System

Status: Phase 2 foundation (implemented in `web/src`); mobile theming groundwork in `mobile/lib/app/theme.dart`.
Implements SRS §9 (Design & UX) and the visual language described in the master spec.

## 1. Principles

- Clean, accessible, mobile-first responsive design.
- Inter typography; high-contrast, WCAG AA minimum.
- Predominantly neutral surfaces with a distinct Tanzanian green/yellow/blue brand palette.
- Never communicate status with color alone — always pair a badge color with a text label.
- Green used primarily for actions and positive status, not as dominant page chrome.

## 2. Color tokens

Defined in `web/src/styles/tokens.css` (Tailwind v4 `@theme`) and mirrored in `mobile/lib/app/theme.dart`.

| Token | Hex | Usage |
|---|---|---|
| primary-green | #1EB53A | Primary actions, brand, positive status |
| deep-green | #15803D | Hover/emphasis, primary containers |
| flag-yellow | #FCD116 | Highlights, sparing accents |
| primary-black | #111111 | Text, structure |
| flag-blue | #00A3DD | Links, info, maps, focus accents |
| surface | #FFFFFF | Cards, surfaces |
| canvas | #F5F7FA | App background |
| border | #E2E8F0 | Borders, dividers |
| muted-text | #64748B | Supporting/caption text |
| error-red | #DC2626 | Errors, destructive actions |
| warning-amber | #D97706 | Warnings |

## 3. Typography

- Family: Inter (loaded in `web/index.html`; requested via `fontFamily` on mobile ThemeData).
- Scale: Display 4xl/800, H1 3xl/700, H2 2xl/700, H3 xl/600, Body base/400, Small sm/400 muted.

## 4. Spacing, shape, elevation

- Radius: sm 6px (inputs), md 8px (buttons/toasts), lg 12px (cards, mobile theme), xl 16px (modals).
- Elevation: minimal — 1px borders preferred over heavy shadows; modal/toast use subtle shadow.
- Breakpoints (responsive targets): 360 (mobile), 768 (tablet), 1280 (desktop), 1920 (wide).

## 5. Component inventory (web)

Located in `web/src/design-system/`, exported from `index.ts`, demonstrated at the in-repo gallery (`/design`, `web/src/gallery/`).

| Component | Variants / notes |
|---|---|
| Button | primary, secondary, outline, danger; sizes sm/md; disabled state |
| Field / TextAreaField | label, hint, error; `aria-invalid` + `aria-describedby` wiring |
| Card | border surface, optional `padded={false}` for list rows |
| Badge | tones: success, info, warning, error, neutral; always with text label |
| Modal | Escape to close, outside-click to close, focus moves to dialog, `role="dialog"` + `aria-labelledby` |
| Toast | `ToastProvider` + `useToast()`; success/error/info; `aria-live="polite"` |
| States | EmptyState, LoadingState (`role="status"`), ErrorState (`role="alert"`, retry action) |

Layout previews (presentational, design validation only — not product UI): landing, auth, citizen dashboard, report form wizard, at `web/src/gallery/layouts/`.

## 6. Report status mapping (ADR-0002, 12 states)

| State | Badge tone |
|---|---|
| SUBMITTED | info |
| UNDER_REVIEW | info |
| NEEDS_INFORMATION | warning |
| VERIFIED | success |
| ASSIGNED | info |
| IN_PROGRESS | info |
| RESOLUTION_REVIEW | warning |
| RESOLVED | success |
| REJECTED | error |
| DUPLICATE | neutral |
| REOPENED | warning |
| CLOSED | neutral |

## 7. Accessibility

- Enforced at lint time: `eslint-plugin-jsx-a11y` (flat config in `web/eslint.config.js`).
- Focus-visible ring on all interactive elements (global CSS).
- Modal: Escape key, outside-click, focus management, labelled dialog.
- Status/meaning never conveyed by color alone.
- Manual checklist (run before merging UI work; automated browser a11y tooling deferred to Phase 15):
  - [ ] Keyboard-only traversal of all interactive elements, visible focus at each stop.
  - [ ] Every input has a programmatically associated label; errors announced (`aria-describedby`).
  - [ ] Text contrast ≥ 4.5:1 (normal), ≥ 3:1 (large); badges meet contrast with their text.
  - [ ] Tap targets ≥ 44×44 CSS px on mobile widths (360).
  - [ ] Zoom to 200% without loss of content or horizontal scroll at 360/768/1280.
  - [ ] Loading and error states announced (`role="status"` / `role="alert"`).
  - [ ] No interaction requires hover alone.

## 8. Related decisions and follow-ups

- ADR: gallery route `/design` (in-repo, no Storybook); ESLint pinned to v9 for jsx-a11y compatibility; Tailwind v4 CSS-first tokens.
- Phase 6–7: build real product pages from these components against real APIs (no dead controls in product surfaces).
- Phase 11 (done): shared mobile widgets (`LoadingView`, `FeedbackView`, snackbars) and
  screen migration off hardcoded `Colors.orange`; shared status→badge mapping lands with the Phase 12 citizen screens.
- Phase 15: Playwright + axe automated accessibility checks, visual regression.
