import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import './compatibility.css'
import './auth.css'
import './dashboard.css'
import './dashboard-clean.css'
import './dashboard-nav.css'
import './dashboard-nav-v2.css'
import './dashboard-nav-v3.css'
import './dashboard-layout-fix.css'
import { checkCompatibility, createBuild, getBuild, listHardware, listModels, login, register, uploadModel, type AuthSession, type BuildJob, type CompatibilityResult, type HardwareProfile, type ModelSummary } from './api'

function formatTensors(values: NonNullable<ModelSummary['inspection']>['inputs']): string {
  if (values.length === 0) {
    return 'none'
  }
  return values.map((tensor) => {
    const shape = tensor.shape.map((dimension) => dimension ?? '?').join(' × ')
    return `${tensor.name}: ${tensor.elementType} [${shape}]`
  }).join(', ')
}

function CompatibilityDetails({ result, profile, inspection }: { result: CompatibilityResult; profile: HardwareProfile; inspection: NonNullable<ModelSummary['inspection']> }) {
  const operators = inspection.operators ?? []
  const supportedOperators = operators.filter((operator) => profile.supportedOperators.includes(operator))
  const dynamicInputs = inspection.inputs.filter((input) => input.shape.some((dimension) => dimension === null || typeof dimension === 'string'))
  const opsets = inspection.opsets.filter((opset) => !opset.domain).map((opset) => opset.version).join(', ') || 'none'
  const unsupportedOperators = result.unsupportedOperators.filter((item) => item.reason === 'Unsupported operator')
  const precisionIssues = result.unsupportedOperators.filter((item) => item.reason === 'Unsupported input or output precision')
  const opsetIssues = result.unsupportedOperators.filter((item) => item.reason === 'Outside configured supported opset range')
  const check = (state: 'pass' | 'warning' | 'fail', title: string, detail: string) => <li className={`check-${state}`}><b>{state === 'pass' ? 'PASS' : state === 'warning' ? 'WARN' : 'FAIL'}</b><span><strong>{title}</strong>{detail}</span></li>
  return <section className="compatibility-result" aria-label={`Compatibility result for ${profile.name}`}>
    <h3>Compatibility result: {result.status}</h3>
    <h4>Compatibility checklist</h4>
    <ul className="compatibility-checklist">
      {check(unsupportedOperators.length ? 'fail' : 'pass', 'Operators', unsupportedOperators.length ? `${unsupportedOperators.length} unsupported operator type(s).` : `${supportedOperators.length}/${operators.length} operator types match the profile.`)}
      {check(precisionIssues.length ? 'fail' : 'pass', 'Input/output precision', precisionIssues.length ? precisionIssues.map((item) => item.operator).join(', ') : `${result.supportedPrecisions.join(', ')} supported by the target.`)}
      {check(dynamicInputs.length ? 'warning' : 'pass', 'Input shapes', dynamicInputs.length ? `Dynamic shape: ${dynamicInputs.map((input) => input.name).join(', ')}.` : 'All analyzed input dimensions are static.')}
      {check(opsetIssues.length ? 'fail' : 'pass', 'ONNX opset', `${opsets}${profile.minimumOpset !== null && profile.maximumOpset !== null ? `; profile range ${profile.minimumOpset}-${profile.maximumOpset}.` : ''}`)}
    </ul>
    <span>Target: {profile.vendor} {profile.name} · {profile.architecture} · {profile.runtime}</span>
    <span>Operators: {supportedOperators.length}/{operators.length} in the initial target profile</span>
    <span>Precision: {result.supportedPrecisions.join(', ')} supported by target</span>
    <span>Input shapes: {dynamicInputs.length ? `dynamic (${dynamicInputs.map((input) => input.name).join(', ')})` : 'static'}</span>
    <span>ONNX default-domain opset: {opsets}{profile.minimumOpset !== null && profile.maximumOpset !== null ? ` (profile range ${profile.minimumOpset}–${profile.maximumOpset})` : ''}</span>
    {result.unsupportedOperators.length > 0 && <div><strong>Blocking findings</strong><ul>{result.unsupportedOperators.map((item) => <li key={`${item.operator}-${item.reason}`}>{item.operator} × {item.count}: {item.reason}</li>)}</ul></div>}
    {result.warnings.length > 0 && <div><strong>Warnings</strong><ul>{result.warnings.map((warning) => <li key={warning}>{warning}</li>)}</ul></div>}
    <small>Static analysis only. This does not build a TensorRT engine or validate execution on Jetson hardware.</small>
  </section>
}

void CompatibilityDetails

function CompatibilityCard({ result, profile, inspection }: { result: CompatibilityResult; profile: HardwareProfile; inspection: NonNullable<ModelSummary['inspection']> }) {
  const unsupported = new Map(result.unsupportedOperators.map((item) => [item.operator, item]))
  const operators = inspection.operators ?? []
  const statusMark = result.status === 'COMPATIBLE' ? '✓' : result.status === 'WARNING' ? '!' : '×'
  const precisionRows = ['FP32', 'FP16', 'INT8']
  const opsets = inspection.opsets.filter((opset) => !opset.domain).map((opset) => opset.version).join(', ') || 'none'
  const dynamicInputs = inspection.inputs.filter((input) => input.shape.some((dimension) => dimension === null || typeof dimension === 'string'))
  return <section className={`compatibility-card status-${result.status.toLowerCase()}`} aria-label={`Compatibility result for ${profile.name}`}>
    <h3>Compatibility Result</h3>
    <div className="compatibility-status"><b>{statusMark}</b><strong>{result.status}</strong></div>
    <div className="compatibility-group"><h4>Target</h4><span>{profile.name}</span><small>{profile.architecture} · {profile.runtime}</small></div>
    <div className="compatibility-group"><h4>ONNX Opset</h4><span>{opsets}</span><small>{profile.minimumOpset !== null && profile.maximumOpset !== null ? `Target profile range: ${profile.minimumOpset}-${profile.maximumOpset}` : 'No target opset range configured'}</small></div>
    <div className="compatibility-group"><h4>Input shapes</h4>{inspection.inputs.map((input) => <span key={input.name}>{input.name}: [{input.shape.map((dimension) => dimension ?? '?').join(' × ')}]</span>)}<small>{dynamicInputs.length ? `Dynamic input shape: ${dynamicInputs.map((input) => input.name).join(', ')}` : 'All analyzed input dimensions are static.'}</small></div>
    <details className="compatibility-group compatibility-operators"><summary>Operators ({operators.length})</summary>{operators.length === 0 ? <span>No analyzed operators</span> : operators.map((operator) => { const finding = unsupported.get(operator); return <div className="compatibility-row" key={operator}><span>{operator}{finding ? ` (${finding.count})` : ''}</span><b className={finding ? 'fail' : 'pass'}>{finding ? '×' : '✓'}</b></div> })}</details>
    <div className="compatibility-group"><h4>Precision</h4>{precisionRows.map((precision) => <div className="compatibility-row" key={precision}><span>{precision}</span><b className={result.supportedPrecisions.includes(precision) ? 'pass' : 'fail'}>{result.supportedPrecisions.includes(precision) ? '✓' : '×'}</b></div>)}</div>
    {result.warnings.length > 0 && <div className="compatibility-group"><h4>Warnings</h4>{result.warnings.map((warning) => <small key={warning}>{warning}</small>)}</div>}
    <small className="compatibility-note">Static compatibility analysis only; no TensorRT build or Jetson execution was performed.</small>
  </section>
}

function LandingPage({ onOpenWorkspace, onUpload, onSignIn, onRegister }: { onOpenWorkspace: () => void; onUpload: () => void; onSignIn: () => void; onRegister: () => void }) {
  return <main className="landing-shell">
    <nav className="landing-nav" aria-label="Main navigation"><a className="brand" href="#top"><span className="brand-mark">E</span> Edge AI Platform</a><div className="nav-links"><a href="#workflow">How it works</a><a href="#trust">Trust</a><button type="button" className="nav-sign-in" onClick={onSignIn}>Sign in</button><button type="button" className="nav-workspace" onClick={onRegister}>Create account <span>→</span></button></div></nav>
    <section className="landing-hero" id="top"><div className="landing-copy"><p className="eyebrow">THE PRACTICAL PATH TO THE EDGE</p><h1>Know what your model needs <em>before</em> it meets a device.</h1><p className="landing-lede">Inspect ONNX models, assess target compatibility, and prepare evidence for real edge hardware—without overstating what has been verified.</p><div className="landing-actions"><button type="button" className="landing-primary" onClick={onUpload}>Upload an ONNX model <span>↑</span></button><button type="button" className="landing-text-button" onClick={onOpenWorkspace}>Explore workspace <span>→</span></button></div><p className="landing-note">Static analysis is available now. Hardware execution is reported only after real device validation.</p></div><div className="signal-field" aria-hidden="true"><div className="signal-core"><b>ONNX</b><span>MODEL</span></div><div className="signal-ring ring-one"/><div className="signal-ring ring-two"/><div className="signal-card card-input">INPUT<br/><b>1 × 3 × 224 × 224</b></div><div className="signal-card card-target">TARGET<br/><b>JETSON</b></div><div className="signal-card card-evidence">EVIDENCE<br/><b>CHECKED</b></div></div></section>
    <section className="proof-strip" aria-label="Current platform capabilities"><span>ONNX inspection</span><i/><span>Compatibility assessment</span><i/><span>Evidence-aware results</span></section>
    <section className="workflow" id="workflow"><div><p className="eyebrow">A CLEARER DEPLOYMENT HANDOFF</p><h2>Move from file to informed decision.</h2></div><ol><li><span>01</span><div><h3>Inspect</h3><p>Parse model structure and interfaces without executing uploaded code.</p></div></li><li><span>02</span><div><h3>Assess</h3><p>Compare metadata against an explicit target capability profile.</p></div></li><li><span>03</span><div><h3>Build evidence</h3><p>Keep build, validation, and benchmark claims separate as the platform grows.</p></div></li></ol></section>
    <section className="trust-panel" id="trust"><p className="eyebrow">BUILT FOR HONEST SIGNALS</p><h2>A compatible report is not a hardware promise.</h2><p>Compatibility is static analysis. A successful engine build, runtime validation, and benchmark each require their own evidence and are displayed distinctly.</p><button type="button" onClick={onOpenWorkspace}>See the workspace <span>→</span></button></section>
    <footer className="landing-footer"><span>Edge AI Platform</span><span>Model inspection and compatibility workflow</span></footer>
  </main>
}

function AuthPage({ mode, onBack, onSwitch, onAuthenticated }: { mode: 'login' | 'register'; onBack: () => void; onSwitch: () => void; onAuthenticated: (session: AuthSession) => void }) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const isRegister = mode === 'register'
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(null)
    const fields = new FormData(event.currentTarget)
    try { onAuthenticated(isRegister ? await register(String(fields.get('email')), String(fields.get('organization')), String(fields.get('password'))) : await login(String(fields.get('email')), String(fields.get('password')))) }
    catch (cause) { setError(cause && typeof cause === 'object' && 'message' in cause ? String(cause.message) : 'Authentication failed.') }
    finally { setBusy(false) }
  }
  return <main className="auth-shell"><button type="button" className="brand brand-button" onClick={onBack}><span className="brand-mark">E</span> Edge AI Platform</button><section className="auth-card"><p className="eyebrow">{isRegister ? 'CREATE YOUR WORKSPACE' : 'WELCOME BACK'}</p><h1>{isRegister ? 'Start with a secure workspace.' : 'Sign in to your workspace.'}</h1><p>{isRegister ? 'Create an account to keep models, devices, and evidence scoped to your organization.' : 'Use your organization account to continue.'}</p><form onSubmit={(event) => void submit(event)}><label>Email<input name="email" type="email" autoComplete="email" required placeholder="you@company.com" /></label>{isRegister && <label>Organization<input name="organization" autoComplete="organization" required placeholder="Your organization" /></label>}<label>Password<input name="password" type="password" autoComplete={isRegister ? 'new-password' : 'current-password'} required minLength={12} placeholder="At least 12 characters" /></label>{isRegister && <label className="checkbox-label"><input type="checkbox" required /> I understand model workloads execute only on approved edge devices.</label>}<button className="auth-submit" type="submit" disabled={busy}>{busy ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'} <span>→</span></button></form>{error && <p className="auth-notice" role="alert">{error}</p>}<p className="auth-switch">{isRegister ? 'Already have an account?' : 'New to Edge AI Platform?'} <button type="button" onClick={onSwitch}>{isRegister ? 'Sign in' : 'Create an account'}</button></p></section></main>
}

export default function App() {
  const fileInputId = useId()
  const fileInput = useRef<HTMLInputElement>(null)
  const [models, setModels] = useState<ModelSummary[]>([])
  const [status, setStatus] = useState<string>('Loading models from local storage…')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [hardware, setHardware] = useState<HardwareProfile[]>([])
  const [results, setResults] = useState<Record<string, CompatibilityResult>>({})
  const [builds, setBuilds] = useState<Record<string, BuildJob>>({})
  const [workspaceMiB, setWorkspaceMiB] = useState(1024)
  const [view, setView] = useState<'landing' | 'workspace' | 'login' | 'register'>('landing')
  const [session, setSession] = useState<AuthSession | null>(null)
  const [sidebarExpanded, setSidebarExpanded] = useState(true)
  const [dashboardView, setDashboardView] = useState<'overview' | 'account'>('overview')

  async function refreshModels() {
    const data = await listModels()
    setModels(data)
    setStatus(data.length === 0 ? 'No models stored locally yet.' : `${data.length} model${data.length === 1 ? '' : 's'} stored locally.`)
  }

  useEffect(() => {
    refreshModels().catch(() => {
      setStatus('API unavailable. Start the backend to store uploads locally.')
    })
  }, [])
  useEffect(() => { listHardware().then(setHardware).catch(() => setHardware([])) }, [])

  async function runCompatibility(modelId: string) { if (!hardware[0]) return; setBusy(true); try { const result = await checkCompatibility(modelId, hardware[0].id); setResults((current) => ({ ...current, [modelId]: result })) } catch (cause) { setError(cause && typeof cause === 'object' && 'message' in cause ? String(cause.message) : 'Compatibility check failed.') } finally { setBusy(false) } }
  async function requestBuild(modelId: string) { if (!hardware[0]) return; setBusy(true); setError(null); try { const build = await createBuild(modelId, hardware[0].id, workspaceMiB); setBuilds((current) => ({ ...current, [modelId]: build })) } catch (cause) { setError(cause && typeof cause === 'object' && 'message' in cause ? String(cause.message) : 'Build request failed.') } finally { setBusy(false) } }

  useEffect(() => {
    const active = Object.entries(builds).filter(([, build]) => !['COMPLETED', 'FAILED', 'CANCELLED'].includes(build.status))
    if (active.length === 0) return
    const timer = window.setTimeout(() => {
      void Promise.all(active.map(async ([modelId, build]) => [modelId, await getBuild(build.id)] as const)).then((updates) => setBuilds((current) => ({ ...current, ...Object.fromEntries(updates) }))).catch(() => setError('Could not refresh build status. Retrying…'))
    }, 3000)
    return () => window.clearTimeout(timer)
  }, [builds])

  async function onFileSelected(file: File | undefined) {
    if (!file || busy) {
      return
    }
    setBusy(true)
    setError(null)
    setStatus(`Uploading ${file.name}…`)
    try {
      await uploadModel(file)
      await refreshModels()
      setStatus(`Stored ${file.name} locally.`)
    } catch (cause) {
      const message = cause && typeof cause === 'object' && 'message' in cause ? String(cause.message) : 'Upload failed.'
      setError(message)
      setStatus('Upload did not complete.')
    } finally {
      setBusy(false)
      if (fileInput.current) {
        fileInput.current.value = ''
      }
    }
  }

  if (view === 'landing') return <LandingPage onOpenWorkspace={() => setView('workspace')} onUpload={() => { setView('workspace'); window.setTimeout(() => fileInput.current?.click(), 0) }} onSignIn={() => setView('login')} onRegister={() => setView('register')} />
  if (view === 'login' || view === 'register') return <AuthPage mode={view} onBack={() => setView('landing')} onSwitch={() => setView(view === 'login' ? 'register' : 'login')} onAuthenticated={(nextSession) => { setSession(nextSession); setView('workspace') }} />

  return <main className={`shell dashboard-shell ${sidebarExpanded ? 'sidebar-expanded' : 'sidebar-collapsed'} ${dashboardView === 'account' ? 'account-open' : ''}`}>
    <aside className="dashboard-side-nav" aria-label="Workspace navigation"><button type="button" className="nav-toggle" aria-label={sidebarExpanded ? 'Collapse navigation' : 'Expand navigation'} onClick={() => setSidebarExpanded((current) => !current)}>☰</button><button type="button" className={dashboardView === 'overview' ? 'active' : ''} onClick={() => setDashboardView('overview')}><b>▦</b><span>Overview</span></button><a href="#models"><b>◫</b><span>Models</span></a><span className="nav-disabled"><b>⌘</b><span>Targets</span></span><span className="nav-disabled"><b>◎</b><span>Checks</span></span><span className="nav-disabled"><b>↗</b><span>Builds</span></span><button type="button" className={dashboardView === 'account' ? 'active nav-account' : 'nav-account'} onClick={() => setDashboardView('account')}><b>◉</b><span>Account</span></button></aside>
    {dashboardView === 'account' && <section className="account-panel" aria-labelledby="account-title"><p className="eyebrow">ACCOUNT</p><h1 id="account-title">Your account</h1><dl><div><dt>Email</dt><dd>{session?.user.email ?? 'Not signed in'}</dd></div><div><dt>Organization</dt><dd>{session?.user.organization ?? 'Local workspace'}</dd></div></dl><p className="account-note">Profile editing and password changes need a dedicated account-update API and are not available yet.</p><button type="button" className="account-signout" onClick={() => { setSession(null); setView('landing') }}>Sign out</button></section>}
    <header className="topbar"><button type="button" className="brand brand-button" onClick={() => setView('landing')}><span className="brand-mark">E</span> Edge AI Platform</button><div className="workspace-nav"><span className="status"><i /> {session ? session.user.email : 'Local uploads'}</span><button type="button" className="back-link" onClick={() => setView('landing')}>← Home</button></div></header>
    <section className="dashboard-intro" aria-labelledby="dashboard-title"><div><p className="eyebrow">YOUR WORKSPACE</p><h1 id="dashboard-title">{session ? `Welcome back, ${session.user.organization}.` : 'Your deployment workspace.'}</h1><p>Manage model evidence, target checks, and build requests from one place. Status labels distinguish static assessment, successful builds, and real hardware proof.</p></div><div className="dashboard-actions"><label className="primary" htmlFor={fileInputId}>Upload ONNX <span>+</span></label><a href="#models">Review models →</a></div></section>
    <section className="dashboard-summary" aria-label="Workspace summary"><article><span>MODELS</span><strong>{models.length}</strong><p>Uploaded ONNX files</p></article><article><span>TARGETS</span><strong>{hardware.length}</strong><p>Configured capability profiles</p></article><article><span>CHECKS</span><strong>{Object.keys(results).length}</strong><p>Static compatibility assessments</p></article><article><span>BUILDS</span><strong>{Object.keys(builds).length}</strong><p>Tracked build requests</p></article></section>
    <section className="dashboard-next"><div><p className="eyebrow">NEXT BEST STEP</p><h2>{models.length ? 'Review a model before building.' : 'Add your first ONNX model.'}</h2><p>{models.length ? 'Run a target compatibility check to uncover static limitations before submitting an FP32 build request.' : 'Start with an ONNX file to inspect metadata, operators, inputs, and outputs safely.'}</p></div><a href={models.length ? '#models' : '#upload'}>{models.length ? 'Review models →' : 'Upload a model →'}</a></section>
    <section className="hero" id="home">
      <div className="eyebrow">MODEL DEPLOYMENT WORKSPACE</div>
      <h1>From model file<br />to edge ready.</h1>
      <p>Validate ONNX models, inspect their interfaces, and understand target compatibility from one workspace.</p>
      <div className="hero-actions">
        <input id={fileInputId} ref={fileInput} className="file-input" type="file" accept=".onnx,application/octet-stream" disabled={busy} onChange={(event) => void onFileSelected(event.target.files?.[0])} />
        <label className="primary" htmlFor={fileInputId}>Upload a model <span>＋</span></label>
        <span className="muted">{busy ? 'Uploading…' : 'ONNX files are stored on this machine'}</span>
      </div>
      <div className="hero-art" aria-hidden="true"><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="chip"><span>AI</span><b>EDGE</b></div><div className="node node-a">ONNX</div><div className="node node-b">NPU</div><div className="node node-c">CPU</div></div>
    </section>
    <section className="section-head"><div><div className="eyebrow">WORKSPACE</div><h2>Deployment overview</h2></div><span className="mock-label">Local storage · compatibility still planned</span></section>
    <section className="cards" aria-label="Platform sections">
      <article className="card">
        <div className="card-top"><span className="icon icon-0">◫</span><span className="card-count">{models.length}</span></div>
        <h3>Uploaded models</h3>
        <p>{models.length === 0 ? 'Models will appear after upload' : 'Stored under local uploads/models'}</p>
        <a href="#models">View details <span>→</span></a>
      </article>
      <article className="card"><div className="card-top"><span className="icon icon-1">⌘</span><span className="card-count">—</span></div><h3>Hardware targets</h3><p>No targets registered yet</p><a href="#home">View details <span>→</span></a></article>
      <article className="card"><div className="card-top"><span className="icon icon-2">◎</span><span className="card-count">—</span></div><h3>Compatibility reports</h3><p>Reports require a model and target</p><a href="#home">View details <span>→</span></a></article>
      <article className="card"><div className="card-top"><span className="icon icon-3">↗</span><span className="card-count">—</span></div><h3>Deployment jobs</h3><p>No jobs available</p><a href="#home">View details <span>→</span></a></article>
    </section>
    <section className="upload-panel" id="upload" aria-labelledby="upload-title">
      <div className="upload-symbol">↑</div>
      <div>
        <h2 id="upload-title">Start with a model</h2>
        <p>Upload an ONNX file to validate its structure and inspect input and output metadata.</p>
      </div>
      <label className="secondary" htmlFor={fileInputId}>Choose ONNX file</label>
    </section>
    <p className={error ? 'upload-alert' : 'upload-status'} role={error ? 'alert' : 'status'}>{error ?? status}</p>
    <section className="model-list" id="models" aria-labelledby="model-list-title">
      <h2 id="model-list-title">Local models</h2>
      {models.length === 0 ? <p className="muted">No ONNX files have been stored yet.</p> : (
        <ul>
          {models.map((model) => (
            <li key={model.id}>
              <strong>{model.fileName}</strong>
              <span>Validation: {model.validationStatus}{model.validationCode ? ` (${model.validationCode})` : ''}</span>
              {model.inspection && <>
              <span>{model.inspection.fileSizeBytes} bytes · IR {model.inspection.irVersion}</span>
              <span>Inputs: {formatTensors(model.inspection.inputs)}</span>
              <span>Outputs: {formatTensors(model.inspection.outputs)}</span>
              <span>Operators: {model.inspection.operators?.join(', ') || 'none'}</span>
              <span>Operator counts: {Object.entries(model.inspection.operatorCounts ?? {}).map(([name, count]) => `${name}: ${count}`).join(', ') || 'none'}</span>
              <span>Parameters: {model.inspection.parameterCount?.toLocaleString() ?? '0'}</span>
              </>}
              {!model.inspection && <span>{model.validationMessage ?? 'Inspection did not return metadata.'}</span>}
              {model.inspection && hardware[0] && <><button type="button" disabled={busy} onClick={() => void runCompatibility(model.id)}>{busy ? 'Checking compatibility…' : `Check compatibility: ${hardware[0].name}`}</button>{results[model.id] && <><CompatibilityCard result={results[model.id]} profile={hardware[0]} inspection={model.inspection} />{results[model.id].status !== 'INCOMPATIBLE' && <div className="build-panel"><h4>Build for {hardware[0].name}</h4>{results[model.id].status === 'WARNING' && <p className="build-warning">Compatibility has warnings. The build can be attempted, but it is not guaranteed.</p>}<label>Workspace (MiB)<input aria-label={`Workspace for ${model.fileName}`} type="number" min="1" max="1024" value={workspaceMiB} disabled={busy} onChange={(event) => setWorkspaceMiB(Math.max(1, Math.min(1024, Number(event.target.value) || 1)))} /></label><small>FP32 only. A successful engine build does not verify runtime inference.</small><button type="button" disabled={busy} onClick={() => void requestBuild(model.id)}>{busy ? 'Submitting build…' : 'Queue FP32 build'}</button>{builds[model.id] && <p aria-live="polite"><strong>Build {builds[model.id].status}</strong> · {builds[model.id].stage}<br />{builds[model.id].message}<br /><small>Polling for worker updates.</small></p>}</div>}</>}</>}
            </li>
          ))}
        </ul>
      )}
    </section>
    <footer>Edge AI Platform <span>Hardware compilation and device deployment are not available yet.</span></footer>
  </main>
}
