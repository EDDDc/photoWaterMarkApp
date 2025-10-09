const { spawn } = require('child_process');
const path = require('path');

const electronBinary = require('electron');
const args = process.argv.slice(2);
const env = { ...process.env };

if (env.ELECTRON_RUN_AS_NODE) {
  delete env.ELECTRON_RUN_AS_NODE;
}

const child = spawn(electronBinary, ['.'].concat(args), {
  stdio: 'inherit',
  env,
  cwd: path.resolve(__dirname, '..'),
});

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
  } else {
    process.exit(code ?? 0);
  }
});
