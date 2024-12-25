import React from 'react';

const CodeEditor = ({ code, onChange }) => {
  const handleChange = (e) => {
    onChange(e.target.value);
  };

  return (
    <div>
      <label htmlFor="code-editor" style={{ display: 'block', marginBottom: '5px' }}>Code Editor：</label>
      <textarea
        id="code-editor"
        value={code}
        onChange={handleChange}
        style={{ width: '600px', height: '200px' }}
      />
    </div>
  );
};

export default CodeEditor;
