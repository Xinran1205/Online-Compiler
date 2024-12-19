import React, { useState } from 'react';
import CodeEditor from './components/CodeEditor.jsx';
import OutputDisplay from './components/OutputDisplay.jsx';
import { runCode } from './services/api.js';

const App = () => {
  const [code, setCode] = useState('# 在这里输入Python代码\nprint("Hello, World!")');
  const [output, setOutput] = useState('');

  const handleRun = async () => {
    const result = await runCode(code);
    setOutput(result);
  };

  return (
    <div className="app-container" style={{margin: '20px'}}>
      <h1>在线编辑编译器</h1>
      <CodeEditor code={code} onChange={setCode} />
      <button onClick={handleRun} style={{marginTop: '10px'}}>运行</button>
      <OutputDisplay output={output} />
    </div>
  );
};

export default App;
