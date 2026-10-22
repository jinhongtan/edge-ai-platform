const sections = [
  { title: 'Uploaded models', count: '—', detail: 'Models will appear after upload' },
  { title: 'Hardware targets', count: '—', detail: 'No targets registered yet' },
  { title: 'Compatibility reports', count: '—', detail: 'Reports require a model and target' },
  { title: 'Deployment jobs', count: '—', detail: 'No jobs available' },
]

export default function App() {
  return <main className="shell">
    <header className="topbar"><a className="brand" href="#home"><span className="brand-mark">E</span> Edge AI Platform</a><span className="status"><i /> API scaffold</span></header>
    <section className="hero" id="home">
      <div className="eyebrow">MODEL DEPLOYMENT WORKSPACE</div>
      <h1>From model file<br />to edge ready.</h1>
      <p>Validate ONNX models, inspect their interfaces, and understand target compatibility from one workspace.</p>
      <div className="hero-actions"><button className="primary" disabled title="Upload is planned">Upload a model <span>＋</span></button><span className="muted">ONNX upload is coming soon</span></div>
      <div className="hero-art" aria-hidden="true"><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="chip"><span>AI</span><b>EDGE</b></div><div className="node node-a">ONNX</div><div className="node node-b">NPU</div><div className="node node-c">CPU</div></div>
    </section>
    <section className="section-head"><div><div className="eyebrow">WORKSPACE</div><h2>Deployment overview</h2></div><span className="mock-label">Preview · mock data</span></section>
    <section className="cards" aria-label="Platform sections">{sections.map((item, index) => <article className="card" key={item.title}><div className="card-top"><span className={`icon icon-${index}`}>{['◫', '⌘', '◎', '↗'][index]}</span><span className="card-count">{item.count}</span></div><h3>{item.title}</h3><p>{item.detail}</p><a href="#home">View details <span>→</span></a></article>)}</section>
    <section className="upload-panel" aria-labelledby="upload-title"><div className="upload-symbol">↑</div><div><h2 id="upload-title">Start with a model</h2><p>Upload an ONNX file to validate its structure and inspect input and output metadata.</p></div><button className="secondary" disabled>Choose ONNX file</button></section>
    <footer>Edge AI Platform <span>Hardware compilation and device deployment are not available yet.</span></footer>
  </main>
}
