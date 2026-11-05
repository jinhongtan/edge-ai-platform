# Edge AI Platform — Landing Page Design & Implementation Specification

## 1. Mission

Build a visually exceptional, credible, responsive public landing page for **Edge AI Platform**, a developer-focused SaaS that helps teams analyze ONNX models, build hardware-specific deployment artifacts, validate inference on real edge devices, and benchmark performance. This document is an implementation brief for an AI coding agent working in the existing repository.

**Primary goal:** Convert interested AI developers and engineering teams into users who click **Get Started** and enter the existing application / New Deployment wizard.

**Secondary goal:** Explain the product's differentiated value in less than 30 seconds, without falsely promising support for hardware, hosted execution, or SDKs that are not available.

## 2. Critical Product Truths

- The product is an orchestration layer, **not** a replacement for NVIDIA TensorRT or other vendor SDKs.
- The first verified execution path is **NVIDIA Jetson Orin Nano Super + TensorRT**, using an Edge Agent and real device execution.
- The system has demonstrated ONNX analysis, TensorRT engine creation, inference, and local GPU benchmark. Do not imply every step is already publicly available as a managed SaaS service.
- Build availability depends on the target SDK, hardware, software environment, and connected execution worker.
- Genuine device validation and benchmark require compatible physical hardware (customer-owned or platform-hosted when available).
- Cross-compilation is possible **only for explicitly supported vendor toolchains/targets**. Never claim universal cloud builds.
- Hailo, Qualcomm, OpenVINO, and other targets may be represented as **roadmap / planned**, not currently supported unless verified from repository/configuration.
- Never invent customers, partner logos, testimonials, certification badges, pricing, uptime, security compliance, or numerical performance claims.
- Any example metrics must be labeled **Illustrative** or **Local Jetson experiment**; the measured local result was approximately 2.856 ms average GPU-only inference latency and 350.19 estimated FPS on one ResNet18 binary classifier, excluding transfers/preprocessing. Prefer not to place these numbers in the hero.

## 3. Required Discovery Before Coding

1. Inspect the repository and identify the frontend root, routing system, styling approach, component library, asset conventions, authentication flow, and existing New Deployment route.
2. Identify whether a landing page already exists. Preserve existing authenticated product screens and ONNX analysis behavior.
3. Check package.json, scripts, test setup, linting, TypeScript configuration, and current design tokens.
4. Check available product capabilities and copy; do not advertise unimplemented features as available.
5. Record findings and a concise implementation plan before modifying code.
6. Reuse existing libraries and components; do not introduce a second UI framework or heavyweight animation dependency without a concrete need.

## 4. Visual Direction

**Theme:** Premium developer tooling meets edge hardware. Polished, modern, technical, restrained, trustworthy.

- Default: dark navy/near-black canvas (#080D18 or compatible existing theme), with restrained electric cyan (#46D7E8) and indigo (#6F7DFF) accents. Use light surfaces for contrast in selected sections only if visually cohesive.
- Spacious grid, strong typographic hierarchy, crisp borders, subtle radial glows, soft gradients, small status dots, and refined shadows. Avoid generic stock-photo-heavy SaaS aesthetics.
- Typography: existing font first; otherwise a clean sans-serif with a distinct monospace style for code, metrics, and labels. Do not require externally downloaded font assets.
- Visual centerpiece: a custom **product UI mockup** built from HTML/CSS/SVG (not a static screenshot), showing model selection, Jetson target, Build → Validate → Benchmark stages, and a small performance result. Mark mock data clearly.
- Use purposeful micro-interactions: card hover, subtle line animation in architecture graphic, scroll reveal with reduced-motion support. Avoid distracting continuous motion.
- Maintain WCAG AA text contrast, visible focus states, accessible navigation and controls.
- Mobile-first: all content and primary CTAs must be readable and operable on 360px-wide screens.

## 5. Information Architecture and Sections (in this order)

### 5.1 Sticky Header

Left: simple original brand mark + `Edge AI Platform` (or the actual brand from the repo). Center/right desktop navigation: `How It Works`, `Capabilities`, `Hardware`, `FAQ`. Right: `Sign In` (only if route exists) and primary `Get Started`. On mobile, use an accessible menu with keyboard focus management. Header becomes subtly opaque with a thin border on scroll.

### 5.2 Hero — Immediate Value Proposition

**Eyebrow:** `FROM ONNX TO EDGE HARDWARE`

**H1 (suggested):** `Deploy AI models to the edge, with confidence.`

**Subheading:** `Analyze ONNX models, build for supported hardware, validate inference on real devices, and understand performance—all through one guided workflow.`

Primary CTA: `Get Started` → existing authenticated app / onboarding / New Deployment route, as appropriate. Secondary CTA: `See How It Works` → `#how-it-works`.

Supporting microcopy: `Start with model analysis. Connect a supported device for hardware verification.`

Hero composition: 2-column on desktop (copy left, product workflow visualization right), stacked on mobile. Visual mockup should show a deployment named `Defect Detector`, target `Jetson Orin Nano Super`, status `Hardware benchmark completed` (explicitly labeled demo), and three metrics or stages. No fabricated live backend data.

### 5.3 Trust / Scope Strip

Do **not** use fabricated customer logos. Instead use truthful capability chips:
`ONNX analysis` · `TensorRT build workflow` · `Customer-owned Edge Agent` · `Real-device benchmarks`.

Use clear availability language if features are still in development.

### 5.4 Problem → Solution

Heading: `One model. Many deployment hurdles.`

Three pain points:
- `Fragmented toolchains` — vendor-specific compilers, SDKs, and environment requirements.
- `Uncertain compatibility` — static model validity does not guarantee target-device execution.
- `Hard-to-compare performance` — results depend on real hardware and runtime settings.

Pair with a short solution statement: `A single workflow to analyze, build, verify, and measure—without hiding what was actually tested.`

### 5.5 How It Works (anchor `how-it-works`)

Create a 5-step horizontal timeline on desktop and vertical timeline on mobile:
1. `Upload` — Provide an ONNX model and external weights when needed.
2. `Analyze` — Inspect model inputs, outputs, operators, and compatibility signals.
3. `Choose hardware` — Select a supported target and available execution mode.
4. `Build & verify` — Compile with the vendor SDK; validate on real hardware when available.
5. `Benchmark & download` — Review latency and downloadable artifacts, with provenance.

Important: steps 4–5 must visually indicate that device availability governs verification and benchmarking.

### 5.6 Capability Cards (anchor `capabilities`)

Four polished cards with simple original icons:
- `Model Analysis` — ONNX structure and compatibility signals.
- `Hardware-Specific Builds` — vendor toolchain orchestration, starting with Jetson/TensorRT.
- `Inference Validation` — execute on a connected supported device; capture success and outputs.
- `Real-Device Benchmarking` — latency distributions and environment metadata.

Add a small `Execution transparency` feature: clearly distinguish `Analyzed`, `Built`, `Hardware Verified`, and `Benchmarked`.

### 5.7 Execution Options — Critical Differentiator

Heading: `Use the hardware you have. Know what you can verify.`

Three option cards:
- **Your device** — install Edge Agent on a supported customer-owned device; execute authorized jobs locally; outbound cloud connection.
- **Hosted device** — supported platform-owned hardware *when available*; show `Availability varies` or `Planned` if not launched.
- **No device yet** — analyze ONNX now; compile only where the SDK and target support it; no fabricated hardware benchmarks.

Use a small capability matrix with columns `Analyze`, `Build`, `Hardware validate`, `Benchmark`. Mark cells as `Available`, `Depends on SDK/worker`, or `Requires device`; avoid universal yes/no where not accurate.

### 5.8 Hardware Support (anchor `hardware`)

Feature the `NVIDIA Jetson Orin Nano Super` as the initial validated hardware path. Show `TensorRT`, `CUDA`, and `Edge Agent` as context, not partner endorsements. For future devices use a restrained `Planned` label. Do not show official brand logos without assets/permissions. Include: `Support depends on exact device, runtime, and SDK versions.`

### 5.9 Product Preview / Results

A detailed mock dashboard showing:
- Build job progress: Analyze → Build → Validate → Benchmark → Package.
- Example artifact names: `model.engine`, `manifest.json`, `validation.json`, `benchmark.json`.
- Benchmark cards: mean, P50, P95, estimated FPS, each clearly marked `Demo data` or `Local test example`.
- Device/runtime provenance (e.g., target device and TensorRT version).

The UI mockup is illustrative and should not issue API requests or present invented real-time state.

### 5.10 FAQ (anchor `faq`)

At least five accessible disclosure items:
1. `Do I need to own a Jetson?` — not for analysis; real-device testing needs compatible hardware accessible to the platform.
2. `Can you build for any edge chip?` — no; support is SDK/target-specific and expanding.
3. `Is an engine portable across devices?` — generally not guaranteed; compatibility depends on hardware and runtime.
4. `What does hardware verified mean?` — successful execution on a specified device/software environment, not a guarantee for every device.
5. `How are benchmarks measured?` — identify scope, device, runtime, warmup, iterations, and whether data transfers are included.
6. `Can I connect my own device?` — supported devices can use the Edge Agent once the service is enabled.

### 5.11 Final CTA + Footer

Final CTA headline: `Make your next edge deployment measurable.`

CTA: `Get Started`; secondary: `Explore the Workflow`.

Footer: brand, short description, navigation, and only real legal/contact/docs links found in the repo. No fake social links or contact addresses.

## 6. Routes and User Journey

- Public route: `/` (unless existing routing dictates another public landing path).
- `Get Started`: if unauthenticated, go to the actual sign-in/sign-up route with a safe return destination to New Deployment; if authenticated, navigate directly to the actual New Deployment wizard. Never invent working routes.
- `Sign In`: show only when an auth route is implemented.
- Anchor navigation must scroll correctly below the sticky header (`scroll-margin-top`).
- The landing page must not trigger model uploads, queue jobs, or access device credentials.
- Keep internal app navigation and deep links functioning.

## 7. Suggested React Component Architecture

Adapt to repository conventions; do not force these exact filenames if structure differs:

```text
frontend/src/
  pages/LandingPage.tsx
  components/landing/
    LandingHeader.tsx
    HeroSection.tsx
    WorkflowPreview.tsx
    CapabilityStrip.tsx
    ProblemSolutionSection.tsx
    HowItWorksSection.tsx
    CapabilitiesSection.tsx
    ExecutionOptionsSection.tsx
    HardwareSupportSection.tsx
    ResultsPreviewSection.tsx
    FaqSection.tsx
    FinalCtaSection.tsx
    LandingFooter.tsx
  styles/landing.css  # or existing CSS modules/Tailwind setup
```

Guidelines:
- Keep section copy/data in typed arrays where useful.
- Prefer semantic `<header>`, `<main>`, `<section>`, `<nav>`, `<footer>`.
- Use `button` for actions and `a`/router links for navigation.
- Use native `<details>` for FAQ or an accessible accordion from the existing component library.
- Use responsive CSS Grid/Flexbox; avoid hardcoded fixed widths that overflow mobile.
- No API dependency for the marketing page.
- Avoid third-party icon packages if an icon set already exists.
- No hardcoded fabricated service availability or numerical claims.

## 8. Implementation Phases — Execute Sequentially

### Phase 0 — Repository Audit

**Tasks:** Inspect frontend, routes, auth, current landing, CSS system, assets, scripts. Confirm `Get Started` destination and implemented capabilities. Write a short plan and list assumptions.

**Done when:** Existing routes and design conventions are documented; no production code changed yet.

### Phase 1 — Page Shell and Hero

**Tasks:** Add public landing route without breaking authenticated routes. Build responsive sticky header, hero copy, two CTAs, custom HTML/CSS workflow mockup, initial design tokens. Handle auth-aware CTA destination using existing routing/auth utilities.

**Done when:** Page loads at public route, hero looks excellent at 360px/768px/1440px, navigation works, and there are no runtime errors.

### Phase 2 — Storytelling Sections

**Tasks:** Implement trust/scope strip, problem/solution, five-step workflow, capability cards. Ensure availability language is honest and accessible.

**Done when:** A new visitor can explain the value proposition and workflow without reading technical docs.

### Phase 3 — Hardware and Execution Transparency

**Tasks:** Add customer-owned/hosted/no-device options, capability matrix, hardware support section, status semantics. Use conditional copy based on actually implemented capabilities if configuration is available.

**Done when:** Visitors cannot reasonably mistake analysis for hardware verification or assume universal chip support.

### Phase 4 — Product Preview, FAQ, Footer

**Tasks:** Implement realistic but clearly labeled demo dashboard, FAQ, final CTA, footer, anchor navigation, metadata/SEO tags. Reuse actual brand assets when present.

**Done when:** Every CTA has a working destination; no placeholder `#` links or fake testimonials.

### Phase 5 — Polish, Accessibility, and Performance

**Tasks:** Add subtle animation and hover states; support `prefers-reduced-motion`; keyboard test menus and FAQ; check text contrast; optimize assets and responsive images; avoid layout shifts; add semantic headings and descriptive alt text; verify mobile menu.

**Done when:** No horizontal overflow at 360px, all keyboard interactions work, and animations are optional.

### Phase 6 — Tests and Verification

**Tasks:** Run repository-specific lint, TypeScript check, unit tests, and production build. Add/update tests for landing rendering, navigation, FAQ, mobile menu, and auth-aware CTA if test infrastructure exists. Manually inspect desktop/tablet/mobile. Fix regressions.

**Done when:** All relevant commands pass or any pre-existing failures are clearly identified, with screenshots or concise verification notes if tooling supports them.

## 9. Functional Acceptance Checklist

- [ ] Landing route accessible without authentication.
- [ ] Existing ONNX analysis and New Deployment wizard remain functional.
- [ ] Hero value proposition visible without scrolling on typical desktop screens.
- [ ] `Get Started` follows real auth/wizard routing.
- [ ] `See How It Works` scrolls to correct section.
- [ ] Mobile navigation opens, closes, and is keyboard-accessible.
- [ ] Clear distinction between build, hardware validation, and benchmark.
- [ ] No unsupported claims about hardware, hosted capacity, or cross-compilation.
- [ ] All demo data labeled; no fake customer/partner endorsements.
- [ ] FAQ is keyboard-accessible and answers hardware ownership questions.
- [ ] No broken links, placeholder links, or dead CTA buttons.
- [ ] No horizontal scrolling at 360px, 768px, 1024px, or 1440px.
- [ ] `prefers-reduced-motion` respected.
- [ ] No new critical console errors or type errors.
- [ ] Lint, tests, and production build pass (or known unrelated failures documented).

## 10. SEO and Metadata

- Title: `Edge AI Platform | Build, Validate & Benchmark ONNX Models for Edge Hardware`
- Description: `Analyze ONNX models, build deployment artifacts for supported edge hardware, validate inference on real devices, and benchmark performance in one workflow.`
- One `<h1>` per page, descriptive headings, canonical URL only if real production domain exists.
- Open Graph title/description and image only when a valid local/public asset exists.
- Do not add structured review/rating markup without actual reviews.

## 11. Design QA Details

- Max content width around 1120–1280px, consistent horizontal gutters.
- Hero heading approx. 48–72px desktop, 36–44px mobile (responsive clamp acceptable).
- Body copy typically 16–18px with comfortable line height.
- Section vertical spacing 80–120px desktop, 56–72px mobile.
- Buttons at least 44px high; obvious hover, focus-visible, disabled states.
- Avoid excessive cards: vary section rhythm with timeline, split layouts, and a prominent product preview.
- Use SVG/CSS architecture lines and terminal-like UI snippets, not an unlicensed NVIDIA logo.
- Test contrast in both gradients and overlaid surfaces.

## 12. Agent Execution Instructions

1. Read this entire document and inspect the repository before coding.
2. Implement **one phase at a time**; avoid large unrelated refactors.
3. Preserve existing app functionality and use existing dependencies/design conventions.
4. If the exact route or auth mechanism is unknown, inspect code; do not guess. If blocked, report the missing detail.
5. Do not build backend APIs for this task unless a small route integration is necessary.
6. Use realistic UI demo data, clearly labeled as demo; do not fake live availability or benchmarks.
7. After each phase, summarize files changed, commands run, and any blockers.
8. Before completion, run lint, typecheck, tests, and build, and provide a brief visual QA summary.
9. Do not commit secrets or introduce client-side AWS credentials.
10. Deliver a coherent landing page, not merely a collection of independent sections.

### Suggested First Prompt to Coding Agent

> Read `docs/EDGE_AI_LANDING_PAGE_IMPLEMENTATION_SPEC.md` in full. Inspect the existing frontend, router, authentication, design system, and New Deployment wizard. Complete Phase 0 and Phase 1 only: implement the public landing route, responsive navigation, polished hero, authentic product workflow visualization, and working CTAs. Preserve existing functionality. Run available lint, typecheck, and build commands. Report changes and assumptions before proceeding.

## 13. Out of Scope for This Task

- New device enrollment backend, SQS jobs, S3 storage, pricing/billing, real-time metrics, marketing analytics integrations, CMS, blog, multi-language support, and vendor-specific compilation implementations.
- Real hosted hardware provisioning or claims of universal model compatibility.

**Final product standard:** The page should feel like a serious, premium developer platform: technically credible, visually distinctive, approachable to first-time users, and directly connected to the existing guided deployment workflow.
