import React from 'react';

const OutputDisplay = ({ output }) => {
  return (
    <div className="output-container">
      <h2>运行结果：</h2>
      <pre>{output}</pre>
    </div>
  );
};

export default OutputDisplay;
