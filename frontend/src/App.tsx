import { useEffect, useRef, useState } from 'react'
import { listModels, uploadModel, type ModelSummary } from './api'

const otherSections = [
  { title: 'Hardware targets', count: '—', detail: 'No targets registered yet' },
  { title: 'Compatibility reports', count: '—', detail: 'Reports require a model and target' },
  { title: 'Deployment jobs', count: '—', detail: 'No jobs available' },
]

function tensorSummary(model: ModelSummary): string {
  const inputs = model.inspection.inputs.length
  const outputs = model.inspection.outputs.length
  return `${inputs} input${inputs === 1 ? '' : 's'}, ${outputs} output${outputs === 1 ? '' : 's'}`
}

export default function App() {
  const fileInput = useRef<HTMLInputElement>(null)
  const [models, setModels] = useState<ModelSummary[]>([])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    listModels()
      .then((items) => {
        if (!cancelled) {
          setModels(items)
        }
      })
      .catch((reason: { message?: string }) => {
        if (!cancelled) {
          setError(reason.message ?? 'Could not load uploaded models.')
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function onFileChosen(file: File | undefined) {
    if (!file || busy) {
      return
    }
    if (!file.name.toLowerCase().endsWith('.onnx')) {
      setError('Only ONNX model files are supported.')
      setMessage(null)
      return
    }
    setBusy(true)
    setError(null)
    setMessage(`Uploading ${file.name}…`)
    try {
      const created = await uploadModel(file)
      setModels((current) => [created, ...current.filter((item) => item.id !== created.id)])
      setMessage(`Validated ${created.filename}.`)
    } catch (reason) {
      const text = reason && typeof reason === 'object' && 'message' in reason && typeof reason.message === 'string'
        ? reason.message
        : 'Upload failed. Start the API and try again.'
      setError(text)
      setMessage(null)
    } finally {
      setBusy(false)
      if (fileInput.current) {
        fileInput.current.value = ''
      }
    }
  }

  function openFilePicker() {
    fileInput.current?.click()
  }

  return <main className="shell">
    <header className="topbar"><a className="brand" href="#home"><span className="brand-mark">E</span> Edge AI Platform</a><span className="status"><i /> API scaffold</span></header>
    <section className="hero" id="home">
      <div className="eyebrow">MODEL DEPLOYMENT WORKSPACE</div>
      <h1>From model file<br />to edge ready.</h1>
      <p>Validate ONNX models, inspect their interfaces, and understand target compatibility from one workspace.</p>
      <div className="hero-actions">
        <input ref={fileInput} id="onnx-file" className="file-input" type="file" accept=".onnx,application/octet-stream" aria-label="ONNX model file" disabled={busy} onChange={(event) => void onFileChosen(event.target.files?.[0])} />
        <button className="primary" type="button" disabled={busy} onClick={openFilePicker}>Upload a model <span>＋</span></button>
        <span className="muted">{busy ? 'Uploading…' : 'ONNX files only, up to 100 MB'}</span>
      </div>
      <div className="hero-art" aria-hidden="true"><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="chip"><span>AI</span><b>EDGE</b></div><div className="node node-a">ONNX</div><div className="node node-b">NPU</div><div className="node node-c">CPU</div></div>
    </section>
    <p className={error ? 'banner error' : message ? 'banner' : 'banner muted'} role="status">{error ?? message ?? 'Choose an ONNX file to validate structure and inspect tensors.'}</p>
    <section className="section-head"><div><div className="eyebrow">WORKSPACE</div><h2>Deployment overview</h2></div><span className="mock-label">Hardware and jobs are not implemented</span></section>
    <section className="cards" aria-label="Platform sections">
      <article className="card">
        <div className="card-top"><span className="icon icon-0">◫</span><span className="card-count">{models.length}</span></div>
        <h3>Uploaded models</h3>
        {models.length === 0
          ? <p>Models will appear after upload</p>
          : <ul className="model-list">{models.map((model) => <li key={model.id}><strong>{model.filename}</strong><span>{tensorSummary(model)}</span></li>)}</ul>}
        <a href="#upload-title">View details <span>→</span></a>
      </article>
      {otherSections.map((item, index) => <article className="card" key={item.title}><div className="card-top"><span className={`icon icon-${index + 1}`}>{['⌘', '◎', '↗'][index]}</span><span className="card-count">{item.count}</span></div><h3>{item.title}</h3><p>{item.detail}</p><a href="#home">View details <span>→</span></a></article>)}
    </section>
    <section className="upload-panel" aria-labelledby="upload-title"><div className="upload-symbol">↑</div><div><h2 id="upload-title">Start with a model</h2><p>Upload an ONNX file to validate its structure and inspect input and output metadata.</p></div><button className="secondary" type="button" disabled={busy} onClick={openFilePicker}>Choose ONNX file</button></section>
    <footer>Edge AI Platform <span>Hardware compilation and device deployment are not available yet.</span></footer>
  </main>
}
