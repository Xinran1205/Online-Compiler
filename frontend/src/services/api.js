export const runCode = async ({ code, language }) => {
  const response = await fetch('/api/run', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, language })
  });
  const data = await response.json();
  return data.output;
};
