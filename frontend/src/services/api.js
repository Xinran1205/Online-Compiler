export async function runCode(code) {
    const response = await fetch('http://localhost:8080/run', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ code })
    });
  
    const data = await response.json();
    return data.output;
  }
  