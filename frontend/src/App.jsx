import React, { useState } from 'react';
import CodeEditor from './components/CodeEditor.jsx';
import OutputDisplay from './components/OutputDisplay.jsx';
import { runCode } from './services/api.js';

const App = () => {
  // 默认语言选择 Python
  const [language, setLanguage] = useState('python');
  // 默认 Python 示例代码
  const [code, setCode] = useState('# Please type Python code here\nprint("Hello, World!")');
  const [output, setOutput] = useState('');

  const handleRun = async () => {
    // 调用 runCode 时，传递 {code, language}
    const result = await runCode({ code, language });
    setOutput(result);
  };

  const handleLanguageChange = (e) => {
    const selectedLanguage = e.target.value;
    setLanguage(selectedLanguage);

    // 根据选中的语言，切换到不同示例模板
    if (selectedLanguage === 'python') {
      setCode(`# Please type Python code here
print("Hello, World!")`);
    } else if (selectedLanguage === 'java') {
      setCode(`import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Java!");
    }
}`);
    }
  };

  return (
    <div className="app-container" style={{ margin: '20px' }}>
      <h1>Online Compiler</h1>
      <div style={{ marginBottom: '10px' }}>
        <label htmlFor="language-select" style={{ marginRight: '10px' }}>
          Select Language：
        </label>
        <select id="language-select" value={language} onChange={handleLanguageChange}>
          <option value="python">Python</option>
          <option value="java">Java</option>
          {/* 如果后续想加更多语言，也可以在这里继续添加 */}
        </select>
      </div>

      <CodeEditor code={code} onChange={setCode} />
      <button onClick={handleRun} style={{ marginTop: '10px' }}>
        Run
      </button>
      <OutputDisplay output={output} />
    </div>
  );
};

export default App;
