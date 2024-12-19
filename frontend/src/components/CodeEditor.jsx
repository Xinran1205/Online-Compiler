import React from 'react';

const CodeEditor = ({ code, onChange }) => {
  const handleChange = (e) => {
    onChange(e.target.value);
  };
  
  return (
    <div>
      <label htmlFor="code-editor" style={{display: 'block', marginBottom: '5px'}}>代码编辑区：</label>
      <textarea id="code-editor" value={code} onChange={handleChange}></textarea>
    </div>
  );
};

export default CodeEditor;
